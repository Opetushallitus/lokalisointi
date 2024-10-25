package fi.vm.sade.lokalisointi.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.vm.sade.lokalisointi.model.CopyLocalisations;
import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@Repository
public class S3 {
  private static final Logger LOG = LoggerFactory.getLogger(S3.class);
  public static final String LOKALISOINTI_TAG = "lokalisointi";
  private final Dokumenttipalvelu dokumenttipalvelu;

  @Value("${lokalisointi.baseurls.pallero}")
  private String baseUrlPallero;

  @Value("${lokalisointi.baseurls.untuva}")
  private String baseUrlUntuva;

  @Value("${lokalisointi.baseurls.hahtuva}")
  private String baseUrlHahtuva;

  @Value("${lokalisointi.baseurls.sade}")
  private String baseUrlSade;

  @Value("${lokalisointi.envname}")
  private String envName;

  private final RestClient.Builder restClientBuilder;

  private String virkailijaBaseUrl(final OphEnvironment env) {
    return switch (env) {
      case pallero -> baseUrlPallero;
      case untuva -> baseUrlUntuva;
      case hahtuva -> baseUrlHahtuva;
      case sade -> baseUrlSade;
    };
  }

  @Autowired
  public S3(final Dokumenttipalvelu dokumenttipalvelu) {
    this.dokumenttipalvelu = dokumenttipalvelu;
    this.restClientBuilder =
        RestClient.builder().requestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  public Collection<Localisation> find(
      final String namespace, final String locale, final String key) {
    LOG.debug(
        "Finding localisations with: namespace {}, locale {}, key {}", namespace, locale, key);
    return dokumenttipalvelu.find(List.of(LOKALISOINTI_TAG)).stream()
        .flatMap(this::transformToLocalisationStream)
        .filter(l -> namespace == null || l.getNamespace().equals(namespace))
        .filter(l -> locale == null || l.getLocale().equals(locale))
        .filter(l -> key == null || l.getKey().equals(key))
        .toList();
  }

  public Set<String> availableNamespaces(final OphEnvironment source) {
    if (source == null) {
      // if source is not given, return namespaces from this environment
      return dokumenttipalvelu.find(List.of(LOKALISOINTI_TAG)).stream()
          .map(
              metadata -> {
                final List<String> splittedObjectKey =
                    Arrays.stream(metadata.key.split("/"))
                        .filter(s -> !s.equals(String.format("t-%s", LOKALISOINTI_TAG)))
                        .toList();
                return splittedObjectKey.getFirst();
              })
          .collect(Collectors.toSet());
    }
    // otherwise call source environment's endpoint
    final String virkailijaBaseUrl = virkailijaBaseUrl(source);
    final RestClient restClient = restClientBuilder.baseUrl(virkailijaBaseUrl).build();
    final String[] availableNamespaces =
        restClient
            .get()
            .uri(
                String.format(
                    "%s/lokalisointi/api/v1/copy/available-namespaces", virkailijaBaseUrl))
            .accept(APPLICATION_JSON)
            .retrieve()
            .body(String[].class);
    if (availableNamespaces != null) {
      return Arrays.stream(availableNamespaces).collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  public void copyLocalisations(final CopyLocalisations copyRequest, final String username)
      throws IOException {
    LOG.info(
        "Copying localisations from {}, namespaces: {}, initiated by: {}",
        copyRequest.getSource(),
        copyRequest.getNamespaces(),
        username);
    if (copyRequest.getSource().equals(OphEnvironment.valueOf(envName))) {
      LOG.info("Trying to copy localisations from current environment - aborting");
      return;
    }
    final String virkailijaBaseUrl = virkailijaBaseUrl(copyRequest.getSource());
    final RestClient restClient = restClientBuilder.baseUrl(virkailijaBaseUrl).build();
    final UriBuilder urlBuilder =
        new DefaultUriBuilderFactory(
                String.format("%s/lokalisointi/api/v1/copy/localisation-files", virkailijaBaseUrl))
            .builder();
    if (copyRequest.getNamespaces() != null && !copyRequest.getNamespaces().isEmpty()) {
      urlBuilder.queryParam("namespaces", copyRequest.getNamespaces());
    }
    final byte[] body =
        restClient
            .get()
            .uri(urlBuilder.build())
            .accept(APPLICATION_OCTET_STREAM)
            .retrieve()
            .body(byte[].class);
    if (body != null) {
      byte[] buffer = new byte[1024];
      final List<ZipEntry> entries = new ArrayList<>();
      try (final ZipInputStream zipArchive = new ZipInputStream(new ByteArrayInputStream(body))) {
        ZipEntry entry;
        while ((entry = zipArchive.getNextEntry()) != null) {
          entries.add(entry);
          final String[] name = entry.getName().split("/");
          final String namespace = name[0], localeFilename = name[1];
          LOG.info("Writing localisation file {} to S3", entry.getName());
          final File tempFile = File.createTempFile("localisation-file", "json");
          final FileOutputStream fos = new FileOutputStream(tempFile);
          int len;
          while ((len = zipArchive.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
          dokumenttipalvelu
              .putObject(
                  String.format("t-%s/%s/%s", LOKALISOINTI_TAG, namespace, localeFilename),
                  localeFilename,
                  "application/json",
                  new FileInputStream(tempFile))
              .join();
          tempFile.delete();
        }
      }
      if (copyRequest.getNamespaces() == null || copyRequest.getNamespaces().isEmpty()) {
        final Set<String> newEntries =
            entries.stream()
                .map(e -> String.format("t-%s/%s", LOKALISOINTI_TAG, e.getName()))
                .collect(Collectors.toSet());
        final Set<String> keysToBeDeleted =
            new HashSet<>(
                dokumenttipalvelu.find(List.of(LOKALISOINTI_TAG)).stream()
                    .map(m -> m.key)
                    .toList());
        keysToBeDeleted.removeAll(newEntries);
        for (final String key : keysToBeDeleted) {
          LOG.info("Deleting localisation file {}", key);
          dokumenttipalvelu.delete(key);
        }
      }
    }
  }

  public StreamingResponseBody getLocalisationFilesZip(final Collection<String> namespaces) {
    final List<ObjectMetadata> withMatchingNamespaces =
        dokumenttipalvelu.find(List.of(LOKALISOINTI_TAG)).stream()
            .filter(
                metadata -> {
                  if (namespaces == null || namespaces.isEmpty()) {
                    return true;
                  }
                  final List<String> splittedObjectKey =
                      Arrays.stream(metadata.key.split("/"))
                          .filter(s -> !s.equals(String.format("t-%s", LOKALISOINTI_TAG)))
                          .toList();
                  return namespaces.contains(splittedObjectKey.getFirst());
                })
            .toList();
    return outputStream -> {
      final ZipOutputStream out = new ZipOutputStream(outputStream);
      for (final ObjectMetadata metadata : withMatchingNamespaces) {
        final ObjectEntity objectEntity = dokumenttipalvelu.get(metadata.key);
        final List<String> splittedObjectKey =
            Arrays.stream(metadata.key.split("/"))
                .filter(s -> !s.equals(String.format("t-%s", LOKALISOINTI_TAG)))
                .toList();
        final String namespace = splittedObjectKey.getFirst();
        final String filename = splittedObjectKey.getLast();
        out.putNextEntry(new ZipEntry(String.format("%s/%s", namespace, filename)));
        IOUtils.copy(objectEntity.entity, out);
        out.closeEntry();
      }
      out.finish();
    };
  }

  private Stream<Localisation> transformToLocalisationStream(final ObjectMetadata metadata) {
    try {
      final ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
      final ObjectEntity objectEntity = dokumenttipalvelu.get(metadata.key);
      final List<String> splittedObjectKey =
          Arrays.stream(metadata.key.split("/"))
              .filter(s -> !s.equals(String.format("t-%s", LOKALISOINTI_TAG)))
              .toList();

      final TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
      final Map<String, String> localisations = mapper.readValue(objectEntity.entity, typeRef);
      return localisations.keySet().stream()
          .map(
              localisationKey ->
                  new Localisation(
                      null,
                      splittedObjectKey.getFirst(),
                      localisationKey,
                      splittedObjectKey.getLast().split("\\.")[0],
                      localisations.get(localisationKey)));
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}

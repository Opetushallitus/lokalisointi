package fi.vm.sade.lokalisointi.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.vm.sade.lokalisointi.model.CopyLocalisations;
import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@Repository
public class S3 implements InitializingBean {
  private static final Logger LOG = LoggerFactory.getLogger(S3.class);
  public static final String LOKALISOINTI_TAG = "lokalisointi";
  private final ExtendedDokumenttipalvelu dokumenttipalvelu;

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

  @Value("${tolgee.slug}")
  private String tolgeeSlug;

  private final RestClient.Builder restClientBuilder;

  private String virkailijaBaseUrl(final OphEnvironment env) {
    return switch (env) {
      case pallero -> baseUrlPallero;
      case untuva -> baseUrlUntuva;
      case hahtuva -> baseUrlHahtuva;
      case sade -> baseUrlSade;
    };
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    LOG.info("tolgeeSlug: {}", tolgeeSlug);
  }

  @Autowired
  public S3(final ExtendedDokumenttipalvelu dokumenttipalvelu) {
    this.dokumenttipalvelu = dokumenttipalvelu;
    this.restClientBuilder =
        RestClient.builder().requestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  public Collection<Localisation> find(
      final String namespace, final String locale, final String key) {
    LOG.debug(
        "Finding localisations with: namespace {}, locale {}, key {}", namespace, locale, key);
    return dokumenttipalvelu.find(List.of(LOKALISOINTI_TAG)).stream()
        .filter(
            o ->
                namespace == null
                    || o.key.startsWith(
                        String.format("t-%s/%s/%s/", LOKALISOINTI_TAG, tolgeeSlug, namespace)))
        .filter(o -> locale == null || o.key.endsWith(String.format("/%s.json", locale)))
        .flatMap(this::transformToLocalisationStream)
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
                        .filter(s -> !s.equals(tolgeeSlug))
                        .toList();
                return splittedObjectKey.size() > 1 ? splittedObjectKey.getFirst() : null;
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
          final String[] pathAndName = entry.getName().split("/");
          String namespace = null, localeFilename = null;
          if (pathAndName.length == 2) {
            namespace = pathAndName[0];
            localeFilename = pathAndName[1];
          } else if (pathAndName.length == 1) {
            localeFilename = pathAndName[0];
          } else {
            throw new RuntimeException(
                """
                    Error parsing file name from localisation zip archive: %s"""
                    .formatted(entry));
          }
          LOG.info("Writing localisation file {} to S3", entry.getName());
          final File tempFile = File.createTempFile("localisation-file", "json");
          final FileOutputStream fos = new FileOutputStream(tempFile);
          int len;
          while ((len = zipArchive.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
          final String key =
              namespace != null
                  ? String.format(
                      "t-%s/%s/%s/%s", LOKALISOINTI_TAG, tolgeeSlug, namespace, localeFilename)
                  : String.format("t-%s/%s/%s", LOKALISOINTI_TAG, tolgeeSlug, localeFilename);
          dokumenttipalvelu
              .putObject(key, localeFilename, "application/json", new FileInputStream(tempFile))
              .join();
          tempFile.delete();
        }
      }
      if (copyRequest.getNamespaces() == null || copyRequest.getNamespaces().isEmpty()) {
        final Set<String> newEntries =
            entries.stream()
                .map(e -> String.format("t-%s/%s/%s", LOKALISOINTI_TAG, tolgeeSlug, e.getName()))
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
                          .filter(s -> !s.equals(tolgeeSlug))
                          .toList();
                  return namespaces.contains(
                      splittedObjectKey.size() > 1 ? splittedObjectKey.getFirst() : null);
                })
            .toList();
    return outputStream -> {
      final ZipOutputStream out = new ZipOutputStream(outputStream);
      for (final ObjectMetadata metadata : withMatchingNamespaces) {
        final ObjectEntity objectEntity = dokumenttipalvelu.get(metadata.key);
        final List<String> splittedObjectKey =
            Arrays.stream(metadata.key.split("/"))
                .filter(s -> !s.equals(String.format("t-%s", LOKALISOINTI_TAG)))
                .filter(s -> !s.equals(tolgeeSlug))
                .toList();
        final String namespace = splittedObjectKey.size() > 1 ? splittedObjectKey.getFirst() : null;
        final String filename = splittedObjectKey.getLast();
        final String entryName =
            namespace != null ? String.format("%s/%s", namespace, filename) : filename;
        out.putNextEntry(new ZipEntry(entryName));
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
              .filter(s -> !s.equals(tolgeeSlug))
              .toList();

      final TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
      try (final InputStreamReader reader = new InputStreamReader(objectEntity.entity)) {
        final Map<String, String> localisations = mapper.readValue(reader, typeRef);
        return localisations.keySet().stream()
            .map(
                localisationKey ->
                    new Localisation(
                        null,
                        splittedObjectKey.size() > 1 ? splittedObjectKey.getFirst() : null,
                        localisationKey,
                        splittedObjectKey.getLast().split("\\.")[0],
                        localisations.get(localisationKey)));
      }
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public ResponseInputStream<GetObjectResponse> getLocalisationFile(
      final String slug,
      final String namespace,
      final String locale,
      final String ifNoneMatch,
      final Instant ifModifiedSince) {
    final String key =
        namespace != null && !namespace.isEmpty()
            ? String.format("t-%s/%s/%s/%s.json", LOKALISOINTI_TAG, slug, namespace, locale)
            : String.format("t-%s/%s/%s.json", LOKALISOINTI_TAG, slug, locale);
    return dokumenttipalvelu.getObject(key, ifNoneMatch, ifModifiedSince);
  }

  public HeadObjectResponse getLocalisationFileHead(
      final String slug, final String namespace, final String locale) {
    final String key =
        namespace != null && !namespace.isEmpty()
            ? String.format("t-%s/%s/%s/%s.json", LOKALISOINTI_TAG, slug, namespace, locale)
            : String.format("t-%s/%s/%s.json", LOKALISOINTI_TAG, slug, locale);
    return dokumenttipalvelu.getHead(key);
  }
}

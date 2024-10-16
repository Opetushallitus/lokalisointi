package fi.vm.sade.lokalisointi.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.lokalisointi.model.CopyLocalisations;
import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Stream;

@Repository
public class S3 {
  private static final Logger LOG = LoggerFactory.getLogger(S3.class);
  public static final String LOKALISOINTI_TAG = "lokalisointi";
  private final Dokumenttipalvelu dokumenttipalvelu;

  @Autowired
  public S3(final Dokumenttipalvelu dokumenttipalvelu) {
    this.dokumenttipalvelu = dokumenttipalvelu;
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

  public void copyLocalisations(final CopyLocalisations body, final String username) {
    LOG.info(
        "Copying localisations from {}, namespaces: {}", body.getSource(), body.getNamespaces());
    // TODO
  }

  private Stream<Localisation> transformToLocalisationStream(final ObjectMetadata metadata) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
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

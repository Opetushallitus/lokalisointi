package fi.vm.sade.lokalisointi.storage;

import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.LocalisationOverride;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

@Repository
public class Database {
  private static final Logger LOG = LoggerFactory.getLogger(Database.class);
  private final JdbcAggregateTemplate template;

  @Autowired
  protected Database(final JdbcAggregateTemplate template) {
    this.template = template;
  }

  public Localisation saveOverride(final Localisation localisation, final String createdBy) {
    LOG.debug("Saving localisation override: {}", localisation);
    return template
        .insert(
            new LocalisationOverride(
                localisation.getNamespace(),
                localisation.getLocale(),
                localisation.getKey(),
                localisation.getValue(),
                createdBy,
                createdBy))
        .toLocalisation();
  }

  public Localisation updateOverride(
      final Integer id, final Localisation localisation, final String updatedBy) {
    final LocalisationOverride existing = template.findById(id, LocalisationOverride.class);
    LOG.debug("Updating existing localisation override with id {}: {}", id, existing);
    if (existing != null) {
      existing.setNamespace(localisation.getNamespace());
      existing.setLocale(localisation.getLocale());
      existing.setKey(localisation.getKey());
      existing.setValue(localisation.getValue());
      existing.setUpdatedBy(updatedBy);
      existing.setUpdated(LocalDateTime.now());
      return template.update(existing).toLocalisation();
    } else {
      return saveOverride(localisation, updatedBy);
    }
  }

  public Collection<Localisation> withOverrides(
      final Collection<Localisation> localisations,
      final String namespace,
      final String locale,
      final String key) {
    final Map<ImmutableTriple<String, String, String>, List<LocalisationOverride>>
        indexedOverrides =
            StreamSupport.stream(template.findAll(LocalisationOverride.class).spliterator(), false)
                .filter(l -> namespace == null || l.getNamespace().equals(namespace))
                .filter(l -> locale == null || l.getLocale().equals(locale))
                .filter(l -> key == null || l.getKey().equals(key))
                .collect(
                    groupingBy(
                        localisationOverride ->
                            uniqueKey(
                                localisationOverride.getNamespace(),
                                localisationOverride.getLocale(),
                                localisationOverride.getKey())));

    // replace localisations with overrides
    final List<Localisation> overriddenLocalisations =
        localisations.stream()
            .map(
                localisation -> {
                  final ImmutableTriple<String, String, String> uKey =
                      uniqueKey(
                          localisation.getNamespace(),
                          localisation.getLocale(),
                          localisation.getKey());
                  if (indexedOverrides.containsKey(uKey)) {
                    return indexedOverrides.get(uKey).getFirst().toLocalisation();
                  }
                  return localisation;
                })
            .toList();
    final Map<ImmutableTriple<String, String, String>, List<Localisation>> indexedLocalisations =
        overriddenLocalisations.stream()
            .collect(
                groupingBy(
                    localisation ->
                        uniqueKey(
                            localisation.getNamespace(),
                            localisation.getLocale(),
                            localisation.getKey())));

    final Set<ImmutableTriple<String, String, String>> nonOverridingLocalisationOverrides =
        indexedOverrides.keySet();
    nonOverridingLocalisationOverrides.removeAll(indexedLocalisations.keySet());

    // return also non-overriding localisation overrides
    return Stream.concat(
            overriddenLocalisations.stream(),
            nonOverridingLocalisationOverrides.stream()
                .map(k -> indexedOverrides.get(k).getFirst().toLocalisation()))
        .toList();
  }

  private ImmutableTriple<String, String, String> uniqueKey(
      final String namespace, final String locale, final String key) {
    return new ImmutableTriple<>(namespace, locale, key);
  }

  public void deleteOverride(final Integer id) {
    final LocalisationOverride override = template.findById(id, LocalisationOverride.class);
    if (override != null) {
      template.delete(override);
    }
  }

  public Collection<Localisation> getById(final Integer id) {
    final LocalisationOverride localisationOverride =
        template.findById(id, LocalisationOverride.class);
    if (localisationOverride != null) {
      return List.of(localisationOverride.toLocalisation());
    } else return List.of();
  }

  public Collection<LocalisationOverride> find() {
    return StreamSupport.stream(template.findAll(LocalisationOverride.class).spliterator(), false)
        .toList();
  }

  public Set<String> availableNamespaces() {
    return StreamSupport.stream(template.findAll(LocalisationOverride.class).spliterator(), false)
        .map(LocalisationOverride::getNamespace)
        .collect(Collectors.toSet());
  }
}

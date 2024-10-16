package fi.vm.sade.lokalisointi.storage;

import fi.vm.sade.lokalisointi.model.Localisation;
import lombok.*;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

  public Collection<Localisation> withOverrides(final Collection<Localisation> localisations) {
    final Map<ImmutableTriple<String, String, String>, List<LocalisationOverride>>
        indexedOverrides =
            StreamSupport.stream(template.findAll(LocalisationOverride.class).spliterator(), false)
                .collect(
                    groupingBy(
                        localisationOverride ->
                            uniqueKey(
                                localisationOverride.getNamespace(),
                                localisationOverride.getLocale(),
                                localisationOverride.getKey())));
    return localisations.stream()
        .map(
            localisation -> {
              final ImmutableTriple<String, String, String> key =
                  uniqueKey(
                      localisation.getNamespace(), localisation.getLocale(), localisation.getKey());
              if (indexedOverrides.containsKey(key)) {
                return indexedOverrides.get(key).getFirst().toLocalisation();
              }
              return localisation;
            })
        .toList();
  }

  private ImmutableTriple<String, String, String> uniqueKey(
      final String namespace, final String locale, final String key) {
    return new ImmutableTriple<>(namespace, locale, key);
  }

  public void deleteOverride(final Integer id) {
    template.delete(id);
  }

  public Collection<Localisation> getById(final Integer id) {
    final LocalisationOverride localisationOverride =
        template.findById(id, LocalisationOverride.class);
    if (localisationOverride != null) {
      return List.of(localisationOverride.toLocalisation());
    } else return List.of();
  }

  @Data
  @AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceCreator))
  public static class LocalisationOverride {
    private @Id @With Integer id;
    @NonNull private String namespace;
    @NonNull private String locale;

    @Column("localisation_key")
    @NonNull
    private String key;

    @Column("localisation_value")
    @NonNull
    private String value;

    @NonNull private String createdBy;
    @NonNull private LocalDateTime created;
    @NonNull private String updatedBy;
    @NonNull private LocalDateTime updated;

    public LocalisationOverride(
        @NonNull final String namespace,
        @NonNull final String locale,
        @NonNull final String key,
        @NonNull final String value,
        @NonNull final String createdBy,
        @NonNull final String updatedBy) {
      this.namespace = namespace;
      this.locale = locale;
      this.key = key;
      this.value = value;
      this.createdBy = createdBy;
      this.updatedBy = updatedBy;
      this.created = LocalDateTime.now();
      this.updated = LocalDateTime.now();
    }

    public Localisation toLocalisation() {
      return new Localisation(this.id, this.namespace, this.locale, this.key, this.value);
    }
  }
}

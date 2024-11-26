package fi.vm.sade.lokalisointi.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@PersistenceCreator))
public class LocalisationOverride {
  private @Id @With Integer id;
  private String namespace;
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

  public LocalisationOverride() {}

  public LocalisationOverride(
      final String namespace,
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
    return new Localisation(this.id, this.namespace, this.key, this.locale, this.value);
  }
}

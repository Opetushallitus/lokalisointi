package fi.vm.sade.lokalisointi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class Localisation {
  private final Integer id;
  @NotNull private final String namespace;
  @NotNull private final String key;
  @NotNull private final String locale;
  @NotNull private final String value;

  public Localisation(
      final Integer id,
      final String namespace,
      final String key,
      final String locale,
      final String value) {
    this.id = id;
    this.namespace = namespace;
    this.key = key;
    this.locale = locale;
    this.value = value;
  }

  public Integer getId() {
    return id;
  }

  public String getNamespace() {
    return namespace;
  }

  @Schema(description = "Alias for namespace, this is for backwards compatibility")
  public String getCategory() {
    return namespace;
  }

  public String getKey() {
    return key;
  }

  public String getLocale() {
    return locale;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "Localisation{"
        + "id="
        + id
        + ", namespace='"
        + namespace
        + '\''
        + ", key='"
        + key
        + '\''
        + ", locale='"
        + locale
        + '\''
        + ", value='"
        + value
        + '\''
        + '}';
  }

  public static Localisation of(
      final Integer id,
      final String namespace,
      final String key,
      final String locale,
      final String value) {
    return new Localisation(id, namespace, key, locale, value);
  }
}

package fi.vm.sade.lokalisointi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NonNull;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Localisation {
  private final Integer id;
  @NonNull protected final String namespace;
  @NonNull protected final String key;
  @NonNull protected final String locale;
  @NonNull protected final String value;

  @Schema(description = "Alias for namespace, this is for backwards compatibility")
  public String getCategory() {
    return namespace;
  }
}

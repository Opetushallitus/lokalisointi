package fi.vm.sade.lokalisointi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class Localisation {
  private Integer id;
  private String namespace;
  private String key;
  private String locale;
  private String value;

  public Localisation() {}

  @Schema(description = "Alias for namespace, this is for backwards compatibility")
  public String getCategory() {
    return namespace;
  }

  public void setCategory(final String category) {
    this.namespace = category;
  }

  @Schema(description = "This is for backwards compatibility, returns always 0")
  public Integer getAccesscount() {
    return 0;
  }
}

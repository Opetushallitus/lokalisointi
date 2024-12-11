package fi.vm.sade.lokalisointi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
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

  @Schema(description = "This is for backwards compatibility, returns always null")
  public Integer getAccesscount() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public String getAccessed() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public String getCreated() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public String getCreatedBy() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public String getModified() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public String getModifiedBy() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public String getDescription() {
    return null;
  }

  @Schema(description = "This is for backwards compatibility, returns always null")
  public Boolean getForce() {
    return null;
  }
}

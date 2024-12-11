package fi.vm.sade.lokalisointi.model;

import lombok.Data;

@Data
public class MassUpdateResult {
  private Integer notModified = 0;
  private Integer created = 0;
  private Integer updated = 0;
  private String status;

  public void incNotModified() {
    notModified++;
  }

  public void incCreated() {
    created++;
  }

  public void incUpdated() {
    updated++;
  }
}

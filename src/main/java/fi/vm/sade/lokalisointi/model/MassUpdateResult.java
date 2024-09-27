package fi.vm.sade.lokalisointi.model;

import lombok.Data;

@Data
public class MassUpdateResult {
  private Integer notModified;
  private Integer created;
  private Integer updated;
  private String status;
}

package fi.vm.sade.lokalisointi.model;

public class MassOperationResult {
  private final Integer notModified;
  private final Integer created;
  private final Integer updated;
  private final String status;

  public MassOperationResult(Integer notModified, Integer created, Integer updated, String status) {
    this.notModified = notModified;
    this.created = created;
    this.updated = updated;
    this.status = status;
  }

  public Integer getNotModified() {
    return notModified;
  }

  public Integer getCreated() {
    return created;
  }

  public Integer getUpdated() {
    return updated;
  }

  public String getStatus() {
    return status;
  }

  public static MassOperationResult of(
      final Integer notModified,
      final Integer created,
      final Integer updated,
      final String status) {
    return new MassOperationResult(notModified, created, updated, status);
  }
}

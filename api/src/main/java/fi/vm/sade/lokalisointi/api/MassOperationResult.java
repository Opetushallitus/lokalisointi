package fi.vm.sade.lokalisointi.api;

import java.io.Serializable;

public class MassOperationResult implements Serializable {
    public int notModified;
    public int created;
    public int updated;
    public String status;
}

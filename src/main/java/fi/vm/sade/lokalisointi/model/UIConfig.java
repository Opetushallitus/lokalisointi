package fi.vm.sade.lokalisointi.model;

import lombok.Data;
import lombok.NonNull;

import java.util.Collection;

@Data
public class UIConfig {
  @NonNull private final Collection<OphEnvironment> sourceEnvironments;
  @NonNull private final OphEnvironment currentEnvironment;
}

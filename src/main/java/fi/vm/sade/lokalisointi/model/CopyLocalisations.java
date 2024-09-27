package fi.vm.sade.lokalisointi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Collection;

@Data
@AllArgsConstructor
public class CopyLocalisations {
  @NonNull private final OphEnvironment source;
  private Collection<String> namespaces;
}

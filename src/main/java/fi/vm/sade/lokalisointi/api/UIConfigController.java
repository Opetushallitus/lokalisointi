package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.lokalisointi.model.UIConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/ui-config")
public class UIConfigController {
  @Value("${ENV_NAME:pallero}")
  private String envName;

  @GetMapping
  public ResponseEntity<UIConfig> uiConfig() {
    return ResponseEntity.ok(
        new UIConfig(
            Arrays.stream(OphEnvironment.values()).filter(e -> !e.name().equals(envName)).toList(),
            OphEnvironment.valueOf(envName)));
  }
}

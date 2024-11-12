package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.lokalisointi.model.UIConfig;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_LOKALISOINTI;

@Tag(name = "ui-config", description = "Query user interface configuration")
@RestController
@RequestMapping("/api/v1/ui-config")
public class UIConfigController extends ControllerBase {
  @Value("${lokalisointi.envname}")
  private String envName;

  @Value("${host.virkailija}")
  private String hostVirkailija;

  @GetMapping
  @Secured({ROLE_LOKALISOINTI})
  public ResponseEntity<UIConfig> uiConfig() {
    return ResponseEntity.ok(
        new UIConfig(
            Arrays.stream(OphEnvironment.values()).filter(e -> !e.name().equals(envName)).toList(),
            OphEnvironment.valueOf(envName),
            String.format("https://%s", hostVirkailija)));
  }
}

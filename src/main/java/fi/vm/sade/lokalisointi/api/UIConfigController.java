package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.lokalisointi.model.UIConfig;
import fi.vm.sade.lokalisointi.storage.Database;
import fi.vm.sade.lokalisointi.storage.S3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_LOKALISOINTI;

@RestController
@RequestMapping("/api/v1/ui-config")
public class UIConfigController extends ControllerBase {
  private final S3 s3;
  private final Database database;

  @Value("${ENV_NAME:pallero}")
  private String envName;

  @Autowired
  public UIConfigController(final S3 s3, final Database database) {
    this.s3 = s3;
    this.database = database;
  }

  @GetMapping
  @Secured({ROLE_LOKALISOINTI})
  public ResponseEntity<UIConfig> uiConfig() {
    return ResponseEntity.ok(
        new UIConfig(
            Arrays.stream(OphEnvironment.values()).filter(e -> !e.name().equals(envName)).toList(),
            OphEnvironment.valueOf(envName)));
  }
}

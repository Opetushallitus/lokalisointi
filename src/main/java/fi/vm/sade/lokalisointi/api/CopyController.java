package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.CopyLocalisations;
import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.lokalisointi.model.Status;
import fi.vm.sade.lokalisointi.storage.S3;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_CRUD;
import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_UPDATE;

@RestController
@RequestMapping("/api/v1/copy")
public class CopyController extends ControllerBase {
  private final S3 s3;

  @Autowired
  public CopyController(final S3 s3) {
    this.s3 = s3;
  }

  @Operation(summary = "Copy localisations from source environment to this environment")
  @PostMapping
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Status> copyLocalisations(
      @RequestBody final CopyLocalisations body, final Principal user) {
    s3.copyLocalisations(body, user.getName());
    return ResponseEntity.badRequest().body(new Status("Not implemented"));
  }

  @Operation(summary = "Find available namespaces for given source environment")
  @GetMapping("/available-namespaces")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<List<String>> availableNamespaces(
      @RequestParam("source") final OphEnvironment source) {
    return ResponseEntity.ok(s3.availableNamespaces(source));
  }
}

package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.CopyLocalisations;
import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.lokalisointi.model.Status;
import fi.vm.sade.lokalisointi.storage.S3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;

import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_CRUD;
import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_UPDATE;

@Tag(name = "copy", description = "Copy localisations between environments")
@RestController
@RequestMapping("/api/v1/copy")
public class CopyController extends ControllerBase {
  private static final Logger LOG = LoggerFactory.getLogger(CopyController.class);
  private final S3 s3;

  @Autowired
  public CopyController(final S3 s3) {
    this.s3 = s3;
  }

  @Operation(summary = "Copy localisations from source environment to this environment")
  @PostMapping
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Status> copyLocalisations(
      @RequestBody final CopyLocalisations copyRequest, final Principal user) throws IOException {
    try {
      s3.copyLocalisations(copyRequest, user.getName());
    } catch (final HttpStatusCodeException e) {
      LOG.warn(
          """
          Copying localisations failed: %s"""
              .formatted(copyRequest),
          e);
      return ResponseEntity.internalServerError().body(new Status("Failed"));
    }
    return ResponseEntity.ok().body(new Status("OK"));
  }

  @Operation(summary = "Find available namespaces for given source environment")
  @GetMapping("/available-namespaces")
  public ResponseEntity<Collection<String>> availableNamespaces(
      @RequestParam(value = "source", required = false) final OphEnvironment source) {
    return ResponseEntity.ok(s3.availableNamespaces(source));
  }

  @Operation(
      summary =
          "Produces a zip of localisation files from this environment, to be copied to another environment")
  @GetMapping(value = "/localisation-files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<StreamingResponseBody> localisationFiles(
      @RequestParam(value = "namespaces", required = false) final Collection<String> namespaces) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=localisations.zip")
        .body(s3.getLocalisationFilesZip(namespaces));
  }
}

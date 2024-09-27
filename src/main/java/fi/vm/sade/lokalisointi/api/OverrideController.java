package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.LocalisationOverride;
import fi.vm.sade.lokalisointi.model.OphEnvironment;
import fi.vm.sade.lokalisointi.model.Status;
import fi.vm.sade.lokalisointi.storage.Database;
import fi.vm.sade.lokalisointi.storage.S3;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.lokalisointi.api.LocalisationController.*;

@RestController
@RequestMapping("/api/v1/override")
public class OverrideController extends ControllerBase {
  private static final Logger LOG = LoggerFactory.getLogger(OverrideController.class);
  private final S3 s3;
  private final Database database;

  @Autowired
  public OverrideController(final S3 s3, final Database database) {
    this.s3 = s3;
    this.database = database;
  }

  @Operation(summary = "Get all localisation overrides")
  @GetMapping
  @Secured({ROLE_LOKALISOINTI})
  public ResponseEntity<Collection<LocalisationOverride>> find() {
    return ResponseEntity.ok(database.find());
  }

  @Operation(summary = "Create localisation override")
  @PostMapping
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Localisation> create(
      @Valid @RequestBody final Localisation localisation, final Principal user) {
    LOG.info("Creating localisation override: {}", localisation);
    return ResponseEntity.ok(database.saveOverride(localisation, user.getName()));
  }

  @Operation(summary = "Update localisation override")
  @PostMapping("/{id}")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Localisation> update(
      @PathVariable final Integer id,
      @RequestBody final Localisation localisation,
      final Principal user) {
    LOG.info("Updating localisation override: {}", localisation);
    return ResponseEntity.ok(database.updateOverride(id, localisation, user.getName()));
  }

  @Operation(summary = "Delete localisation override")
  @DeleteMapping("/{id}")
  @Secured({ROLE_CRUD})
  public ResponseEntity<Status> delete(@PathVariable final Integer id) {
    database.deleteOverride(id);
    return ResponseEntity.ok(new Status("OK"));
  }

  @Operation(
      summary =
          "Find available namespaces in this environment, includes also namespaces used in overrides")
  @GetMapping("/available-namespaces")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Collection<String>> availableNamespaces() {
    final Set<String> s3Namespaces = s3.availableNamespaces(null);
    final Set<String> overrideNamespaces = database.availableNamespaces();
    return ResponseEntity.ok(
        Stream.concat(s3Namespaces.stream(), overrideNamespaces.stream())
            .collect(Collectors.toSet()));
  }
}

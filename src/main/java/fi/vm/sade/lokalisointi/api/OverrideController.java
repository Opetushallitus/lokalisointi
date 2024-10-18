package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.LocalisationOverride;
import fi.vm.sade.lokalisointi.model.Status;
import fi.vm.sade.lokalisointi.storage.Database;
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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_CRUD;
import static fi.vm.sade.lokalisointi.api.LocalisationController.ROLE_UPDATE;

@RestController
@RequestMapping("/api/v1/override")
public class OverrideController extends ControllerBase {
  private static final Logger LOG = LoggerFactory.getLogger(OverrideController.class);
  private final Database database;

  @Autowired
  public OverrideController(Database database) {
    this.database = database;
  }

  @Operation(summary = "Get all localisation overrides")
  @GetMapping
  @Secured({ROLE_UPDATE, ROLE_CRUD})
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
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Status> delete(@PathVariable final Integer id) {
    database.deleteOverride(id);
    return ResponseEntity.ok(new Status("OK"));
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
    DbActionExecutionException.class,
    IllegalArgumentException.class,
    HttpMessageNotReadableException.class
  })
  public Map<String, ?> handleUserErrors(final RuntimeException ex) {
    return Map.of(
        "error",
        Stream.of(
                Optional.of(new ImmutablePair<>("message", ex.getMessage())),
                Optional.ofNullable(ex.getCause())
                    .map(c -> new ImmutablePair<>("cause", c.getMessage())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight)));
  }
}

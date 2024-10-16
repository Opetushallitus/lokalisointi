package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.*;
import fi.vm.sade.lokalisointi.storage.Database;
import fi.vm.sade.lokalisointi.storage.S3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping({"/api/v1/localisation", "/cxf/rest/v1/localisation"})
public class LokalisointiController implements InitializingBean {
  private static final String ROLE_LOKALISOINTI = "ROLE_APP_LOKALISOINTI";
  private static final String ROLE_READ = "ROLE_APP_LOKALISOINTI_READ";
  private static final String ROLE_UPDATE = "ROLE_APP_LOKALISOINTI_READ_UPDATE";
  private static final String ROLE_CRUD = "ROLE_APP_LOKALISOINTI_CRUD";

  private static final Logger LOG = LoggerFactory.getLogger(LokalisointiController.class);

  @Value("${lokalisointi.public-cache-max-age-minutes:5}")
  private Integer cacheMaxAgeMinutes;

  private final S3 s3;
  private final Database database;

  @Autowired
  public LokalisointiController(final S3 s3, final Database database) {
    this.s3 = s3;
    this.database = database;
  }

  public void afterPropertiesSet() {
    LOG.info("cacheMaxAgeMinutes: {}", cacheMaxAgeMinutes);
  }

  @Operation(summary = "Query localisations")
  @GetMapping
  public ResponseEntity<Collection<Localisation>> query(
      @Parameter(description = "Id of (overridden) localisation") @RequestParam(required = false)
          final Integer id,
      @Parameter(description = "Alias for namespace, this field is for backwards compatibility")
          @RequestParam(required = false)
          final String category,
      @Parameter(description = "Namespace of a localisation, such as 'koodisto' or 'ehoks'")
          @RequestParam(required = false)
          final String namespace,
      @Parameter(description = "Key of a localisation, such as 'create-item'")
          @RequestParam(required = false)
          final String key,
      @Parameter(description = "Language code for a localisation, 'fi', 'sv' or 'en'")
          @RequestParam(required = false)
          final String locale,
      @Parameter(
              description =
                  "If this param contains value 'false' cache-control header with 'no-cache' will be sent in response, otherwise cache-control will set as 'max-age=n, public'")
          @RequestParam(required = false, defaultValue = "true")
          final Boolean cache) {
    if (category != null && namespace != null && !category.equals(namespace)) {
      throw new IllegalArgumentException(
          "category and namespace are both defined and but do not match");
    }
    LOG.debug(
        "Querying for localisations: {}, {}, {}, {}, {}, {}",
        id,
        category,
        namespace,
        key,
        locale,
        cache);
    final Collection<Localisation> localisations =
        id != null
            ? database.getById(id)
            : database.withOverrides(
                s3.find(namespace != null ? namespace : category, locale, key));
    return ResponseEntity.ok()
        .cacheControl(
            Boolean.FALSE.equals(cache)
                ? CacheControl.noCache()
                : CacheControl.maxAge(Duration.of(cacheMaxAgeMinutes, ChronoUnit.MINUTES))
                    .cachePublic())
        .body(localisations);
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

  @Operation(summary = "Copy localisations from source environment to this environment")
  @PostMapping("/copy")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Status> copyLocalisations(
      @RequestBody final CopyLocalisations body, final Principal user) {
    s3.copyLocalisations(body, user.getName());
    return ResponseEntity.badRequest().body(new Status("Not implemented"));
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

package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.BadRequestException;
import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.MassOperationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LokalisointiController implements InitializingBean {
  // private static final String ROLE_LOKALISOINTI = "ROLE_APP_LOKALISOINTI";
  // private static final String ROLE_READ = "ROLE_APP_LOKALISOINTI_READ";
  private static final String ROLE_UPDATE = "ROLE_APP_LOKALISOINTI_READ_UPDATE";
  private static final String ROLE_CRUD = "ROLE_APP_LOKALISOINTI_CRUD";

  private static final Logger LOG = LoggerFactory.getLogger(LokalisointiController.class);

  @Value("${lokalisointi.public-cache-max-age-minutes:5}")
  private Integer cacheMaxAgeMinutes;

  public void afterPropertiesSet() {
    LOG.info("cacheMaxAgeMinutes: {}", cacheMaxAgeMinutes);
  }

  @Operation(summary = "Query localisations")
  @GetMapping("/api/v1/localisation")
  public ResponseEntity<Collection<Localisation>> query(
      @Parameter(description = "Id of an overridden localisation") @RequestParam(required = false)
          Integer id,
      @Parameter(description = "Alias for namespace, this is for backwards compatibility")
          @RequestParam(required = false)
          String category,
      @Parameter(description = "Namespace of a localisation, such as 'koodisto' or 'ehoks'")
          @RequestParam(required = false)
          String namespace,
      @Parameter(description = "Key of a localisation, such as 'create-item'")
          @RequestParam(required = false)
          String key,
      @Parameter(description = "Language code for a localisation, 'fi', 'sv' or 'en'")
          @RequestParam(required = false)
          String locale,
      @Parameter(
              description =
                  "If this param contains value 'cached' cache-control header will be sent in response")
          @RequestParam(required = false)
          String value) {
    if (category != null && namespace != null && !category.equals(namespace)) {
      throw new BadRequestException("category and namespace are both defined and but do not match");
    }
    LOG.debug(
        "Querying for localisations: {}, {}, {}, {}, {}, {}",
        id,
        category,
        namespace,
        key,
        locale,
        value);
    // TODO
    return ResponseEntity.ok()
        .cacheControl(
            "cached".equalsIgnoreCase(value)
                ? CacheControl.maxAge(Duration.of(cacheMaxAgeMinutes, ChronoUnit.MINUTES))
                    .cachePublic()
                : CacheControl.noCache())
        .body(List.of(Localisation.of(1, "ehoks", "create-hoks", "fi", "Luo HOKS")));
  }

  @Operation(summary = "Create localisation")
  @PostMapping("/api/v1/localisation")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<Localisation> create(@Valid @RequestBody Localisation localisation) {
    LOG.info("Creating localisation: {}", localisation);
    // TODO
    return ResponseEntity.ok(localisation);
  }

  @Operation(summary = "Mass update localisations")
  @PostMapping("/api/v1/localisation/update")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<MassOperationResult> update(
      @RequestBody Collection<Localisation> localisations) {
    LOG.info("Mass updating {} localisations", localisations.size());
    // TODO
    return ResponseEntity.ok(MassOperationResult.of(0, 0, 0, "ok"));
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
    final Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return errors;
  }
}

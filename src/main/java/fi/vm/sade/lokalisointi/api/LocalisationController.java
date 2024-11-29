package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.model.*;
import fi.vm.sade.lokalisointi.storage.Database;
import fi.vm.sade.lokalisointi.storage.S3;
import fi.vm.sade.lokalisointi.storage.Tolgee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

@Tag(name = "localisation", description = "Query localisations")
@RestController
@RequestMapping({"/api/v1/localisation", "/cxf/rest/v1/localisation"})
public class LocalisationController extends ControllerBase implements InitializingBean {
  public static final String ROLE_LOKALISOINTI = "ROLE_APP_LOKALISOINTI";
  public static final String ROLE_READ = "ROLE_APP_LOKALISOINTI_READ";
  public static final String ROLE_UPDATE = "ROLE_APP_LOKALISOINTI_READ_UPDATE";
  public static final String ROLE_CRUD = "ROLE_APP_LOKALISOINTI_CRUD";

  private static final Logger LOG = LoggerFactory.getLogger(LocalisationController.class);

  @Value("${lokalisointi.public-cache-max-age-minutes:5}")
  private Integer cacheMaxAgeMinutes;

  @Value("${lokalisointi.envname}")
  private String envName;

  private final S3 s3;
  private final Database database;
  private final Tolgee tolgee;

  @Autowired
  public LocalisationController(final S3 s3, final Database database, final Tolgee tolgee) {
    this.s3 = s3;
    this.database = database;
    this.tolgee = tolgee;
  }

  public void afterPropertiesSet() {
    LOG.info("cacheMaxAgeMinutes: {}", cacheMaxAgeMinutes);
    LOG.info("envName: {}", envName);
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
    final Collection<Localisation> localisationsFromS3 =
        s3.find(namespace != null ? namespace : category, locale, key);
    final Collection<Localisation> localisations =
        id != null
            ? database.getById(id)
            : database.withOverrides(
                localisationsFromS3, namespace != null ? namespace : category, locale, key);
    return ResponseEntity.ok()
        .cacheControl(
            Boolean.FALSE.equals(cache)
                ? CacheControl.noCache()
                : CacheControl.maxAge(Duration.of(cacheMaxAgeMinutes, ChronoUnit.MINUTES))
                    .cachePublic())
        .body(localisations);
  }

  @Operation(
      summary = "Create localisations",
      description =
          "Creates new localisations. In test environments tries to import new localisation keys to Tolgee, in production environment discards updates and returns bad request.")
  @PostMapping("/update")
  @Secured({ROLE_UPDATE, ROLE_CRUD})
  public ResponseEntity<MassUpdateResult> update(
      @RequestBody final Collection<Localisation> localisations) {
    final MassUpdateResult result = new MassUpdateResult();
    result.setStatus("OK");
    if (envName != null && OphEnvironment.valueOf(envName).equals(OphEnvironment.sade)) {
      result.setStatus("Failure");
      result.setNotModified(localisations.size());
      return ResponseEntity.badRequest().body(result);
    }
    for (final Localisation localisation : localisations) {
      if (localisation.getId() == null && tolgee.importKey(localisation)) {
        result.incCreated();
      } else {
        LOG.info("Bypassed localisation update: {}", localisation);
        result.incNotModified();
      }
    }
    return ResponseEntity.ok().body(result);
  }
}

package fi.vm.sade.lokalisointi.api

import fi.vm.sade.lokalisointi.model.Localisation
import fi.vm.sade.lokalisointi.model.MassUpdateResult
import fi.vm.sade.lokalisointi.model.OphEnvironment
import fi.vm.sade.lokalisointi.storage.Database
import fi.vm.sade.lokalisointi.storage.S3
import fi.vm.sade.lokalisointi.storage.Tolgee
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.time.Duration
import java.time.temporal.ChronoUnit

@Tag(name = "localisation", description = "Query localisations")
@RestController
@RequestMapping("/api/v1/localisation", "/cxf/rest/v1/localisation")
class LocalisationController @Autowired constructor(
    private val s3: S3,
    private val database: Database,
    private val tolgee: Tolgee
) :
    ControllerBase(), InitializingBean {
    @Value("\${lokalisointi.public-cache-max-age-minutes:5}")
    private val cacheMaxAgeMinutes: Int? = null

    @Value("\${lokalisointi.envname}")
    private val envName: String? = null

    override fun afterPropertiesSet() {
        LOG.info("cacheMaxAgeMinutes: {}", cacheMaxAgeMinutes)
        LOG.info("envName: {}", envName)
    }

    @Operation(summary = "Query localisations")
    @GetMapping
    fun query(
        @Parameter(description = "Id of (overridden) localisation") @RequestParam(required = false) id: Int?,
        @Parameter(description = "Alias for namespace, this field is for backwards compatibility") @RequestParam(
            required = false
        ) category: String?,
        @Parameter(description = "Namespace of a localisation, such as 'koodisto' or 'ehoks'") @RequestParam(required = false) namespace: String?,
        @Parameter(description = "Key of a localisation, such as 'create-item'") @RequestParam(required = false) key: String?,
        @Parameter(description = "Language code for a localisation, 'fi', 'sv' or 'en'") @RequestParam(required = false) locale: String?,
        @Parameter(description = "If this param contains value 'false' cache-control header with 'no-cache' will be sent in response, otherwise cache-control will set as 'max-age=n, public'") @RequestParam(
            required = false,
            defaultValue = "true"
        ) cache: Boolean
    ): ResponseEntity<Collection<Localisation>> {
        require(!(category != null && namespace != null && category != namespace)) { "category and namespace are both defined and but do not match" }
        LOG.debug(
            "Querying for localisations: {}, {}, {}, {}, {}, {}",
            id,
            category,
            namespace,
            key,
            locale,
            cache
        )
        val localisationsFromS3 =
            s3.find(namespace ?: category, locale, key)
        val localisations =
            if (id != null)
                database.getById(id)
            else
                database.withOverrides(
                    localisationsFromS3, namespace ?: category, locale, key
                )
        return ResponseEntity.ok()
            .cacheControl(
                if (java.lang.Boolean.FALSE == cache)
                    CacheControl.noCache()
                else
                    CacheControl.maxAge(Duration.of(cacheMaxAgeMinutes!!.toLong(), ChronoUnit.MINUTES))
                        .cachePublic()
            )
            .body(localisations)
    }

    @Operation(
        summary = "Create/update localisations",
        description = "Creates or updates localisations. In test environment tries to import new localisation keys to Tolgee, in another environments tries to create/update localisation overrides."
    )
    @PostMapping("/update")
    @Secured(
        ROLE_UPDATE, ROLE_CRUD
    )
    fun update(
        @RequestBody localisations: Collection<Localisation>, user: Principal
    ): ResponseEntity<MassUpdateResult> {
        val result = MassUpdateResult()
        result.status = "OK"
        for (localisation in localisations) {
            if (localisation.id != null) {
                // id should always refer to (existing) localisation override
                val existing =
                    database.getById(localisation.id).firstOrNull()
                if (existing != null) {
                    if (existing.namespace == localisation.namespace
                        && existing.key == localisation.key
                        && existing.locale == localisation.locale
                        && existing.value == localisation.value
                    ) {
                        LOG.info("Localisation not changed - not updating: {}", localisation)
                        result.incNotModified()
                    } else {
                        LOG.info("Updating localisation: {}", localisation)
                        database.updateOverride(localisation.id, localisation, user.name)
                        result.incUpdated()
                    }
                } else {
                    LOG.info("Existing not found, creating new localisation override: {}", localisation)
                    database.saveOverride(localisation, user.name)
                    result.incCreated()
                }
            } else if (envName != null
                && OphEnvironment.valueOf(envName) == OphEnvironment.pallero
            ) {
                // in test environment save localisation to Tolgee
                if (tolgee.importKey(localisation)) {
                    LOG.info("Imported localisation to Tolgee: {}", localisation)
                    result.incCreated()
                } else {
                    LOG.info("Bypassed localisation import to Tolgee: {}", localisation)
                    result.incNotModified()
                }
            } else {
                // otherwise create new localisation override
                LOG.info("Creating new localisation override: {}", localisation)
                database.saveOverride(localisation, user.name)
                result.incCreated()
            }
        }
        return ResponseEntity.ok().body(result)
    }

    companion object {
        const val ROLE_LOKALISOINTI: String = "ROLE_APP_LOKALISOINTI"
        const val ROLE_READ: String = "ROLE_APP_LOKALISOINTI_READ"
        const val ROLE_UPDATE: String = "ROLE_APP_LOKALISOINTI_READ_UPDATE"
        const val ROLE_CRUD: String = "ROLE_APP_LOKALISOINTI_CRUD"

        private val LOG: Logger = LoggerFactory.getLogger(LocalisationController::class.java)
    }
}

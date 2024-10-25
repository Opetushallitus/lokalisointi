package fi.vm.sade.lokalisointi.api

import fi.vm.sade.lokalisointi.model.Localisation
import fi.vm.sade.lokalisointi.model.LocalisationOverride
import fi.vm.sade.lokalisointi.model.Status
import fi.vm.sade.lokalisointi.storage.Database
import fi.vm.sade.lokalisointi.storage.S3
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import java.security.Principal

@Tag(name = "override", description = "Manage localisation overrides")
@RestController
@RequestMapping("/api/v1/override")
class OverrideController @Autowired constructor(private val s3: S3, private val database: Database) :
    ControllerBase() {
    @Operation(summary = "Get all localisation overrides")
    @GetMapping
    @Secured(LocalisationController.ROLE_LOKALISOINTI)
    fun find(): ResponseEntity<Collection<LocalisationOverride>> {
        return ResponseEntity.ok(database.find())
    }

    @Operation(summary = "Create localisation override")
    @PostMapping
    @Secured(LocalisationController.ROLE_UPDATE, LocalisationController.ROLE_CRUD)
    fun create(
        @Valid @RequestBody localisation: Localisation, user: Principal
    ): ResponseEntity<LocalisationOverride> {
        LOG.info("Creating localisation override: {}", localisation)
        return ResponseEntity.ok(database.saveOverride(localisation, user.name))
    }

    @Operation(summary = "Update localisation override")
    @PostMapping("/{id}")
    @Secured(LocalisationController.ROLE_UPDATE, LocalisationController.ROLE_CRUD)
    fun update(
        @PathVariable id: Int,
        @RequestBody localisation: Localisation,
        user: Principal
    ): ResponseEntity<LocalisationOverride> {
        LOG.info("Updating localisation override: {}", localisation)
        return ResponseEntity.ok(database.updateOverride(id, localisation, user.name))
    }

    @Operation(summary = "Delete localisation override")
    @DeleteMapping("/{id}")
    @Secured(LocalisationController.ROLE_CRUD)
    fun delete(@PathVariable id: Int): ResponseEntity<Status> {
        database.deleteOverride(id)
        return ResponseEntity.ok(Status("OK"))
    }

    @Operation(summary = "Find available namespaces in this environment, includes also namespaces used in overrides")
    @GetMapping("/available-namespaces")
    @Secured(LocalisationController.ROLE_UPDATE, LocalisationController.ROLE_CRUD)
    fun availableNamespaces(): ResponseEntity<Collection<String>> {
        val s3Namespaces: Set<String> = s3.availableNamespaces(null)
        val overrideNamespaces: Set<String> = database.availableNamespaces()
        return ResponseEntity.ok(
            s3Namespaces + overrideNamespaces
        )
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(OverrideController::class.java)
    }
}

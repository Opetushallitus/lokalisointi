package fi.vm.sade.lokalisointi.api

import fi.vm.sade.lokalisointi.model.CopyLocalisations
import fi.vm.sade.lokalisointi.model.OphEnvironment
import fi.vm.sade.lokalisointi.model.Status
import fi.vm.sade.lokalisointi.storage.S3
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.security.Principal

@Tag(name = "copy", description = "Copy localisations between environments")
@RestController
@RequestMapping("/api/v1/copy")
class CopyController @Autowired constructor(private val s3: S3) : ControllerBase() {
    @Operation(summary = "Copy localisations from source environment to this environment")
    @PostMapping
    @Secured(LocalisationController.ROLE_UPDATE, LocalisationController.ROLE_CRUD)
    fun copyLocalisations(
        @RequestBody copyRequest: CopyLocalisations, user: Principal
    ): ResponseEntity<Status> {
        s3.copyLocalisations(copyRequest, user.name)
        return ResponseEntity.ok().body(Status("OK"))
    }

    @Operation(summary = "Find available namespaces for given source environment")
    @GetMapping("/available-namespaces")
    fun availableNamespaces(
        @RequestParam(value = "source", required = false) source: OphEnvironment?
    ): ResponseEntity<Collection<String>> {
        return ResponseEntity.ok(s3.availableNamespaces(source))
    }

    @Operation(summary = "Produces a zip of localisation files from this environment, to be copied to another environment")
    @GetMapping(value = ["/localisation-files"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun localisationFiles(
        @RequestParam(value = "namespaces", required = false) namespaces: Collection<String?>?
    ): ResponseEntity<StreamingResponseBody> {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=localisations.zip")
            .body(s3.getLocalisationFilesZip(namespaces))
    }
}

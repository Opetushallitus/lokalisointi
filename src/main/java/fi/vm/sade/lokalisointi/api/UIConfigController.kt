package fi.vm.sade.lokalisointi.api

import fi.vm.sade.lokalisointi.model.OphEnvironment
import fi.vm.sade.lokalisointi.model.UIConfig
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "ui-config", description = "Query user interface configuration")
@RestController
@RequestMapping("/api/v1/ui-config")
class UIConfigController : ControllerBase() {
    @Value("\${lokalisointi.envname}")
    private val envName: String? = null

    @GetMapping
    @Secured(LocalisationController.ROLE_LOKALISOINTI)
    fun uiConfig(): ResponseEntity<UIConfig> {
        return ResponseEntity.ok(
            UIConfig(
                OphEnvironment.entries.filter { e: OphEnvironment -> e.name != envName },
                OphEnvironment.valueOf(envName!!)
            )
        )
    }
}

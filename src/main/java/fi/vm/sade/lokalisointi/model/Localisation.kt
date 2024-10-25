package fi.vm.sade.lokalisointi.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Localisation(
    val id: Int? = null,
    val namespace: String,
    val key: String,
    val locale: String,
    val value: String
) {
    @Schema(description = "Alias for namespace, this is for backwards compatibility")
    fun getCategory(): String {
        return namespace
    }
}

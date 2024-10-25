package fi.vm.sade.lokalisointi.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime

data class LocalisationOverride(
    @Id
    var id: Int? = null,
    var namespace: String,
    var locale: String,
    @Column("localisation_key")
    var key: String,
    @Column("localisation_value")
    var value: String,
    var createdBy: String,
    var created: LocalDateTime,
    var updatedBy: String,
    var updated: LocalDateTime
) {
    fun toLocalisation(): Localisation {
        return Localisation(this.id, this.namespace, this.key, this.locale, this.value)
    }
}

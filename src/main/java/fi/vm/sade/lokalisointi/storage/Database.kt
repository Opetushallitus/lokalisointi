package fi.vm.sade.lokalisointi.storage

import fi.vm.sade.lokalisointi.model.Localisation
import fi.vm.sade.lokalisointi.model.LocalisationOverride
import org.apache.commons.lang3.tuple.ImmutableTriple
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class Database @Autowired constructor(val template: JdbcAggregateTemplate) {
    fun saveOverride(
        localisation: Localisation, createdBy: String
    ): LocalisationOverride {
        LOG.debug("Saving localisation override: {}", localisation)
        return template.insert(
            LocalisationOverride(
                null,
                localisation.namespace,
                localisation.locale,
                localisation.key,
                localisation.value,
                createdBy,
                LocalDateTime.now(),
                createdBy,
                LocalDateTime.now()
            )
        )
    }

    fun updateOverride(
        id: Int, localisation: Localisation, updatedBy: String
    ): LocalisationOverride {
        val existing: LocalisationOverride? = template.findById(id, LocalisationOverride::class.java)
        LOG.debug("Updating existing localisation override with id {}: {}", id, existing)
        if (existing != null) {
            existing.namespace = localisation.namespace
            existing.locale = localisation.locale
            existing.key = localisation.key
            existing.value = localisation.value
            existing.updatedBy = updatedBy
            existing.updated = LocalDateTime.now()
            return template.update(existing)
        } else {
            return saveOverride(localisation, updatedBy)
        }
    }

    fun withOverrides(
        localisations: Collection<Localisation>,
        namespace: String?,
        locale: String?,
        key: String?
    ): Collection<Localisation> {
        val indexedOverrides: Map<ImmutableTriple<String, String, String>, List<LocalisationOverride>> =
            template.findAll(LocalisationOverride::class.java)
                .filter { l: LocalisationOverride -> namespace == null || l.namespace == namespace }
                .filter { l: LocalisationOverride -> locale == null || l.locale == locale }
                .filter { l: LocalisationOverride -> key == null || l.key == key }
                .groupBy { localisationOverride: LocalisationOverride ->
                    uniqueKey(
                        localisationOverride.namespace,
                        localisationOverride.key,
                        localisationOverride.locale
                    )
                }

        // replace localisations with overrides
        val overriddenLocalisations: List<Localisation> =
            localisations
                .map { localisation: Localisation ->
                    val uKey =
                        uniqueKey(
                            localisation.namespace,
                            localisation.key,
                            localisation.locale
                        )
                    if (indexedOverrides.containsKey(uKey)) {
                        indexedOverrides[uKey]!!.first().toLocalisation()
                    } else {
                        localisation
                    }
                }

        val overriddenLocalisationKeys: Set<ImmutableTriple<String, String, String>> = overriddenLocalisations
            .map { localisation: Localisation ->
                uniqueKey(
                    localisation.namespace,
                    localisation.key,
                    localisation.locale
                )
            }.toSet()

        // return also non-overriding localisation overrides
        return overriddenLocalisations + (indexedOverrides.keys - overriddenLocalisationKeys).map { k: ImmutableTriple<String, String, String> ->
            indexedOverrides[k]!!.first().toLocalisation()
        }

    }

    private fun uniqueKey(
        namespace: String, key: String, locale: String
    ): ImmutableTriple<String, String, String> {
        return ImmutableTriple(namespace, key, locale)
    }

    fun deleteOverride(id: Int) {
        val override: LocalisationOverride? = template.findById(id, LocalisationOverride::class.java)
        if (override != null) {
            template.delete(override)
        }
    }

    fun getById(id: Int): Collection<Localisation> {
        val localisationOverride: LocalisationOverride? =
            template.findById(id, LocalisationOverride::class.java)
        return if (localisationOverride != null) {
            listOf(localisationOverride.toLocalisation())
        } else listOf()
    }

    fun find(): Collection<LocalisationOverride> {
        return template.findAll(LocalisationOverride::class.java).toList()
    }

    fun availableNamespaces(): Set<String> {
        return template.findAll(LocalisationOverride::class.java)
            .map { override -> override.namespace }
            .toSet()
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Database::class.java)
    }
}

package fi.vm.sade.lokalisointi.storage

import fi.vm.sade.lokalisointi.model.Localisation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Repository
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@Repository
class Tolgee {
    @Value("\${tolgee.baseurl}")
    private val baseUrl: String? = null

    @Value("\${tolgee.apikey:none}")
    private val apiKey: String? = null

    @Value("\${tolgee.projectid}")
    private val projectId: String? = null

    private val restClientBuilder: RestClient.Builder =
        RestClient.builder().requestFactory(HttpComponentsClientHttpRequestFactory())

    fun importKey(localisation: Localisation): Boolean {
        val restClient = restClientBuilder.baseUrl(baseUrl!!).build()
        try {
            restClient
                .post()
                .uri(String.format("/v2/projects/%s/keys/import-resolvable", projectId!!))
                .body(
                    mapOf(
                        Pair(
                            "keys",
                            listOf(
                                mapOf(
                                    Pair(
                                        "name",
                                        localisation.key
                                    ),
                                    Pair(
                                        "namespace",
                                        localisation.namespace
                                    ),
                                    Pair(
                                        "translations",
                                        mapOf(
                                            Pair(
                                                localisation.locale,
                                                mapOf(Pair("text", localisation.value), Pair("resolution", "NEW"))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(API_KEY, apiKey!!)
                .retrieve()
                .toEntity(String::class.java)
            return true
        } catch (e: HttpClientErrorException) {
            LOG.warn(
                "Localisation {} was not imported to Tolgee: ({}) {}",
                localisation,
                e.statusCode,
                e.responseBodyAsString
            )
            return false
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Tolgee::class.java)
        private const val API_KEY = "X-API-Key"
    }
}

package fi.vm.sade.lokalisointi.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import fi.vm.sade.lokalisointi.model.CopyLocalisations
import fi.vm.sade.lokalisointi.model.Localisation
import fi.vm.sade.lokalisointi.model.OphEnvironment
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestClient
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.util.DefaultUriBuilderFactory
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.HashSet
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptySet

@Repository
class S3 @Autowired constructor(private val dokumenttipalvelu: Dokumenttipalvelu) {
    @Value("\${lokalisointi.baseurls.pallero}")
    private val baseUrlPallero: String? = null

    @Value("\${lokalisointi.baseurls.untuva}")
    private val baseUrlUntuva: String? = null

    @Value("\${lokalisointi.baseurls.hahtuva}")
    private val baseUrlHahtuva: String? = null

    @Value("\${lokalisointi.baseurls.sade}")
    private val baseUrlSade: String? = null

    @Value("\${lokalisointi.envname}")
    private val envName: String? = null

    private val restClientBuilder: RestClient.Builder =
        RestClient.builder().requestFactory(HttpComponentsClientHttpRequestFactory())

    private fun virkailijaBaseUrl(env: OphEnvironment): String {
        return when (env) {
            OphEnvironment.pallero -> baseUrlPallero!!
            OphEnvironment.untuva -> baseUrlUntuva!!
            OphEnvironment.hahtuva -> baseUrlHahtuva!!
            OphEnvironment.sade -> baseUrlSade!!
        }
    }

    fun find(
        namespace: String?, locale: String?, key: String?
    ): Collection<Localisation> {
        LOG.debug(
            "Finding localisations with: namespace {}, locale {}, key {}", namespace, locale, key
        )
        return dokumenttipalvelu.find(listOf(LOKALISOINTI_TAG))
            .flatMap { metadata: ObjectMetadata -> this.transformToLocalisationStream(metadata) }
            .filter { l: Localisation -> namespace == null || l.namespace == namespace }
            .filter { l: Localisation -> locale == null || l.locale == locale }
            .filter { l: Localisation -> key == null || l.key == key }
            .toList()
    }

    fun availableNamespaces(source: OphEnvironment?): Set<String> {
        if (source == null) {
            // if source is not given, return namespaces from this environment
            return dokumenttipalvelu.find(listOf(LOKALISOINTI_TAG))
                .map { metadata: ObjectMetadata ->
                    metadata.key.split("/").first { s: String -> s != String.format("t-%s", LOKALISOINTI_TAG) }
                }
                .toSet()
        }
        // otherwise call source environment's endpoint
        val virkailijaBaseUrl = virkailijaBaseUrl(source)
        val restClient = restClientBuilder.baseUrl(virkailijaBaseUrl).build()
        val availableNamespaces =
            restClient
                .get()
                .uri(
                    String.format(
                        "%s/lokalisointi/api/v1/copy/available-namespaces", virkailijaBaseUrl
                    )
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Array<String>::class.java)
        if (availableNamespaces != null) {
            return availableNamespaces.toSet()
        }
        return emptySet()
    }

    fun copyLocalisations(copyRequest: CopyLocalisations, username: String?) {
        LOG.info(
            "Copying localisations from {}, namespaces: {}, initiated by: {}",
            copyRequest.source,
            copyRequest.namespaces,
            username
        )
        if (copyRequest.source == OphEnvironment.valueOf(envName!!)) {
            LOG.info("Trying to copy localisations from current environment - aborting")
            return
        }
        val virkailijaBaseUrl = virkailijaBaseUrl(copyRequest.source)
        val restClient = restClientBuilder.baseUrl(virkailijaBaseUrl).build()
        val urlBuilder =
            DefaultUriBuilderFactory(
                String.format("%s/lokalisointi/api/v1/copy/localisation-files", virkailijaBaseUrl)
            )
                .builder()
        if (!copyRequest.namespaces.isNullOrEmpty()) {
            urlBuilder.queryParam("namespaces", copyRequest.namespaces)
        }
        val body =
            restClient
                .get()
                .uri(urlBuilder.build())
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .body(ByteArray::class.java)
        if (body != null) {
            val buffer = ByteArray(1024)
            val entries: MutableList<ZipEntry?> = ArrayList()
            ZipInputStream(ByteArrayInputStream(body)).use { zipArchive ->
                var entry: ZipEntry?
                while ((zipArchive.nextEntry.also { entry = it }) != null) {
                    entries.add(entry)
                    val name = entry!!.name.split("/")
                    val namespace = name[0]
                    val localeFilename = name[1]
                    LOG.info("Writing localisation file {} to S3", entry!!.name)
                    val tempFile = File.createTempFile("localisation-file", "json")
                    val fos = FileOutputStream(tempFile)
                    var len: Int
                    while ((zipArchive.read(buffer).also { len = it }) > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                    dokumenttipalvelu
                        .putObject(
                            String.format("t-%s/%s/%s", LOKALISOINTI_TAG, namespace, localeFilename),
                            localeFilename,
                            "application/json",
                            FileInputStream(tempFile)
                        )
                        .join()
                    tempFile.delete()
                }
            }
            if (copyRequest.namespaces.isNullOrEmpty()) {
                val newEntries =
                    entries
                        .map { e: ZipEntry? -> String.format("t-%s/%s", LOKALISOINTI_TAG, e!!.name) }
                        .toSet()
                val existingKeys: MutableSet<String> =
                    HashSet(
                        dokumenttipalvelu.find(listOf(LOKALISOINTI_TAG))
                            .map { m: ObjectMetadata -> m.key }
                            .toList())

                for (key in (existingKeys - newEntries)) {
                    LOG.info("Deleting localisation file {}", key)
                    dokumenttipalvelu.delete(key)
                }
            }
        }
    }

    fun getLocalisationFilesZip(namespaces: Collection<String?>?): StreamingResponseBody {
        val withMatchingNamespaces =
            dokumenttipalvelu.find(listOf(LOKALISOINTI_TAG))
                .filter { metadata: ObjectMetadata ->
                    namespaces.isNullOrEmpty() || namespaces.contains(
                        metadata.key.split("/").first { s: String -> s != String.format("t-%s", LOKALISOINTI_TAG) }
                    )
                }
                .toList()
        return StreamingResponseBody { outputStream: OutputStream ->
            val out = ZipOutputStream(outputStream)
            for (metadata in withMatchingNamespaces) {
                val objectEntity = dokumenttipalvelu[metadata.key]
                val splittedObjectKey =
                    metadata.key.split("/")
                        .filter { s: String -> s != String.format("t-%s", LOKALISOINTI_TAG) }
                val namespace: String = splittedObjectKey.first()
                val filename: String = splittedObjectKey.last()
                out.putNextEntry(ZipEntry(String.format("%s/%s", namespace, filename)))
                IOUtils.copy(objectEntity.entity, out)
                out.closeEntry()
            }
            out.finish()
        }
    }

    private fun transformToLocalisationStream(metadata: ObjectMetadata): List<Localisation> {
        try {
            val objectEntity = dokumenttipalvelu[metadata.key]
            val splittedObjectKey =
                metadata.key.split("/")
                    .filter { s: String -> s != String.format("t-%s", LOKALISOINTI_TAG) }
            val localisations: Map<String, String> = MAPPER.readValue(objectEntity.entity)
            return localisations.keys
                .map { localisationKey: String ->
                    Localisation(
                        null,
                        splittedObjectKey.first(),
                        localisationKey,
                        splittedObjectKey.last().split(".").first(),
                        localisations[localisationKey]!!
                    )
                }
        } catch (ex: Exception) {
            throw RuntimeException(ex)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(S3::class.java)
        private val MAPPER: ObjectMapper = JsonMapper.builder().addModule(JavaTimeModule()).build()
        const val LOKALISOINTI_TAG: String = "lokalisointi"
    }
}

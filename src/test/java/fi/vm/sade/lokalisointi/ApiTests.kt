package fi.vm.sade.lokalisointi

import com.fasterxml.jackson.module.kotlin.readValue
import fi.vm.sade.lokalisointi.configuration.DevConfiguration
import fi.vm.sade.lokalisointi.model.Localisation
import fi.vm.sade.lokalisointi.model.LocalisationOverride
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.io.ByteArrayInputStream
import java.io.IOException
import java.time.Duration
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ApiTests : IntegrationTestBase() {
    @BeforeEach
    @Throws(IOException::class)
    fun reset() {
        val clientBuilder =
            S3AsyncClient.builder()
                .forcePathStyle(true)
                .endpointOverride(LOCAL_STACK.endpoint)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            LOCAL_STACK.accessKey, LOCAL_STACK.secretKey
                        )
                    )
                )
                .region(Region.of(LOCAL_STACK.region))
                .httpClientBuilder(
                    NettyNioAsyncHttpClient.builder()
                        .tlsTrustManagersProvider { InsecureTrustManagerFactory.INSTANCE.trustManagers }
                        .connectionTimeout(Duration.ofSeconds(60))
                        .maxConcurrency(100))
        DevConfiguration.addLocalisationFiles(dokumenttipalvelu, clientBuilder, BUCKET_NAME)
        database.find().forEach { override -> database.deleteOverride(override.id) }
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsWorksWithoutAuthentication() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/localisation")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.length()",
                    Matchers.`is`(8)
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsWithCategoryAndNamespaceReturnsError() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/localisation?category=foobar&namespace=example")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.error.message",
                    Matchers.`is`("category and namespace are both defined and but do not match")
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsFilterByNamespace() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/localisation?namespace=example").accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(6)))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsFilterByCategory() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/localisation?category=lokalisointi").accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(2)))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsFilterByNamespaceAndKey() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/localisation?namespace=example&key=create.item")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(2)))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsFilterByNamespaceAndKeyAndLocale() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/localisation?namespace=example&key=create.item&locale=fi")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(1)))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsReturnsCacheHeaderByDefault() {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.header().string("Cache-Control", "max-age=600, public"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsWithCacheFalse() {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/localisation?cache=false").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.header().string("Cache-Control", "no-cache"))
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testAddOverride() {
        addLocalisationOverride("foobar", "testi", "fi", "Testi")
        val mvcResult =
            mvc.perform(MockMvcRequestBuilders.get("/api/v1/override").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(1)))
                .andReturn()

        val localisationOverrides: List<LocalisationOverride> =
            objectMapper.readValue(
                mvcResult.response.contentAsString
            )
        val override: LocalisationOverride = localisationOverrides.first()
        Assertions.assertTrue(localisationOverrides.stream().findFirst().isPresent)
        Assertions.assertNotNull(override.id)
        Assertions.assertEquals("foobar", override.namespace)
        Assertions.assertEquals("testi", override.key)
        Assertions.assertEquals("fi", override.locale)
        Assertions.assertEquals("Testi", override.value)
        Assertions.assertNotNull(override.created)
        Assertions.assertNotNull(override.updated)
        Assertions.assertEquals("1.2.246.562.24.00000000001", override.createdBy)
        Assertions.assertEquals("1.2.246.562.24.00000000001", override.updatedBy)

        override.value = "Muokattu"
        mvc.perform(
            MockMvcRequestBuilders.post(String.format("/api/v1/override/%d", override.id))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(override.toLocalisation()))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.value", Matchers.`is`("Muokattu")))
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testUpdateOverride() {
        val override = addLocalisationOverride("foobar", "testi", "fi", "Testi")
        override.value = "Muokattu"
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/override/{id}", override.id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(override.toLocalisation()))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.value", Matchers.`is`("Muokattu")))
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testDeleteOverride() {
        val override = addLocalisationOverride("foobar", "testi", "fi", "Testi")
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/override/{id}", override.id).accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.`is`("OK")))
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/override").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(0)))
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testSavingLocalisationOverrideOverridesPublishedLocalisation() {
        addLocalisationOverride("example", "testi", "fi", "Testi")
        addLocalisationOverride("example", "testi", "en", "Test")
        val mvcResult =
            mvc.perform(MockMvcRequestBuilders.get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(8)))
                .andReturn()

        val localisations: List<Localisation> =
            objectMapper.readValue(
                mvcResult.response.contentAsByteArray
            )
        val localisation =
            localisations.stream()
                .filter { l: Localisation ->
                    l.namespace == "example"
                            && l.key == "testi"
                            && l.locale == "fi"
                }
                .findFirst()
        Assertions.assertTrue(localisation.isPresent)
        Assertions.assertEquals("Testi", localisation.get().value)
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testGetLocalisationsReturnsNonOverridingOverrides() {
        addLocalisationOverride("foobar", "fookey", "fi", "Testi")
        val mvcResult =
            mvc.perform(MockMvcRequestBuilders.get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(9)))
                .andReturn()

        val localisations: List<Localisation> =
            objectMapper.readValue(
                mvcResult.response.contentAsByteArray
            )
        val localisation =
            localisations.stream()
                .filter { l: Localisation ->
                    l.namespace == "foobar"
                            && l.key == "fookey"
                            && l.locale == "fi"
                }
                .findFirst()
        Assertions.assertTrue(localisation.isPresent)
        val l = localisation.get()
        Assertions.assertNotNull(l.id)
        Assertions.assertEquals("foobar", l.namespace)
        Assertions.assertEquals("fookey", l.key)
        Assertions.assertEquals("fi", l.locale)
        Assertions.assertEquals("Testi", l.value)
    }

    @Test
    @Throws(Exception::class)
    fun testGetLocalisationFilesArchive() {
        val mvcResult =
            mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/copy/localisation-files").accept(MediaType.APPLICATION_OCTET_STREAM)
            )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andDo { obj: MvcResult -> obj.asyncResult }
                .andReturn()
        val inputStream =
            ByteArrayInputStream(mvcResult.response.contentAsByteArray)
        ZipInputStream(inputStream).use { zipArchive ->
            val zipEntries: MutableList<ZipEntry> = ArrayList()
            var entry: ZipEntry
            while ((zipArchive.nextEntry.also { entry = it }) != null) {
                zipEntries.add(entry)
            }

            Assertions.assertFalse(zipEntries.isEmpty())
            Assertions.assertTrue(zipEntries.stream().anyMatch { e: ZipEntry -> e.name == "lokalisointi/fi.json" })
            Assertions.assertTrue(zipEntries.stream().anyMatch { e: ZipEntry -> e.name == "lokalisointi/en.json" })
            Assertions.assertTrue(zipEntries.stream().anyMatch { e: ZipEntry -> e.name == "example/fi.json" })
            Assertions.assertTrue(zipEntries.stream().anyMatch { e: ZipEntry -> e.name == "example/en.json" })
        }
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testGetAvailableNamespacesForOverrides() {
        addLocalisationOverride("foofoo", "fookey", "fi", "Testi")
        val mvcResult =
            mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/override/available-namespaces").accept(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        val namespaces: Set<String> =
            objectMapper.readValue(mvcResult.response.contentAsByteArray)
        Assertions.assertEquals(setOf("example", "lokalisointi", "foofoo"), namespaces)
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testGetAvailableNamespacesForCopyWithoutSource() {
        addLocalisationOverride("foofaa", "fookey", "fi", "Testi")
        val mvcResult =
            mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/copy/available-namespaces").accept(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        val namespaces: Set<String> =
            objectMapper.readValue(mvcResult.response.contentAsByteArray)
        Assertions.assertEquals(setOf("example", "lokalisointi"), namespaces)
    }

    @Test
    @Throws(Exception::class)
    fun testGetAvailableNamespacesForCopyWithSource() {
        val mvcResult =
            mvc.perform(
                MockMvcRequestBuilders.get("/api/v1/copy/available-namespaces?source=untuva")
                    .accept(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        val namespaces: Set<String> =
            objectMapper.readValue(mvcResult.response.contentAsByteArray)
        Assertions.assertEquals(setOf("esimerkki", "lokalisointi"), namespaces)
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testCopyAllLocalisationsFromAnotherEnvironment() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/copy")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(mapOf(Pair("source", "untuva"))))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.`is`("OK")))
        val result =
            mvc.perform(MockMvcRequestBuilders.get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(8)))
                .andReturn()
        val localisations: List<Localisation> =
            objectMapper.readValue(result.response.contentAsByteArray)
        Assertions.assertEquals(
            setOf("example", "foobar", "lorem"),
            localisations.stream().map { obj: Localisation -> obj.namespace }.collect(Collectors.toSet())
        )
        Assertions.assertEquals(
            setOf("testi", "create.item", "test-item", "localisation-1"),
            localisations.stream().map { obj: Localisation -> obj.key }.collect(Collectors.toSet())
        )
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testCopySelectedLocalisationsFromAnotherEnvironment() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/copy")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        mapOf(Pair("source", "untuva"), Pair("namespaces", setOf("example", "lorem")))
                    )
                )
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.`is`("OK")))
        val result =
            mvc.perform(MockMvcRequestBuilders.get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.`is`(8)))
                .andReturn()
        val localisations: List<Localisation> =
            objectMapper.readValue(result.response.contentAsByteArray)
        Assertions.assertEquals(
            setOf("example", "lokalisointi", "lorem"),
            localisations.stream().map { obj: Localisation -> obj.namespace }.collect(Collectors.toSet())
        )
        Assertions.assertEquals(
            setOf("testi", "create.item", "localisation", "localisation-1"),
            localisations.stream().map { obj: Localisation -> obj.key }.collect(Collectors.toSet())
        )
    }

    @WithMockUser("1.2.246.562.24.00000000001")
    @Test
    @Throws(Exception::class)
    fun testGetUiConfig() {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/ui-config").accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.currentEnvironment", Matchers.`is`("pallero")))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.sourceEnvironments",
                    Matchers.`is`(listOf("untuva", "hahtuva", "sade"))
                )
            )
    }

    @Throws(Exception::class)
    fun addLocalisationOverride(
        namespace: String, key: String, locale: String, value: String
    ): LocalisationOverride {
        val result =
            mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/override")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            mapOf(
                                Pair(
                                    "namespace",
                                    namespace
                                ),
                                Pair(
                                    "key",
                                    key
                                ),
                                Pair(
                                    "locale",
                                    locale
                                ),
                                Pair(
                                    "value",
                                    value
                                )
                            )
                        )
                    )
            )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        return objectMapper.readValue(
            result.response.contentAsByteArray
        )
    }
}

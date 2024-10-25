package fi.vm.sade.lokalisointi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vm.sade.lokalisointi.configuration.SecurityConfiguration
import fi.vm.sade.lokalisointi.model.LocalisationOverride
import fi.vm.sade.lokalisointi.storage.Database
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Repository
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File
import java.io.IOException
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [LokalisointiApplication::class])
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = [IntegrationTestBase.Initializer::class])
@ComponentScan("fi.vm.sade.lokalisointi")
abstract class IntegrationTestBase {
    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    lateinit var dokumenttipalvelu: Dokumenttipalvelu

    @Autowired
    lateinit var database: Database

    val objectMapper: ObjectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRESQL_CONTAINER.jdbcUrl,
                "spring.datasource.username=" + POSTGRESQL_CONTAINER.username,
                "spring.datasource.password=" + POSTGRESQL_CONTAINER.password,
                "cas-service.service=http://localhost:10080/lokalisointi",  // mocking for "untuva" source environment
                "lokalisointi.baseurls.untuva=http://localhost:10080/lokalisointi"
            )
                .applyTo(configurableApplicationContext.environment)
        }
    }

    @SpringBootConfiguration
    @EnableWebSecurity
    internal class TestConfiguration {
        @Bean
        fun dokumenttipalvelu(): Dokumenttipalvelu {
            // re-wire dokumenttipalvelu with localstack
            val dokumenttipalvelu: Dokumenttipalvelu =
                object : Dokumenttipalvelu(LOCAL_STACK.region, BUCKET_NAME) {
                    public override fun getClient(): S3AsyncClient {
                        return S3AsyncClient.builder()
                            .endpointOverride(LOCAL_STACK.getEndpointOverride(LocalStackContainer.Service.S3))
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
                                    .connectionTimeout(Duration.ofSeconds(60))
                                    .maxConcurrency(100)
                            )
                            .build()
                    }

                    public override fun getPresigner(): S3Presigner {
                        return S3Presigner.builder()
                            .region(Region.of(LOCAL_STACK.region))
                            .endpointOverride(LOCAL_STACK.getEndpointOverride(LocalStackContainer.Service.S3))
                            .credentialsProvider(
                                StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(
                                        LOCAL_STACK.accessKey, LOCAL_STACK.secretKey
                                    )
                                )
                            )
                            .build()
                    }
                }
            dokumenttipalvelu.listObjectsMaxKeys = 5
            return dokumenttipalvelu
        }

        @Bean
        fun filterChain(http: HttpSecurity): SecurityFilterChain {
            return http.headers { obj: HeadersConfigurer<HttpSecurity> -> obj.disable() }
                .csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
                .securityMatcher("/**")
                .authorizeHttpRequests(
                    SecurityConfiguration.nonAuthenticatedRoutes(
                        "/lokalisointi/api/v1/copy/available-namespaces",
                        "/lokalisointi/api/v1/copy/localisation-files"
                    )
                )
                .build()
        }
    }

    @RestController
    @RequestMapping("/lokalisointi/api/v1/copy")
    internal class CopyControllerInUntuvaEnvironment {
        @GetMapping(value = ["/localisation-files"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
        fun mockLocalisationArchive(
            @RequestParam(value = "namespaces", required = false) namespaces: Collection<String?>?
        ): ResponseEntity<ByteArray> {
            val pathname =
                if (namespaces == null || namespaces.isEmpty())
                    "src/test/resources/localisations.zip"
                else
                    "src/test/resources/localisations-example.zip"

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=localisations.zip")
                .body(FileUtils.readFileToByteArray(File(pathname)))
        }

        @GetMapping("/available-namespaces")
        fun mockAvailableNamespaces(): ResponseEntity<Collection<String>> {
            return ResponseEntity.ok(setOf("lokalisointi", "esimerkki"))
        }
    }

    companion object {
        const val BUCKET_NAME: String = "opintopolku-test-dokumenttipalvelu"

        @Container
        val LOCAL_STACK: LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
            .withServices(LocalStackContainer.Service.S3)
            .waitingFor(DockerHealthcheckWaitStrategy())

        @Container
        val POSTGRESQL_CONTAINER: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:15"))
                .withDatabaseName("lokalisointi")
                .withUsername("lokalisointi_user")
                .withPassword("lokalisointi")

        @JvmStatic
        @BeforeAll
        fun createBucket(): Unit {
            LOCAL_STACK.execInContainer("awslocal", "s3", "mb", "s3://$BUCKET_NAME")
        }
    }
}

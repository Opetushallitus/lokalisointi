package fi.vm.sade.lokalisointi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.vm.sade.lokalisointi.model.LocalisationOverride;
import fi.vm.sade.lokalisointi.storage.ExtendedDokumenttipalvelu;
import fi.vm.sade.lokalisointi.storage.Database;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static fi.vm.sade.lokalisointi.configuration.SecurityConfiguration.nonAuthenticatedRoutes;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = {LokalisointiApplication.class})
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = {ApiTests.Initializer.class})
@ComponentScan("fi.vm.sade.lokalisointi")
public abstract class IntegrationTestBase {
  static final String BUCKET_NAME = "opintopolku-test-dokumenttipalvelu";
  final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
  @Autowired MockMvc mvc;

  @Container
  static final LocalStackContainer LOCAL_STACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
          .withServices(S3)
          .waitingFor(new DockerHealthcheckWaitStrategy());

  @Container
  static final PostgreSQLContainer POSTGRESQL_CONTAINER =
      new PostgreSQLContainer(DockerImageName.parse("postgres:15"))
          .withDatabaseName("lokalisointi")
          .withUsername("lokalisointi_user")
          .withPassword("lokalisointi");

  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + POSTGRESQL_CONTAINER.getJdbcUrl(),
              "spring.datasource.username=" + POSTGRESQL_CONTAINER.getUsername(),
              "spring.datasource.password=" + POSTGRESQL_CONTAINER.getPassword())
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }

  @Autowired ExtendedDokumenttipalvelu dokumenttipalvelu;
  @Autowired TestDatabase database;

  @BeforeAll
  static void createBucket() throws IOException, InterruptedException {
    LOCAL_STACK.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
  }

  @SpringBootConfiguration
  @EnableWebSecurity
  static class TestConfiguration {
    @Bean
    public ExtendedDokumenttipalvelu dokumenttipalvelu() {
      // re-wire dokumenttipalvelu with localstack
      return new ExtendedDokumenttipalvelu(LOCAL_STACK.getRegion(), BUCKET_NAME) {
        @Override
        public S3AsyncClient getClient() {
          return S3AsyncClient.builder()
              .endpointOverride(LOCAL_STACK.getEndpointOverride(S3))
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create(
                          LOCAL_STACK.getAccessKey(), LOCAL_STACK.getSecretKey())))
              .region(Region.of(LOCAL_STACK.getRegion()))
              .httpClientBuilder(
                  NettyNioAsyncHttpClient.builder()
                      .connectionTimeout(Duration.ofSeconds(60))
                      .maxConcurrency(100))
              .build();
        }

        @Override
        public S3Presigner getPresigner() {
          return S3Presigner.builder()
              .region(Region.of(LOCAL_STACK.getRegion()))
              .endpointOverride(LOCAL_STACK.getEndpointOverride(S3))
              .credentialsProvider(
                  StaticCredentialsProvider.create(
                      AwsBasicCredentials.create(
                          LOCAL_STACK.getAccessKey(), LOCAL_STACK.getSecretKey())))
              .build();
        }
      };
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
      return http.headers(AbstractHttpConfigurer::disable)
          .csrf(AbstractHttpConfigurer::disable)
          .securityMatcher("/**")
          .authorizeHttpRequests(
              nonAuthenticatedRoutes(
                  List.of(
                      "/lokalisointi/api/v1/copy/available-namespaces",
                      "/lokalisointi/api/v1/copy/localisation-files"),
                  List.of("/tolgee/v2/projects/{projectId}/keys/import-resolvable")))
          .build();
    }
  }

  @Repository
  static class TestDatabase extends Database {
    protected TestDatabase(JdbcAggregateTemplate template) {
      super(template);
    }

    public void deleteAllOverrides() {
      template.deleteAll(LocalisationOverride.class);
    }
  }

  @Data
  @AllArgsConstructor
  static class TolgeeTranslation {
    @NonNull private String text;
    @NonNull private String resolution;

    public TolgeeTranslation() {}
  }

  @Data
  @AllArgsConstructor
  static class TolgeeKey {
    @NonNull private String name;
    @NonNull private String namespace;
    @NonNull private Map<String, TolgeeTranslation> translations;

    public TolgeeKey() {}
  }

  @Data
  @AllArgsConstructor
  static class TolgeeImport {
    @NonNull private Collection<TolgeeKey> keys;

    public TolgeeImport() {}
  }

  @RestController
  @RequestMapping("/lokalisointi/api/v1/copy")
  static class CopyControllerInUntuvaEnvironment {
    @GetMapping(value = "/localisation-files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> mockLocalisationArchive(
        @RequestParam(value = "namespaces", required = false) final Collection<String> namespaces)
        throws IOException {
      final String pathname =
          namespaces == null || namespaces.isEmpty()
              ? "src/test/resources/localisations.zip"
              : "src/test/resources/localisations-example.zip";

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .header("Content-Disposition", "attachment; filename=localisations.zip")
          .body(FileUtils.readFileToByteArray(new File(pathname)));
    }

    @GetMapping("/available-namespaces")
    public ResponseEntity<Collection<String>> mockAvailableNamespaces() {
      return ResponseEntity.ok(Set.of("lokalisointi", "esimerkki"));
    }
  }

  @RestController
  @RequestMapping("/tolgee/v2/projects/{projectId}/keys")
  static class TolgeeMock {
    @PostMapping("/import-resolvable")
    public ResponseEntity<Map<String, ?>> tolgeeImportResolvable(
        @RequestHeader("X-API-Key") final String apiKey,
        @PathVariable String projectId,
        @Valid @RequestBody final TolgeeImport body) {
      if (projectId == null || !projectId.equals("11100")) {
        return ResponseEntity.badRequest().build();
      }
      if (apiKey == null || !apiKey.equals("testkey")) {
        return ResponseEntity.status(401).build();
      }
      return ResponseEntity.ok(Map.of("keys", body.getKeys()));
    }
  }
}

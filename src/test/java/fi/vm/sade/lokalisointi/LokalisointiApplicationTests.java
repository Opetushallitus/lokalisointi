package fi.vm.sade.lokalisointi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.lokalisointi.configuration.DevConfiguration;
import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@RunWith(SpringRunner.class)
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {LokalisointiApplication.class})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = {LokalisointiApplicationTests.Initializer.class})
@ComponentScan("fi.vm.sade.lokalisointi")
class LokalisointiApplicationTests {
  @BeforeEach
  public void resetBucketContents() throws IOException, InterruptedException {
    LOCAL_STACK.execInContainer(
        "awslocal", "s3", "rm", "s3://" + BUCKET_NAME, "--recursive", "--include", "'*'");
    final S3AsyncClientBuilder clientBuilder =
        S3AsyncClient.builder()
            .forcePathStyle(true)
            .endpointOverride(LOCAL_STACK.getEndpoint())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        LOCAL_STACK.getAccessKey(), LOCAL_STACK.getSecretKey())))
            .region(Region.of(LOCAL_STACK.getRegion()))
            .httpClientBuilder(
                NettyNioAsyncHttpClient.builder()
                    .tlsTrustManagersProvider(
                        InsecureTrustManagerFactory.INSTANCE::getTrustManagers)
                    .connectionTimeout(Duration.ofSeconds(60))
                    .maxConcurrency(100));
    DevConfiguration.addLocalisationFiles(dokumenttipalvelu, clientBuilder, BUCKET_NAME);
  }

  @Test
  public void getLocalisations() throws Exception {
    mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(6)));
  }

  private void addOverride(final Localisation localisation) throws Exception {
    mvc.perform(
            post("/api/v1/override")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        Map.of(
                            "namespace",
                            localisation.getNamespace(),
                            "key",
                            localisation.getKey(),
                            "locale",
                            localisation.getLocale(),
                            "value",
                            localisation.getValue()))))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.namespace", is(localisation.getNamespace())))
        .andExpect(jsonPath("$.key", is(localisation.getKey())))
        .andExpect(jsonPath("$.locale", is(localisation.getLocale())))
        .andExpect(jsonPath("$.value", is(localisation.getValue())))
        .andExpect(jsonPath("$.id", notNullValue()));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testAddOverrides() throws Exception {
    addOverride(new Localisation(null, "example", "testi", "fi", "Testi"));
    addOverride(new Localisation(null, "example", "testi1", "fi", "Testi 1"));
    final MvcResult mvcResult =
        mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()", is(7)))
            .andReturn();

    final TypeReference<List<Localisation>> listOfLocalisations = new TypeReference<>() {};
    final List<Localisation> localisations =
        objectMapper.readValue(
            mvcResult.getResponse().getContentAsByteArray(), listOfLocalisations);
    final Optional<Localisation> localisation =
        localisations.stream()
            .filter(l -> l.getKey().equals("testi") && l.getLocale().equals("fi"))
            .findFirst();
    assertTrue(localisation.isPresent());
    assertEquals("Testi", localisation.get().getValue());
  }

  final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private MockMvc mvc;
  private static final String BUCKET_NAME = "opintopolku-test-dokumenttipalvelu";

  @Container
  private static final LocalStackContainer LOCAL_STACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
          .withServices(S3)
          .waitingFor(new DockerHealthcheckWaitStrategy());

  @Container
  private static final PostgreSQLContainer POSTGRESQL_CONTAINER =
      new PostgreSQLContainer(DockerImageName.parse("postgres:15"))
          .withDatabaseName("lokalisointi")
          .withUsername("lokalisointi_user")
          .withPassword("lokalisointi");

  static class Initializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @LocalServerPort protected Integer port;

    @SneakyThrows
    public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              "spring.datasource.url=" + POSTGRESQL_CONTAINER.getJdbcUrl(),
              "spring.datasource.username=" + POSTGRESQL_CONTAINER.getUsername(),
              "spring.datasource.password=" + POSTGRESQL_CONTAINER.getPassword(),
              "cas-service.service=" + String.format("http://localhost:%d/lokalisointi", port))
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }

  @Autowired private Dokumenttipalvelu dokumenttipalvelu;

  @BeforeAll
  static void createBucket() throws IOException, InterruptedException {
    LOCAL_STACK.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
  }

  @SpringBootConfiguration
  @EnableWebSecurity
  static class TestConfiguration {
    @Bean
    public Dokumenttipalvelu dokumenttipalvelu() {
      // re-wire dokumenttipalvelu with localstack
      final Dokumenttipalvelu dokumenttipalvelu =
          new Dokumenttipalvelu(LOCAL_STACK.getRegion(), BUCKET_NAME) {
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
      dokumenttipalvelu.listObjectsMaxKeys = 5;
      return dokumenttipalvelu;
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
      return http.headers(AbstractHttpConfigurer::disable)
          .csrf(AbstractHttpConfigurer::disable)
          .securityMatcher("/**")
          .authorizeHttpRequests(
              (authz) ->
                  authz
                      .requestMatchers(
                          HttpMethod.GET,
                          "/buildversion.txt",
                          "/actuator/health",
                          "/v3/api-docs",
                          "/v3/api-docs/**",
                          "/swagger",
                          "/swagger/**",
                          "/swagger-ui/**",
                          "/swagger-ui.html",
                          "/webjars/swagger-ui/**",
                          "/cxf/rest/v1/localisation",
                          "/api/v1/localisation",
                          "/api/v1/copy/localisation-files",
                          "/api/v1/copy/available-namespaces")
                      .permitAll()
                      .anyRequest()
                      .authenticated())
          .build();
    }
  }
}

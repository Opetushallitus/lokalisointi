package fi.vm.sade.lokalisointi;

import com.fasterxml.jackson.core.type.TypeReference;
import fi.vm.sade.lokalisointi.configuration.DevConfiguration;
import fi.vm.sade.lokalisointi.model.Localisation;
import fi.vm.sade.lokalisointi.model.LocalisationOverride;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ApiTests extends IntegrationTestBase {
  @Value("${tolgee.slug}")
  private String tolgeeSlug;

  @Autowired private CacheManager cacheManager;

  @BeforeEach
  public void reset() throws IOException {
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
    DevConfiguration.addLocalisationFiles(
        dokumenttipalvelu, clientBuilder, BUCKET_NAME, tolgeeSlug);
    database.deleteAllOverrides();
    cacheManager
        .getCacheNames()
        .forEach(
            name -> {
              cacheManager.getCache(name).clear();
            });
  }

  @Test
  public void testGetLocalisationsWorksWithoutAuthentication() throws Exception {
    mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(102)));
  }

  @Test
  public void testGetLocalisationsCors() throws Exception {
    mvc.perform(
            options("/api/v1/localisation")
                .accept(MediaType.APPLICATION_JSON)
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Caller-Id")
                .header("Caller-Id", "test"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
        .andExpect(
            header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD"))
        .andExpect(header().string("Access-Control-Allow-Headers", "Caller-Id"));

    mvc.perform(
            options("/cxf/rest/v1/localisation")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "https://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "X-PINGOTHER")
                .header("X-PINGOTHER", "test"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost:3000"))
        .andExpect(
            header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD"))
        .andExpect(header().string("Access-Control-Allow-Headers", "X-PINGOTHER"));

    mvc.perform(
            options("/tolgee/fi.json")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Origin", "https://virkailija.testiopintopolku.fi")
                .header("Access-Control-Request-Method", "PUT")
                .header("Access-Control-Request-Headers", "clientSubSystemCode")
                .header("clientSubSystemCode", "test"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(
            header()
                .string("Access-Control-Allow-Origin", "https://virkailija.testiopintopolku.fi"))
        .andExpect(
            header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,HEAD"))
        .andExpect(header().string("Access-Control-Allow-Headers", "clientSubSystemCode"));
  }

  @Test
  public void testGetLocalisationsWithCategoryAndNamespaceReturnsError() throws Exception {
    mvc.perform(
            get("/api/v1/localisation?category=foobar&namespace=example")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(
            jsonPath(
                "$.error.message",
                is("category and namespace are both defined and but do not match")));
  }

  @Test
  public void testGetLocalisationsFilterByNamespace() throws Exception {
    mvc.perform(get("/api/v1/localisation?namespace=example").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(6)));
  }

  @Test
  public void testGetLocalisationsFilterByCategory() throws Exception {
    mvc.perform(
            get("/api/v1/localisation?category=lokalisointi").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(2)));
  }

  @Test
  public void testGetLocalisationsFilterByNamespaceAndKey() throws Exception {
    mvc.perform(
            get("/api/v1/localisation?namespace=example&key=create.item")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(2)));
  }

  @Test
  public void testGetLocalisationsFilterByNamespaceAndKeyAndLocale() throws Exception {
    mvc.perform(
            get("/api/v1/localisation?namespace=example&key=create.item&locale=fi")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(1)));
  }

  @Test
  public void testGetLocalisationsReturnsCacheHeaderByDefault() throws Exception {
    mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Cache-Control", "max-age=600, public"));
  }

  @Test
  public void testGetLocalisationsWithCacheFalse() throws Exception {
    mvc.perform(get("/api/v1/localisation?cache=false").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(header().string("Cache-Control", "no-cache"));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testAddOverride() throws Exception {
    addLocalisationOverride("foobar", "testi", "fi", "Testi");
    final MvcResult mvcResult =
        mvc.perform(get("/api/v1/override").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()", is(1)))
            .andReturn();

    final List<LocalisationOverride> localisationOverrides =
        objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(), listOfLocalisationOverrides);
    final LocalisationOverride override = localisationOverrides.getFirst();
    assertTrue(localisationOverrides.stream().findFirst().isPresent());
    assertNotNull(override.getId());
    assertEquals("foobar", override.getNamespace());
    assertEquals("testi", override.getKey());
    assertEquals("fi", override.getLocale());
    assertEquals("Testi", override.getValue());
    assertNotNull(override.getCreated());
    assertNotNull(override.getUpdated());
    assertEquals("1.2.246.562.24.00000000001", override.getCreatedBy());
    assertEquals("1.2.246.562.24.00000000001", override.getUpdatedBy());

    override.setValue("Muokattu");
    mvc.perform(
            post(String.format("/api/v1/override/%d", override.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(override.toLocalisation())))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.value", is("Muokattu")));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testUpdateOverride() throws Exception {
    final LocalisationOverride override = addLocalisationOverride("foobar", "testi", "fi", "Testi");
    override.setValue("Muokattu");
    mvc.perform(
            post("/api/v1/override/{id}", override.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(override.toLocalisation())))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.value", is("Muokattu")));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testDeleteOverride() throws Exception {
    final LocalisationOverride override = addLocalisationOverride("foobar", "testi", "fi", "Testi");
    mvc.perform(
            delete("/api/v1/override/{id}", override.getId()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("OK")));
    mvc.perform(get("/api/v1/override").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(0)));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testSavingLocalisationOverrideOverridesPublishedLocalisation() throws Exception {
    addLocalisationOverride("example", "testi", "fi", "Testi");
    addLocalisationOverride("example", "testi", "en", "Test");
    addLocalisationOverride(null, "root-key", "fi", "Test value");
    final MvcResult mvcResult =
        mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()", is(102)))
            .andReturn();

    final List<Localisation> localisations =
        objectMapper.readValue(
            mvcResult.getResponse().getContentAsByteArray(), listOfLocalisations);
    final Optional<Localisation> localisation =
        localisations.stream()
            .filter(
                l ->
                    l.getNamespace().equals("example")
                        && l.getKey().equals("testi")
                        && l.getLocale().equals("fi"))
            .findFirst();
    assertTrue(localisation.isPresent());
    assertEquals("Testi", localisation.get().getValue());
    final Optional<Localisation> rootLocalisation =
        localisations.stream()
            .filter(
                l ->
                    l.getNamespace() == null
                        && l.getKey().equals("root-key")
                        && l.getLocale().equals("fi"))
            .findFirst();
    assertTrue(rootLocalisation.isPresent());
    assertEquals("Test value", rootLocalisation.get().getValue());
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testGetLocalisationsReturnsNonOverridingOverrides() throws Exception {
    addLocalisationOverride("foobar", "fookey", "fi", "Testi");
    final MvcResult mvcResult =
        mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()", is(103)))
            .andReturn();

    final List<Localisation> localisations =
        objectMapper.readValue(
            mvcResult.getResponse().getContentAsByteArray(), listOfLocalisations);
    final Optional<Localisation> localisation =
        localisations.stream()
            .filter(
                l ->
                    l.getNamespace() != null
                        && l.getNamespace().equals("foobar")
                        && l.getKey().equals("fookey")
                        && l.getLocale().equals("fi"))
            .findFirst();
    assertTrue(localisation.isPresent());
    final Localisation l = localisation.get();
    assertNotNull(l.getId());
    assertEquals("foobar", l.getNamespace());
    assertEquals("fookey", l.getKey());
    assertEquals("fi", l.getLocale());
    assertEquals("Testi", l.getValue());
  }

  @Test
  public void testGetLocalisationFilesArchive() throws Exception {
    final MvcResult mvcResult =
        mvc.perform(
                get("/api/v1/copy/localisation-files").accept(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andDo(MvcResult::getAsyncResult)
            .andReturn();
    final ByteArrayInputStream inputStream =
        new ByteArrayInputStream(mvcResult.getResponse().getContentAsByteArray());
    try (final ZipInputStream zipArchive = new ZipInputStream(inputStream)) {
      final List<ZipEntry> zipEntries = new ArrayList<>();
      ZipEntry entry;
      while ((entry = zipArchive.getNextEntry()) != null) {
        zipEntries.add(entry);
      }

      assertFalse(zipEntries.isEmpty());
      assertEquals(
          Set.of(
              "lokalisointi/fi.json",
              "lokalisointi/en.json",
              "example/fi.json",
              "example/en.json",
              "virkailijaraamit/fi.json",
              "fi.json"),
          zipEntries.stream().map(ZipEntry::getName).collect(Collectors.toSet()));
    }
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testGetAvailableNamespacesForOverrides() throws Exception {
    addLocalisationOverride("foofoo", "fookey", "fi", "Testi");
    final MvcResult mvcResult =
        mvc.perform(get("/api/v1/override/available-namespaces").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    final Set<String> namespaces =
        objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), setOfStrings);
    assertEquals(Set.of("virkailijaraamit", "example", "lokalisointi", "foofoo"), namespaces);
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testGetAvailableNamespacesForCopyWithoutSource() throws Exception {
    addLocalisationOverride("foofaa", "fookey", "fi", "Testi");
    final MvcResult mvcResult =
        mvc.perform(get("/api/v1/copy/available-namespaces").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    final Set<String> namespaces =
        objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), setOfStrings);
    assertEquals(setOf(null, "example", "lokalisointi", "virkailijaraamit"), namespaces);
  }

  @Test
  public void testGetAvailableNamespacesForCopyWithSource() throws Exception {
    final MvcResult mvcResult =
        mvc.perform(
                get("/api/v1/copy/available-namespaces?source=untuva")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    final Set<String> namespaces =
        objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), setOfStrings);
    assertEquals(Set.of("esimerkki", "lokalisointi"), namespaces);
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testCopyAllLocalisationsFromAnotherEnvironment() throws Exception {
    mvc.perform(
            post("/api/v1/copy")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("source", "untuva"))))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("OK")));
    final MvcResult result =
        mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()", is(8)))
            .andReturn();
    final List<Localisation> localisations =
        objectMapper.readValue(result.getResponse().getContentAsByteArray(), listOfLocalisations);
    assertEquals(
        Set.of("example", "foobar", "lorem"),
        localisations.stream().map(Localisation::getNamespace).collect(Collectors.toSet()));
    assertEquals(
        Set.of("testi", "create.item", "test-item", "localisation-1"),
        localisations.stream().map(Localisation::getKey).collect(Collectors.toSet()));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testCopySelectedLocalisationsFromAnotherEnvironment() throws Exception {
    mvc.perform(
            post("/api/v1/copy")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        Map.of("source", "untuva", "namespaces", setOf("example", "lorem", null)))))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status", is("OK")));
    final MvcResult result =
        mvc.perform(get("/api/v1/localisation").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()", is(102)))
            .andReturn();
    final List<Localisation> localisations =
        objectMapper.readValue(result.getResponse().getContentAsByteArray(), listOfLocalisations);
    assertEquals(
        setOf(null, "example", "lokalisointi", "lorem", "virkailijaraamit"),
        localisations.stream().map(Localisation::getNamespace).collect(Collectors.toSet()));
    assertEquals(
        98, localisations.stream().map(Localisation::getKey).collect(Collectors.toSet()).size());
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testGetUiConfig() throws Exception {
    mvc.perform(get("/api/v1/ui-config").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.currentEnvironment", is("pallero")))
        .andExpect(jsonPath("$.sourceEnvironments", is(List.of("untuva", "hahtuva", "sade"))));
  }

  @WithMockUser("1.2.246.562.24.00000000001")
  @Test
  public void testLocalisationUpdate() throws Exception {
    mvc.perform(
            post("/api/v1/localisation/update")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        List.of(
                            new Localisation(null, "example", "key1", "fi", "Avain 1"),
                            new Localisation(null, "example", "key2", "fi", "Avain 2")))))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.created", is(2)));
  }

  @Test
  public void testLocalisationUpdateRequiresAuthentication() throws Exception {
    mvc.perform(
            post("/api/v1/localisation/update")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsBytes(
                        List.of(
                            new Localisation(null, "example", "key1", "fi", "Avain 1"),
                            new Localisation(null, "example", "key2", "fi", "Avain 2")))))
        .andExpect(status().isForbidden());
  }

  @Test
  public void testGetLocalisationInTolgeeFormat() throws Exception {
    final MvcResult result =
        mvc.perform(get("/tolgee/example/fi.json").accept(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(status().is2xxSuccessful())
            .andExpect(header().string("Cache-Control", "max-age=600, public"))
            .andExpect(header().string("Last-Modified", notNullValue()))
            .andExpect(header().string("eTag", notNullValue()))
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andReturn();
    final String cacheControl = result.getResponse().getHeader("Cache-Control");
    final String eTag = result.getResponse().getHeader("eTag");
    final String lastModified = result.getResponse().getHeader("Last-Modified");
    mvc.perform(
            get("/tolgee/example/fi.json")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .header("If-None-Match", eTag))
        .andExpect(status().is(304))
        .andExpect(header().string("Cache-Control", cacheControl))
        .andExpect(header().string("ETag", eTag))
        .andExpect(header().string("Last-Modified", lastModified));
    mvc.perform(
            get("/tolgee/example/fi.json")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .header("If-Modified-Since", lastModified))
        .andExpect(status().is(304))
        .andExpect(header().string("Cache-Control", cacheControl))
        .andExpect(header().string("ETag", eTag))
        .andExpect(header().string("Last-Modified", lastModified));
    mvc.perform(
            get("/tolgee/example/fi.json")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .header("If-Modified-Since", lastModified)
                .header("If-None-Match", eTag))
        .andExpect(status().is(304))
        .andExpect(header().string("Cache-Control", cacheControl))
        .andExpect(header().string("ETag", eTag))
        .andExpect(header().string("Last-Modified", lastModified));
  }

  @Test
  public void testGetRootLocalisationInTolgeeFormat() throws Exception {
    mvc.perform(get("/tolgee/fi.json").accept(MediaType.APPLICATION_OCTET_STREAM))
        .andExpect(status().is2xxSuccessful())
        .andExpect(header().string("Cache-Control", "max-age=600, public"))
        .andExpect(header().string("Last-Modified", notNullValue()))
        .andExpect(header().string("eTag", notNullValue()))
        .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));
  }

  LocalisationOverride addLocalisationOverride(
      final String namespace, final String key, final String locale, final String value)
      throws Exception {
    final MvcResult result =
        mvc.perform(
                post("/api/v1/override")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsBytes(
                            mapOf(
                                ImmutablePair.of("namespace", namespace),
                                ImmutablePair.of("key", key),
                                ImmutablePair.of("locale", locale),
                                ImmutablePair.of("value", value)))))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
    return objectMapper.readValue(
        result.getResponse().getContentAsByteArray(), localisationOverrideType);
  }

  @SafeVarargs
  private <T> Set<T> setOf(final T... values) {
    return Arrays.stream(values).collect(Collectors.toSet());
  }

  @SafeVarargs
  private <K, V> Map<K, V> mapOf(final ImmutablePair<K, V>... pairs) {
    final Map<K, V> map = new HashMap<>();
    for (final ImmutablePair<K, V> pair : pairs) {
      map.put(pair.getLeft(), pair.getRight());
    }
    return map;
  }

  final TypeReference<List<Localisation>> listOfLocalisations = new TypeReference<>() {};
  final TypeReference<List<LocalisationOverride>> listOfLocalisationOverrides =
      new TypeReference<>() {};
  final TypeReference<Set<String>> setOfStrings = new TypeReference<>() {};
  final TypeReference<LocalisationOverride> localisationOverrideType = new TypeReference<>() {};
}

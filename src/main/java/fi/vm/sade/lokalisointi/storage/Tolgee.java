package fi.vm.sade.lokalisointi.storage;

import fi.vm.sade.lokalisointi.model.Localisation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Repository
public class Tolgee {
  private static final Logger LOG = LoggerFactory.getLogger(Tolgee.class);
  private static final String API_KEY = "X-API-Key";

  @Value("${tolgee.baseurl}")
  private String baseUrl;

  @Value("${tolgee.apikey:none}")
  private String apiKey;

  @Value("${tolgee.projectid}")
  private String projectId;

  private final RestClient.Builder restClientBuilder;

  public Tolgee() {
    this.restClientBuilder =
        RestClient.builder().requestFactory(new HttpComponentsClientHttpRequestFactory());
  }

  public boolean importKey(final Localisation localisation) {
    final RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
    try {
      restClient
          .post()
          .uri(String.format("/v2/projects/%s/keys/import-resolvable", projectId))
          .body(
              Map.of(
                  "keys",
                  Collections.singletonList(
                      Map.of(
                          "name",
                          localisation.getKey(),
                          "namespace",
                          localisation.getNamespace(),
                          "translations",
                          Map.of(
                              localisation.getLocale(),
                              Map.of("text", localisation.getValue(), "resolution", "NEW"))))))
          .contentType(APPLICATION_JSON)
          .accept(APPLICATION_JSON)
          .header(API_KEY, apiKey)
          .retrieve()
          .toEntity(String.class);
      return true;
    } catch (final HttpClientErrorException e) {
      LOG.warn(
          "Localisation {} was not imported to Tolgee: ({}) {}",
          localisation,
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return false;
    }
  }
}

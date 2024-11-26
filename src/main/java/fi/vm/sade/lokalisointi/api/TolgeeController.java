package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.storage.S3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionException;

@Tag(name = "tolgee", description = "Get localisations in Tolgee format")
@RestController
@RequestMapping("/tolgee")
public class TolgeeController extends ControllerBase {
  private static final Logger LOG = LoggerFactory.getLogger(TolgeeController.class);
  private final S3 s3;

  @Value("${lokalisointi.public-cache-max-age-minutes:5}")
  private Integer cacheMaxAgeMinutes;

  @Autowired
  public TolgeeController(final S3 s3) {
    this.s3 = s3;
  }

  @Operation(
      summary = "Get localisations",
      description = "Get localisations that have no namespace",
      responses = {
        @ApiResponse(responseCode = "200", description = "A localisation file"),
        @ApiResponse(responseCode = "304", description = "Not modified"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
      })
  @GetMapping("/{slug}/{locale}.json")
  public ResponseEntity<StreamingResponseBody> rootLocalisation(
      @Parameter(example = "2dfce2b80ca92938dacc631f231044cc") @PathVariable("slug")
          final String slug,
      @Parameter(example = "fi") @PathVariable("locale") final String locale,
      @RequestHeader(value = "If-None-Match", required = false) final String ifNoneMatch,
      @RequestHeader(value = "If-Modified-Since", required = false) final Instant ifModifiedSince) {
    return getResponse(slug, null, locale, ifNoneMatch, ifModifiedSince);
  }

  @Operation(
      summary = "Get localisations with namespace",
      description = "Get localisations with specific namespace",
      responses = {
        @ApiResponse(responseCode = "200", description = "A localisation file"),
        @ApiResponse(responseCode = "304", description = "Not modified"),
        @ApiResponse(responseCode = "400", description = "Bad request")
      })
  @GetMapping("/{slug}/{namespace}/{locale}.json")
  public ResponseEntity<StreamingResponseBody> namespaceLocalisation(
      @Parameter(example = "2dfce2b80ca92938dacc631f231044cc") @PathVariable("slug")
          final String slug,
      @Parameter(example = "virkailijaraamit") @PathVariable("namespace") final String namespace,
      @Parameter(example = "fi") @PathVariable("locale") final String locale,
      @RequestHeader(value = "If-None-Match", required = false) final String ifNoneMatch,
      @RequestHeader(value = "If-Modified-Since", required = false) final Instant ifModifiedSince) {
    return getResponse(slug, namespace, locale, ifNoneMatch, ifModifiedSince);
  }

  private ResponseEntity<StreamingResponseBody> getResponse(
      final String slug,
      final String namespace,
      final String locale,
      final String ifNoneMatch,
      final Instant ifModifiedSince) {
    try {
      final ResponseInputStream<GetObjectResponse> response =
          s3.getLocalisationFile(slug, namespace, locale, ifNoneMatch, ifModifiedSince);
      final GetObjectResponse metadata = response.response();
      // FIXME lisää yliajot, vai tarvitaanko niitä tolgeen formaatin kanssa?
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .header(
              "Content-Disposition",
              """
            attachment; filename=%s.json"""
                  .formatted(locale))
          .contentLength(metadata.contentLength())
          .lastModified(metadata.lastModified())
          .eTag(metadata.eTag())
          .cacheControl(
              CacheControl.maxAge(Duration.of(cacheMaxAgeMinutes, ChronoUnit.MINUTES))
                  .cachePublic())
          .body(outputStream -> IOUtils.copy(response, outputStream));
    } catch (final CompletionException e) {
      if (e.getCause() instanceof NoSuchKeyException) {
        return ResponseEntity.notFound().build();
      }
      if (e.getCause() instanceof S3Exception && ((S3Exception) e.getCause()).statusCode() == 304) {
        final HeadObjectResponse headObjectResponse =
            s3.getLocalisationFileHead(slug, namespace, locale);
        return ResponseEntity.status(304)
            // headers required by spec https://datatracker.ietf.org/doc/html/rfc7232#section-4.1
            .lastModified(headObjectResponse.lastModified())
            .eTag(headObjectResponse.eTag())
            .cacheControl(
                CacheControl.maxAge(Duration.of(cacheMaxAgeMinutes, ChronoUnit.MINUTES))
                    .cachePublic())
            .build();
      }
      LOG.warn("Could not return localisation file", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}

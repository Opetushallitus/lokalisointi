package fi.vm.sade.lokalisointi.storage;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class S3Tests {
  @Test
  public void testRetries() {
    final ExtendedDokumenttipalvelu dokumenttipalvelu = mock(ExtendedDokumenttipalvelu.class);
    final S3 s3 = spy(new S3(dokumenttipalvelu));

    when(dokumenttipalvelu.get("t-lokalisointi/abcd/example/fi.json"))
        .thenThrow(
            new CompletionException(
                RetryableException.builder().message("test exception").build()));

    try {
      s3.transformToLocalisationStream(
          new ObjectMetadata(
              "t-lokalisointi/abcd/example/fi.json",
              null,
              Collections.singleton("t-lokalisointi"),
              Instant.now(),
              0L,
              "foo"));
    } catch (final RuntimeException e) {
      assertEquals(
          "java.lang.RuntimeException: Could not retrieve object with key t-lokalisointi/abcd/example/fi.json",
          e.getMessage());
    }
    verify(s3, times(3)).getS3Object("t-lokalisointi/abcd/example/fi.json");
  }

  @Test
  public void testNonRetriableError() {
    final ExtendedDokumenttipalvelu dokumenttipalvelu = mock(ExtendedDokumenttipalvelu.class);
    final S3 s3 = spy(new S3(dokumenttipalvelu));

    when(dokumenttipalvelu.get("t-lokalisointi/abcd/example/fi.json"))
        .thenThrow(
            new CompletionException(
                NonRetryableException.builder().message("test exception").build()));

    try {
      s3.transformToLocalisationStream(
          new ObjectMetadata(
              "t-lokalisointi/abcd/example/fi.json",
              null,
              Collections.singleton("t-lokalisointi"),
              Instant.now(),
              0L,
              "foo"));
    } catch (final RuntimeException e) {
      assertEquals(
          "java.util.concurrent.CompletionException: software.amazon.awssdk.core.exception.NonRetryableException: test exception",
          e.getMessage());
    }
    verify(s3, times(1)).getS3Object("t-lokalisointi/abcd/example/fi.json");
  }
}

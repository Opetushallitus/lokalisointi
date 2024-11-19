package fi.vm.sade.lokalisointi.storage;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.internal.async.InputStreamResponseTransformer;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Instant;

/**
 * Extending dokumenttipalvelu to get more metadata from S3 object, most likely for a short period
 * of time.
 */
public class ExtendedDokumenttipalvelu extends Dokumenttipalvelu {
  private final String bucketName;

  public ExtendedDokumenttipalvelu(final String awsRegion, final String bucketName) {
    super(awsRegion, bucketName);
    this.bucketName = bucketName;
  }

  public ResponseInputStream<GetObjectResponse> getObject(
      final String key, final String ifNoneMatch, final Instant ifModifiedSince) {
    return getClient()
        .getObject(
            GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .ifNoneMatch(ifNoneMatch)
                .ifModifiedSince(ifModifiedSince)
                .build(),
            new InputStreamResponseTransformer<>())
        .join();
  }

  public HeadObjectResponse getHead(final String key) {
    return getClient()
        .headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build())
        .join();
  }
}

package fi.vm.sade.lokalisointi.storage;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.internal.async.InputStreamResponseTransformer;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Instant;
import java.util.Collection;

/**
 * Extending dokumenttipalvelu to get more metadata from S3 object, most likely for a short period
 * of time.
 */
public class ExtendedDokumenttipalvelu extends Dokumenttipalvelu {
  private static final Logger LOG = LoggerFactory.getLogger(ExtendedDokumenttipalvelu.class);
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

  @Cacheable("find")
  public Collection<ObjectMetadata> cachedFind(final Collection<String> terms) {
    return find(terms);
  }

  @CacheEvict(value = "find", allEntries = true)
  @Scheduled(fixedRateString = "${lokalisointi.find-cache-ttl-ms}")
  public void emptyFindCache() {
    LOG.info("Emptying find cache");
  }
}

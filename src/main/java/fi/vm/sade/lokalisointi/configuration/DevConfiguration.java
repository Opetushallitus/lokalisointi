package fi.vm.sade.lokalisointi.configuration;

import fi.vm.sade.lokalisointi.storage.ExtendedDokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.catalina.valves.rewrite.RewriteValve;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.Duration;

import static fi.vm.sade.lokalisointi.storage.S3.LOKALISOINTI_TAG;

/** Changes S3 endpoint to localstack, adds example localisation files to S3 bucket. */
@Configuration
@Profile("dev")
public class DevConfiguration implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  private static final Logger LOG = LoggerFactory.getLogger(DevConfiguration.class);
  private static final String BUCKET_NAME = "opintopolku-local-dokumenttipalvelu";
  private static final String BUCKET_REGION = "eu-west-1";
  private static final String DEV_ACCESS_KEY_ID = "LSIAQAAAAAAVNCBMPNSG";
  private static final String DEV_SECRET_KEY = "test";

  @Value("${tolgee.slug}")
  private String tolgeeSlug;

  /** Redirect calls to /virkailija-raamit to QA when running with dev profile */
  @Override
  public void customize(final TomcatServletWebServerFactory tomcat) {
    try {
      // Using Tomcat's engine valve (outside Spring container) for rewrites, so need to create
      // config directory manually
      final File tempDirectory = Files.createTempDirectory("tomcat").toFile();
      tempDirectory.deleteOnExit();
      new File(tempDirectory, "conf/Tomcat").mkdirs();
      tomcat.setBaseDirectory(tempDirectory);
      final File target =
          new File(
              """
          %s/conf/Tomcat/rewrite.config"""
                  .formatted(tempDirectory.getAbsolutePath()));
      LOG.info("Copying rewrite.config to: {}", target);
      IOUtils.copy(
          new FileInputStream("src/test/resources/rewrite.config"), new FileOutputStream(target));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    final RewriteValve valve = new RewriteValve();
    LOG.info("Adding rewrite valve: {}", valve);
    valve.setAsyncSupported(true);
    tomcat.addEngineValves(valve);
  }

  @Bean
  public ExtendedDokumenttipalvelu dokumenttipalvelu()
      throws FileNotFoundException, URISyntaxException {
    final URI s3endpoint = new URI("https://localhost.localstack.cloud:4567");
    LOG.info("Using dev s3endpoint: {}", s3endpoint);
    final S3AsyncClientBuilder clientBuilder =
        S3AsyncClient.builder()
            .forcePathStyle(true)
            .endpointOverride(s3endpoint)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(DEV_ACCESS_KEY_ID, DEV_SECRET_KEY)))
            .region(Region.of(BUCKET_REGION))
            .httpClientBuilder(
                NettyNioAsyncHttpClient.builder()
                    .tlsTrustManagersProvider(
                        InsecureTrustManagerFactory.INSTANCE::getTrustManagers)
                    .connectionTimeout(Duration.ofSeconds(60))
                    .maxConcurrency(100));
    final ExtendedDokumenttipalvelu dokumenttipalvelu =
        new ExtendedDokumenttipalvelu("eu-west-1", BUCKET_NAME) {
          @Override
          public S3AsyncClient getClient() {
            return clientBuilder.build();
          }

          @Override
          public S3Presigner getPresigner() {
            return S3Presigner.builder()
                .region(Region.of(BUCKET_REGION))
                .endpointOverride(s3endpoint)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(DEV_ACCESS_KEY_ID, DEV_SECRET_KEY)))
                .build();
          }
        };

    addLocalisationFiles(dokumenttipalvelu, clientBuilder, BUCKET_NAME, tolgeeSlug);

    return dokumenttipalvelu;
  }

  public static void addLocalisationFiles(
      final Dokumenttipalvelu dokumenttipalvelu,
      final S3AsyncClientBuilder clientBuilder,
      final String bucketName,
      final String slug)
      throws FileNotFoundException {
    // recreate bucket
    try (final S3AsyncClient client = clientBuilder.build()) {
      try {
        LOG.info("Recreating bucket {}", bucketName);
        client
            .listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
            .join()
            .contents()
            .forEach(
                o -> {
                  client
                      .deleteObject(
                          DeleteObjectRequest.builder().bucket(bucketName).key(o.key()).build())
                      .join();
                });
        client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join();
      } catch (final Exception e) {
        LOG.warn(e.getMessage());
      }
      client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).join();
    }
    // add example localisation files
    for (final File fileHandle :
        FileUtils.listFilesAndDirs(
            new File("src/test/resources/localisations/"),
            new SuffixFileFilter(".json"),
            DirectoryFileFilter.INSTANCE)) {
      if (fileHandle.isDirectory()) {
        final File[] localeFiles = fileHandle.listFiles();
        if (localeFiles != null) {
          for (final File localeFile : localeFiles) {
            if (localeFile.isFile()) {
              if (fileHandle.getName().equals("localisations")) {
                LOG.info("Adding root localization file: {}", localeFile.getName());
                dokumenttipalvelu
                    .putObject(
                        String.format("t-%s/%s/%s", LOKALISOINTI_TAG, slug, localeFile.getName()),
                        localeFile.getName(),
                        "application/json",
                        new FileInputStream(localeFile))
                    .join();
              } else {
                LOG.info(
                    "Adding localization file: {}/{}", fileHandle.getName(), localeFile.getName());
                dokumenttipalvelu
                    .putObject(
                        String.format(
                            "t-%s/%s/%s/%s",
                            LOKALISOINTI_TAG, slug, fileHandle.getName(), localeFile.getName()),
                        localeFile.getName(),
                        "application/json",
                        new FileInputStream(localeFile))
                    .join();
              }
            }
          }
        }
      }
    }
  }
}

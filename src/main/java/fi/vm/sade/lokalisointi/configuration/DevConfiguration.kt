package fi.vm.sade.lokalisointi.configuration

import fi.vm.sade.lokalisointi.storage.S3
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.DirectoryFileFilter
import org.apache.commons.io.filefilter.RegexFileFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.URI
import java.net.URISyntaxException
import java.time.Duration
import java.util.function.Consumer
import javax.net.ssl.SSLException

/** Changes S3 endpoint to localstack, adds example localisation files to S3 bucket.  */
@Configuration
@Profile("dev")
class DevConfiguration {
    @Bean
    fun dokumenttipalvelu(): Dokumenttipalvelu {
        val s3endpoint = URI("https://localhost.localstack.cloud:4567")
        LOG.info("Using dev s3endpoint: {}", s3endpoint)
        val clientBuilder =
            S3AsyncClient.builder()
                .forcePathStyle(true)
                .endpointOverride(s3endpoint)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(DEV_ACCESS_KEY_ID, DEV_SECRET_KEY)
                    )
                )
                .region(Region.of(BUCKET_REGION))
                .httpClientBuilder(
                    NettyNioAsyncHttpClient.builder()
                        .tlsTrustManagersProvider { InsecureTrustManagerFactory.INSTANCE.trustManagers }
                        .connectionTimeout(Duration.ofSeconds(60))
                        .maxConcurrency(100))
        val dokumenttipalvelu: Dokumenttipalvelu =
            object : Dokumenttipalvelu("eu-west-1", BUCKET_NAME) {
                public override fun getClient(): S3AsyncClient {
                    return clientBuilder.build()
                }

                public override fun getPresigner(): S3Presigner {
                    return S3Presigner.builder()
                        .region(Region.of(BUCKET_REGION))
                        .endpointOverride(s3endpoint)
                        .credentialsProvider(
                            StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(DEV_ACCESS_KEY_ID, DEV_SECRET_KEY)
                            )
                        )
                        .build()
                }
            }

        dokumenttipalvelu.listObjectsMaxKeys = 5

        addLocalisationFiles(dokumenttipalvelu, clientBuilder, BUCKET_NAME)

        return dokumenttipalvelu
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DevConfiguration::class.java)
        private const val BUCKET_NAME = "opintopolku-local-dokumenttipalvelu"
        private const val BUCKET_REGION = "eu-west-1"
        private const val DEV_ACCESS_KEY_ID = "LSIAQAAAAAAVNCBMPNSG"
        private const val DEV_SECRET_KEY = "test"

        fun addLocalisationFiles(
            dokumenttipalvelu: Dokumenttipalvelu,
            clientBuilder: S3AsyncClientBuilder,
            bucketName: String?
        ) {
            // recreate bucket
            clientBuilder.build().use { client ->
                try {
                    LOG.info("Recreating bucket {}", bucketName)
                    client
                        .listObjects(ListObjectsRequest.builder().bucket(bucketName).build())
                        .join()
                        .contents()
                        .forEach(
                            Consumer { o: S3Object ->
                                client
                                    .deleteObject(
                                        DeleteObjectRequest.builder().bucket(bucketName).key(o.key()).build()
                                    )
                                    .join()
                            })
                    client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build()).join()
                } catch (e: Exception) {
                    LOG.warn(e.message)
                }
                client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build()).join()
            }
            // add example localisation files
            for (namespaceDenotingDirectoryHandle in FileUtils.listFilesAndDirs(
                File("src/test/resources/localisations"),
                RegexFileFilter("^(.*?)"),
                DirectoryFileFilter.INSTANCE
            )) {
                if (namespaceDenotingDirectoryHandle.isDirectory) {
                    val localeFiles = namespaceDenotingDirectoryHandle.listFiles()
                    if (localeFiles != null) {
                        for (localeFile in localeFiles) {
                            if (localeFile.isFile) {
                                LOG.info(
                                    "Adding localization file: {}/{}",
                                    namespaceDenotingDirectoryHandle.name,
                                    localeFile.name
                                )
                                dokumenttipalvelu
                                    .putObject(
                                        String.format(
                                            "t-%s/%s/%s",
                                            S3.LOKALISOINTI_TAG,
                                            namespaceDenotingDirectoryHandle.name,
                                            localeFile.name
                                        ),
                                        localeFile.name,
                                        "application/json",
                                        FileInputStream(localeFile)
                                    )
                                    .join()
                            }
                        }
                    }
                }
            }
        }
    }
}

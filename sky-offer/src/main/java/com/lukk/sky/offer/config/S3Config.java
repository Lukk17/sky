package com.lukk.sky.offer.config;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
@Slf4j
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties props) {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
        );

        return S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .forcePathStyle(props.pathStyleAccess())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties props) {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
        );

        return S3Presigner.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(credentials)
                .build();
    }

    @Bean
    public BucketInitializer bucketInitializer(S3Client s3Client, S3Properties props) {
        return new BucketInitializer(s3Client, props.bucket());
    }

    public static class BucketInitializer {

        private final S3Client s3Client;
        private final String bucket;

        public BucketInitializer(S3Client s3Client, String bucket) {
            this.s3Client = s3Client;
            this.bucket = bucket;
        }

        @EventListener(ApplicationReadyEvent.class)
        public void ensureBucketExists() {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                log.info("S3 bucket '{}' already exists.", bucket);
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Created S3 bucket '{}'.", bucket);
            } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
                log.warn("Cannot reach S3 endpoint for bucket '{}' — MinIO may be unavailable. " +
                        "Photo upload will fail until MinIO is reachable. Cause: {}", bucket, e.getMessage());
            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                log.warn("S3 error while verifying bucket '{}' (status {}). " +
                                "Check credentials and endpoint configuration. Cause: {}",
                        bucket, e.statusCode(), e.getMessage());
            }
        }
    }
}

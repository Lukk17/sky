package com.lukk.sky.offer;

import com.lukk.sky.offer.domain.ports.storage.PhotoStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.mockito.Mockito.mock;

/**
 * Test-profile stub: replaces all MinIO/S3 beans so tests run without a real MinIO instance.
 *
 * <p>S3Client and S3Presigner are bare Mockito mocks (not wired to any endpoint) so
 * BucketInitializer does not attempt a real TCP connection on startup.  PhotoStorage is
 * a no-op stub that returns a fixed URL, which lets the existing integration tests
 * assert offer fields without hitting S3.
 */
@TestConfiguration
public class TestS3Config {

    @Bean
    @Primary
    public S3Client testS3Client() {
        return mock(S3Client.class);
    }

    @Bean
    @Primary
    public S3Presigner testS3Presigner() {
        return mock(S3Presigner.class);
    }

    @Bean
    @Primary
    public PhotoStorage testPhotoStorage() {
        return new PhotoStorage() {
            @Override
            public String upload(byte[] content, String contentType, String filename) {
                return "offers/test-uuid-" + filename;
            }

            @Override
            public String presignedUrl(String key) {
                if (key == null || key.isBlank()) {
                    return null;
                }
                return "http://localhost:9000/sky-offers-test/" + key + "?X-Amz-Signature=test";
            }
        };
    }
}

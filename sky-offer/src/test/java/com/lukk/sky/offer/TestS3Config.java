package com.lukk.sky.offer;

import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;

import static org.mockito.Mockito.mock;

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
            public String upload(InputStream content, long contentLength, String contentType, String filename) {
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

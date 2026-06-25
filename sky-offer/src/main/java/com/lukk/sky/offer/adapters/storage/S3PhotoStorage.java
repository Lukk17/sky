package com.lukk.sky.offer.adapters.storage;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.ports.storage.PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3PhotoStorage implements PhotoStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String upload(InputStream content, long contentLength, String contentType, String filename) {
        String sanitized = sanitize(filename);
        String key = "offers/" + UUID.randomUUID() + "-" + sanitized;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .contentLength(contentLength)
                            .build(),
                    RequestBody.fromInputStream(content, contentLength)
            );
        } catch (SdkException ex) {
            log.error("S3 upload failed key={} bucket={}", key, s3Properties.bucket(), ex);
            throw new OfferException("Photo upload failed: " + ex.getMessage());
        }

        log.info("Uploaded photo key={} bucket={}", key, s3Properties.bucket());

        return key;
    }

    @Override
    public String presignedUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        try {
            var presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(s3Properties.presignTtl())
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(s3Properties.bucket())
                            .key(key)
                            .build())
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException ex) {
            log.error("S3 presign failed key={}", key, ex);
            throw new OfferException("Presign URL generation failed: " + ex.getMessage());
        }
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo";
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

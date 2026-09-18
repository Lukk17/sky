package com.lukk.sky.offer.adapters.outbound.storage;

import com.lukk.sky.offer.config.propertyBind.S3Properties;
import com.lukk.sky.offer.domain.exception.PhotoStorageBadResponseException;
import com.lukk.sky.offer.domain.exception.PhotoStorageException;
import com.lukk.sky.offer.domain.exception.PhotoStorageUnavailableException;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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

    private static final int FIRST_SERVER_ERROR_STATUS = 500;
    private static final int TOO_MANY_REQUESTS_STATUS = 429;

    private static final String UNAVAILABLE_SUFFIX = " The object store is unavailable.";
    private static final String REFUSED_SUFFIX = " The object store refused the request.";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String upload(UUID offerId, InputStream content, long contentLength, String contentType, String filename) {
        String sanitized = sanitize(filename);
        String key = offerKeyPrefix(offerId) + UUID.randomUUID() + "-" + sanitized;

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
            log.error("photo_storage_call_failed operation=upload key={} bucket={} storeStatus={}",
                    key, s3Properties.bucket(), storeStatus(ex), ex);

            throw storageFailure("Photo upload failed.", ex);
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
            log.error("photo_storage_call_failed operation=presign key={} storeStatus={}",
                    key, storeStatus(ex), ex);

            throw storageFailure("Photo address could not be signed.", ex);
        }
    }

    @Override
    public void delete(UUID offerId, String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        if (!key.startsWith(offerKeyPrefix(offerId))) {
            log.warn("photo_delete_skipped key={} offerId={} reason=key outside the offer namespace", key, offerId);

            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build());
        } catch (SdkException ex) {
            log.error("photo_storage_call_failed operation=delete key={} bucket={} storeStatus={}",
                    key, s3Properties.bucket(), storeStatus(ex), ex);

            throw storageFailure("Photo delete failed.", ex);
        }

        log.info("Deleted photo key={} bucket={}", key, s3Properties.bucket());
    }

    private static PhotoStorageException storageFailure(String operationMessage, SdkException cause) {
        if (cause instanceof SdkServiceException answered && !isTransient(answered)) {
            return new PhotoStorageBadResponseException(operationMessage + REFUSED_SUFFIX, cause);
        }

        return new PhotoStorageUnavailableException(operationMessage + UNAVAILABLE_SUFFIX, cause);
    }

    private static boolean isTransient(SdkServiceException answered) {
        return answered.statusCode() >= FIRST_SERVER_ERROR_STATUS
                || answered.statusCode() == TOO_MANY_REQUESTS_STATUS;
    }

    private static String storeStatus(SdkException ex) {
        if (ex instanceof SdkServiceException answered) {
            return String.valueOf(answered.statusCode());
        }

        return "none";
    }

    private static String offerKeyPrefix(UUID offerId) {
        return "offers/" + offerId + "/";
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "photo";
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

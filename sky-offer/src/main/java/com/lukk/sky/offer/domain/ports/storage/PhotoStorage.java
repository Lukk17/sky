package com.lukk.sky.offer.domain.ports.storage;

/**
 * Domain port for photo binary storage.
 *
 * <p>Upload returns the object key (not a URL). A separate presignedUrl call
 * turns a stored key into a time-limited GET URL. The two-step design keeps the
 * domain free of endpoint details and lets callers decide when to resolve URLs.
 */
public interface PhotoStorage {

    /**
     * Stores the given bytes and returns the object key under which they were saved.
     *
     * @param content     raw file bytes
     * @param contentType MIME type (e.g. {@code "image/jpeg"})
     * @param filename    original filename used to build the key suffix
     * @return the stored object key
     */
    String upload(byte[] content, String contentType, String filename);

    /**
     * Returns a time-limited presigned GET URL for the given object key, or
     * {@code null} if the key is blank.
     *
     * @param key object key returned by {@link #upload}
     * @return presigned URL string, or {@code null} when key is blank
     */
    String presignedUrl(String key);
}

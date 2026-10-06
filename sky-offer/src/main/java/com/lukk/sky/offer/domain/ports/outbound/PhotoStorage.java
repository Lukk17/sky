package com.lukk.sky.offer.domain.ports.outbound;

import java.io.InputStream;
import java.util.UUID;

public interface PhotoStorage {

    String upload(UUID offerId, InputStream content, long contentLength, String contentType, String filename);

    String presignedUrl(String key);

    void delete(UUID offerId, String key);
}

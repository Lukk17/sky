package com.lukk.sky.offer.domain.ports.outbound;

import java.io.InputStream;

public interface PhotoStorage {

    String upload(InputStream content, long contentLength, String contentType, String filename);

    String presignedUrl(String key);
}

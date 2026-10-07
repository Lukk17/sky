package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.offer.domain.exception.GalleryLimitExceededException;
import com.lukk.sky.offer.domain.exception.OfferException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PhotoUploadSupport {

    static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;

    private static final String IMAGE_WEBP_VALUE = "image/webp";
    private static final Set<String> SNIFFED_IMAGE_TYPES =
            Set.of(MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_GIF_VALUE);

    private static final int WEBP_HEADER_LENGTH = 12;
    private static final int WEBP_MARKER_OFFSET = 8;
    private static final byte[] WEBP_RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};

    static void rejectWhenTooLarge(MultipartFile file) {
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new GalleryLimitExceededException(
                    "Uploaded file must not exceed 5 MB.");
        }
    }

    static String detectContentType(MultipartFile file) throws IOException {
        try (InputStream content = new BufferedInputStream(file.getInputStream())) {
            if (isWebP(content)) {
                return IMAGE_WEBP_VALUE;
            }

            String sniffedContentType = URLConnection.guessContentTypeFromStream(content);

            if (sniffedContentType != null && SNIFFED_IMAGE_TYPES.contains(sniffedContentType)) {
                return sniffedContentType;
            }
        }

        throw new OfferException(
                "Unsupported image format. Allowed types: JPEG, PNG, GIF, WebP.");
    }

    private static boolean isWebP(InputStream markSupportingContent) throws IOException {
        byte[] header = new byte[WEBP_HEADER_LENGTH];

        markSupportingContent.mark(WEBP_HEADER_LENGTH);
        int read = markSupportingContent.readNBytes(header, 0, WEBP_HEADER_LENGTH);
        markSupportingContent.reset();

        return read == WEBP_HEADER_LENGTH
                && Arrays.equals(header, 0, WEBP_RIFF.length, WEBP_RIFF, 0, WEBP_RIFF.length)
                && Arrays.equals(header, WEBP_MARKER_OFFSET, WEBP_HEADER_LENGTH,
                WEBP_MARKER, 0, WEBP_MARKER.length);
    }
}

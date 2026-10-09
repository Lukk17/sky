package com.lukk.sky.offer.domain.ports.inbound;

import java.util.UUID;

public record GalleryPhotoView(UUID id, int position, String url, boolean main) {
}

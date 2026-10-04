package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.GalleryPhotoNotFoundException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.model.OfferPhoto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class GalleryOrdering {

    private GalleryOrdering() {
    }

    static List<OfferPhoto> moved(List<OfferPhoto> photos, UUID photoId, int newPosition) {
        if (newPosition < 0 || newPosition >= photos.size()) {
            throw new OfferException("Invalid position.");
        }
        OfferPhoto target = photos.stream()
                .filter(photo -> photo.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new GalleryPhotoNotFoundException("Photo not found."));
        List<OfferPhoto> ordered = new ArrayList<>(photos);
        ordered.remove(target);
        ordered.add(newPosition, target);
        renumber(ordered);
        return ordered;
    }

    static void renumber(List<OfferPhoto> photos) {
        for (int index = 0; index < photos.size(); index++) {
            photos.get(index).setPosition(index);
        }
    }
}

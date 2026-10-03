package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.model.OfferPhoto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class GalleryOrdering {

    private GalleryOrdering() {
    }

    static List<OfferPhoto> moved(List<OfferPhoto> photos, UUID photoId, int newPosition) {
        OfferPhoto target = photos.stream()
                .filter(photo -> photo.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new com.lukk.sky.offer.domain.exception.OfferNotFoundException("Photo not found."));
        List<OfferPhoto> ordered = new ArrayList<>(photos);
        ordered.remove(target);
        ordered.add(newPosition, target);
        renumber(ordered);
        return ordered;
    }

    static void renumber(List<OfferPhoto> photos) {
        List<OfferPhoto> ordered = new ArrayList<>(photos);
        for (OfferPhoto photo : ordered) {
            photo.setPosition(-(photo.getPosition() + 1));
        }
        for (int index = 0; index < ordered.size(); index++) {
            ordered.get(index).setPosition(index);
        }
    }
}

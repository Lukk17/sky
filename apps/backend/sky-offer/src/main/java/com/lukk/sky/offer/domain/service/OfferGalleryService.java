package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.GalleryCoverConflictException;
import com.lukk.sky.offer.domain.exception.GalleryLimitExceededException;
import com.lukk.sky.offer.domain.exception.GalleryPhotoNotFoundException;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageException;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.model.OfferPhoto;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import com.lukk.sky.offer.domain.ports.outbound.OfferPhotoRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
class OfferGalleryService {

    private static final int GALLERY_CAP = 10;

    private final OfferRepository offerRepository;
    private final OfferPhotoRepository offerPhotoRepository;
    private final PhotoStorage photoStorage;

    OfferView uploadGalleryPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                                 String validatedContentType, String filename) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only upload photos for your own offers.");
        List<OfferPhoto> photos = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        if (photos.size() >= GALLERY_CAP) {
            throw new GalleryLimitExceededException("Gallery holds at most " + GALLERY_CAP + " photos.");
        }
        String key = photoStorage.upload(offerId, content, contentLength, validatedContentType, filename);
        OfferPhoto photo = OfferPhoto.builder().offer(offer).position(photos.size())
                .objectKey(key).main(photos.isEmpty()).build();
        try {
            offerPhotoRepository.saveAndFlush(photo);
        } catch (DataIntegrityViolationException ex) {
            throw new GalleryCoverConflictException("Gallery cover was changed concurrently.", ex);
        }
        log.info("Gallery photo uploaded for offer ID: {} key={}", offerId, key);
        return detailedView(offer);
    }

    OfferView deleteGalleryPhoto(UUID offerId, UUID photoId, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only delete photos of your own offers.");
        OfferPhoto photo = offerPhotoRepository.findById(photoId)
                .orElseThrow(() -> new GalleryPhotoNotFoundException("Photo not found."));
        if (!photo.getOffer().getId().equals(offerId)) {
            throw new GalleryPhotoNotFoundException("Photo not found.");
        }
        boolean wasMain = photo.isMain();
        List<OfferPhoto> remaining = new ArrayList<>(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId));
        remaining.removeIf(candidate -> candidate.getId().equals(photoId));
        offerPhotoRepository.delete(photo);
        offerPhotoRepository.flush();
        shiftToTemporaryPositions(remaining);
        offerPhotoRepository.saveAllAndFlush(remaining);
        GalleryOrdering.renumber(remaining);
        if (wasMain && !remaining.isEmpty()) {
            remaining.get(0).setMain(true);
        }
        try {
            offerPhotoRepository.saveAllAndFlush(remaining);
        } catch (DataIntegrityViolationException ex) {
            throw new GalleryCoverConflictException("Gallery cover was changed concurrently.", ex);
        }
        removeStoredPhoto(offerId, photo.getObjectKey());
        return detailedView(offer);
    }

    OfferView reorderGalleryPhoto(UUID offerId, UUID photoId, int newPosition, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only reorder photos of your own offers.");
        List<OfferPhoto> photos = new ArrayList<>(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId));
        List<OfferPhoto> ordered = GalleryOrdering.moved(photos, photoId, newPosition);
        for (OfferPhoto photo : ordered) {
            photo.setMain(false);
        }
        shiftToTemporaryPositions(ordered);
        offerPhotoRepository.saveAllAndFlush(ordered);
        for (int index = 0; index < ordered.size(); index++) {
            ordered.get(index).setPosition(index);
            ordered.get(index).setMain(index == 0);
        }
        offerPhotoRepository.saveAllAndFlush(ordered);
        return detailedView(offer);
    }

    OfferView setGalleryCover(UUID offerId, UUID photoId, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only reorder photos of your own offers.");
        List<OfferPhoto> photos = new ArrayList<>(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId));
        OfferPhoto target = photos.stream()
                .filter(photo -> photo.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new GalleryPhotoNotFoundException("Photo not found."));
        if (target.isMain() && photos.stream().filter(OfferPhoto::isMain).count() == 1) {
            return detailedView(offer);
        }
        try {
            for (OfferPhoto photo : photos) {
                photo.setMain(false);
            }
            offerPhotoRepository.saveAllAndFlush(photos);
            target.setMain(true);
            offerPhotoRepository.saveAllAndFlush(photos);
        } catch (DataIntegrityViolationException ex) {
            throw new GalleryCoverConflictException("Gallery cover was changed concurrently.", ex);
        }
        return detailedView(offer);
    }

    List<OfferPhoto> deleteGalleryForOffer(UUID offerId) {
        List<OfferPhoto> gallery = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        if (!gallery.isEmpty()) {
            offerPhotoRepository.deleteAll(gallery);
        }
        return gallery;
    }

    Offer requireOwnedOffer(UUID offerId, String ownerEmail, String accessDeniedMessage) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new OfferNotFoundException(String.format("Offer with ID: %s not exist.", offerId)));

        if (!offer.getOwnerEmail().equals(ownerEmail)) {
            throw new OfferAccessDeniedException(accessDeniedMessage);
        }

        return offer;
    }

    void removeStoredPhoto(UUID offerId, String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            photoStorage.delete(offerId, key);
        } catch (PhotoStorageException ex) {
            log.warn("photo_delete_failed key={} reason={}", key, ex.getMessage());
        }
    }

    OfferView detailedView(Offer offer) {
        return assembleView(offer, this::galleryWithPresignedUrls);
    }

    OfferView summaryView(Offer offer) {
        return assembleView(offer, this::galleryWithPresignedUrls);
    }

    private OfferView assembleView(Offer offer, Function<Offer, List<GalleryPhotoView>> gallery) {
        List<GalleryPhotoView> photos = gallery.apply(offer);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.stream()
                .filter(GalleryPhotoView::main).findFirst().orElse(photos.get(0)).url();
        String photoUrl = photoAddress(offer);
        if (photoUrl == null) {
            photoUrl = coverPhotoUrl;
        }
        return new OfferView(offer.getId(), offer.getHotelName(), offer.getDescription(),
                offer.getComment(), offer.getPrice(), offer.getOwnerEmail(), offer.getRoomCapacity(),
                offer.getCity(), offer.getCountry(), offer.getExternalPhotoUrl(),
                photoUrl, photos, coverPhotoUrl);
    }

    private List<GalleryPhotoView> galleryWithPresignedUrls(Offer offer) {
        return collectGallery(offer, photo -> galleryPhotoUrl(photo, true));
    }

    private List<GalleryPhotoView> collectGallery(Offer offer, Function<OfferPhoto, String> urlOf) {
        List<OfferPhoto> photos = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offer.getId());
        if (photos.isEmpty()) {
            return new ArrayList<>();
        }
        List<OfferPhoto> ordered = new ArrayList<>(photos);
        ordered.sort(Comparator.comparingInt(OfferPhoto::getPosition));
        List<GalleryPhotoView> result = new ArrayList<>();
        for (OfferPhoto photo : ordered) {
            result.add(new GalleryPhotoView(photo.getId(), photo.getPosition(), urlOf.apply(photo), photo.isMain()));
        }
        return result;
    }

    private String galleryPhotoUrl(OfferPhoto photo, boolean presignGallery) {
        if (photo.getObjectKey() != null && !photo.getObjectKey().isBlank()) {
            return presignGallery ? photoStorage.presignedUrl(photo.getObjectKey()) : null;
        }
        return photo.getExternalUrl();
    }

    private String photoAddress(Offer offer) {
        String key = offer.getPhotoObjectKey();

        if (key == null || key.isBlank()) {
            return offer.getExternalPhotoUrl();
        }

        return photoStorage.presignedUrl(key);
    }

    private void shiftToTemporaryPositions(List<OfferPhoto> photos) {
        int tempOffset = photos.stream().mapToInt(OfferPhoto::getPosition).max().orElse(-1)
                + photos.size() + 1;
        for (OfferPhoto photo : photos) {
            photo.setPosition(photo.getPosition() + tempOffset);
        }
    }
}

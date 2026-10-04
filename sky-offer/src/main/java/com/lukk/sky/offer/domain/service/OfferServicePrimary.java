package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.domain.exception.GalleryCoverConflictException;
import com.lukk.sky.offer.domain.exception.GalleryLimitExceededException;
import com.lukk.sky.offer.domain.exception.GalleryPhotoNotFoundException;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.model.OfferPhoto;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.EditOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import com.lukk.sky.offer.domain.ports.outbound.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.outbound.OfferPhotoRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferSearch;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Primary
public class OfferServicePrimary implements OfferService {

    private final OfferRepository offerRepository;
    private final OfferPhotoRepository offerPhotoRepository;
    private final OfferSearch offerSearch;
    private final EventSourceService eventSourceService;
    private final PhotoStorage photoStorage;
    private final OfferNotificationService offerNotificationService;

    @Override
    @Transactional(readOnly = true)
    public Page<OfferView> getAllOffers(Pageable pageable) {
        log.info("Pulling all offers page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAll(pageable).map(this::toSummaryView);
    }

    @Override
    public OfferView addOffer(CreateOfferCommand command) throws OfferException {
        Offer newOffer = Offer.builder()
                .hotelName(command.hotelName())
                .city(command.city())
                .country(command.country())
                .ownerEmail(command.ownerEmail())
                .description(command.description())
                .comment(command.comment())
                .price(command.price())
                .roomCapacity(command.roomCapacity())
                .externalPhotoUrl(command.externalPhotoUrl())
                .build();

        Offer savedOffer = offerRepository.save(newOffer);

        log.info("Saved offer with ID: {} from user: {}", savedOffer.getId(), savedOffer.getOwnerEmail());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_CREATED);

        OfferView created = toDetailedView(savedOffer);
        offerNotificationService.publishCreated(created, savedOffer.getOwnerEmail());

        return created;
    }

    @Override
    public void deleteOffer(UUID offerID, String userEmail) throws OfferException {
        Offer offerToDelete = offerRepository.findById(offerID)
                .orElseThrow(() -> new OfferNotFoundException("Can't remove non-existing offer!"));

        if (offerToDelete.getOwnerEmail().equals(userEmail)) {
            List<OfferPhoto> gallery =
                    offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerID);
            if (!gallery.isEmpty()) {
                offerPhotoRepository.deleteAll(gallery);
            }
            offerRepository.delete(offerToDelete);

            log.info("Deleted offer with ID: {}", offerToDelete.getId());
            eventSourceService.saveEvent(offerToDelete, EventType.OFFER_DELETED);
            removeStoredPhoto(offerToDelete.getId(), offerToDelete.getPhotoObjectKey());
            for (OfferPhoto photo : gallery) {
                removeStoredPhoto(offerToDelete.getId(), photo.getObjectKey());
            }

            offerNotificationService.publishDeleted(offerID, userEmail);

        } else {
            throw new OfferAccessDeniedException("You can only delete offers you own.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferView> getOwnedOffers(String ownerEmail, Pageable pageable) {
        log.info("Pulling offers which owner is user: {} page={} size={}",
                ownerEmail, pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAllByOwnerEmail(ownerEmail, pageable).map(this::toSummaryView);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferView> searchOffers(String searched, Pageable pageable) {
        log.info("Searching offers for: {}", searched);

        return offerSearch.searchByTerm(searched, pageable).map(this::toSummaryView);
    }

    @Override
    @Transactional(readOnly = true)
    public OfferView getOfferById(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new OfferNotFoundException(
                        String.format("Offer with ID: %s not exist.", offerId)));
        log.info("Found offer with ID: {}", offerId);
        return toDetailedView(offer);
    }

    @Override
    public OfferView editOffer(EditOfferCommand command, String ownerEmail) {
        Offer storedOffer = offerRepository
                .findById(command.id())
                .orElseThrow(() -> new OfferNotFoundException("Offer not found."));

        if (!storedOffer.getOwnerEmail().equals(ownerEmail)) {
            throw new OfferAccessDeniedException("You can only edit offers you own.");
        }

        applyEdit(command, storedOffer);
        Offer savedOffer = offerRepository.save(storedOffer);

        log.info("Offer with ID: {} edited.", savedOffer.getId());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_UPDATED);

        OfferView edited = toDetailedView(savedOffer);
        offerNotificationService.publishEdited(edited, ownerEmail);

        return edited;
    }

    @Override
    @Transactional(readOnly = true)
    public String findOfferOwner(UUID offerId) {
        String ownerEmail = offerRepository
                .findById(offerId)
                .map(Offer::getOwnerEmail)
                .orElseThrow(() -> new OfferNotFoundException(String.format("Offer with ID: %s not exist.", offerId)));

        log.info("Found owner of offer with ID: {}", offerId);

        return ownerEmail;
    }

    @Override
    public OfferView uploadPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                                 String validatedContentType, String filename) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only upload photos for your own offers.");

        String previousKey = offer.getPhotoObjectKey();
        String key = photoStorage.upload(offerId, content, contentLength, validatedContentType, filename);
        offer.setPhotoObjectKey(key);
        Offer saved = offerRepository.save(offer);

        log.info("Photo uploaded for offer ID: {} key={}", offerId, key);

        if (!key.equals(previousKey)) {
            removeStoredPhoto(offerId, previousKey);
        }

        return toDetailedView(saved);
    }

    @Override
    public void deletePhoto(UUID offerId, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only delete photos of your own offers.");

        String key = offer.getPhotoObjectKey();
        offer.setPhotoObjectKey(null);
        offerRepository.save(offer);

        log.info("Photo cleared for offer ID: {} key={}", offerId, key);
        removeStoredPhoto(offerId, key);
    }

    private static final int GALLERY_CAP = 10;

    @Override
    public OfferView uploadGalleryPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
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
        return toDetailedView(offer);
    }

    @Override
    public OfferView deleteGalleryPhoto(UUID offerId, UUID photoId, String ownerEmail) {
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
        return toDetailedView(offer);
    }

    @Override
    public OfferView reorderGalleryPhoto(UUID offerId, UUID photoId, int newPosition, String ownerEmail) {
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
        return toDetailedView(offer);
    }

    private void shiftToTemporaryPositions(List<OfferPhoto> photos) {
        int tempOffset = photos.stream().mapToInt(OfferPhoto::getPosition).max().orElse(-1)
                + photos.size() + 1;
        for (OfferPhoto photo : photos) {
            photo.setPosition(photo.getPosition() + tempOffset);
        }
    }

    @Override
    public OfferView setGalleryCover(UUID offerId, UUID photoId, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only reorder photos of your own offers.");
        List<OfferPhoto> photos = new ArrayList<>(offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId));
        OfferPhoto target = photos.stream()
                .filter(photo -> photo.getId().equals(photoId))
                .findFirst()
                .orElseThrow(() -> new GalleryPhotoNotFoundException("Photo not found."));
        for (OfferPhoto photo : photos) {
            photo.setMain(photo.getId().equals(target.getId()));
        }
        try {
            offerPhotoRepository.saveAllAndFlush(photos);
        } catch (DataIntegrityViolationException ex) {
            throw new GalleryCoverConflictException("Gallery cover was changed concurrently.", ex);
        }
        return toDetailedView(offer);
    }

    private Offer requireOwnedOffer(UUID offerId, String ownerEmail, String accessDeniedMessage) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new OfferNotFoundException(String.format("Offer with ID: %s not exist.", offerId)));

        if (!offer.getOwnerEmail().equals(ownerEmail)) {
            throw new OfferAccessDeniedException(accessDeniedMessage);
        }

        return offer;
    }

    private void removeStoredPhoto(UUID offerId, String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            photoStorage.delete(offerId, key);
        } catch (PhotoStorageException ex) {
            log.warn("photo_delete_failed key={} reason={}", key, ex.getMessage());
        }
    }

    private void applyEdit(EditOfferCommand command, Offer storedOffer) {
        if (command.hotelName() != null) {
            storedOffer.setHotelName(command.hotelName());
        }
        if (command.city() != null) {
            storedOffer.setCity(command.city());
        }
        if (command.country() != null) {
            storedOffer.setCountry(command.country());
        }
        if (command.description() != null) {
            storedOffer.setDescription(command.description());
        }
        if (command.comment() != null) {
            storedOffer.setComment(command.comment());
        }
        if (command.price() != null) {
            storedOffer.setPrice(command.price());
        }
        if (command.roomCapacity() != null) {
            storedOffer.setRoomCapacity(command.roomCapacity());
        }
        if (command.externalPhotoUrl() != null) {
            storedOffer.setExternalPhotoUrl(command.externalPhotoUrl());
        }
    }

    private OfferView toDetailedView(Offer offer) {
        return assembleView(offer, this::galleryWithPresignedUrls);
    }

    private OfferView toSummaryView(Offer offer) {
        return assembleView(offer, this::galleryWithoutPresignedUrls);
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

    private List<GalleryPhotoView> galleryWithoutPresignedUrls(Offer offer) {
        return collectGallery(offer, photo -> galleryPhotoUrl(photo, false));
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
}

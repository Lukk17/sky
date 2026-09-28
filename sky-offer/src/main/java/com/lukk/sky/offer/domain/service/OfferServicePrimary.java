package com.lukk.sky.offer.domain.service;

import com.lukk.sky.offer.adapters.dto.OfferDTO;
import com.lukk.sky.offer.adapters.dto.OfferEditDTO;
import com.lukk.sky.offer.adapters.dto.PhotoDTO;
import com.lukk.sky.offer.domain.exception.GalleryLimitExceededException;
import com.lukk.sky.offer.domain.exception.OfferAccessDeniedException;
import com.lukk.sky.offer.domain.exception.OfferException;
import com.lukk.sky.offer.domain.exception.OfferNotFoundException;
import com.lukk.sky.offer.domain.exception.PhotoStorageException;
import com.lukk.sky.offer.domain.model.EventType;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.model.OfferPhoto;
import com.lukk.sky.offer.domain.ports.inbound.OfferService;
import com.lukk.sky.offer.domain.ports.outbound.OfferNotificationService;
import com.lukk.sky.offer.domain.ports.outbound.OfferPhotoRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferRepository;
import com.lukk.sky.offer.domain.ports.outbound.OfferSearch;
import com.lukk.sky.offer.domain.ports.outbound.PhotoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

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
    public Page<OfferDTO> getAllOffers(Pageable pageable) {
        log.info("Pulling all offers page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAll(pageable).map(this::toDto);
    }

    @Override
    public OfferDTO addOffer(OfferDTO offerDTO) throws OfferException {
        Offer newOffer = offerDTO.toDomain();
        newOffer.setId(null);

        Offer savedOffer = offerRepository.save(newOffer);

        log.info("Saved offer with ID: {} from user: {}", savedOffer.getId(), savedOffer.getOwnerEmail());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_CREATED);

        OfferDTO created = toDto(savedOffer);
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
    public Page<OfferDTO> getOwnedOffers(String ownerEmail, Pageable pageable) {
        log.info("Pulling offers which owner is user: {} page={} size={}",
                ownerEmail, pageable.getPageNumber(), pageable.getPageSize());

        return offerRepository.findAllByOwnerEmail(ownerEmail, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OfferDTO> searchOffers(String searched, Pageable pageable) {
        log.info("Searching offers for: {}", searched);

        return offerSearch.searchByTerm(searched, pageable).map(this::toDto);
    }

    @Override
    public OfferDTO editOffer(OfferEditDTO offerEditDTO, String ownerEmail) {
        Offer storedOffer = offerRepository
                .findById(offerEditDTO.getId())
                .orElseThrow(() -> new OfferNotFoundException("Offer not found."));

        if (!storedOffer.getOwnerEmail().equals(ownerEmail)) {
            throw new OfferAccessDeniedException("You can only edit offers you own.");
        }

        offerEditDTO.applyTo(storedOffer);
        Offer savedOffer = offerRepository.save(storedOffer);

        log.info("Offer with ID: {} edited.", savedOffer.getId());
        eventSourceService.saveEvent(savedOffer, EventType.OFFER_UPDATED);

        OfferDTO edited = toDto(savedOffer);
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

        log.info("Found owner with ID:{} of offer with ID: {}", ownerEmail, offerId);

        return ownerEmail;
    }

    @Override
    public OfferDTO uploadPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
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

        return toDto(saved);
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

    public static final int GALLERY_CAP = 10;

    @Override
    public OfferDTO uploadGalleryPhoto(UUID offerId, String ownerEmail, InputStream content, long contentLength,
                                       String validatedContentType, String filename) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only upload photos for your own offers.");
        List<OfferPhoto> photos = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        if (photos.size() >= GALLERY_CAP) {
            throw new GalleryLimitExceededException("Gallery holds at most " + GALLERY_CAP + " photos.");
        }
        String key = photoStorage.upload(offerId, content, contentLength, validatedContentType, filename);
        OfferPhoto photo = OfferPhoto.builder().offer(offer).position(photos.size())
                .objectKey(key).build();
        offerPhotoRepository.save(photo);
        log.info("Gallery photo uploaded for offer ID: {} key={}", offerId, key);
        return toDto(offer);
    }

    @Override
    public OfferDTO deleteGalleryPhoto(UUID offerId, UUID photoId, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only delete photos of your own offers.");
        OfferPhoto photo = offerPhotoRepository.findById(photoId)
                .orElseThrow(() -> new OfferNotFoundException("Photo not found."));
        if (!photo.getOffer().getId().equals(offerId)) {
            throw new OfferNotFoundException("Photo not found.");
        }
        String key = photo.getObjectKey();
        offerPhotoRepository.delete(photo);
        renumber(offerId);
        if (key != null && !key.isBlank()) {
            try {
                photoStorage.delete(offerId, key);
            } catch (PhotoStorageException ex) {
                log.warn("photo_delete_failed key={} reason={}", key, ex.getMessage());
            }
        }
        return toDto(offer);
    }

    @Override
    public OfferDTO reorderGalleryPhoto(UUID offerId, UUID photoId, int newPosition, String ownerEmail) {
        Offer offer = requireOwnedOffer(offerId, ownerEmail, "You can only reorder photos of your own offers.");
        List<OfferPhoto> photos = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        OfferPhoto target = photos.stream().filter(p -> p.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new OfferNotFoundException("Photo not found."));
        if (newPosition < 0 || newPosition >= photos.size()) {
            throw new OfferException("Invalid position.");
        }
        int oldPosition = photos.indexOf(target);
        photos.remove(oldPosition);
        photos.add(newPosition, target);
        target.setPosition(photos.size() + 10000);
        offerPhotoRepository.saveAndFlush(target);
        if (newPosition < oldPosition) {
            for (int i = oldPosition; i >= newPosition; i--) {
                OfferPhoto current = photos.get(i);
                if (current != target) {
                    current.setPosition(i);
                    offerPhotoRepository.saveAndFlush(current);
                }
            }
        } else if (newPosition > oldPosition) {
            for (int i = oldPosition; i <= newPosition; i++) {
                OfferPhoto current = photos.get(i);
                if (current != target) {
                    current.setPosition(i);
                    offerPhotoRepository.saveAndFlush(current);
                }
            }
        }
        target.setPosition(newPosition);
        offerPhotoRepository.saveAndFlush(target);
        return toDto(offer);
    }

    @Override
    public OfferDTO setGalleryCover(UUID offerId, UUID photoId, String ownerEmail) {
        return reorderGalleryPhoto(offerId, photoId, 0, ownerEmail);
    }

    private void renumber(UUID offerId) {
        List<OfferPhoto> photos = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offerId);
        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getPosition() != i) {
                photos.get(i).setPosition(i);
                offerPhotoRepository.saveAndFlush(photos.get(i));
            }
        }
    }

    private Offer requireOwnedOffer(UUID offerId, String ownerEmail, String accessDeniedMessage) {        Offer offer = offerRepository.findById(offerId)
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

    private OfferDTO toDto(Offer offer) {
        OfferDTO dto = OfferDTO.of(offer);
        dto.setPhotoUrl(photoAddress(offer));
        List<PhotoDTO> gallery = galleryOf(offer);
        dto.setGallery(gallery);
        dto.setCoverPhotoUrl(gallery.isEmpty() ? null : gallery.get(0).getUrl());
        if (dto.getPhotoUrl() == null) {
            dto.setPhotoUrl(dto.getCoverPhotoUrl());
        }
        return dto;
    }

    private List<PhotoDTO> galleryOf(Offer offer) {
        List<OfferPhoto> photos = offerPhotoRepository.findAllByOfferIdOrderByPositionAsc(offer.getId());
        if (photos.isEmpty()) {
            String fallback = photoAddress(offer);
            if (fallback == null) {
                return new java.util.ArrayList<>();
            }
            return new java.util.ArrayList<>(List.of(
                    PhotoDTO.builder().id(null).position(0).url(fallback).build()));
        }
        List<PhotoDTO> result = new java.util.ArrayList<>();
        for (OfferPhoto photo : photos) {
            String url = photo.getObjectKey() != null && !photo.getObjectKey().isBlank()
                    ? photoStorage.presignedUrl(photo.getObjectKey())
                    : photo.getExternalUrl();
            result.add(PhotoDTO.builder().id(photo.getId()).position(photo.getPosition()).url(url).build());
        }
        return result;
    }

    private String photoAddress(Offer offer) {
        String key = offer.getPhotoObjectKey();

        if (key == null || key.isBlank()) {
            return offer.getExternalPhotoUrl();
        }

        return photoStorage.presignedUrl(key);
    }
}

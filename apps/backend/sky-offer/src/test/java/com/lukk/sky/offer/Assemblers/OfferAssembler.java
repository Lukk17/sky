package com.lukk.sky.offer.assemblers;

import com.lukk.sky.contract.dto.OfferDTO;
import com.lukk.sky.contract.dto.OfferEditDTO;
import com.lukk.sky.contract.dto.PhotoDTO;
import com.lukk.sky.offer.domain.model.Offer;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.EditOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.common.test.TestUsers.TEST_OWNER_EMAIL;

public class OfferAssembler {
    public static final String TEST_HOTEL_NAME = "testHotelName";
    public static final String TEST_CITY = "testCity";
    public static final String TEST_COUNTRY = "testCountry";
    public static final String TEST_COMMENT = "testComment";
    public static final String TEST_DESCRIPTION = "testDescription";
    public static final String TEST_EXTERNAL_PHOTO_URL = "https://images.example.com/test-hotel.jpeg";
    public static final BigDecimal TEST_PRICE = BigDecimal.valueOf(20);
    public static final Long TEST_ROOM_CAPACITY = 5L;

    public static final UUID TEST_DEFAULT_OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_DEFAULT_OFFER_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    public static String testPhotoObjectKey(UUID offerId) {
        return "offers/" + offerId + "/3f2a1c88-5d24-4b6e-9c0f-1a2b3c4d5e6f-hotel.png";
    }

    public static List<Offer> getPopulatedOffers() {
        return List.of(
                getPopulatedOffer(TEST_DEFAULT_OFFER_ID),
                getPopulatedOffer(TEST_DEFAULT_OFFER_ID_2)
        );
    }

    public static Offer getPopulatedOffer(UUID id) {
        return Offer.builder()
                .hotelName(TEST_HOTEL_NAME)
                .ownerEmail(TEST_OWNER_EMAIL)
                .city(TEST_CITY)
                .comment(TEST_COMMENT)
                .country(TEST_COUNTRY)
                .description(TEST_DESCRIPTION)
                .photoObjectKey(testPhotoObjectKey(id))
                .externalPhotoUrl(TEST_EXTERNAL_PHOTO_URL)
                .id(id)
                .roomCapacity(TEST_ROOM_CAPACITY)
                .price(TEST_PRICE)
                .build();
    }

    public static List<OfferDTO> getPopulatedOffersDTO() {
        return List.of(
                getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID),
                getPopulatedOfferDTO(TEST_DEFAULT_OFFER_ID_2)
        );
    }

    public static OfferDTO getPopulatedOfferDTO(UUID id) {
        OfferDTO dto = new OfferDTO();
        dto.setId(id);
        dto.setHotelName(TEST_HOTEL_NAME);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setComment(TEST_COMMENT);
        dto.setPrice(TEST_PRICE);
        dto.setOwnerEmail(TEST_OWNER_EMAIL);
        dto.setRoomCapacity(TEST_ROOM_CAPACITY);
        dto.setCity(TEST_CITY);
        dto.setCountry(TEST_COUNTRY);
        dto.setExternalPhotoUrl(TEST_EXTERNAL_PHOTO_URL);
        return dto;
    }

    public static CreateOfferCommand toCreateCommand(OfferDTO dto) {
        return new CreateOfferCommand(dto.getHotelName(), dto.getDescription(), dto.getComment(),
                dto.getPrice(), dto.getOwnerEmail(), dto.getRoomCapacity(),
                dto.getCity(), dto.getCountry(), dto.getExternalPhotoUrl());
    }

    public static EditOfferCommand toEditCommand(OfferEditDTO dto) {
        return new EditOfferCommand(dto.getId(), dto.getHotelName(), dto.getCity(), dto.getCountry(),
                dto.getDescription(), dto.getComment(), dto.getPrice(),
                dto.getRoomCapacity(), dto.getExternalPhotoUrl());
    }

    public static OfferDTO toOfferDTO(OfferView view) {
        OfferDTO dto = new OfferDTO();
        dto.setId(view.id());
        dto.setHotelName(view.hotelName());
        dto.setDescription(view.description());
        dto.setComment(view.comment());
        dto.setPrice(view.price());
        dto.setOwnerEmail(view.ownerEmail());
        dto.setRoomCapacity(view.roomCapacity());
        dto.setCity(view.city());
        dto.setCountry(view.country());
        dto.setExternalPhotoUrl(view.externalPhotoUrl());
        dto.setPhotoUrl(view.photoUrl());
        dto.setGallery(toPhotoDtos(view.gallery()));
        dto.setCoverPhotoUrl(view.coverPhotoUrl());
        return dto;
    }

    public static OfferDTO toOfferDTO(Offer offer) {
        OfferDTO dto = new OfferDTO();
        dto.setId(offer.getId());
        dto.setHotelName(offer.getHotelName());
        dto.setDescription(offer.getDescription());
        dto.setComment(offer.getComment());
        dto.setPrice(offer.getPrice());
        dto.setOwnerEmail(offer.getOwnerEmail());
        dto.setRoomCapacity(offer.getRoomCapacity());
        dto.setCity(offer.getCity());
        dto.setCountry(offer.getCountry());
        dto.setExternalPhotoUrl(offer.getExternalPhotoUrl());
        return dto;
    }

    public static OfferEditDTO toOfferEditDTO(Offer offer) {
        OfferEditDTO dto = new OfferEditDTO();
        dto.setId(offer.getId());
        dto.setHotelName(offer.getHotelName());
        dto.setDescription(offer.getDescription());
        dto.setComment(offer.getComment());
        dto.setPrice(offer.getPrice());
        dto.setOwnerEmail(offer.getOwnerEmail());
        dto.setRoomCapacity(offer.getRoomCapacity());
        dto.setCity(offer.getCity());
        dto.setCountry(offer.getCountry());
        dto.setExternalPhotoUrl(offer.getExternalPhotoUrl());
        return dto;
    }

    public static OfferView toOfferView(OfferDTO dto) {
        return new OfferView(dto.getId(), dto.getHotelName(), dto.getDescription(), dto.getComment(),
                dto.getPrice(), dto.getOwnerEmail(), dto.getRoomCapacity(), dto.getCity(), dto.getCountry(),
                dto.getExternalPhotoUrl(), dto.getPhotoUrl(),
                dto.getGallery() == null ? List.of() : dto.getGallery().stream()
                        .map(photo -> new GalleryPhotoView(photo.getId(), photo.getPosition(),
                                photo.getUrl(), Boolean.TRUE.equals(photo.getMain())))
                        .toList(),
                dto.getCoverPhotoUrl());
    }

    public static List<OfferView> toOfferViews(List<OfferDTO> dtos) {
        return dtos.stream().map(OfferAssembler::toOfferView).toList();
    }

    public static OfferEditDTO getPopulatedOfferEditDTO(UUID id) {
        OfferEditDTO dto = new OfferEditDTO();
        dto.setId(id);
        dto.setHotelName(TEST_HOTEL_NAME);
        dto.setDescription(TEST_DESCRIPTION);
        dto.setComment(TEST_COMMENT);
        dto.setPrice(TEST_PRICE);
        dto.setOwnerEmail(TEST_OWNER_EMAIL);
        dto.setRoomCapacity(TEST_ROOM_CAPACITY);
        dto.setCity(TEST_CITY);
        dto.setCountry(TEST_COUNTRY);
        dto.setExternalPhotoUrl(TEST_EXTERNAL_PHOTO_URL);
        return dto;
    }

    private static List<PhotoDTO> toPhotoDtos(List<GalleryPhotoView> photos) {
        if (photos == null) {
            return new ArrayList<>();
        }

        return photos.stream()
                .map(photo -> {
                    PhotoDTO dto = new PhotoDTO();
                    dto.setId(photo.id());
                    dto.setPosition(photo.position());
                    dto.setUrl(photo.url());
                    dto.setMain(photo.main());
                    return dto;
                })
                .toList();
    }
}

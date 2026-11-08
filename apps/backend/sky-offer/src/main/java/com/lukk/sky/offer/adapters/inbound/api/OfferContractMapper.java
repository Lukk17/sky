package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.contract.dto.OfferDTO;
import com.lukk.sky.contract.dto.OfferEditDTO;
import com.lukk.sky.contract.dto.OfferPage;
import com.lukk.sky.contract.dto.PageableObject;
import com.lukk.sky.contract.dto.PhotoDTO;
import com.lukk.sky.contract.dto.SortObject;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.EditOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

final class OfferContractMapper {

    private OfferContractMapper() {
    }

    static OfferDTO toDto(OfferView view) {
        OfferDTO target = new OfferDTO();
        target.setId(view.id());
        target.setHotelName(view.hotelName());
        target.setDescription(view.description());
        target.setComment(view.comment());
        target.setPrice(view.price());
        target.setOwnerEmail(view.ownerEmail());
        target.setRoomCapacity(view.roomCapacity());
        target.setCity(view.city());
        target.setCountry(view.country());
        target.setExternalPhotoUrl(view.externalPhotoUrl());
        target.setPhotoUrl(view.photoUrl());
        target.setGallery(toGallery(view.gallery()));
        target.setCoverPhotoUrl(view.coverPhotoUrl());
        return target;
    }

    static OfferPage toPage(Page<OfferView> source) {
        OfferPage target = new OfferPage();
        target.setContent(toContent(source.getContent()));
        target.setEmpty(source.isEmpty());
        target.setFirst(source.isFirst());
        target.setLast(source.isLast());
        target.setNumber(source.getNumber());
        target.setNumberOfElements(source.getNumberOfElements());
        target.setPageable(toPageableObject(source.getPageable()));
        target.setSize(source.getSize());
        target.setSort(toSortObject(source.getSort()));
        target.setTotalElements(source.getTotalElements());
        target.setTotalPages(source.getTotalPages());
        return target;
    }

    static CreateOfferCommand toCreateCommand(OfferDTO offer, String ownerEmail) {
        return new CreateOfferCommand(offer.getHotelName(), offer.getDescription(), offer.getComment(),
                offer.getPrice(), ownerEmail, offer.getRoomCapacity(),
                offer.getCity(), offer.getCountry(), offer.getExternalPhotoUrl());
    }

    static EditOfferCommand toEditCommand(OfferEditDTO offer) {
        return new EditOfferCommand(offer.getId(), offer.getHotelName(), offer.getCity(), offer.getCountry(),
                offer.getDescription(), offer.getComment(), offer.getPrice(),
                offer.getRoomCapacity(), offer.getExternalPhotoUrl());
    }

    private static List<OfferDTO> toContent(List<OfferView> source) {
        return source.stream()
                .map(OfferContractMapper::toDto)
                .toList();
    }

    private static List<PhotoDTO> toGallery(List<GalleryPhotoView> source) {
        if (source == null) {
            return List.of();
        }

        return source.stream()
                .map(OfferContractMapper::toPhotoDto)
                .toList();
    }

    private static PhotoDTO toPhotoDto(GalleryPhotoView photo) {
        PhotoDTO target = new PhotoDTO();
        target.setId(photo.id());
        target.setPosition(photo.position());
        target.setUrl(photo.url());
        target.setMain(photo.main());
        return target;
    }

    private static PageableObject toPageableObject(Pageable source) {
        PageableObject target = new PageableObject();
        target.setOffset(source.getOffset());
        target.setPageNumber(source.getPageNumber());
        target.setPageSize(source.getPageSize());
        target.setPaged(source.isPaged());
        target.setSort(toSortObject(source.getSort()));
        target.setUnpaged(source.isUnpaged());
        return target;
    }

    private static SortObject toSortObject(Sort source) {
        SortObject target = new SortObject();
        target.setEmpty(source.isEmpty());
        target.setSorted(source.isSorted());
        target.setUnsorted(source.isUnsorted());
        return target;
    }
}

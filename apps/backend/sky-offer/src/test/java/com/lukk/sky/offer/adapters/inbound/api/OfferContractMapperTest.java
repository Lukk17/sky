package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.contract.dto.OfferDTO;
import com.lukk.sky.contract.dto.OfferPage;
import com.lukk.sky.offer.domain.ports.inbound.CreateOfferCommand;
import com.lukk.sky.offer.domain.ports.inbound.GalleryPhotoView;
import com.lukk.sky.offer.domain.ports.inbound.OfferView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OfferContractMapper: domain views and contract wire types")
class OfferContractMapperTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String OWNER = "owner@sky.dev";

    @Test
    @DisplayName("toDto_whenGalleryIsNull_thenReturnsEmptyGallery")
    void toDto_whenGalleryIsNull_thenReturnsEmptyGallery() {
        // given
        OfferView view = new OfferView(ID, "H", "D", "C", BigDecimal.TEN,
                OWNER, 2L, "City", "Country", null, null, null, null);

        // when
        OfferDTO dto = OfferContractMapper.toDto(view);

        // then
        assertThat(dto.getId()).isEqualTo(ID);
        assertThat(dto.getGallery()).isEmpty();
    }

    @Test
    @DisplayName("toDto maps every view field and the gallery main flag")
    void toDto_whenViewIsFullyPopulated_thenMapEveryField() {
        // given
        UUID photoId = UUID.randomUUID();
        OfferView view = new OfferView(ID, "H", "D", "C", BigDecimal.TEN,
                OWNER, 2L, "City", "Country", "https://external.example/photo.png",
                "https://cdn.example/cover.png",
                List.of(new GalleryPhotoView(photoId, 0, "https://cdn.example/cover.png", true)),
                "https://cdn.example/cover.png");

        // when
        OfferDTO dto = OfferContractMapper.toDto(view);

        // then
        assertThat(dto.getId()).isEqualTo(ID);
        assertThat(dto.getHotelName()).isEqualTo("H");
        assertThat(dto.getDescription()).isEqualTo("D");
        assertThat(dto.getComment()).isEqualTo("C");
        assertThat(dto.getPrice()).isEqualTo(BigDecimal.TEN);
        assertThat(dto.getOwnerEmail()).isEqualTo(OWNER);
        assertThat(dto.getRoomCapacity()).isEqualTo(2L);
        assertThat(dto.getCity()).isEqualTo("City");
        assertThat(dto.getCountry()).isEqualTo("Country");
        assertThat(dto.getExternalPhotoUrl()).isEqualTo("https://external.example/photo.png");
        assertThat(dto.getPhotoUrl()).isEqualTo("https://cdn.example/cover.png");
        assertThat(dto.getCoverPhotoUrl()).isEqualTo("https://cdn.example/cover.png");
        assertThat(dto.getGallery()).hasSize(1);
        assertThat(dto.getGallery().get(0).getId()).isEqualTo(photoId);
        assertThat(dto.getGallery().get(0).getPosition()).isZero();
        assertThat(dto.getGallery().get(0).getUrl()).isEqualTo("https://cdn.example/cover.png");
        assertThat(dto.getGallery().get(0).getMain()).isTrue();
    }

    @Test
    @DisplayName("toPage maps every Spring Data page field into the generated contract page")
    void toPage_whenPageHasContent_thenCopyEveryField() {
        // given
        List<OfferView> offers = List.of(view("First"), view("Second"));
        Sort sort = Sort.by("id").ascending();
        PageRequest pageable = PageRequest.of(1, 2, sort);
        Page<OfferView> source = new PageImpl<>(offers, pageable, 5);

        // when
        OfferPage actual = OfferContractMapper.toPage(source);

        // then
        assertThat(actual.getContent())
                .extracting(OfferDTO::getHotelName)
                .containsExactly("First", "Second");
        assertThat(actual.getEmpty()).isFalse();
        assertThat(actual.getFirst()).isFalse();
        assertThat(actual.getLast()).isFalse();
        assertThat(actual.getNumber()).isEqualTo(1);
        assertThat(actual.getNumberOfElements()).isEqualTo(2);
        assertThat(actual.getSize()).isEqualTo(2);
        assertThat(actual.getTotalElements()).isEqualTo(5);
        assertThat(actual.getTotalPages()).isEqualTo(3);

        assertThat(actual.getPageable().getOffset()).isEqualTo(2);
        assertThat(actual.getPageable().getPageNumber()).isEqualTo(1);
        assertThat(actual.getPageable().getPageSize()).isEqualTo(2);
        assertThat(actual.getPageable().getPaged()).isTrue();
        assertThat(actual.getPageable().getUnpaged()).isFalse();
        assertThat(actual.getPageable().getSort().getSorted()).isTrue();
        assertThat(actual.getPageable().getSort().getEmpty()).isFalse();
        assertThat(actual.getPageable().getSort().getUnsorted()).isFalse();

        assertThat(actual.getSort().getSorted()).isTrue();
        assertThat(actual.getSort().getEmpty()).isFalse();
        assertThat(actual.getSort().getUnsorted()).isFalse();
    }

    @Test
    @DisplayName("toPage maps an empty page without inventing content")
    void toPage_whenPageIsEmpty_thenCopyEmptyAndUnpagedFlags() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        Page<OfferView> source = new PageImpl<>(List.of(), pageable, 0);

        // when
        OfferPage actual = OfferContractMapper.toPage(source);

        // then
        assertThat(actual.getContent()).isEmpty();
        assertThat(actual.getEmpty()).isTrue();
        assertThat(actual.getFirst()).isTrue();
        assertThat(actual.getLast()).isTrue();
        assertThat(actual.getNumber()).isZero();
        assertThat(actual.getNumberOfElements()).isZero();
        assertThat(actual.getSize()).isEqualTo(20);
        assertThat(actual.getTotalElements()).isZero();
        assertThat(actual.getTotalPages()).isZero();
        assertThat(actual.getPageable().getOffset()).isZero();
        assertThat(actual.getPageable().getPageNumber()).isZero();
        assertThat(actual.getPageable().getPageSize()).isEqualTo(20);
        assertThat(actual.getSort().getEmpty()).isTrue();
        assertThat(actual.getSort().getSorted()).isFalse();
        assertThat(actual.getSort().getUnsorted()).isTrue();
    }

    @Test
    @DisplayName("toCreateCommand uses the authenticated owner, never the read-only ownerEmail field")
    void toCreateCommand_whenPayloadNamesAnotherOwner_thenUseTheAuthenticatedOwner() {
        // given
        OfferDTO offer = new OfferDTO();
        offer.setHotelName("H");
        offer.setDescription("D");
        offer.setComment("C");
        offer.setPrice(BigDecimal.TEN);
        offer.setOwnerEmail("payload-owner@sky.dev");
        offer.setRoomCapacity(2L);
        offer.setCity("City");
        offer.setCountry("Country");
        offer.setExternalPhotoUrl("https://external.example/photo.png");

        // when
        CreateOfferCommand command = OfferContractMapper.toCreateCommand(offer, OWNER);

        // then
        assertThat(command.ownerEmail()).isEqualTo(OWNER);
        assertThat(command.hotelName()).isEqualTo("H");
        assertThat(command.description()).isEqualTo("D");
        assertThat(command.comment()).isEqualTo("C");
        assertThat(command.price()).isEqualTo(BigDecimal.TEN);
        assertThat(command.roomCapacity()).isEqualTo(2L);
        assertThat(command.city()).isEqualTo("City");
        assertThat(command.country()).isEqualTo("Country");
        assertThat(command.externalPhotoUrl()).isEqualTo("https://external.example/photo.png");
    }

    private static OfferView view(String hotelName) {
        return new OfferView(ID, hotelName, "D", "C", BigDecimal.TEN,
                OWNER, 2L, "City", "Country", null, null, List.of(), null);
    }
}

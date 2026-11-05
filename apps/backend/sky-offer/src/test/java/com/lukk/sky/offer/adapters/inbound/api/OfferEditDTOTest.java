package com.lukk.sky.offer.adapters.inbound.api;

import com.lukk.sky.contract.dto.OfferEditDTO;
import com.lukk.sky.offer.domain.ports.inbound.EditOfferCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OfferEditDTO: mapping a partial contract payload onto the edit command")
class OfferEditDTOTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("toEditCommand_whenOptionalFieldsAreAbsent_thenPreserveNullsSoTheServiceKeepsStoredValues")
    void toEditCommand_whenOptionalFieldsAreAbsent_thenPreserveNulls() {
        // given
        OfferEditDTO partialEdit = new OfferEditDTO();
        partialEdit.setId(ID);
        partialEdit.setHotelName("UpdatedName");

        // when
        EditOfferCommand command = OfferContractMapper.toEditCommand(partialEdit);

        // then
        assertThat(command.id()).isEqualTo(ID);
        assertThat(command.hotelName()).isEqualTo("UpdatedName");
        assertThat(command.description()).isNull();
        assertThat(command.comment()).isNull();
        assertThat(command.price()).isNull();
        assertThat(command.roomCapacity()).isNull();
        assertThat(command.city()).isNull();
        assertThat(command.country()).isNull();
        assertThat(command.externalPhotoUrl()).isNull();
    }

    @Test
    @DisplayName("toEditCommand_whenEveryFieldIsSupplied_thenCopyEveryValue")
    void toEditCommand_whenEveryFieldIsSupplied_thenCopyEveryValue() {
        // given
        OfferEditDTO fullEdit = new OfferEditDTO();
        fullEdit.setId(ID);
        fullEdit.setHotelName("Hotel");
        fullEdit.setDescription("Description");
        fullEdit.setComment("Comment");
        fullEdit.setPrice(BigDecimal.TEN);
        fullEdit.setRoomCapacity(2L);
        fullEdit.setCity("City");
        fullEdit.setCountry("Country");
        fullEdit.setExternalPhotoUrl("https://images.example.com/hotel.jpeg");

        // when
        EditOfferCommand command = OfferContractMapper.toEditCommand(fullEdit);

        // then
        assertThat(command.id()).isEqualTo(ID);
        assertThat(command.hotelName()).isEqualTo("Hotel");
        assertThat(command.description()).isEqualTo("Description");
        assertThat(command.comment()).isEqualTo("Comment");
        assertThat(command.price()).isEqualTo(BigDecimal.TEN);
        assertThat(command.roomCapacity()).isEqualTo(2L);
        assertThat(command.city()).isEqualTo("City");
        assertThat(command.country()).isEqualTo("Country");
        assertThat(command.externalPhotoUrl()).isEqualTo("https://images.example.com/hotel.jpeg");
    }
}

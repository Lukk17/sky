package com.lukk.sky.booking.adapters.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookingDTO wire format")
class BookingDTOSerializationTest {

    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("a fully populated booking serialises to the same JSON object Gson produced")
    void writeValueAsString_whenEveryFieldIsSet_thenMatchesThePreviousWireFormat() {
        BookingDTO booking = booking("owner@owner.com");

        String actual = objectMapper.writeValueAsString(booking);

        String expected = """
                {"id":"00000000-0000-0000-0000-000000000001",\
                "offerId":"00000000-0000-0000-0000-000000000101",\
                "bookedDate":"2201-06-20",\
                "bookingUser":"testUser@user.com",\
                "ownerEmail":"owner@owner.com"}""";
        assertThat(objectMapper.readTree(actual)).isEqualTo(objectMapper.readTree(expected));
    }

    @Test
    @DisplayName("a null field is omitted from the JSON rather than written as null")
    void writeValueAsString_whenAFieldIsNull_thenTheKeyIsOmitted() {
        BookingDTO booking = booking(null);

        String actual = objectMapper.writeValueAsString(booking);

        assertThat(actual).doesNotContain("ownerEmail").doesNotContain("null");
        assertThat(objectMapper.readTree(actual).has("ownerEmail")).isFalse();
    }

    @Test
    @DisplayName("HTML-significant characters are written raw and survive a round trip unchanged")
    void writeValueAsString_whenValueContainsHtmlCharacters_thenTheyAreWrittenRaw() {
        BookingDTO booking = booking("a<b>&c='d'@owner.com");

        String actual = objectMapper.writeValueAsString(booking);

        assertThat(actual).contains("\"a<b>&c='d'@owner.com\"").doesNotContain("\\u003");
        assertThat(objectMapper.readTree(actual).get("ownerEmail").asString())
                .isEqualTo("a<b>&c='d'@owner.com");
    }

    private static BookingDTO booking(String ownerEmail) {
        return BookingDTO.builder()
                .id(BOOKING_ID)
                .offerId(OFFER_ID)
                .bookedDate("2201-06-20")
                .bookingUser("testUser@user.com")
                .ownerEmail(ownerEmail)
                .build();
    }
}

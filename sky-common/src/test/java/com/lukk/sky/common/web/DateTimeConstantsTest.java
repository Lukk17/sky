package com.lukk.sky.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DateTimeConstants")
class DateTimeConstantsTest {

    @Test
    @DisplayName("DATE_TIME_FORMAT_rendersAnInstantInTheDisplayZone")
    void dateTimeFormat_rendersAnInstantInTheDisplayZone() {
        // given: 06:30 UTC in June is 08:30 in Europe/Warsaw (CEST)
        Instant instant = Instant.parse("2102-06-20T06:30:00Z");

        // when
        String formatted = DateTimeConstants.DATE_TIME_FORMAT.format(instant);

        // then
        assertThat(formatted).isEqualTo("08:30:00 20.06.2102");
    }

    @Test
    @DisplayName("DATE_TIME_FORMAT_roundTripsAnInstantToTheSecond")
    void dateTimeFormat_roundTripsAnInstantToTheSecond() {
        // given
        Instant instant = Instant.parse("2102-01-20T06:30:15Z");

        // when
        String formatted = DateTimeConstants.DATE_TIME_FORMAT.format(instant);
        Instant parsed = DateTimeConstants.DATE_TIME_FORMAT.parse(formatted, Instant::from);

        // then
        assertThat(parsed)
                .as("a formatted instant must parse back to the same point in time")
                .isEqualTo(instant);
    }

    @Test
    @DisplayName("DATE_FORMAT_roundTripsACalendarDate")
    void dateFormat_roundTripsACalendarDate() {
        // given
        LocalDate date = LocalDate.of(2102, 6, 20);

        // when
        String formatted = DateTimeConstants.DATE_FORMAT.format(date);
        LocalDate parsed = LocalDate.parse(formatted, DateTimeConstants.DATE_FORMAT);

        // then
        assertThat(formatted).isEqualTo("2102-06-20");
        assertThat(parsed).isEqualTo(date);
    }

    @Test
    @DisplayName("DISPLAY_ZONE_isEuropeWarsaw")
    void displayZone_isEuropeWarsaw() {
        assertThat(DateTimeConstants.DISPLAY_ZONE.getId()).isEqualTo("Europe/Warsaw");
    }
}

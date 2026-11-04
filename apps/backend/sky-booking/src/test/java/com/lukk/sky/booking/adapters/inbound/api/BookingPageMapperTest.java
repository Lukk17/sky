package com.lukk.sky.booking.adapters.inbound.api;

import com.lukk.sky.booking.adapters.dto.BookingDTO;
import com.lukk.sky.booking.adapters.dto.contract.Page;
import com.lukk.sky.booking.assemblers.BookingAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BookingPageMapper")
class BookingPageMapperTest {

    @Test
    @DisplayName("maps every Spring Data page field into the generated contract page")
    void toPage_whenPageHasContent_thenCopyEveryField() {
        // given
        List<BookingDTO> bookings = BookingAssembler.getPopulatedBookedDTOList();
        Sort sort = Sort.by("bookedDate").ascending();
        PageRequest pageable = PageRequest.of(1, 2, sort);
        org.springframework.data.domain.Page<BookingDTO> source =
                new PageImpl<>(bookings, pageable, 5);

        // when
        Page actual = BookingPageMapper.toPage(source);

        // then
        assertThat(actual.getContent()).containsExactlyElementsOf(bookings);
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
    @DisplayName("maps an empty page without inventing content")
    void toPage_whenPageIsEmpty_thenCopyEmptyAndUnpagedFlags() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        org.springframework.data.domain.Page<BookingDTO> source =
                new PageImpl<>(List.of(), pageable, 0);

        // when
        Page actual = BookingPageMapper.toPage(source);

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
}

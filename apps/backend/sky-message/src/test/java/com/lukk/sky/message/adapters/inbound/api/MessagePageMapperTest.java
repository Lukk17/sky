package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.adapters.dto.contract.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessagePageMapper: Spring Data page to contract page")
class MessagePageMapperTest {

    @Test
    @DisplayName("toPage copies content, pagination and sort details")
    void toPage_whenSpringPageHasSort_thenCopyEveryField() {
        // given
        MessageDTO message = MessageDTO.builder().text("hello").receiverEmail("to@example.com").build();
        PageRequest pageRequest = PageRequest.of(1, 5, Sort.by("createdTime"));
        org.springframework.data.domain.Page<MessageDTO> source =
                new PageImpl<>(List.of(message), pageRequest, 6);

        // when
        Page actual = MessagePageMapper.toPage(source);

        // then
        assertEquals(1, actual.getContent().size());
        assertEquals(message, actual.getContent().get(0));
        assertFalse(actual.getEmpty());
        assertFalse(actual.getFirst());
        assertTrue(actual.getLast());
        assertEquals(1, actual.getNumber());
        assertEquals(1, actual.getNumberOfElements());
        assertEquals(5, actual.getSize());
        assertEquals(6, actual.getTotalElements());
        assertEquals(2, actual.getTotalPages());

        assertEquals(5L, actual.getPageable().getOffset());
        assertEquals(1, actual.getPageable().getPageNumber());
        assertEquals(5, actual.getPageable().getPageSize());
        assertTrue(actual.getPageable().getPaged());
        assertFalse(actual.getPageable().getUnpaged());

        assertFalse(actual.getSort().getEmpty());
        assertTrue(actual.getSort().getSorted());
        assertFalse(actual.getSort().getUnsorted());
        assertTrue(actual.getPageable().getSort().getSorted());
    }

    @Test
    @DisplayName("toPage maps an unsorted empty page as unsorted")
    void toPage_whenSpringPageIsEmptyAndUnsorted_thenMarkSortUnsorted() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 20);
        org.springframework.data.domain.Page<MessageDTO> source =
                new PageImpl<>(List.of(), pageRequest, 0);

        // when
        Page actual = MessagePageMapper.toPage(source);

        // then
        assertTrue(actual.getContent().isEmpty());
        assertTrue(actual.getEmpty());
        assertTrue(actual.getFirst());
        assertTrue(actual.getLast());
        assertTrue(actual.getSort().getEmpty());
        assertFalse(actual.getSort().getSorted());
        assertTrue(actual.getSort().getUnsorted());
        assertTrue(actual.getPageable().getSort().getUnsorted());
    }
}

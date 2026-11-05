package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.contract.dto.MessageDTO;
import com.lukk.sky.contract.dto.MessagePage;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MessageContractMapper: domain view to contract wire types")
class MessageContractMapperTest {

    private static final UUID MESSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CREATED_TIME = "12:00:00 01.01.2026";

    @Test
    @DisplayName("toDto copies every field from the domain view")
    void toDto_whenViewHasEveryField_thenCopyEveryField() {
        // given
        MessageView view = new MessageView(
                MESSAGE_ID, "hello", "to@example.com", "from@example.com", CREATED_TIME, false);

        // when
        MessageDTO actual = MessageContractMapper.toDto(view);

        // then
        assertEquals(MESSAGE_ID, actual.getId());
        assertEquals("hello", actual.getText());
        assertEquals("to@example.com", actual.getReceiverEmail());
        assertEquals("from@example.com", actual.getSenderEmail());
        assertEquals(CREATED_TIME, actual.getCreatedTime());
        assertFalse(actual.getRead());
    }

    @Test
    @DisplayName("toPage copies content, pagination and sort details")
    void toPage_whenSpringPageHasSort_thenCopyEveryField() {
        // given
        MessageView message = new MessageView(
                MESSAGE_ID, "hello", "to@example.com", "from@example.com", CREATED_TIME, false);
        PageRequest pageRequest = PageRequest.of(1, 5, Sort.by("createdTime"));
        org.springframework.data.domain.Page<MessageView> source =
                new PageImpl<>(List.of(message), pageRequest, 6);

        // when
        MessagePage actual = MessageContractMapper.toPage(source);

        // then
        assertEquals(1, actual.getContent().size());
        assertEquals("hello", actual.getContent().get(0).getText());
        assertEquals("to@example.com", actual.getContent().get(0).getReceiverEmail());
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
        org.springframework.data.domain.Page<MessageView> source =
                new PageImpl<>(List.of(), pageRequest, 0);

        // when
        MessagePage actual = MessageContractMapper.toPage(source);

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

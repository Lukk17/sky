package com.lukk.sky.message.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Message — equals and hashCode contract (Hibernate-safe, id-based)")
class MessageEqualsHashCodeTest {

    private Message buildMessage(Long id) {
        return Message.builder()
                .id(id)
                .text("Hello")
                .createdTime(Instant.parse("2025-01-01T11:00:00Z"))
                .isRead(false)
                .receiverEmail("receiver@example.com")
                .senderEmail("sender@example.com")
                .build();
    }

    @Test
    @DisplayName("equals_whenSameInstance_thenReturnTrue")
    void equals_whenSameInstance_thenReturnTrue() {
        Message message = buildMessage(1L);

        assertEquals(message, message);
    }

    @Test
    @DisplayName("equals_whenBothHaveSameId_thenReturnTrue")
    void equals_whenBothHaveSameId_thenReturnTrue() {
        Message a = buildMessage(1L);
        Message b = buildMessage(1L);

        assertEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenIdsDiffer_thenReturnFalse")
    void equals_whenIdsDiffer_thenReturnFalse() {
        Message a = buildMessage(1L);
        Message b = buildMessage(2L);

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenOneIdIsNull_thenReturnFalse")
    void equals_whenOneIdIsNull_thenReturnFalse() {
        Message withId = buildMessage(1L);
        Message withoutId = buildMessage(null);

        assertNotEquals(withId, withoutId);
        assertNotEquals(withoutId, withId);
    }

    @Test
    @DisplayName("hashCode_whenEqualMessages_thenSameHashCode")
    void hashCode_whenEqualMessages_thenSameHashCode() {
        Message a = buildMessage(1L);
        Message b = buildMessage(1L);

        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("hashCode_whenNullId_thenConsistentWithEquals")
    void hashCode_whenNullId_thenConsistentWithEquals() {
        Message a = buildMessage(null);
        Message b = buildMessage(null);

        int hashA = a.hashCode();
        int hashB = b.hashCode();

        assertEquals(hashA, hashB);
        assertNotEquals(a, b);
    }
}

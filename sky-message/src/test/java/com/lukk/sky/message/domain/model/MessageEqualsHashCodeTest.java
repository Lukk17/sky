package com.lukk.sky.message.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Message — equals and hashCode contract (Hibernate-safe, id-based)")
class MessageEqualsHashCodeTest {

    private static final UUID ID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private Message buildMessage(UUID id) {
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
        Message message = buildMessage(ID_A);

        assertEquals(message, message);
    }

    @Test
    @DisplayName("equals_whenBothHaveSameId_thenReturnTrue")
    void equals_whenBothHaveSameId_thenReturnTrue() {
        Message a = buildMessage(ID_A);
        Message b = buildMessage(ID_A);

        assertEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenIdsDiffer_thenReturnFalse")
    void equals_whenIdsDiffer_thenReturnFalse() {
        Message a = buildMessage(ID_A);
        Message b = buildMessage(ID_B);

        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("equals_whenOneIdIsNull_thenReturnFalse")
    void equals_whenOneIdIsNull_thenReturnFalse() {
        Message withId = buildMessage(ID_A);
        Message withoutId = buildMessage(null);

        assertNotEquals(withId, withoutId);
        assertNotEquals(withoutId, withId);
    }

    @Test
    @DisplayName("hashCode_whenEqualMessages_thenSameHashCode")
    void hashCode_whenEqualMessages_thenSameHashCode() {
        Message a = buildMessage(ID_A);
        Message b = buildMessage(ID_A);

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

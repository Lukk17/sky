package com.lukk.sky.message.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Message: JPA identity contract")
class MessageEqualityTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID OTHER_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @Test
    @DisplayName("two instances carrying the same id are equal even when their business fields differ")
    void equals_whenSameIdAndDifferentBusinessFields_thenInstancesAreEqual() {
        // given
        Message persisted = message(ID, "original text");
        Message reloaded = message(ID, "text edited after the load");

        // when / then
        assertEquals(persisted, reloaded);
        assertEquals(persisted.hashCode(), reloaded.hashCode());
    }

    @Test
    @DisplayName("two instances carrying different ids are not equal")
    void equals_whenDifferentIds_thenInstancesAreNotEqual() {
        // given
        Message first = message(ID, "same text");
        Message second = message(OTHER_ID, "same text");

        // when / then
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("two unsaved instances are distinct, because no identifier has been assigned yet")
    void equals_whenBothIdsAreNull_thenInstancesAreNotEqual() {
        // given
        Message first = message(null, "first draft");
        Message second = message(null, "second draft");

        // when / then
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("an unsaved instance still equals itself")
    void equals_whenSameUnsavedInstance_thenEqual() {
        // given
        Message draft = message(null, "draft");

        // when / then
        assertEquals(draft, draft);
    }

    @Test
    @DisplayName("hashCode survives the identifier being assigned, so a HashSet keeps finding the entity")
    void hashCode_whenIdAssignedAfterInsertionIntoSet_thenSetStillContainsMessage() {
        // given
        Message draft = message(null, "queued before flush");
        Set<Message> pending = new HashSet<>();
        pending.add(draft);

        // when
        draft.setId(ID);

        // then
        assertTrue(pending.contains(draft));
    }

    @Test
    @DisplayName("a message is never equal to null, and the comparison returns false instead of throwing")
    void equals_whenComparedToNull_thenNotEqual() {
        // given
        Message persisted = message(ID, "text");

        // when
        boolean equal = persisted.equals(null);

        // then
        assertFalse(equal);
    }

    @Test
    @DisplayName("a message is never equal to a value of another type, even one carrying the same id text")
    void equals_whenComparedToAnotherType_thenNotEqual() {
        // given
        Message persisted = message(ID, "text");

        // when
        boolean equal = persisted.equals(ID.toString());

        // then
        assertFalse(equal);
    }

    private static Message message(UUID id, String text) {
        return Message.builder()
                .id(id)
                .text(text)
                .createdTime(Instant.parse("2102-06-20T06:30:00Z"))
                .senderEmail("sender@test")
                .receiverEmail("receiver@test")
                .build();
    }
}

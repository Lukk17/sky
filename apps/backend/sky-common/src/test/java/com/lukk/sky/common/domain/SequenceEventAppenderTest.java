package com.lukk.sky.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("SequenceEventAppender unit tests")
class SequenceEventAppenderTest {

    @Test
    @DisplayName("nextSequenceNumber starts at 1 when the store holds no sequence number")
    void nextSequenceNumber_whenNoPreviousSequence_thenReturnOne() {
        // when
        UUID aggregateId = UUID.randomUUID();

        int next = SequenceEventAppender.nextSequenceNumber(aggregateId, id -> Optional.empty());

        // then
        assertEquals(1, next);
    }

    @Test
    @DisplayName("nextSequenceNumber returns the stored sequence number plus one")
    void nextSequenceNumber_whenPreviousSequenceExists_thenReturnItPlusOne() {
        // when
        UUID aggregateId = UUID.randomUUID();

        int next = SequenceEventAppender.nextSequenceNumber(aggregateId, id -> Optional.of(7));

        // then
        assertEquals(8, next);
    }
}

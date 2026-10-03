package com.lukk.sky.common.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SequenceEventAppender {

    private SequenceEventAppender() {
    }

    public interface EventFactory<E, T> {
        E create(UUID aggregateId, int sequenceNumber, T eventType, String payload, Instant timestamp);
    }

    public static <E, T> E appendNext(UUID aggregateId, T eventType, String payload,
            Function<UUID, Optional<Integer>> findLastSequenceNumber,
            BiFunction<E, Integer, E> withSequence,
            Function<E, E> persist,
            EventFactory<E, T> factory) {
        int next = findLastSequenceNumber.apply(aggregateId).orElse(0) + 1;
        E event = factory.create(aggregateId, next, eventType, payload, Instant.now());
        return persist.apply(event);
    }

    public static <E> int nextSequenceNumber(UUID aggregateId,
            Function<UUID, Optional<Integer>> findLastSequenceNumber) {
        return findLastSequenceNumber.apply(aggregateId).orElse(0) + 1;
    }
}

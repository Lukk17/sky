package com.lukk.sky.common.domain;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class SequenceEventAppender {

    private SequenceEventAppender() {
    }

    public static <E> int nextSequenceNumber(UUID aggregateId,
            Function<UUID, Optional<Integer>> findLastSequenceNumber) {
        return findLastSequenceNumber.apply(aggregateId).orElse(0) + 1;
    }
}

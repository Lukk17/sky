## Context

Spring Kafka's defaults optimize for throughput, not durability. That's the right call for an analytics pipeline; the wrong call for a user-visible notification fan-out. Every Sky Kafka topic carries an event a user expects to see in real time (offer posted, booking confirmed). Losing one means a user is confused; duplicating one means a user is annoyed. Both are bad; loss is worse.

The defaults stack uncomfortably:
- Producer `acks=1` accepts leader-only writes.
- Consumer `enable.auto.commit=true` commits offsets every 5s regardless of processing success.
- No idempotence means even with `acks=all` + retries, duplicates leak through.
- No DLQ means a single bad message either jams the partition (with manual ack) or silently disappears (with auto commit).

We're shifting all four knobs to the durability side. Cost: latency (waiting for ISR replication) and operational complexity (DLT topics to monitor).

## Goals / Non-Goals

**Goals:**
- At-least-once delivery from producer to consumer, with idempotence preventing visible duplicates.
- Poison pills route to a DLT after bounded retries; partition never stalls.
- Listener acknowledges only after the WebSocket emission completes.
- All knobs explicit in code, not relying on Spring defaults.

**Non-Goals:**
- Exactly-once semantics across producer + consumer + WebSocket. (Real exactly-once requires transactional outbox + WS-level dedupe; over-engineering for this workload.)
- Schema registry adoption (Avro/JSON-Schema). Worth doing later; bundled into `extract-sky-common`'s `KafkaPayloadModel` unification for now.
- Kafka Streams or KSQL.

## Decisions

1. **Manual ack mode = `MANUAL_IMMEDIATE`**, not `MANUAL`. `IMMEDIATE` commits synchronously when `ack.acknowledge()` is called; `MANUAL` batches with the container's commit interval and risks losing in-flight acks on shutdown.
2. **`DefaultErrorHandler` + `DeadLetterPublishingRecoverer`**, not the older `SeekToCurrentErrorHandler` (deprecated since Spring Kafka 2.8) or custom recoverers. Standard, well-documented.
3. **DLT topic naming `${topic}.DLT`** is the Spring Kafka default and what every operational tool expects.
4. **`FixedBackOff(1000ms, 3 retries)`** is conservative. Tune to exponential later if traffic warrants. Important: with manual ack + retries, total time per failed message is bounded (~3s) — partition unblock guaranteed.
5. **Idempotence requires `max.in.flight.requests.per.connection ≤ 5`**. Set it to 5 (the safe max) for some pipelining.
6. **Listener parameter list grows** (`Acknowledgment` added). Slight inelegance; accept it.
7. **Non-retryable exception classification matters more than backoff tuning.** A `JsonProcessingException` retried 3x wastes 3 seconds before DLT; classified non-retryable, it hits DLT instantly. Same for client validation errors.
8. **DLT processing**: out of scope here. Recommended follow-up: a manual UI or a scheduled job that surfaces DLT entries.

## Risks / Trade-offs

- **Manual ack increases the chance of a partition stall** if a listener throws without classifying the exception. Mitigation: every listener wraps the body in try/catch + DLT publish on unexpected failure (last-resort safety net before relying on `DefaultErrorHandler`).
- **Idempotence has a small memory overhead** on the broker. Negligible at our volume.
- **At-least-once still permits duplicates** at the WebSocket boundary (we emit → ack; if WS emit succeeds but ack write fails, we'll re-emit). Clients should be idempotent or carry a message ID. For Sky's notification semantics ("offer X posted"), duplicate WS messages are at worst a flash, not a correctness issue.
- **Latency**: `acks=all` waits for ISR. With one broker in dev, immediate; with three in prod, single-digit ms. Fine.
- **DLT monitoring**: a topic with no consumer is dead weight if nobody watches it. The follow-up job mentioned above is mandatory operationally; tracked as a separate change after this lands.

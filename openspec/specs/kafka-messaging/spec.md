# kafka-messaging Specification

## Purpose
TBD - created by archiving change kafka-reliability. Update Purpose after archive.
## Requirements
### Requirement: Producers acknowledge fully replicated, idempotent writes
Every Kafka producer in the system MUST be configured with `acks=all`, `enable.idempotence=true`, `retries=Integer.MAX_VALUE`, `delivery.timeout.ms=120000`, and `max.in.flight.requests.per.connection <= 5`. No producer may rely on Spring Kafka defaults for these properties.

#### Scenario: Verifying producer configuration
- **WHEN** the application boots and exposes `ProducerFactory.getConfigurationProperties()`
- **THEN** all five properties hold the configured values; the producer integration test asserts this

### Requirement: Consumers commit offsets only after side-effects succeed
Every `@KafkaListener` in sky-notify MUST run on a container configured with `AckMode.MANUAL_IMMEDIATE` and MUST acknowledge messages only after the downstream side-effect (WebSocket emission) completes successfully.

#### Scenario: A WebSocket emission fails mid-processing
- **WHEN** the WebSocket service throws during `convertAndSendToUser`
- **THEN** the listener does not call `ack.acknowledge()`; the message is re-delivered on the next poll

#### Scenario: A WebSocket emission succeeds
- **WHEN** `convertAndSendToUser` returns normally
- **THEN** `ack.acknowledge()` runs immediately; the offset commits and the message is not re-delivered

### Requirement: Failed messages route to a Dead Letter Topic
Every Kafka consumer container MUST register a `DefaultErrorHandler` with a `DeadLetterPublishingRecoverer` that publishes to `${topic}.DLT` after a bounded number of retries (default: 3 attempts, 1s backoff). Non-retryable exceptions (`JsonProcessingException`, `IllegalArgumentException`) MUST route to DLT on the first failure.

#### Scenario: A poison pill arrives
- **WHEN** a malformed JSON message lands on `bookingTopic-1`
- **THEN** the consumer routes it to `bookingTopic-1.DLT` on the first failure, partition processing continues uninterrupted

#### Scenario: A retryable downstream failure
- **WHEN** a downstream call fails with a transient exception on a valid message
- **THEN** the consumer retries up to the configured limit before publishing to DLT


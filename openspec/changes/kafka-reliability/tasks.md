## 1. Producer hardening (sky-common)

- [x] 1.1 In `sky-common`'s `KafkaProducerAutoConfiguration`, set the five producer properties listed in proposal.md.
- [x] 1.2 Add `KafkaProducerProperties` (`@ConfigurationProperties("sky.kafka.producer")`) exposing `bootstrap-servers`, `acks`, `retries`, `enable-idempotence`, `delivery-timeout`, `max-in-flight` with sensible defaults.
- [x] 1.3 Wire `spring.kafka.producer.bootstrap-servers` from each service's `application.yml` (already present as `kafka.host`/`kafka.port`).
- [x] 1.4 Add a producer-side integration test in `sky-booking` (existing `@EmbeddedKafka` test) asserting properties via `ProducerFactory.getConfigurationProperties()`.

## 2. Consumer hardening (sky-notify)

- [x] 2.1 Edit `KafkaConsumerConfig` in sky-notify: set `factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE)`; set `enable.auto.commit=false`; set `auto.offset.reset=earliest` (for resilience after restart).
- [x] 2.2 Update `KafkaListeners.bookingListener` and `offerListener` to take `Acknowledgment ack` parameter and call `ack.acknowledge()` only after `NotificationTransmissionService.notifyClient(...)` returns successfully.
- [x] 2.3 Wrap the listener body in try/catch to convert non-retryable failures into the right exception type (so DLT routing works correctly).

## 3. DLT + error handler

- [x] 3.1 Add `KafkaErrorHandlerConfig` in sky-common (or in sky-notify if not generally reusable). Wires:
  - `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` that routes to `${topic}.DLT`.
  - `FixedBackOff(1000L, 3L)` initially.
  - `addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class)`.
- [x] 3.2 Register the error handler on the `ConcurrentKafkaListenerContainerFactory` in sky-notify.
- [x] 3.3 Update Helm Kafka chart values to declare `bookingTopic-1.DLT` and `offerTopic-1.DLT` (Bitnami Kafka chart `provisioning.topics`).

## 4. Observability

- [x] 4.1 Add `LoggingErrorHandler` adapter or a `RetryListener` that logs `topic/partition/offset/attempt` on every retry.
- [x] 4.2 Expose Micrometer metrics: spring-kafka publishes consumer-lag metrics by default once Micrometer is wired. Confirm in `/actuator/metrics`.
- [x] 4.3 Document recommended Grafana panels (lag, DLT count) in a comment in the Kafka chart or Helm README — actual dashboards out of scope.

## 5. Tests

- [x] 5.1 sky-notify unit test: poison pill (malformed JSON) → routed to DLT, not retried indefinitely.
- [x] 5.2 sky-notify unit test: WS emission failure → message not acked → re-delivered (using `@EmbeddedKafka` with a controllable WS service).
- [x] 5.3 sky-notify unit test: successful path → acknowledged once → not re-delivered.
- [x] 5.4 Producer test (existing) updated to assert idempotence + acks=all from config.

## 6. Verify

- [x] 6.1 `./gradlew test` — all green.
- [x] 6.2 Smoke: book an offer locally, watch sky-notify logs for one (and only one) WS emission per Kafka message.
- [x] 6.3 Smoke: stop sky-notify, book three offers; restart sky-notify; observe all three notifications emit (consumer caught up).
- [x] 6.4 Smoke: inject a malformed payload; observe one entry in DLT, no infinite retries.

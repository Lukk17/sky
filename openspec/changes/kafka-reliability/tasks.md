## 1. Producer hardening (sky-common)

- [ ] 1.1 In `sky-common`'s `KafkaProducerAutoConfiguration`, set the five producer properties listed in proposal.md.
- [ ] 1.2 Add `KafkaProducerProperties` (`@ConfigurationProperties("sky.kafka.producer")`) exposing `bootstrap-servers`, `acks`, `retries`, `enable-idempotence`, `delivery-timeout`, `max-in-flight` with sensible defaults.
- [ ] 1.3 Wire `spring.kafka.producer.bootstrap-servers` from each service's `application.yml` (already present as `kafka.host`/`kafka.port`).
- [ ] 1.4 Add a producer-side integration test in `sky-booking` (existing `@EmbeddedKafka` test) asserting properties via `ProducerFactory.getConfigurationProperties()`.

## 2. Consumer hardening (sky-notify)

- [ ] 2.1 Edit `KafkaConsumerConfig` in sky-notify: set `factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE)`; set `enable.auto.commit=false`; set `auto.offset.reset=earliest` (for resilience after restart).
- [ ] 2.2 Update `KafkaListeners.bookingListener` and `offerListener` to take `Acknowledgment ack` parameter and call `ack.acknowledge()` only after `NotificationTransmissionService.notifyClient(...)` returns successfully.
- [ ] 2.3 Wrap the listener body in try/catch to convert non-retryable failures into the right exception type (so DLT routing works correctly).

## 3. DLT + error handler

- [ ] 3.1 Add `KafkaErrorHandlerConfig` in sky-common (or in sky-notify if not generally reusable). Wires:
  - `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` that routes to `${topic}.DLT`.
  - `FixedBackOff(1000L, 3L)` initially.
  - `addNotRetryableExceptions(JsonProcessingException.class, IllegalArgumentException.class)`.
- [ ] 3.2 Register the error handler on the `ConcurrentKafkaListenerContainerFactory` in sky-notify.
- [ ] 3.3 Update Helm Kafka chart values to declare `bookingTopic-1.DLT` and `offerTopic-1.DLT` (Bitnami Kafka chart `provisioning.topics`).

## 4. Observability

- [ ] 4.1 Add `LoggingErrorHandler` adapter or a `RetryListener` that logs `topic/partition/offset/attempt` on every retry.
- [ ] 4.2 Expose Micrometer metrics: spring-kafka publishes consumer-lag metrics by default once Micrometer is wired. Confirm in `/actuator/metrics`.
- [ ] 4.3 Document recommended Grafana panels (lag, DLT count) in a comment in the Kafka chart or Helm README — actual dashboards out of scope.

## 5. Tests

- [ ] 5.1 sky-notify unit test: poison pill (malformed JSON) → routed to DLT, not retried indefinitely.
- [ ] 5.2 sky-notify unit test: WS emission failure → message not acked → re-delivered (using `@EmbeddedKafka` with a controllable WS service).
- [ ] 5.3 sky-notify unit test: successful path → acknowledged once → not re-delivered.
- [ ] 5.4 Producer test (existing) updated to assert idempotence + acks=all from config.

## 6. Verify

- [ ] 6.1 `./gradlew test` — all green.
- [ ] 6.2 Smoke: book an offer locally, watch sky-notify logs for one (and only one) WS emission per Kafka message.
- [ ] 6.3 Smoke: stop sky-notify, book three offers; restart sky-notify; observe all three notifications emit (consumer caught up).
- [ ] 6.4 Smoke: inject a malformed payload; observe one entry in DLT, no infinite retries.

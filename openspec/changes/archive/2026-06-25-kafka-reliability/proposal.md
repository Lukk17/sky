## Why

The audit identified five production reliability gaps in the current Kafka setup, every one of which is the default value silently in effect:

1. **`acks=1`** (Kafka producer default) — leader writes succeed; replicas may not have the record before leader fails. Loss window.
2. **`enable.auto.commit=true`** (consumer default) — offsets commit before processing succeeds. Any exception during `NotificationTransmissionService.notifyClient()` → offset advanced → message lost.
3. **No `enable.idempotence`** — retries can duplicate.
4. **No retries config** — transient failures drop messages silently.
5. **No DLQ / no `DefaultErrorHandler`** — a poison pill blocks the partition forever or skips silently.

In the Sky data flow (sky-booking/offer → Kafka → sky-notify → WebSocket clients), losing or duplicating events means users miss notifications. The fix is well-trodden Spring Kafka idiom; the cost is a few config lines plus one DLQ topic per source topic. The change pairs with `extract-sky-common` (DLQ payload type lives in `sky-common`) and `websocket-auth` (manual ack on the consumer side requires us to ack only after WS emit succeeds).

## What Changes

- **Producer (sky-booking, sky-offer)**: in `sky-common`'s `KafkaProducerAutoConfiguration`, set:
  - `acks=all`
  - `enable.idempotence=true`
  - `retries=Integer.MAX_VALUE`
  - `delivery.timeout.ms=120000`
  - `max.in.flight.requests.per.connection=5` (idempotence cap)
- **Consumer (sky-notify)**: switch container `ack-mode` to `MANUAL_IMMEDIATE`. Listeners take `Acknowledgment ack` and `ack.acknowledge()` only after successful WebSocket emit. Set `enable.auto.commit=false`.
- **DLQ**: configure `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` (routes to `${topic}.DLT` after N retries). Add `bookingTopic-1.DLT` and `offerTopic-1.DLT` to the Kafka chart's topic list.
- **Backoff**: `FixedBackOff(1000L, 3L)` — 3 retries with 1s pause; tune later. (Alternative: `ExponentialBackOffWithMaxRetries`.)
- **Non-retryable exceptions**: classify (`HttpClientErrorException.BadRequest`, `JsonProcessingException` for malformed JSON) so poison pills hit DLQ immediately.
- **Observability**: log every retry and every DLT publish with topic/partition/offset.
- **Monitoring**: expose Kafka consumer lag and DLT depth via Actuator + Micrometer (already on classpath via actuator).

## Capabilities

### New Capabilities
- `kafka-reliability`: Producers acknowledge fully replicated and idempotent writes; consumers commit manually after side-effects succeed; failed messages route to a Dead Letter Topic; retries and backoff are explicit, not default.

### Modified Capabilities
- `sky-common` (from `extract-sky-common`): adds DLT payload wrapper type and shared error-handler factory.

## Impact

- **Touched files**: `sky-common` (producer config + DLT helper), `sky-booking` and `sky-offer` (producer configuration through auto-config — no per-service edit needed), `sky-notify` consumer config + each `@KafkaListener` method (gain `Acknowledgment` param), Helm Kafka chart values (declare DLT topics).
- **Wire compatibility**: no payload format changes. Just QoS upgrades.
- **Performance**: `acks=all` adds latency (waits for replicas). At Sky's volume, negligible.
- **Risk**: medium. Manual ack means a bug in the listener can stall a partition. Mitigated by DLQ + observability.
- **Dependency order**: depends on `extract-sky-common` (producer config moves there first). Should land before `websocket-auth` only if the auth interceptor also needs manual-ack semantics; otherwise independent.

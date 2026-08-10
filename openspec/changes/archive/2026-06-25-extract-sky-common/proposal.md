## Why

Four types of code are copy-pasted across services with active drift:

1. **`KafkaPayloadModel`** — defined in sky-booking, sky-offer, and sky-notify. Notify's version uses `message`; the other two use `payload`. This is real schema drift on a wire contract.
2. **`Constants`** (`DATE_TIME_FORMAT`, `USER_INFO_HEADERS`) — duplicated in booking, offer, and message.
3. **`KafkaProducerConfig`** — duplicated near-verbatim in booking and offer.
4. **`GlobalExceptionHandler`** — duplicated across three services with identical handler shapes; only the service-specific exception types differ.

A shared `sky-common` module removes the drift surface, makes Kafka contracts type-safe across producer/consumer, and lets every service inherit a single exception-handler base. This change is the prerequisite for `kafka-reliability` (so the DLQ contract is shared) and pairs with `hexagonal-enforcement-archunit`.

## What Changes

- **Add** `:sky-common` Gradle module (requires `gradle-multi-project` to be in place first).
- **Move** `KafkaPayloadModel` into `com.lukk.sky.common.kafka.KafkaPayloadModel`. Unify the field name to `payload` (notify migrates). Mark as `record`.
- **Move** `Constants` into `com.lukk.sky.common.web.Constants` (or split: `DateTimeConstants`, `WebHeaders`).
- **Move** Kafka producer config into `com.lukk.sky.common.kafka.KafkaProducerConfig` (`@AutoConfiguration` with `@ConditionalOnClass(KafkaTemplate.class)` so only Kafka-using services pick it up). Optionally expose a `@ConfigurationProperties` class for bootstrap servers.
- **Move** `GlobalExceptionHandler` skeleton into `com.lukk.sky.common.web.AbstractGlobalExceptionHandler` — handles `MethodArgumentNotValidException`, generic 500 fallback, and consistent `ErrorResponse` shape. Each service subclasses with `@RestControllerAdvice` and adds its own exception types (BookingException, OfferException, MessageException).
- **Update** `sky-booking`, `sky-offer`, `sky-message`, `sky-notify` `build.gradle.kts` to depend on `:sky-common` and delete the duplicated files.
- **Auto-configuration** registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` so Kafka/Web/etc. setup is discovered only by services on the right classpath.

## Capabilities

### New Capabilities
- `sky-common`: Shared module owning cross-service wire types (Kafka payloads), web utilities (constants, exception handler base), and conditional auto-configurations for Kafka producers.

### Modified Capabilities
- _None._ Behavior is unchanged; only the location of types changes. The Kafka payload field rename (`message` → `payload` in notify) is the one breaking change inside the repo — coordinated within the same commit.

## Impact

- **Touched files**: new `sky-common/` tree (~10 files), 4 service `build.gradle.kts` edits, ~12 deletions of duplicated files across services, ~6 import updates in services that subclass the new exception handler.
- **Wire compatibility**: Kafka topic payloads should already be the same JSON shape from booking and offer; notify deserializes the same JSON. Field rename is internal — only `KafkaPayloadModel.message` → `.payload` field access changes in notify consumer code. In-flight messages during deploy are unaffected because both names map to the same JSON key (we'll keep the JSON key stable via `@JsonProperty` if needed).
- **CI/CD**: builds gain a new module; deploy scripts unchanged.
- **Risk**: medium. Schema-touching, but contained.
- **Dependency order**: depends on `gradle-multi-project`. Should land before `kafka-reliability` (DLQ contract belongs in common) and `hexagonal-enforcement-archunit` (ArchUnit rules will allow `sky.common.*` imports explicitly).

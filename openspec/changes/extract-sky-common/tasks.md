## 1. Create the module

- [ ] 1.1 Add `sky-common/` directory with `build.gradle.kts` applying `sky.java-conventions` only (no Spring Boot plugin — this is a library, not an app). Set `bootJar.enabled = false`, `jar.enabled = true`.
- [ ] 1.2 Add `:sky-common` to root `settings.gradle.kts`.
- [ ] 1.3 Add minimal deps in `sky-common/build.gradle.kts`: spring-context (for `@Configuration`), spring-kafka API, jackson-databind, validation API, lombok. Keep it framework-light; pure libraries only.

## 2. Move shared types

- [ ] 2.1 Create `sky-common/src/main/java/com/lukk/sky/common/kafka/KafkaPayloadModel.java` as a record with field `payload` (unifying notify's `message`). Add `@JsonProperty` aliases if needed to stay wire-compatible with old payloads in flight.
- [ ] 2.2 Create `sky-common/src/main/java/com/lukk/sky/common/web/DateTimeConstants.java` (formatter) and `com.lukk.sky.common.web.WebHeaders.java` (user info header set).
- [ ] 2.3 Create `sky-common/src/main/java/com/lukk/sky/common/kafka/KafkaProducerAutoConfiguration.java` — `@AutoConfiguration`, `@ConditionalOnClass(KafkaTemplate.class)`, exposes `ProducerFactory<String,String>` and `KafkaTemplate<String,String>` beans. Reuses the booking/offer config verbatim.
- [ ] 2.4 Create `sky-common/src/main/java/com/lukk/sky/common/web/AbstractGlobalExceptionHandler.java` — abstract class (not `@RestControllerAdvice`) with `@ExceptionHandler` for `MethodArgumentNotValidException` (returns 400 with field errors) and a catch-all `Exception` handler (returns 500 with sanitized message). Concrete service handlers extend and add their own exception types.
- [ ] 2.5 Register auto-configurations: `sky-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` listing `com.lukk.sky.common.kafka.KafkaProducerAutoConfiguration`.

## 3. Migrate sky-booking

- [ ] 3.1 Add `implementation(project(":sky-common"))` to `sky-booking/build.gradle.kts`.
- [ ] 3.2 Delete `sky-booking/src/main/java/com/lukk/sky/booking/adapters/dto/KafkaPayloadModel.java`, `config/Constants.java`, `config/kafka/KafkaProducerConfig.java`.
- [ ] 3.3 Update imports in producer code (`BookingNotificationServicePrimary`) to `com.lukk.sky.common.kafka.KafkaPayloadModel`.
- [ ] 3.4 Update `GlobalExceptionHandler` to extend `AbstractGlobalExceptionHandler`, keep the `BookingException` handler method.

## 4. Migrate sky-offer

- [ ] 4.1 Add `implementation(project(":sky-common"))`. Delete duplicated files. Update imports. Subclass exception handler.

## 5. Migrate sky-message

- [ ] 5.1 Add `implementation(project(":sky-common"))`. Delete duplicated `Constants` and `GlobalExceptionHandler` skeleton. Subclass exception handler.

## 6. Migrate sky-notify

- [ ] 6.1 Add `implementation(project(":sky-common"))`. Delete duplicated `KafkaPayloadModel`.
- [ ] 6.2 Update consumer code that referenced `.message` field to `.payload`. Add a regression test for JSON deserialization with both field names if the wire format used both historically.

## 7. Verify

- [ ] 7.1 `./gradlew :sky-common:build` — module compiles standalone.
- [ ] 7.2 `./gradlew build` — all four services compile and tests pass.
- [ ] 7.3 Integration test: produce a Kafka message from booking, consume in notify (via existing `@EmbeddedKafka` test), assert payload deserializes with the unified field name.
- [ ] 7.4 Grep for `class KafkaPayloadModel` — should appear only in `sky-common`.
- [ ] 7.5 Grep for `class Constants` under `sky-booking`/`sky-offer`/`sky-message`/`sky-notify` — should be gone.

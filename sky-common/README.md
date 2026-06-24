# sky-common

*Shared library for the Sky platform.*

Not a deployable service: no port, no `main` class, no `bootJar`.

---

### What it is

`sky-common` is a plain Java library consumed by `sky-booking`, `sky-offer`, `sky-message`, and `sky-notify` via
`implementation(project(":sky-common"))`. It holds the types and auto-configurations that would otherwise be
duplicated across services.

Current version: `1.0.2`.

---

### What it exports

**`com.lukk.sky.common.kafka`**

- `KafkaPayloadModel`: a record carrying `payload`, `accessedAt`, and `userInfo`. All four services use this as the
  Kafka message envelope.
- `KafkaProducerAutoConfiguration`: Spring auto-configuration that wires a Kafka producer template. Registered in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

**`com.lukk.sky.common.web`**

- `WebHeaders`: holds `USER_INFO_HEADERS`, the set of trusted identity headers. Currently `{"x-auth-request-email"}`.
  All services read user identity through this constant, not by accessing headers directly.
- `AbstractRestExceptionHandler`: base class for RFC 7807-style error responses. Services extend it to add
  service-specific exception mappings.
- `DateTimeConstants`: shared date/time format constants.

---

### Dependency discipline

All Spring dependencies in `sky-common` are declared `compileOnly`. A service that does not use Spring MVC (or Kafka)
must not pull those frameworks transitively just by depending on this library. Tests re-add the real dependencies via
`testImplementation`.

Do not promote a `compileOnly` dependency to `implementation` without checking every consumer first.

---

### Extending it

Adding a shared type:

1. Place it in the appropriate package (`common.web`, `common.kafka`, or a new one).
2. Keep any new framework dependency `compileOnly` unless every consumer already pulls it.
3. If you add an auto-configuration class, register it in
   `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
4. Verify by running the consuming service tests; `sky-common` has no standalone application to run.

Build:

```bash
./gradlew :sky-common:build
```

```powershell
.\gradlew.bat :sky-common:build
```

---

### Docs map

| Document | What it covers |
|---|---|
| [../README.md](../README.md) | Root README: full platform overview, build, deployment |
| [AGENTS.md](AGENTS.md) | Module-local agent/coding conventions |

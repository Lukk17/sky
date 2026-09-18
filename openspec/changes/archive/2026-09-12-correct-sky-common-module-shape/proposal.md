## Why

The single requirement in `sky-common` describes a module shape that does not exist, and two of its three wrong facts
would mislead a contributor into the wrong design rather than merely into a wrong name.

It says the module owns an exception handler base class. There is no base class to extend. The handler is
`sky-common/src/main/java/com/lukk/sky/common/web/SkyRestExceptionHandler.java`, a `@RestControllerAdvice` that extends
Spring's own `ResponseEntityExceptionHandler`, and it reaches a service as a bean rather than as a superclass:
`sky-common/src/main/java/com/lukk/sky/common/web/RestExceptionHandlerAutoConfiguration.java` registers it at line 20
under `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`. No service extends it. All three REST services
carry their own independent `GlobalExceptionHandler` alongside it. A contributor following the specification would write
an `extends` that the design does not want.

It says the module holds conditional auto-configurations for shared infrastructure, naming Kafka producer beans, and its
second scenario attributes Kafka isolation to an auto-configuration annotated
`@ConditionalOnClass(KafkaTemplate.class)`. No such auto-configuration exists. The ten classes registered in
`sky-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` include
no Kafka entry, and `@ConditionalOnClass(KafkaTemplate.class)` appears nowhere in the module.
`sky-common/src/main/java/com/lukk/sky/common/kafka/KafkaNotificationPublisher.java` is a plain `final` class that each
service constructs itself in its own outbound notification adapter. What actually keeps Kafka off a non-Kafka
consumer's classpath is the `compileOnly` declaration at `sky-common/build.gradle.kts` line 27 plus per-module opt-in:
`sky-message/build.gradle.kts` applies `sky.web-conventions` and not `sky.kafka-conventions`, so it never gets
`spring-boot-starter-kafka` and `KafkaTemplate` is not on its classpath at all. That mechanism is the module's central
design rule and the specification describes a different one.

It says `sky-common` packages are limited to `kafka`, `web` and `time`. There are six packages under
`sky-common/src/main/java/com/lukk/sky/common`: `config`, `kafka`, `openapi`, `security`, `startup` and `web`. None is
named `time`, and `security` is the largest of them. The same scenario credits the limit to an ArchUnit rule in a
capability called `hexagonal-enforcement`. No capability of that name exists under `openspec/specs/`, the capability is
`architecture`, and `sky-common` has no ArchUnit test at all: the four `HexagonalArchitectureTest` classes live in
`sky-booking`, `sky-offer`, `sky-message` and `sky-notify`.

## What Changes

- Restate the requirement so the shared web utilities are described as an auto-configured handler bean rather than a
  base class, and so the prohibition that matters is stated: a service does not extend it, it replaces it by defining
  its own bean.
- Replace the claim about Kafka producer auto-configurations with the mechanism that is really in force, every shared
  runtime dependency declared `compileOnly` so a consumer opts in with its own `implementation` entry.
- Correct the package list to the six that exist, and state the rule as what the packages may hold rather than as a
  closed list of three names, so the constraint survives the next package being added for a good reason.
- Drop the reference to an ArchUnit rule in `hexagonal-enforcement`, which names a capability that does not exist and a
  rule that does not exist, and state the real enforcement, which is review plus the `compileOnly` split that makes a
  service-specific dependency fail to compile here.
- Keep the scenario `Auto-configuration is opt-in` and rewrite its body around a case that is actually conditional,
  because `sky-notify` applies no web conventions and therefore gets neither the springdoc auto-configuration nor the
  Spring Data exception handler, both of which are `@ConditionalOnClass`.
- Remove the semicolon clause joins from all three lines in the file, which is every line that has one, because the
  requirement and both affected scenarios are being rewritten from evidence anyway.

Nothing here changes code. The requirement is aligned to a module that already ships.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `sky-common`: the whole single requirement, covering the handler base class, the Kafka auto-configuration claim, the
  package list, and the ArchUnit enforcement claim.

## Impact

- Affected file: `openspec/specs/sky-common/spec.md`, rewritten at archive time from the delta spec in this change.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched.
- `sky-common/AGENTS.md` already describes the real shape and was used as a lead rather than as evidence. Every fact
  above was re-established from the source tree, the auto-configuration imports file and the build files, and the guide
  was found accurate on all four points.
- Risk: low. Every correction moves the specification toward a class, a file or a build line that already exists, and
  the one judgement call is stating the package rule as a constraint on content rather than as a list of names.

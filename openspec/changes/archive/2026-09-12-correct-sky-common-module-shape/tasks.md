## 1. Establish the exception handler shape

- [x] 1.1 Confirm there is no base class by reading
  `sky-common/src/main/java/com/lukk/sky/common/web/SkyRestExceptionHandler.java` and verifying it is a
  `@RestControllerAdvice` that extends Spring's `ResponseEntityExceptionHandler`, and is not itself abstract
- [x] 1.2 Confirm how it reaches a consumer by reading
  `sky-common/src/main/java/com/lukk/sky/common/web/RestExceptionHandlerAutoConfiguration.java` and recording that it is
  registered as a bean under `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)`, which is the sanctioned
  override route
- [x] 1.3 Confirm no service extends it by grepping the four service source trees for `SkyRestExceptionHandler` and
  `SpringDataExceptionHandler` and verifying zero matches
- [x] 1.4 Confirm the services carry independent handlers by grepping for `class GlobalExceptionHandler` and verifying
  the three REST services each declare one that extends nothing from `sky-common`

## 2. Establish the Kafka mechanism

- [x] 2.1 Confirm no Kafka auto-configuration is registered by reading
  `sky-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` and
  verifying its ten entries include no Kafka class
- [x] 2.2 Confirm the annotation the specification names is absent by grepping the module for
  `@ConditionalOnClass(KafkaTemplate` and verifying zero matches, then listing every `@ConditionalOnClass` in the module
  to record what the real conditions are
- [x] 2.3 Confirm the publisher is constructed by hand by reading
  `sky-common/src/main/java/com/lukk/sky/common/kafka/KafkaNotificationPublisher.java` and grepping its uses, verifying
  it is a plain `final` class that `sky-booking` and `sky-offer` each instantiate in their own outbound notification
  adapter
- [x] 2.4 Confirm the real isolation mechanism by reading the `compileOnly` declaration for Spring Kafka in
  `sky-common/build.gradle.kts` and the `plugins` block of `sky-message/build.gradle.kts`, verifying sky-message applies
  `sky.web-conventions` and not `sky.kafka-conventions`
- [x] 2.5 Confirm the case for the `Auto-configuration is opt-in` scenario by reading the `plugins` block and
  `dependencies` block of `sky-notify/build.gradle.kts`, verifying it applies no `sky.web-conventions` and so has neither
  springdoc nor Spring Data, and that the springdoc and Spring Data auto-configurations are both `@ConditionalOnClass`

## 3. Establish the package list and the enforcement claim

- [x] 3.1 List the immediate subdirectories of `sky-common/src/main/java/com/lukk/sky/common` and verify there are six,
  that they are `config`, `kafka`, `openapi`, `security`, `startup` and `web`, and that none is named `time`
- [x] 3.2 Locate the type the stale `time` package would have held by finding `DateTimeConstants` and verifying it lives
  in `web`
- [x] 3.3 Confirm no ArchUnit rule exists here by listing `sky-common/src/test` and verifying it holds no architecture
  test, and that the four `HexagonalArchitectureTest` classes are in the four services instead
- [x] 3.4 Confirm `hexagonal-enforcement` is not a capability by listing `openspec/specs/` and verifying the
  architecture capability is named `architecture`, and that `hexagonal-enforcement-archunit` is an archived change rather
  than a capability
- [x] 3.5 Record each package's concern from its own class list, so the six concerns in the delta come from the source
  tree rather than from `sky-common/AGENTS.md`
- [x] 3.6 Having established all four facts independently, compare them against `sky-common/AGENTS.md` and record whether
  the guide is accurate, because a guide can be stale too and this one was used only as a lead

## 4. Write the delta specification

- [x] 4.1 Write `specs/sky-common/spec.md` under `## MODIFIED Requirements`, carrying the whole single requirement block,
  and verify the requirement header matches the merged file character for character
- [x] 4.2 Verify the delta carries all three existing scenario names unchanged, `Kafka payload type is defined once`,
  `Auto-configuration is opt-in` and `No domain logic in sky-common`, because the archive step refuses a MODIFIED block
  that drops a scenario name
- [x] 4.3 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against a fixture
  containing an em dash, an en dash, an arrow and a bullet
- [x] 4.4 Verify the delta respects the wrap width of the merged file, which is one physical line per paragraph and per
  scenario bullet
- [x] 4.5 Run `openspec validate correct-sky-common-module-shape --strict` and verify it reports no error

## 5. Archive and sync

- [x] 5.1 Archive the change and verify `openspec/specs/sky-common/spec.md` contains no `exception handler base class`,
  no `ConditionalOnClass(KafkaTemplate`, no `hexagonal-enforcement` and no `time` namespace claim
- [ ] 5.2 Verify the same file now names the `compileOnly` rule, the `@ConditionalOnMissingBean` override route, and all
  six packages
- [x] 5.3 Verify the surviving rules are intact: the module owns the Kafka payload record once, and it holds no business
  or domain logic
- [x] 5.4 Verify the change directory moved under `openspec/changes/archive/` and that `openspec list` no longer reports
  it active
- [x] 5.5 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 6. Notes from the run

- The merged-file rewrite was performed by `openspec archive correct-sky-common-module-shape --yes`, which reported
  `~ 1 modified` against `sky-common`. No hand edit of a merged specification was needed.
- Task 5.2 is the one task here that did not pass, and it caught a defect in this change rather than in the repository.
  Two of its three checks passed: the merged specification names the `compileOnly` rule and all six packages. The third
  failed. design.md, Decisions, states that the corrected requirement would name the `@ConditionalOnMissingBean` override
  route, because saying the handler is a bean rather than a base class describes the mechanism without telling a service
  what to do when it needs different behaviour. The delta described the mechanism and omitted the route, so the merged
  requirement forbids extending the handler without saying how to replace it.
- That gap is closed by the follow-up change `add-sky-common-handler-override-route`, through a second MODIFIED delta on
  the same requirement, rather than by editing the merged specification in place. Leaving task 5.2 unchecked here is
  deliberate: this change did not do what its own design said it would.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. Left as
  written, consistent with the three changes archived before it today.

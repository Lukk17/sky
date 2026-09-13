## Why

The requirement `Hexagonal layer separation is enforced by ArchUnit` in the `architecture` capability names four
guarantees and credits all of them to each service's own ArchUnit test. None of the four was enforced. The predecessor
change `correct-canonical-package-layout` found them and reported them without correcting them, because correcting them
was outside its approved scope. This is false confidence rather than stale prose: a reader who trusts the sentence
believes a build gate exists where there is nothing at all.

A repository injected straight into a controller is supposed to fail the build. It does not. A repository interface
lives in `domain.ports.outbound`, which makes it a port, and the only adapter rule in place forbids an adapter
depending on `domain.service`. A controller reaching a repository therefore passes every rule in every service. No
controller does it today, so the missing rule has cost nothing yet, which is exactly why it has gone unnoticed.

The domain is supposed to depend only on itself, on `sky-common` and on `java`. That has never been true of any
service and it contradicts the other requirement in the same capability. `Booking`, `Offer`, `Event` and `Message` each
carry `jakarta.persistence` annotations, extend nothing and are the persistence entity as well as the domain model,
which is the pattern this project chose. The layout requirement enforces that choice by placing every `@Entity` in
`domain.model`. One requirement therefore mandates what the other forbids, and no rule enforces either half of the
contradiction, so both have gone unchallenged.

No cross-service import is supposed to be permitted, and it holds, but nothing tested it. The guarantee rests on each
service's `build.gradle.kts` declaring `implementation(project(":sky-common"))` and no other project dependency.

`config` is supposed to be allowed to depend on both `adapters` and `domain`. That is a permission rather than a
prohibition, so there is nothing for a rule to fail on, and the requirement was crediting a test with enforcing
something no test can enforce.

## What Changes

- Add a rule to sky-booking, sky-offer and sky-message that fails the build when a class under `adapters.inbound`
  depends on a Spring Data repository or on a `Repository`-suffixed interface. sky-notify holds no persistence and
  gets no such rule.
- Add a rule to all four services that restricts the domain to an allow list of packages, written to permit exactly
  what the domain reaches today and nothing more, so a new framework dependency in the domain fails the build until
  this specification is changed to admit it. sky-notify's allow list is shorter, because it stores nothing.
- Add a rule to the three services that carry a `@RestControllerAdvice` under `domain.exception`, bounding that one
  class to the domain allow list plus the Spring annotation and status types it needs, so the carve-out is a bounded
  exception rather than an open hole.
- Add a rule to all four services that fails the build when a class depends on another service's classes, as a second
  line behind the Gradle module graph rather than as a replacement for it.
- Rewrite the requirement so every guarantee names the thing that enforces it, correct the domain dependency sentence
  to the allow list the services genuinely need, record the three entries in that list that are debt rather than
  design, and say plainly that the `config` permission is not test-enforced because a permission has nothing to fail
  on.

No production class changed. Every new rule passes on the tree as it stands, and each one was proved to bite by
injecting a violation, running the suite, reading the failure and removing the injection again.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `architecture`: the requirement `Hexagonal layer separation is enforced by ArchUnit`, its dependency rules, its
  scope and its scenarios.

## Impact

- Affected specification: `openspec/specs/architecture/spec.md`, rewritten at archive time from the delta in this
  change. One of its two requirements is touched and the other is left byte-identical.
- Affected tests, one file per service, all four under `src/test/java/com/lukk/sky/<service>/architecture`:
  `HexagonalArchitectureTest`. sky-booking, sky-offer and sky-message gain four rules each and sky-notify gains two.
  `sky-gateway` gains nothing and still carries no architecture test, by the recorded decision that its routing table
  is configuration rather than code.
- Affected module guides: `sky-booking/AGENTS.md`, `sky-offer/AGENTS.md`, `sky-message/AGENTS.md` and
  `sky-notify/AGENTS.md`, each of which tells a contributor what its module's ArchUnit test enforces.
- No production source, chart, compose file, migration, Bruno request or OpenAPI contract is touched.
- Risk: low on the tests, since every rule was run red before it was run green. The residual risk is in the allow
  list, which is a snapshot of today's dependencies: a legitimate new dependency in the domain now needs a
  specification change before it can compile past the gate, which is the intended cost.

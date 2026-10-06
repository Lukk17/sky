## Context

The `architecture` capability has two requirements. `Canonical package layout per service` was corrected on
2026-09-12 and is accurate. `Hexagonal layer separation is enforced by ArchUnit` was left alone by that change, and
its own proposal recorded four defects in it. This change settles all four.

Three of the four are a missing rule. The fourth is a sentence about the domain that no rule could enforce without
first deciding whether the sentence or the code is wrong.

## Goals

- Every guarantee the requirement states is traceable to a named rule or to a named build file, with the requirement
  saying which.
- Every new rule was observed failing on a deliberate violation before it was trusted, because an ArchUnit rule whose
  selector matches nothing passes and reads like enforcement.
- The four services stay deliberately unlike each other where their shapes differ, rather than being flattened into
  one identical test.

## Non-Goals

- Removing the existing rules that the new allow list subsumes. The older narrow rules give a sharper message for the
  mistake they name, and deleting a passing test was not the approved scope.
- Repairing the three deviations the allow list records. Each is a real refactor with a behavioural surface, so each
  is named in the specification and left for its own change.
- Giving `sky-gateway` an architecture test. Its routing table is configuration and the recorded decision stands.

## Decisions

### The domain allow list admits the persistence API, and the old sentence was wrong

The sentence said the domain may depend only on itself, on `sky-common` and on `java`. Two facts settle it. First, no
service has ever satisfied it: `Booking`, `Offer`, `Event` and `Message` carry `jakarta.persistence` and
`jakarta.validation` annotations, and the repository ports extend Spring Data's `JpaRepository`. Second, the other
requirement in the same capability enforces that arrangement, by requiring every `@Entity` to live in `domain.model`
and every repository interface to live in `domain.ports.outbound`. A specification cannot mandate a persistence
annotation in a package and forbid the persistence API from that package.

So the sentence is the thing that is wrong, not the code. The pattern the project chose, one class serving as both
the domain model and the persistence entity, is a legitimate trade: it keeps a small service free of a mapping layer
and it costs the domain its framework independence. The alternative, a separate persistence entity per aggregate with
a mapper on each side, is better isolation and more code, and adopting it is a decision in its own right rather than
a side effect of writing a rule.

The rule therefore enumerates what the domain genuinely needs and rejects everything else. That is worth more than
the old sentence was, because it converts an untrue absolute into a gate: a new framework reaching the domain now
fails the build until this specification admits it by name.

### Three entries in the allow list are debt, and the specification says so

`com.lukk.sky.<service>.adapters.dto` is in the list because `domain.service` builds and reads the wire DTO directly.
`tools.jackson.databind` is in the list because `EventSourceServicePrimary` in sky-booking and sky-offer serialises
that DTO into an event payload inside the domain. The `@RestControllerAdvice` living under `domain.exception` is a
driving adapter sitting in the domain, carved out by annotation rather than by package so that exactly one class gets
the exemption and it stays bound by the rest of the list.

Each is recorded in the requirement as a deviation to be resolved, in the same form the layout requirement already
uses for `adapters.outbound.service` in sky-notify. Naming them in the specification is the difference between an
allow list that documents a decision and one that quietly launders debt.

### The cross-service rule is worth writing, against the first instinct

The instinct was that a test which can never fail is not worth writing, and that the honest fix was to credit the
Gradle module graph. That was measured rather than assumed, and the measurement changed the answer. ArchUnit records a
dependency on a class outside the imported set: a rule in sky-booking forbidding `com.lukk.sky.common` fails with 39
violations even though sky-common is a separate jar that the importer never reads. So a cross-service rule is not
vacuous. It was then proved end to end by adding `implementation(project(":sky-offer"))` to sky-booking and importing
`com.lukk.sky.offer.domain.model.Offer` into a domain class, which the rule caught by name.

The rule still cannot fire on a Java edit alone, because the import does not compile until the build file gains the
project dependency. So the build file is the primary gate and the rule is a second line, and the requirement says
exactly that rather than crediting the test with the whole guarantee.

The predicate is written as `com.lukk.sky` minus the service's own root and `com.lukk.sky.common`, rather than as a
list of the other three services, so a fifth module needs no edit in four files.

### The repository rule matches the port, not just the package

A controller could reach a repository declared anywhere, so the rule matches the type rather than the location: a
class assignable to Spring Data's `Repository`, or an interface whose simple name ends in `Repository`. The second
clause is deliberate belt and braces, in case hierarchy resolution ever fails to reach the Spring Data jar. The
selector is `adapters.inbound` rather than the controller annotation, so a Kafka listener reaching a repository is
caught too.

The rule lands green, because no controller injects a repository today. Two of the three controllers do inject an
outbound notification port and publish Kafka events themselves, which is a layering smell of the same family and is
not what this rule addresses. Widening the rule to forbid an inbound adapter from reaching any outbound port would go
red on sky-booking and sky-offer, and the fix would be moving event publication into the domain service, which
changes behaviour ordering and its tests. That is a separate change with its own decision to make, so it is reported
rather than folded in here.

### The advice carve-out excludes the HTTP client packages

The exception advice needs `org.springframework.http`, `org.springframework.web` itself for `ErrorResponse`, and
`org.springframework.web.bind.annotation`. It does not get `org.springframework.web..` as a whole, because that would
admit `org.springframework.web.client.RestClient` and quietly falsify the requirement's own scenario about a domain
class importing a Spring web client.

## Risks

- The allow list is a snapshot. A legitimate new dependency in the domain now fails the build until the
  specification is changed. That friction is the point, and it is stated in the requirement so a contributor meeting
  it knows what to do rather than reaching for an exclusion.
- The new rules overlap the older narrow ones, so a single mistake can fail two tests at once. That costs noise in a
  failure report and buys a sharper message for the common cases.

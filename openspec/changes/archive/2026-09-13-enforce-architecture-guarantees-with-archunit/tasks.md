## 1. Verify each claimed guarantee against the code before writing any rule

- [x] 1.1 Read all four `HexagonalArchitectureTest` classes in full and list every rule, so a rule is not added twice
  under a second name
- [x] 1.2 Establish whether any controller injects a repository, by reading the constructor of `BookingController`,
  `OfferApiController` and `MessageController`, and record the answer whichever way it comes out, because it decides
  whether the new rule lands green or red
- [x] 1.3 Establish what the domain genuinely depends on, from bytecode rather than from imports, by walking
  `getDirectDependenciesFromSelf` over every class under `domain` and printing the distinct targets, so a dependency
  Lombok generates is not missed
- [x] 1.4 Read `Booking` and confirm the entity carries `jakarta.persistence` annotations and an `org.hibernate`
  identity contract, so the claim about the domain being framework free is settled against the code
- [x] 1.5 Verify that each service's `build.gradle.kts` declares exactly one project dependency, which is
  `:sky-common`, so the cross-service guarantee is traced to the artefact that actually provides it
- [x] 1.6 Test whether ArchUnit sees a dependency on a class outside the imported set, by writing a throwaway rule in
  sky-booking forbidding `com.lukk.sky.common`, and record the result, because it decides whether a cross-service rule
  can ever fire

## 2. Write the rules, one service at a time, and prove each one bites

- [x] 2.1 Add the repository rule to sky-booking, run it green, then inject a `BookingRepository` field into
  `BookingController`, run it red, read the message and remove the injection
- [x] 2.2 Add the domain allow list rule to sky-booking, run it green, then inject a `ByteArrayResource` field into
  `BookingPersister`, run it red, read the message and remove the injection
- [x] 2.3 Add the `@RestControllerAdvice` carve-out rule to sky-booking, run it green, then inject a
  `ByteArrayResource` field into `GlobalExceptionHandler`, run it red, read the message and remove the injection
- [x] 2.4 Add the cross-service rule to sky-booking, then prove it end to end by adding
  `implementation(project(":sky-offer"))` to `sky-booking/build.gradle.kts` and importing
  `com.lukk.sky.offer.domain.model.Offer` into a domain class, run it red, and revert both edits
- [x] 2.5 Repeat 2.1 to 2.3 in sky-offer, injecting all three violations at once so one run shows all three failures,
  and prove the cross-service rule there by narrowing the own-module list rather than editing a build file
- [x] 2.6 Repeat 2.5 in sky-message
- [x] 2.7 Add the allow list and cross-service rules only to sky-notify, with a shorter allow list and no repository
  or advice rule, and prove both bite the same way
- [x] 2.8 Narrow the advice carve-out from `org.springframework.web..` to `org.springframework.http..`, the package
  `org.springframework.web` itself and `org.springframework.web.bind.annotation..`, so the carve-out cannot admit
  `org.springframework.web.client.RestClient` and falsify the requirement's own scenario
- [x] 2.9 Verify that no production source changed, with a `git status` limited to the four `src/main` trees and the
  four build files

## 3. Write the delta specification

- [x] 3.1 Copy the entire requirement block `Hexagonal layer separation is enforced by ArchUnit` from the merged file
  and verify the header matches character for character, so a plain MODIFIED applies
- [x] 3.2 Rewrite the requirement so each guarantee names its enforcer, the domain sentence becomes the allow list the
  services genuinely need, the three deviations are recorded, and the `config` permission is stated as unenforced
- [x] 3.3 Keep both existing scenario names so no rename is needed, and verify each still describes a failure the new
  rules actually produce
- [x] 3.4 Verify every package name written into the allow list appears in the bytecode dependency dump from task 1.3,
  and that no name appears that is absent from it
- [x] 3.5 Verify the delta holds no em dash, no en dash, no semicolon and no bold or italic outside the `**WHEN**` and
  `**THEN**` markers, with a matcher first validated against a fixture holding an em dash, an en dash, an arrow, a
  bullet and a box drawing character
- [x] 3.6 Run `openspec validate enforce-architecture-guarantees-with-archunit --strict` and verify it reports no error

## 4. Update the module guides

- [x] 4.1 Add to each of the four module guides what its own architecture test now enforces, and say in sky-notify's
  guide why it carries two of the four rules rather than all four

## 5. Verify and archive

- [x] 5.1 Run `./gradlew build` from the repository root and verify it is green, including
  `jacocoTestCoverageVerification` at 0.90 line and 0.90 branch
- [x] 5.2 Archive the change, letting the CLI rewrite the merged file
- [x] 5.3 Verify `openspec validate --specs --strict` reports no error and that `openspec/changes` holds nothing but
  `archive`
- [x] 5.4 Verify the requirement `Canonical package layout per service` is byte-identical in the rewritten merged file

## 6. Notes from the run

- Task 1.2 came out green. No controller in any service injects a repository, so the rule lands as a regression net
  rather than as a repair. Two of the three controllers do inject an outbound notification port and publish their own
  Kafka events, which is a layering smell of the same family. Widening the rule to forbid an inbound adapter from
  reaching any outbound port would fail sky-booking and sky-offer, and the repair moves event publication into the
  domain service, which changes behaviour ordering and its tests. That is a separate change and it is reported rather
  than folded in here.
- Task 1.3 was worth doing from bytecode. Two dependencies exist that no import statement shows: `lombok.Generated`,
  because `lombok.addLombokGeneratedAnnotation` is true in the root `lombok.config`, and `org.slf4j.Logger` plus
  `org.slf4j.LoggerFactory`, which `@Slf4j` generates. An allow list written from the import statements alone would
  have failed on the first run for reasons that look like a bug in the rule.
- Task 1.6 overturned the starting instinct. The throwaway rule failed with 39 violations, which proves ArchUnit
  records a dependency on a class it never imported, so a cross-service rule is not vacuous. Task 2.4 then proved it
  end to end against a real second project dependency. The rule still cannot fire on a Java edit alone, so the
  requirement credits the build file first and the rule second rather than crediting the test with the whole
  guarantee.
- Task 2.2 exposed a wording problem in the generated failure message. An ArchUnit `DescribedPredicate` description is
  spliced in after the words `classes that`, so a description phrased as a noun reads as `classes that a Spring Data
  repository`. Every new predicate description here is phrased to start with a verb, which is a change from the
  existing `areHandWrittenQueryApi` predicate in sky-booking and sky-offer. That older predicate is left alone.
- The new allow list rule subsumes the older narrow rule `Domain has no outbound HTTP client dependency` in all four
  services, and the notify rule `Domain never depends on transport or serialization frameworks`. Neither was deleted.
  A subsumed rule costs a second failure line in a report and gives a sharper message for the mistake it names, and
  deleting a passing test was not the approved scope.
- The allow lists differ across the four on purpose. sky-message drops `org.springframework.util` and
  `tools.jackson.databind` because it reaches neither, and sky-notify drops every persistence, validation and Spring
  Data entry plus the `adapters.dto` entry because it stores nothing and its domain touches no wire type. Copying one
  list into all four would have admitted dependencies those services do not have.

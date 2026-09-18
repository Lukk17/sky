## 1. Establish the layout from the source trees

- [x] 1.1 List every package directory under the root package of each of the four services, from
  `sky-booking/src/main/java/com/lukk/sky/booking`, `sky-offer/src/main/java/com/lukk/sky/offer`,
  `sky-message/src/main/java/com/lukk/sky/message` and `sky-notify/src/main/java/com/lukk/sky/notify`, and record the
  four lists side by side rather than reading any module guide
- [x] 1.2 Verify there is no `adapters/api` and no top-level `adapters/persistence` in any of the four, and record where
  `persistence` actually sits, which is `adapters/outbound/persistence` in sky-booking and sky-offer and nowhere else
- [x] 1.3 Verify `domain/ports` is split into `inbound` and `outbound` in sky-booking, sky-offer and sky-message, and is
  flat in sky-notify
- [x] 1.4 Record every difference between the four lists and decide for each whether it follows from a concern the
  service does not have, by naming the missing concern, or whether it is unexplained
- [x] 1.5 Verify the controller paths the scenario will name, by locating every class under `adapters/inbound/api` and
  confirming the three are `BookingController`, `OfferApiController` and `MessageController`
- [x] 1.6 Verify `sky-gateway` holds no hexagonal layout, by listing its main sources and confirming there are two
  classes, and no ArchUnit test, by listing its test sources
- [x] 1.7 Cross-check each module guide against the tree and record every disagreement, so a stale guide is reported
  rather than silently relied on

## 2. Establish what the ArchUnit tests really enforce

- [x] 2.1 Read all four `HexagonalArchitectureTest` classes in full and list every rule, separating the rules that
  assert where a class lives from the rules that assert which packages a class may depend on
- [x] 2.2 Verify the three location rules shared by sky-booking, sky-offer and sky-message, which are that a
  `@RestController` or `@Controller` resides in `..adapters.inbound.api..`, that an `@Entity` resides in
  `..domain.model..`, and that a `Repository`-suffixed interface or `JpaRepository` subtype resides in
  `..domain.ports.outbound..`
- [x] 2.3 Verify sky-notify's own location rules, which are that every class in `..domain.ports..` is an interface, that
  a class whose simple name contains `Listener` resides in `..adapters.inbound..`, and that only `..adapters.outbound..`
  may depend on `SimpMessagingTemplate`
- [x] 2.4 Verify no rule in any of the four mentions `config`, and that no rule checks a cross-service import, by
  grepping all four test classes
- [x] 2.5 Test the claim that a repository injected into a controller fails the build, by checking whether any rule
  forbids an adapter depending on `domain.ports.outbound`, and record the answer whichever way it comes out
- [x] 2.6 Verify the naming conventions against the tree: that no class in any module carries a `Port` suffix, that all
  three REST controllers end in `Controller`, and that every `*Primary` class is the implementation of the interface
  whose name it carries, in `adapters.outbound` as well as in `domain.service`

## 3. Write the delta specification

- [x] 3.1 Confirm the existing scenario name survives the correction, so a plain MODIFIED is available and no rename is
  needed
- [x] 3.2 Write `openspec/changes/correct-canonical-package-layout/specs/architecture/spec.md` under
  `## MODIFIED Requirements`, carrying the entire requirement block including the existing scenario under its existing
  name, and verify the requirement header matches the merged file character for character
- [x] 3.3 Verify every package name written into the requirement appears in at least one of the four trees recorded in
  task 1.1, and that no name appears that is absent from all four
- [x] 3.4 Verify the three enforced locations in the requirement are exactly the three from task 2.2, and that nothing
  else in the requirement is described as enforced
- [x] 3.5 Verify the absences and the one deviation named in the requirement are exactly the differences recorded in
  task 1.4
- [x] 3.6 Verify the delta holds no em dash, no en dash, no semicolon, and no bold or italic outside the `**WHEN**` and
  `**THEN**` markers the delta format requires, using a byte-exact matcher first validated against a fixture containing
  an em dash, an en dash, an arrow, a bullet and a box-drawing character
- [x] 3.7 Verify the delta respects the merged file's wrap width, which is one physical line per requirement paragraph
  and one per scenario line
- [x] 3.8 Run `openspec validate correct-canonical-package-layout --strict` and verify it reports no error

## 4. Archive and sync

- [x] 4.1 Archive the change, letting the CLI perform the merged-file rewrite, and verify
  `openspec/specs/architecture/spec.md` no longer contains `adapters.{api,inbound,outbound,persistence}` or the path
  `adapters/api/BookingController.java`
- [x] 4.2 Verify the merged file's requirement count is unchanged at two, and that the requirement
  `Hexagonal layer separation is enforced by ArchUnit` is byte-identical to what it was before this archive, including
  the three semicolons in its list, by diffing the block out of `git show HEAD:` against the rewritten file
- [x] 4.3 Verify the change directory moved to
  `openspec/changes/archive/2026-09-12-correct-canonical-package-layout` and that `openspec list` reports it as no
  longer active
- [x] 4.4 Verify `git status` shows no file outside `openspec/changes` and `openspec/specs` that this change touched

## 5. Notes from the run

- Task 1.1 was worth doing the hard way. Two packages exist that no module guide mentions,
  `sky-booking/.../adapters/outbound/persistence` and `sky-offer/.../adapters/outbound/persistence`, and they are where
  the `persistence` the old requirement asked for actually lives. A requirement written from the guides would have
  missed them and kept looking for a top-level `adapters/persistence`.
- Task 1.4 found four absences and one unexplained deviation. The absences are sky-message without `adapters.outbound`
  or `config.kafka`, and sky-notify without `domain.model` or `domain.exception`, plus sky-notify's flat `domain.ports`
  and missing `adapters.inbound.api`. Each maps to a concern the service does not have. The deviation is sky-notify's
  `adapters.outbound.service`, whose name states neither a technology nor a concern. It is recorded in the requirement
  as a deviation rather than listed beside the principled absences.
- Task 1.7 found one badly stale module guide and three mildly stale ones. `sky-message/AGENTS.md` describes an
  `adapters/outbound/directory` package, a `UserDirectory` outbound port, a `KeycloakUserDirectory` adapter, two
  Resilience4j beans, a `UserDirectoryUnavailableException` mapped to 503 with `Retry-After`, a
  `ReceiverNotFoundException` mapped to 404, a `USER_DIRECTORY_CLIENT_SECRET` startup check, a test resources file and
  two test classes. None of it
  exists: the module's main tree holds fourteen classes with no `adapters/outbound` at all, there is no
  `src/test/resources` directory, and its `GlobalExceptionHandler` maps only 404, 403 and 400. The repository's own
  cluster notes record that the lookup and its secret are gone. The same guide also says sky-booking folds an
  offer-service outage into a 400, and sky-booking's handler maps `OfferServiceUnavailableException` to 503 with
  `Retry-After: 10` and `OfferServiceBadResponseException` to 502, so that claim is stale too. `sky-booking/AGENTS.md`
  and `sky-offer/AGENTS.md` each omit an event-store port and an event-appender domain class as well as the
  `adapters/outbound/persistence` package, and `sky-notify/AGENTS.md` omits `adapters/outbound/service`. No guide was
  edited: they are markdown outside `openspec` and outside this change's scope. All of it is reported to the owner.
- Task 2.5 answered against the specification. No rule in any of the four services forbids an adapter depending on
  `domain.ports.outbound`, and a repository interface is a port that lives there, so a controller can inject a
  repository today and nothing fails. The first scenario of the other requirement in this capability claims otherwise.
  That requirement is not touched here and the finding is reported, because the specification is asking for something
  better than what is built.
- Task 4.2 holds. The requirement `Hexagonal layer separation is enforced by ArchUnit` is byte-identical including its
  three list semicolons, confirmed by extracting the block by header from `git show HEAD:` and from the rewritten file
  and diffing the two.
- The archive step also inserted two blank lines into the merged file, one after the Purpose text and one after the
  `## Requirements` heading, which this file was missing and every other merged specification has. That came from the
  CLI's rewrite rather than from the delta, it changes no content, and it leaves the file matching the house shape.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. The
  section carries the evidence for the layout and for the scenario path. Left as written.
- The merged file's Purpose says every service is held to the layout and that the package structure is checked by a
  test, and after this change neither is exactly true: `sky-gateway` is out of scope and only three locations are
  test-checked. A Purpose cannot be changed through a delta, so it is corrected by the separate change
  `correct-spec-purpose-statements`, which edits the merged file directly under `skip_specs`.

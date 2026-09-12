## Why

The requirement `Canonical package layout per service` in `architecture` names a package layout no service has. It
requires `adapters.{api,inbound,outbound,persistence}` and `domain.{model,ports,service,exception}`, and its scenario
points a new contributor at `sky-booking/.../adapters/api/BookingController.java`. There is no `adapters.api` package
and no top-level `adapters.persistence` package in any service, so the one path the requirement offers as a worked
example does not exist, and a contributor following the requirement would create two packages the ArchUnit rules would
then reject.

The real layout, established from the source trees of all four services rather than from any module guide, puts a
controller at `adapters/inbound/api`, wire types at `adapters/dto`, and driven adapters under `adapters/outbound` in a
subpackage named for what they adapt, which is where `persistence` actually lives. Under `domain`, `ports` is split into
`inbound` and `outbound`. Three of those locations are not conventions at all but rules: each service's own ArchUnit
test asserts that a controller resides in `adapters.inbound.api`, that a JPA entity resides in `domain.model`, and that
a repository interface resides in `domain.ports.outbound`. Those tests are the best evidence of what the rule is, and
they disagree with the requirement on the first of the three.

## What Changes

- Replace the layout requirement with the layout the four services carry, naming the three locations ArchUnit enforces
  separately from the ones held by convention, so a reader can tell which ones fail a build.
- Correct the scenario's example path from `adapters/api/BookingController.java` to
  `adapters/inbound/api/BookingController.java`, and give the equivalent path in two other services rather than one.
- Correct, in the same block, three stale details that cannot be carried over unchanged:
  - The `Port` suffix convention. The requirement says a port carries a `Port` suffix. No class in any service does,
    and the ports are named for what they do instead, as `BookingRepository`, `PhotoStorage`, `RequestUriStrategy` and
    `NotificationPublisher` are.
    The requirement now says the suffix is not used and why: the package already says it is a port.
  - The `ServicePrimary` convention, which is stated too narrowly. The real convention is that the single
    implementation of an interface takes that interface's name plus `Primary`, and it holds in `adapters.outbound` as
    well as in `domain.service`, so `NotificationPublisherPrimary` and `RequestUriStrategyPrimary` follow the same rule
    as `BookingServicePrimary`.
  - The scope. The requirement says every service, and there are five deployable modules. `sky-gateway` holds two
    classes, no domain model and no ArchUnit test, because its routing table is configuration rather than code, so the
    requirement now names it as outside the rule instead of silently failing it.
- Record the four places where the services genuinely diverge from each other, with the reason each one is principled,
  and say explicitly that sky-notify's shape is not a second sanctioned layout. One divergence is recorded as an
  unexplained deviation rather than a principled one, because it has no reason behind it that the code reveals.

Nothing here changes code, a test or a package. The requirement moves toward a tree that already exists and is already
enforced.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `architecture`: the canonical package layout requirement, its naming conventions, its scope and its scenario.

## Impact

- Affected file: `openspec/specs/architecture/spec.md`, rewritten at archive time from the delta in this change. One of
  its two requirements is touched.
- The other requirement in this capability, `Hexagonal layer separation is enforced by ArchUnit`, carries four defects
  this pass found and does not correct, because correcting it was not the approved scope and two of the four need a
  decision rather than a repair:
  - It says `domain` may depend only on itself, `sky.common` and `java.*`. That is false and unenforced. `domain.model`
    carries `jakarta.persistence` annotations and `domain.ports.outbound` extends Spring Data's `JpaRepository`, so the
    domain depends on two frameworks by design. No ArchUnit rule restricts domain dependencies to that set. The rules
    that do exist forbid specific packages: HTTP clients in all four, and the reactive stack, messaging, servlet and
    JSON libraries in some.
  - It says `config` may depend on both `adapters` and `domain`. No rule in any of the four services mentions `config`
    at all.
  - It says no cross-service imports are permitted. No ArchUnit rule checks that either. It holds, but because each
    service's Gradle module depends only on `:sky-common`, so the guarantee comes from the build rather than from the
    test the requirement credits.
  - Its first scenario says that injecting a `*Repository` directly into a controller fails the build. It does not. A
    repository interface is a port in `domain.ports.outbound`, and the rule that exists forbids an adapter depending on
    `domain.service`, not on a port. So a controller can inject a repository today and no rule objects. This is a case
    where the specification asks for something better than what is built, so it is reported rather than weakened.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched, so there is nothing to build
  and nothing to deploy.
- Risk: low. Every package name in the corrected requirement was read out of a source tree, and every enforced location
  was read out of the ArchUnit test that enforces it.

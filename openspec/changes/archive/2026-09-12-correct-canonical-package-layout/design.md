## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach rather than the content.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit. The CLI at version 1.11.0 performs that rewrite itself.

A delta replaces a requirement block by matching its header, so restating one requirement means standing behind every
fact inside it. This block carried three stale details beyond the package names, and all of them are corrected here.

The CLI refuses a MODIFIED block that drops a scenario name the merged requirement has, and it has no vocabulary for
renaming a scenario. The one existing scenario name, `New contributor navigating across services`, survives the
correction unchanged, so a plain MODIFIED works and the requirement header needs no rename either: it is the body that
was wrong, not the title.

## Goals / Non-Goals

**Goals:**

- Describe the package layout the four services carry, established from their source trees and from the ArchUnit tests
  that enforce parts of it, not from a module guide.
- Separate the locations a build enforces from the ones review enforces, so a reader knows which ones cost them a red
  build and which ones cost them a review comment.
- State the divergences between the four services explicitly, with the reason each is principled, and name the one that
  has no reason behind it rather than smoothing it into the list.
- Fix the example path, which is the one concrete thing the old requirement offered a contributor and the one thing in
  it that was most obviously wrong.

**Non-Goals:**

- Any change to a package, a class or a test. The requirement is being brought to the tree, not the other way round.
- The other requirement in this capability. Four defects in it are reported in the proposal, and two of them need a
  decision about what the rule should be rather than a correction of what it says.
- Renaming `adapters.outbound.service` in sky-notify, which is a code change. The requirement records it as a deviation
  and forbids adding to it, which is as far as a specification can go on its own.
- Adding an ArchUnit rule for anything the requirement holds by convention. That is a code change and a separate
  decision.

## Decisions

Take the layout from the four source trees and from the ArchUnit tests, and treat a module guide as a claim to check
rather than as evidence. That turned out to matter. `sky-message`'s own guide describes an `adapters/outbound/directory`
package holding a Keycloak user-directory adapter, a `UserDirectory` outbound port, two Resilience4j beans and a
`UserDirectoryUnavailableException` mapped to 503. None of it exists: the module's main source tree holds fourteen
classes and no `adapters/outbound` package at all, and the repository's own cluster notes record that the lookup and its
client secret are gone. A layout requirement written from the guides would have described a package that was deleted.

Name the three enforced locations separately from the conventions. The old requirement mixed a layout rule with a naming
rule and gave no sign that anything was checked, which invites a reader to treat the whole paragraph as advice. Three
locations are asserted by ArchUnit in each service's own test source, and that is a different kind of statement from the
rest, so the requirement says which three they are. It does not claim more than that: the rules assert the location of a
controller, an entity and a repository interface, and nothing asserts the location of a DTO, a port or a domain service.

State the ports-suffix rule as a prohibition rather than dropping it. The old text required a `Port` suffix and no class
has ever carried one, so the simplest correction would have been to delete the sentence. Deleting it leaves the question
open and invites someone to add the suffix later in the name of clarity, which would then be inconsistent with every
existing port. Saying the suffix MUST NOT be used, and giving the reason that the package already carries that
information, closes it.

Widen the `Primary` convention rather than restating it narrowly. The old text tied it to use-case implementations,
which describes `BookingServicePrimary` and misses `NotificationPublisherPrimary`, `RequestUriStrategyPrimary`,
`BookingNotificationServicePrimary` and `OfferNotificationServicePrimary`, all of which are outbound adapters following
the same rule. Stating the rule as the implementation of an interface taking that interface's name plus `Primary` covers
every case in the repository and explains the ones in `adapters.outbound` that the narrow version made look irregular.

Put `sky-gateway` outside the requirement explicitly rather than leaving it to be inferred. The old text said every
service, and `sky-gateway` is a deployable service by every other measure in this repository: it has a module, a
Dockerfile, a port and a compose entry. It also has two classes, no domain logic and no ArchUnit test, by a deliberate
decision recorded in its own module guide, so under the old wording it failed the requirement permanently and silently.
Naming it as out of scope, with the reason, turns a standing false negative into a recorded boundary. `sky-common` is
named alongside it for the same reason.

Record sky-notify's divergences as principled and `adapters.outbound.service` as not. Three of sky-notify's four
differences follow from it being a stateless event relay with no REST surface, and each is visible in its own ArchUnit
rules, which gate `adapters.inbound` and `adapters.outbound` without an `api` subpackage and require every class in a
flat `domain.ports` to be an interface. The fourth, `adapters.outbound.service`, has no such reason: its name states
neither a technology nor a concern, and it reads as a second `service` package beside `domain.service`. Listing it with
the principled three would have made it look sanctioned. Listing it separately, saying nothing explains it and
forbidding additions to it, keeps the finding alive without pretending this change can rename a package.

Assert that the services agree, because they do, and say where they differ anyway. The brief's instruction was to report
a disagreement rather than pick a winner. The four services agree on every name in the layout and differ only by
absence, with one exception. That is a documentation gap rather than a finding, so the requirement describes the shared
layout and enumerates the absences, and the single exception is the one thing escalated.

## Risks / Trade-offs

The requirement now enumerates four absences and one deviation, which ties it to today's tree. The next service that
gains a Kafka publisher or loses a REST surface makes one of those lines wrong. The alternative, a requirement stating
only the shared skeleton, would have been stable and would also have left a reader unable to tell whether sky-notify's
flat `domain.ports` was a violation, which is the ambiguity that let the old text survive this long. The enumeration is
short, it sits in one paragraph, and the scenario that checks it names each item, so the cost of keeping it current is a
paragraph rather than an investigation.

The requirement credits three locations to ArchUnit and leaves the rest to review, which is honest and also makes
visible how little of the layout is actually enforced. Nothing fails a build when a DTO lands in `domain`, when a port
is added outside `domain.ports`, or when a new `adapters.outbound` subpackage is named badly, which is how
`adapters.outbound.service` arrived. Adding those rules is the obvious follow-up and it is a code change.

Naming `sky-gateway` out of scope removes a standing false negative and also removes any pressure on it. If the gateway
ever grows logic worth testing, nothing in this requirement notices. The boundary is stated by reason rather than by
name alone, so the reason stops applying the moment the gateway holds a domain model.

## Migration Plan

Archive this change and confirm `openspec/specs/architecture/spec.md` carries the corrected block, that the other
requirement is byte-identical including its three list semicolons, and that the capability still validates. Rollback is
`git checkout` on the one merged specification, because nothing is built, deployed or migrated.

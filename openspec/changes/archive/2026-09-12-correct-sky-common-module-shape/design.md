## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec sync, so the correction reaches the merged
specification as a delta rather than as a direct edit.

The capability has exactly one requirement, so the whole of `sky-common`'s contract is in one block. A MODIFIED delta
replaces that block wholesale, which means the correction is all or nothing: there is no way to fix the base-class claim
without also deciding what the Kafka sentence, the package list and the enforcement claim should say.

The archive step refuses a MODIFIED block that drops a scenario name the merged requirement has. All three existing
scenario names are kept unchanged, and the one scenario that is added is an addition rather than a rename.

## Goals / Non-Goals

**Goals:**

- Stop the specification pointing a contributor at a design the module rejects. The base-class claim is the one defect
  here that would produce wrong code rather than a wrong document, because `extends SkyRestExceptionHandler` is a
  plausible thing to write and nothing in the module invites it.
- Put the real isolation mechanism in the contract. The `compileOnly` split is the module's central rule, it is the
  thing a careless dependency promotion would break for all five consumers at once, and it appears nowhere in the
  specification today.
- Leave the package rule enforceable without freezing it. A closed list of three names was wrong within one release and
  a closed list of six will be wrong within the next.

**Non-Goals:**

- Adding an ArchUnit rule to `sky-common` so the package claim becomes machine-checked. That is a code change with its
  own design question, namely what the rule would assert once the list of names is no longer the rule, and this change
  only stops the specification claiming a rule that does not exist.
- The `architecture` capability, which is where a real ArchUnit contract lives. The stale reference here is to a
  capability name that never existed, `hexagonal-enforcement`, so nothing in `architecture` needs correcting. It was
  read and left alone.
- Any code change, and any change to `sky-common/AGENTS.md`, which was found accurate.

## Decisions

Verify against the source tree and the imports file, and treat `sky-common/AGENTS.md` as a lead only. The guide
describes the real shape, and a guide that agrees with the code is exactly what a stale guide also looks like until
someone checks. All four facts were re-established independently: the handler from the class declaration and the
auto-configuration that registers it, the absent Kafka auto-configuration from the ten-line imports file and from a grep
for `@ConditionalOnClass` across the module, the six packages from a directory listing, and the absent ArchUnit rule
from the module's test tree. The guide was then confirmed correct on all four, which is worth recording because it means
the guide is currently the better document of the two.

State the handler rule as a prohibition on extending plus a route to replacing. Saying it is a bean rather than a base
class describes the mechanism but does not tell a service what to do when it needs different behaviour, and the answer
is in the registration: `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)` means defining your own bean
replaces the shared one. That is the sanctioned override, so the contract says so.

Replace the Kafka auto-configuration claim with the `compileOnly` rule rather than deleting it. The old sentence was
answering a real question, how a non-Kafka consumer avoids Kafka, with the wrong mechanism. Deleting it would leave the
question unanswered, so the corrected requirement answers it with the dependency declaration, and a new scenario pins
the `sky-message` case that the old scenario was gesturing at.

Rewrite the `Auto-configuration is opt-in` scenario around `sky-notify` rather than `sky-message`. The scenario name has
to survive, the tool will not rename it, and the name promises a conditional auto-configuration. `sky-message` is the
wrong example for that promise because its Kafka isolation is a build-file fact rather than a condition. `sky-notify` is
the right one: it applies no web conventions, so it has neither springdoc nor Spring Data, and two genuinely
`@ConditionalOnClass` auto-configurations therefore do not fire. The `sky-message` case moves to the added scenario,
where the mechanism named matches the mechanism in force.

State the package rule as concerns rather than as a list of permitted names. The rule that matters is that no package
holds a service's business logic, and a name list cannot express that: `security` is a permitted name under which domain
logic would fit perfectly well. Naming each package's concern makes the next addition a judgement about content, which
is the judgement the rule was always trying to make.

## Risks / Trade-offs

The corrected requirement is longer than the one it replaces, and a long requirement is harder to hold to. Mitigation:
the length is in the last scenario, which is a list of six concerns a reader consults rather than reads, and the three
normative rules in the requirement body stay short.

Naming six package concerns is a snapshot that a seventh package makes incomplete. Mitigation: the requirement says a
package outside those concerns is a deliberate addition, so adding one is a decision that shows up rather than an
omission that hides, which is the opposite of how the three-name list failed.

The claim that the `compileOnly` split makes most domain-logic attempts fail to compile is a qualified claim, and the
qualifier is load-bearing: logic needing only the JDK would compile here fine. Mitigation: the word is "most", and code
review is named first as the enforcement rather than second.

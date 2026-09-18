## Context

See proposal.md, Why, for the motivation and for the two requirements this change deliberately does not touch.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops a
scenario name the merged requirement has. The one existing scenario name is carried through unchanged and no scenario is
added.

This is the only change in the punctuation pass that leaves its capability with semicolons still in it. That is the
point of it: correcting the remaining two lines is blocked on a decision, and shipping one clean requirement is better
than either waiting or restating two requirements nobody authorised.

## Goals / Non-Goals

**Goals:**

- Clear the one semicolon in this capability that can be cleared on evidence alone.
- Make the plural rule auditable without producing two false positives, because the literal reading of the old text
  flags `/offers/{offerId}/owner` and the `/owner` prefix as violations and they are neither.
- Leave a written record, in the proposal, of exactly why the other two requirements still carry semicolons, so the next
  pass does not repeat the investigation.

**Non-Goals:**

- The header-resolver requirement and the internal-endpoint requirement. Both need an owner decision, both are named in
  the proposal with the commit that diverged from them, and neither is restated here.
- Any change to a controller mapping. The rule is being described, not applied.

## Decisions

Name the two shapes that are outside the rule rather than leaving them to judgement. Sixteen request mappings were read
across the three REST services and every collection segment is plural. Two paths would still look wrong to someone
applying the rule literally: `/offers/{offerId}/owner`, where `owner` is the single owner of one offer, and the `/owner`
prefix in `/owner/offers`, which scopes a set of endpoints rather than naming a collection. Writing both into the
requirement costs two sentences and prevents a future change from pluralising either one in the name of compliance.

Do not restate the other two requirements, even though the punctuation pass is the natural moment to do it. The
alternative was tempting because all three semicolons sit in one file and one change could have cleared them. It was
rejected for the reason the whole pass exists: restating a requirement asserts that everything in it is true, and
asserting that the `X-API-Version` header is the primary resolver would be false, while asserting that a path segment is
the resolver would be a decision about the API contract taken by the wrong person. The same reasoning kept the embedded
Kafka rule strict earlier today until its owner decided.

Treat the two blocked requirements as one report rather than two. They are one decision in substance: commit 77a6f44
chose to keep version information in the path and commit c459904 chose to keep access concerns out of it, and both moved
away from the same proposal-time design for the same reason. Whoever takes one will almost certainly take the other.

## Risks / Trade-offs

The capability is left with two known formatting violations, which means any future sweep for semicolons finds this file
and has to re-derive why it was skipped. Mitigation: the proposal names both requirements, both commits and both
possible resolutions, so the re-derivation is a read rather than an investigation.

Naming two exempt shapes in the requirement makes it longer and slightly more specific than the rule it encodes. The
alternative, a shorter rule plus a reviewer who knows the intent, is what produced the ambiguity in the first place.

The claim that every collection segment is plural rests on reading the mapping annotations of the three REST services,
not on a test. A controller added with a singular collection path would be caught by review rather than by the build.

## Migration Plan

Archive this change and confirm the merged specification carries the corrected block, that its other two requirements
are unchanged including their semicolons, and that the capability still validates. Rollback is `git checkout` on the one
merged specification, because nothing is built, deployed or migrated.

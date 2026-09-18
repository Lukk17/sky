## Context

See proposal.md, Why, for the motivation. One constraint decides the whole shape of this change.

`openspec/specs/` normally has exactly one sanctioned writer, the spec rewrite the archive step performs from a delta. A
delta carries requirements only, so that writer cannot reach a Purpose section. `openspec instructions specs` states the
consequence directly: a delta for an existing capability must not carry `## Purpose`, the delta's copy is ignored at
archive time, and the documented way to change an existing Purpose is to edit the merged specification directly.

So this change has no delta to write, and `openspec validate` rejects a zero-delta change without `skip_specs: true`.
That flag is set, which is its documented use: no spec-level behaviour changes.

## Goals / Non-Goals

**Goals:**

- Each of the two Purpose statements agrees with the requirement list directly below it, after this evening's
  corrections.
- The change is reviewable as a diff of two lines, with no requirement text among them.

**Non-Goals:**

- Any requirement text. All three requirement corrections this evening are already archived and none is revisited here.
- The Purpose of `api-versioning`, which survived its own requirement corrections intact and is left alone rather than
  reworded for the sake of touching it.
- The sixteen other Purpose statements, which `2026-09-12-replace-spec-purpose-placeholders` wrote and which no change
  this evening contradicted.

## Decisions

Open a fourth change rather than fixing the Purpose inside each of the three requirement changes. A requirement change
writes to a merged specification through its delta and through the archive step, and nothing else. Adding a direct edit
of the same file to the same change would give that change two writers, one of them unreviewable as a delta, which is
the pattern the whole spec workflow exists to prevent. Keeping the direct edits in one `skip_specs` change preserves the
property that a requirement change's entire effect on `openspec/specs/` is visible in its delta.

Leave `api-versioning` alone. The temptation was to reword all three for consistency, since all three capabilities were
touched tonight. Its Purpose says endpoints are explicitly versioned and consistently named so a breaking payload change
ships as a new version, and every word of that remains true under the path-segment resolver. Rewriting a correct
sentence to match the rhythm of two corrected ones would add a diff nobody can check against anything.

State the enforcement honestly in the `architecture` Purpose rather than dropping the claim. The old wording implied the
whole package structure is test-checked, which is the kind of claim that stops a reader looking for the gaps. Saying
that each service's own ArchUnit test asserts the parts a build can check keeps the useful information, that enforcement
is mechanical where it exists, without overstating how much of the layout it reaches.

Name the exemption in the `spring-boot-hygiene` Purpose rather than deleting the credential clause. Deleting it would
have been shorter and would have lost the point of the capability, which is that credentials are not defaulted. Saying
they are not defaulted outside the deliberately relaxed `local` profile puts the rule and its one boundary in the same
breath, which is what the requirement below it now does at length.

## Risks / Trade-offs

A Purpose is prose and nothing validates its content, so the only guard against a Purpose drifting again is that
somebody reads it next to the requirements. This change is that reading, and it will need repeating the next time a
requirement in either capability moves. Naming the two Purpose statements in the notes of the three requirement changes
that caused the drift is the cheap mitigation, so a reader of any of them is pointed here.

Mentioning the `local` profile in the `spring-boot-hygiene` Purpose ties a one-line summary to a profile name. If that
profile is ever renamed, the Purpose goes stale alongside the requirement that names it many more times, so the Purpose
is not the weakest link in that scenario.

## Migration Plan

Edit the two Purpose lines, confirm no other line in either file moved, confirm all eighteen specifications still
validate, then archive the change, which performs no spec sync because there is no delta. Rollback is `git checkout` on
the two merged specifications, because nothing is built, deployed or migrated.

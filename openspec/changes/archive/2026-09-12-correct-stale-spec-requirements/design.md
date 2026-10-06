## Context

See proposal.md, Why, for the motivation. Two constraints shape the approach rather than the content.

The first is that `openspec/specs/` has exactly one sanctioned writer, the archive step's spec sync. A merged
specification edited in place carries no record of what was decided or why, which is the failure this change exists to
correct rather than repeat: one of the two defects below was already edited in place by an agent, and that edit was
reverted with `git checkout` before this change was opened.

The second is that a delta carries requirements only. `openspec instructions specs` states it directly: a delta for an
existing capability must not carry a `## Purpose` section, because the merged specification already has one and the
delta's copy is ignored at archive time. So the `TBD` Purpose placeholder both of these files carry cannot be fixed
through a delta at all, whatever change it is attached to.

## Goals / Non-Goals

Goals:

- Correct the two requirement blocks through a delta, so the decision is recorded in the change rather than inferred
  from a diff on a merged file.
- Verify each correction against the committed artefact that is the evidence for it, the runbook for one and the
  exception handlers for the other, rather than against anybody's recollection.

Non-Goals:

- The `TBD` Purpose placeholder, in these two files and the other sixteen. It needs a direct edit rather than a delta,
  so it is a separate change, `replace-spec-purpose-placeholders`, and keeping it out of here leaves this change
  reviewable as two requirement diffs instead of twenty edits.
- Every other staleness in `test-strategy`. Three of its requirements still name MySQL, an 80 percent line and 70
  percent branch coverage floor, and `./gradlew build` as the gate. Those are real and they are reported, not fixed
  here, because they were not the approved scope and each needs its own evidence check.
- Any code change. Both corrections move the specification toward behaviour that already ships.

## Decisions

Restate the whole requirement block, not the defective line. Archive matches a delta to a merged requirement by its
header and replaces the block wholesale, so a delta carrying a partial block silently discards the scenarios it left
out. Both deltas therefore repeat the scenario text verbatim where it was already correct. The alternative, editing
only the defective sentence in the merged file, is the unsanctioned route this change replaces.

Use `## MODIFIED Requirements`, which is new vocabulary in this repository: all nineteen delta blocks in
`openspec/changes/archive/` so far use `## ADDED Requirements`. MODIFIED is the correct verb because both requirement
headers already exist in the merged files and neither capability is being renamed or removed. ADDED would either
duplicate the requirement or be rejected.

Name the assertion target as the object store rather than MinIO. The runbook asserts through a presigned URL and a
canary byte marker, which any S3-compatible store answers, so naming the product over-constrains the contract. MinIO
is the implementation that happens to be deployed, which belongs in the Helm values rather than in a behaviour spec.

State the rejection statuses as a list of independently checkable entries rather than as prose. Each line is then one
thing a test can be held to, and a future status lands as one more line instead of a rewrite.

## Risks / Trade-offs

The corrected status list is a snapshot of two handler classes, so it goes stale the next time either gains a mapping.
Mitigation: the list lives in one scenario in one requirement, and the delta names both handler file paths as the
evidence, so the next reader knows exactly what to re-read.

Restating a block verbatim risks a transcription error in the part that was correct. Mitigation: the unchanged
scenario was copied from the merged file rather than retyped, and the archive summary shows the resulting diff before
it is applied.

The `openspec-sync-specs` skill that the archive step delegates the merged-file rewrite to is absent from this
checkout, so the sync falls back to a direct edit of the merged specification. Mitigation: it happens inside the
archive step of a real change, with the delta committed as the record of what was decided, and the fallback is
reported so the skill can be imported.

## Migration Plan

Archive this change, take the sync when offered, and confirm both merged files carry the corrected blocks. Rollback is
`git checkout` on the two merged specifications, because nothing is built, deployed or migrated.

## Context

See proposal.md, Why, for the motivation. One constraint decides the whole shape of this change.

`openspec/specs/` normally has exactly one sanctioned writer, the spec sync the archive step performs from a delta. A
delta carries requirements only, so that writer cannot reach a Purpose section. `openspec instructions specs` states
the consequence directly: a delta for an existing capability must not carry `## Purpose`, the delta's copy is ignored
at archive time, and the documented way to change an existing Purpose, including a leftover placeholder, is to edit the
merged specification directly.

So this change has no delta to write, and `openspec validate` would reject a zero-delta change without
`skip_specs: true`. That flag is set, which is its documented use: no spec-level behaviour changes.

## Goals / Non-Goals

Goals:

- Every merged specification opens with a statement of what its capability is for, written from that capability's own
  requirement list.
- The eighteen rewrites are reviewable as one diff of eighteen Purpose sections, with no requirement line among them.

Non-Goals:

- Any requirement text. Three specifications are factually stale in their requirements (`database-schemas` and
  `db-migrations` still say MySQL, `test-strategy` still names an 80 percent line and 70 percent branch coverage floor
  against `./gradlew build`). None of that is corrected here. It is reported instead, because each needs its own
  evidence check and its own change.
- The two requirement corrections in `correct-stale-spec-requirements`, which is already archived.

## Decisions

Edit the merged specifications directly, as a task. The alternative, a delta carrying a Purpose, is not a weaker
option, it does nothing: the archive step discards it. The direct edit is acceptable here precisely because it happens
inside a real change, with this document and the task list committed as the record of what was decided and why, which
is the property an in-place edit by itself lacks.

Write each Purpose from the capability's requirement headers rather than from the archived proposal that created it.
The proposal describes a moment in the past, and the requirements are the contract in force. This is also what removes
the placeholder's worst property, which is that it points a reader at an archived change.

Describe the concern, not the current implementation. A Purpose outlives the technology in the requirements beneath it,
and `database-schemas` proves the point: its requirements name MySQL and the repository runs PostgreSQL, so a Purpose
naming the engine would be born wrong. Stating where a service's tables and its migration history live stays true
across that change.

Keep each Purpose to one or two sentences and over fifty characters, which is what `openspec validate --strict`
accepts. Longer than two sentences and it starts duplicating the requirements it sits above.

## Risks / Trade-offs

A Purpose is descriptive and nothing validates its content, so a wrong one misleads a reader silently and indefinitely.
Mitigation: each is written from the requirement headers in the same file, so it can be checked against them without
leaving the file.

Editing a merged specification by hand risks touching a line that is not a Purpose. Mitigation: the verification task
diffs all eighteen files and confirms that every changed line falls inside a `## Purpose` section, and that the
requirement count per file is unchanged.

The `openspec-sync-specs` skill the archive skill delegates to is absent from this checkout. It is not needed for this
change, because there is no delta to sync, but the absence is reported so the skill can be imported.

## Migration Plan

Rewrite the eighteen Purpose sections, verify, then archive. The archive performs no spec sync here, because
`skip_specs: true` means there is nothing to sync. Rollback is `git checkout` on `openspec/specs/`, since nothing is
built, deployed or migrated.

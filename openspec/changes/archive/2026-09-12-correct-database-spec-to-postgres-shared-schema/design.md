## Context

See proposal.md, Why, for the motivation. Four constraints of the tooling shape the approach rather than the content,
and two of them were discovered by reading the archive implementation rather than the documentation.

`openspec/specs/` has one sanctioned writer, the archive step's spec sync. A merged specification edited in place
carries no record of what was decided or why, which is the failure the change
`2026-09-12-correct-stale-spec-requirements` was opened to stop repeating.

A delta matches a merged requirement by its header and replaces the block wholesale, so a partial block silently
discards the scenarios it omits. Every restated requirement here therefore carries its whole block.

The archive step applies delta operations in a fixed order, RENAMED then REMOVED then MODIFIED then ADDED, which is
visible in `specs-apply.js` in the installed `@fission-ai/openspec` 1.11.0. That order is what makes renaming a
requirement and restating it in the same delta safe: the MODIFIED block is matched against the new header, because the
rename has already been applied by the time MODIFIED runs.

The same file refuses a MODIFIED block that drops a scenario name the merged requirement has, by name and with
multiplicity. So a scenario cannot be renamed through MODIFIED at all. Every scenario name in a MODIFIED block here is
carried over character for character, and where a scenario name was itself the defect the requirement is retired and
replaced rather than modified.

## Goals / Non-Goals

**Goals:**

- Make both specifications describe the layout that ships, established from the migrations, the three datasource
  blocks, the three Flyway settings, the chart and the two compose files, rather than from any document that describes
  the repository.
- Name the mechanism that makes the shipped layout safe, the per-service Flyway history table, because that is the part
  a reader cannot infer and the part a future change could break without noticing.
- Record the retirement of the per-service schema contract as a retirement, with its reason, rather than quietly
  rewriting it into something else.

**Non-Goals:**

- Any code change. Both specifications move toward behaviour that already ships, and the evidence for every correction
  is a file being read rather than changed.
- The semicolon clause joins in the nine other merged specifications that carry them. Those files have no correctness
  defect, so fixing their punctuation would mean restating requirement blocks for punctuation alone, which is a content
  edit to a contract and obliges a full evidence pass on each block restated. That is its own change.
- The per-service data isolation the retired requirement wanted. Retiring it does not endorse the shared schema as the
  better design, and the root README already names the shared database as a place this project loses. Wanting that
  isolation back is a proposal about the code, not a correction to a specification.

## Decisions

Correct both capabilities in one change, because they describe one decision. The engine, the database, the schema and
the migration history are a single layout, and a reader who takes the shared `public` schema from one file and a
Testcontainers MySQL from the other has a contradiction rather than a contract.

Retire and replace the per-service schema requirement rather than renaming and restating it. Renaming was tried first
and abandoned on evidence: its two scenario names are `Verifying schema isolation` and
`A service queries only its own schema`, the archive step will not let a MODIFIED block rename either of them, and both
names assert the schema-per-service model in the one place the correction has to contradict it. Keeping them to satisfy
the tool would have left the corrected requirement arguing against its own scenario headings. REMOVED carries a reason
and a migration pointer, which says what happened, and the replacement requirement keeps every rule the old one
carried that is still true.

Rename the Flyway history requirement rather than retiring it, because the opposite is true there: its scenario name,
`Inspecting migration state`, is engine-neutral and layout-neutral, and its substance survives the move intact. Only
the location changes, from a schema per service to a table per service, so the header changes and the block is
restated under it.

Name the three history table names in the requirement text. The previous version named a generic
`flyway_schema_history`, which is exactly the value that would break the shipped layout if a service were configured
with it, so the specific names are the content worth pinning.

State the demo seed's exclusion from `db/migration/` as a requirement rather than as an observation. The old
requirement said all seed SQL lives under `db/migration/`, which is both false and the thing that must not become true:
a seed under `db/migration/` would be applied on every deploy, in production. So the corrected requirement says where
seed data goes and why it stays out.

## Risks / Trade-offs

A retirement loses a contract. If the per-service schema split is ever wanted back, the requirement stating it is no
longer in the merged specification. Mitigation: the REMOVED block names the reversed decision and points at the
archived change `2026-06-25-db-per-service-schemas`, which still holds the full proposal, design and tasks for that
layout, so reviving it means reviving a change rather than reconstructing an idea.

The three history table names are a snapshot of three configuration lines, so they go stale if a service is renamed.
Mitigation: they are stale-detectable, because a wrong name here contradicts a committed `spring.flyway.table` value,
which is a one-line check, unlike the MySQL claim that survived an engine migration unnoticed.

Restating a block risks a transcription error in the part that was correct. Mitigation: the surviving rules were copied
from the merged file rather than retyped, and the archive summary shows the resulting diff before writing it.

## Migration Plan

Archive this change and confirm both merged specifications carry the corrected blocks. Rollback is `git checkout` on the
two merged specifications, because nothing is built, deployed or migrated.

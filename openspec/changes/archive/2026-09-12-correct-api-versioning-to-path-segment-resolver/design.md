## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach rather than the content.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit. The CLI at version 1.11.0 performs that rewrite itself.

A delta replaces a requirement block by matching its header, so restating one requirement means standing behind every
fact inside it, including the facts that were not the reason for opening the change. Both blocks here carried stale
details beyond the mechanism, and all of them are corrected in the same delta rather than left for a later pass.

The CLI refuses a MODIFIED block that drops any scenario name the merged requirement has, and it has no vocabulary
for renaming a scenario. That was established by running the validator against a first draft of this delta and reading
`findMissingCurrentScenarios` in the installed CLI, which compares scenario names for exact equality. One scenario here
is named `Default version applies when header is absent`, so its name asserts the very mechanism being removed and
cannot be carried over. That single constraint decides the delta's shape.

## Goals / Non-Goals

**Goals:**

- Describe the resolver and the route shape that ship, each verified against the committed code and the committed test
  rather than against a proposal that predates both.
- Record why the header resolver was not chosen, inside the requirement, so the next reader sees a decision with a
  reason rather than an absence to be corrected.
- Leave the capability with no semicolon-joined clause, which closes the one file the punctuation pass had to leave
  open.

**Non-Goals:**

- Any code, configuration, Bruno request or OpenAPI change. Both requirements move toward behaviour that already ships.
- Building the header resolver or restoring an internal namespace. The owner's decision was the opposite direction, and
  the requirement now says so explicitly so that a future reader does not read the absence as an oversight.
- The third requirement in this capability, the plural-noun rule, which was corrected earlier today and is true as it
  stands.
- Rewriting the `2026-06-25-api-cleanup` archived record. See the decision below.

## Decisions

Remove both requirements and add their replacements, rather than renaming and modifying them. A rename plus a
modify was drafted first and rejected on two counts. It is blocked outright, because the scenario name carrying the
word `header` cannot survive a MODIFIED block and cannot be renamed. And it would misdescribe what happened: a
MODIFIED block says a requirement was refined, while both of these recorded a decision that was then reversed, which is
the same situation `2026-09-12-correct-database-spec-to-postgres-shared-schema` met when MySQL per-service schemas gave
way to one shared PostgreSQL database. That change used REMOVED with a Reason and a Migration, then ADDED, and this one
follows it.

Carry the continuity in the Migration line rather than in the header. The risk of a removal is that a reader finds a
deleted requirement and no forward pointer, so each Reason names the commit that reversed the decision and each
Migration names the requirement that now carries the rule and lists what it kept. A reader landing on the removal is
sent forward in the same paragraph.

State the rejected alternative inside the requirement rather than only in this change. A requirement that merely
describes the path-segment resolver invites the next reader to add a header resolver as an obvious improvement, which
is exactly how the original text came to exist. Naming the header, saying it was rejected, and giving the reason costs
two sentences and converts a silent absence into a recorded decision. `A request header MUST NOT be the resolver` is
the normative half of that, so the rule stays checkable rather than becoming commentary.

Describe the unsupported-version rejection at the strategy rather than as an end-to-end HTTP status. The committed test
`ApiVersioningAutoConfigurationTest.validateVersion_rejects_whenTheRequestAsksForAnUnsupportedVersion` asserts that the
strategy rejects version 2, and the exception it throws carries 400, both of which were read directly. What was not
measured is the status a live service returns for `GET /api/v99/offers`, because the controller mapping is the literal
path `/api/v1`, so such a request may never reach version validation at all. The scenario therefore asserts what the
evidence supports and does not claim a response code nobody has observed.

Correct the named exception while restating the block. The old scenario credited
`NotAcceptableApiVersionException` for the rejection. Reading the class files shows
`DefaultApiVersionStrategy.validateVersion` constructs `InvalidApiVersionException`, that
`NotAcceptableApiVersionException` is a subclass of it thrown from `VersionRequestCondition` when a mapping mismatches
on version alone, and that the status on the base class is `BAD_REQUEST`. The corrected scenario names neither class,
because the distinction is an implementation detail of the framework and naming the wrong one is how the original went
stale. It names the status, which is the observable part.

Leave the false claims in the archived `2026-06-25-api-cleanup` record exactly where they are. Its task 5.1 says every
controller test sends `X-API-Version: 1` and its task 6.1 says the Bruno collection carries that header as a
collection-level default. Neither was ever true: the string appears in no Java file and in no commit that ever touched
one, and `docs/api/request/opencollection.yml` has no headers block at all. Three options were weighed. Editing the
archived tasks to unticked would rewrite a record of what somebody believed at the time, which is the one thing an
archive exists to preserve, and it would also leave a reader wondering who changed it and when. Adding a note inside
the archived directory would put new text under a date that is not its own, so the directory would no longer be a
snapshot of one day. Recording the correction in this change, which is the change that investigated the claim, keeps
the archive immutable and puts the correction where a reader who asks "is the header real" will land, because this is
the change that answers that question. That is the option taken, and the evidence sits in tasks 1.5 and 1.6 with the
exact file and search that disproves each claim.

## Risks / Trade-offs

A reader who finds `2026-06-25-api-cleanup` first still reads two ticked tasks that are false, and nothing in that
directory points forward. Mitigation: the capability those tasks claim to satisfy is `api-versioning`, and its merged
specification after this change describes the path-segment resolver and says the header was rejected, so the
contradiction surfaces on the first cross-check rather than being hidden. The alternative mitigation, a pointer file
inside the archived directory, was rejected above.

Writing the rejected alternative into the requirement makes the requirement longer and ties it to one historical
decision. If the frontend is ever rewritten and the URL stability argument stops applying, the reason recorded here
becomes the thing to revisit rather than a rule to obey, which is the correct outcome but needs a reader willing to
re-derive it. Naming the reason rather than only the rule is what makes that possible at all.

The owner sub-resource carries the owner's email address, so it is personal data on a route any authenticated caller
can read for any offer id. That is the existing behaviour and it is not changed here. The requirement now states the
authenticated-not-role rule explicitly, which makes the exposure visible in the contract rather than only in
`SecurityConfig`, and whether to narrow it is a separate decision.

## Migration Plan

Archive this change and confirm `openspec/specs/api-versioning/spec.md` carries the two replacement requirements,
carries neither removed one, that the plural-noun requirement is byte-identical to what the earlier change left, and
that the capability still validates. Rollback is `git checkout` on the one merged specification, because nothing is
built, deployed or migrated.

## Context

See proposal.md for why. Two of the three edits here are corrections with no design question in them: a stale
reference is replaced by a true one, and a literal that was pinned without a reason gains its reason. The design
question is the third one, which was asked directly and deserves an argument rather than an assertion. Does the fact
that the three published documents are now generated output belong in a specification at all, and if it does, which
capability owns it.

## Goals

- Answer that question on grounds that survive the toolchain changing, because the tool is the least durable part of
  what happened today.
- Put the property where the next reader looking for the API contract will look, rather than where it happens to fit.
- Leave `api-versioning` able to explain its own literal without importing a paragraph about document generation.

## Non-Goals

- Naming the plugin, the task, the profile or the port. Those are in `docs/api/README.md`, they are the mechanism, and
  a specification that carried them would go stale on the next plugin.
- Requiring that the documents be tracked in the repository. That is how they are published here and it could change
  without the property changing, which is the test that kept it out.
- Fixing the `repo-hygiene` Purpose, or its build-output scenario. Both are raised in proposal.md for the owner.

## Decisions

### Whether the property belongs in a specification at all

The case against is worth stating first, because it is not weak. The property is held entirely by build wiring. Delete
the plugin and the property dies silently, and no sentence under `openspec/specs/` would have prevented that. A
requirement that nobody can be caught failing is decoration, and this repository already has enough prose.

Three things answer it.

It has already cost something. Three documents published `/api/1/...` on every endpoint, for a service answering
`/api/v1/...`, and they were committed. The reason nothing caught it is precisely that the specification said the
published documents were working unchanged, which was true when it was written and had quietly stopped being true.

It is violable by hand, not only by deletion. Editing a published document directly is the obvious thing to do when
the document is wrong, it is what anybody would have done last week, and it now produces an edit that the next build
silently discards. `docs/api/README.md` says so, and a repository convention in a README is a weaker instrument than a
requirement when the two disagree with somebody's habit.

It is checkable without naming any mechanism. Produce the documents again and no file differs. That is a sharper test
than most requirements in this repository carry, and it needs nothing but the repository itself.

So it belongs. The remaining question is where.

### Why not test-strategy

Its Purpose is the bar every module's tests are held to: what they run against, which paths they cover, the statuses a
rejection is asserted as, the coverage floor, and how an end-to-end flow is exercised. Producing a document is not a
test and asserts nothing. The only thing that makes the fit tempting is that the property has a check, and the check
is a diff rather than a test. Filing it here would widen that capability from tests to verification in general, which
is a change to what the capability is, made as a side effect of finding somewhere to put one requirement.

### Why not spring-boot-hygiene

Its Purpose is each service's configuration being honest: only starters that are used, no credential defaulted outside
the local profile, and a cross-origin list without a wildcard. springdoc is already in its territory, since its first
requirement governs which modules carry the UI starter, which is the strongest argument for this home.

It still loses, for two reasons. The property is about a published artefact rather than a service's configuration, and
the difference is not pedantry: every requirement in that capability is a rule about what a module declares, checkable
by reading a build file or a YAML file, and this one is a rule about what a document says. And the one piece of this
work that genuinely is service configuration, the `openapi` profile the three services gained, was checked against
that capability's own credential rule and passes: none of the three `application-openapi.yaml` files defaults a
credential, because the fork runs `local,openapi` and the `local` half already carries them. The capability that
governs the new configuration has nothing to complain about, which is a sign it is not the capability that governs the
new documents.

### Why not repo-hygiene, which was the closest call

Its Purpose is keeping the repository to source only, so that a clone carries nothing a build would regenerate. It
owns the question of what may be committed, which is exactly the question three generated files tracked on purpose
raise, and it is the capability whose text this change actually falsifies.

It loses on one point and it is decisive. The property is that the document is derived from the service. Whether the
result is committed, published from a pipeline, or served from a running instance is a separate decision, and the
property is identical under all three. If these documents moved into `build/` tomorrow and were published from CI,
`repo-hygiene` would have nothing left to say and the property would be unchanged. A capability that can lose its
subject while the property survives is not the owner of the property.

That its Purpose is falsified is true whatever this change does, because three regenerable files are tracked either
way. It is a separate correction, and it is raised rather than smuggled in here.

### Why a capability of its own rather than a clause on api-versioning

The version literal is one consequence of the documents being generated. It is the consequence that has already bitten
and it is the one `api-versioning` must state, because it is a rule about the address a service serves. It is not the
property. Filing the property under versioning would mean that the next reader asking where the published contract
comes from finds the answer inside a requirement about path segments, and that a second consequence, for instance a
response annotation nobody applied, has nowhere to go.

A capability holding one requirement is already the shape of four of the eighteen here, so this is the established
size rather than an exception. The boundary between the two is drawn in both texts: `api-contract` owns the document,
`api-versioning` owns the addresses and the literal that reaches them, and the new requirement points at the versioning
one for the case it does not own.

### What the new requirement states, and what it leaves out

The rule applied is the one the previous change used: the specification carries what can be observed, plus the least
mechanism without which the observable property cannot be kept.

Stated, with the reason each passes.

- That the document is produced from the service and not maintained beside it. This is the property.
- That everything the document says must be reachable from the service. This is what makes the property actionable
  rather than aspirational, because it says where a contributor puts a fact that belongs in the contract.
- That a published document is never edited directly. Observable in its consequence, which is the edit vanishing, and
  it is the violation most likely to be attempted.
- That the kept copy equals what the current source produces. This is the check, and without it the requirement has no
  way of being wrong.

Left out, each because the property survives it changing.

- Which plugin, which Gradle task, which profile, which port, and which document path.
- That `build` depends on generation, which is a convenience and could become a separate command tomorrow.
- The format. It is OpenAPI today and the requirement would read the same for any other description language.
- That the documents live under `docs/api/openapi/`, for the reason given above about publication being separable.

## Risks and Mitigations

- A reader takes the requirement as forbidding a hand-written API document anywhere in the repository. Mitigated by
  the requirement naming what it governs, which is the document a service publishes, rather than every YAML file that
  happens to describe an endpoint.
- The check it names, a regeneration that yields no difference, depends on generation being deterministic. It is today,
  and deliberately so: each service sets `springdoc.writer-with-order-by-keys` under the generation profile, which is
  what makes the output stable enough for a diff to mean something. That is mechanism, so it is not in the
  requirement, and if it were lost the check would degrade into noise rather than fail loudly. This is the weakest
  joint in the change and it is recorded rather than papered over.
- The property is not enforced by any build gate. A contributor who never runs the generation leaves the kept copy
  stale, and nothing fails. The requirement makes that a defect somebody can be shown rather than a matter of taste,
  which is the whole of what a specification can do here.

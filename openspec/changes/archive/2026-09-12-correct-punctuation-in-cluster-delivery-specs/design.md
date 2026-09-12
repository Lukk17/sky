## Context

See proposal.md, Why, for the motivation. Four constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops a
scenario name the merged requirement has. All five existing scenario names across the three restated requirements are
carried through unchanged, including the placeholder-values scenario that the repository currently fails and that is
kept exactly as demanding as it was.

Nothing is deployed, templated or applied by this change, and no cluster is touched. Every value comes from reading the
four service charts under `config/k8s/helm/service/`, their `values-local.yaml` and `values-prod.yaml` overlays, the six
module build files, `config/k8s/_deployment-scripts/deployment_README.md` and `config/k8s/local_README.md`.

Two of the three requirements here are ahead of the repository rather than behind it, which changes what this change may
do to them. The reasoning is in Decisions.

## Goals / Non-Goals

**Goals:**

- Clear four semicolon-joined clauses without softening any rule they carried, which in this file means resisting the
  pull to restate two rules down to what the charts currently do.
- Replace the last Auth0 reference in this capability with the annotations that actually encode the same concern, so the
  example teaches a reader what to look for in the charts they have.
- State the tag-to-version correspondence precisely enough to be checked, including the `v` prefix that makes a literal
  string comparison fail.

**Non-Goals:**

- Editing a chart, an overlay, a runbook or a manifest. The two divergences reported in the proposal are real and this
  change closes neither.
- The `TLS secret names are not misleading` requirement. It carries no semicolon, so it does not have to be restated,
  and restating a block obliges correcting every stale detail inside it. It was read, and the reading is in the
  proposal's Impact, because the first glance at it is misleading in the other direction.
- Deciding whether chart defaults should be production-ready or placeholders. That is the decision the divergence
  rests on, it has an owner, and it is not a punctuation question.

## Decisions

Keep the placeholder-defaults scenario exactly as strict as it was, and report that the repository fails it. This is the
same shape as the `@EmbeddedKafka` divergence that `2026-09-12-correct-test-strategy-stack-and-coverage-gate` left
standing earlier today, and it gets the same treatment. Rewriting the scenario to describe production-ready defaults
would not be a correction, it would be the deletion of a rule, and the rule guards against something concrete: a
`helm upgrade` that forgets its overlay currently installs an ingress for the production hostname instead of failing.
The chart's own comment, in the header of `config/k8s/helm/service/sky-booking/values-prod.yaml`, says the
production-ready default is deliberate. A comment in a values file is evidence that somebody chose convenience, not
evidence that the contract was retired, so the contract stands until its owner retires it.

Keep the single-path rule absolute rather than scoping it to components that already have a chart. The first draft of
this delta scoped it that way, which read better and quietly legalised `config/k8s/local/` and
`config/k8s/Xperimantal/`. Narrowing a rule is the kind of decision that needed an owner in the `@EmbeddedKafka` case
and needs one here too, so the restatement keeps the absolute form and adds only the sentence explaining why it is
absolute. The three paths that break it are named in the proposal instead.

Name the oauth2-proxy annotations rather than the provider. The old example, "Auth0 callbacks", was a concrete instance
of the right idea that stopped existing when the provider changed. The durable version of the same example is the pair
of ingress annotations that point at an environment's authentication front door, because those are what a reviewer will
actually find in a default values file, and they are there right now with a production hostname inside them.

State the tag and the version as the two literals they are. Saying the tag matches the Gradle `version` field invites a
reader to compare `v2.0.0` with `2.0.0`, conclude they differ, and either file a defect or edit a chart. Writing both
values and naming the `v` prefix settles it once.

Do not restate the `image.tag` rule to permit `latest`. Every `values-local.yaml` sets `tag: "latest"` with
`pullPolicy: "Never"`, which is coherent for an image built on the machine and never pulled, and is still a
chart value file setting `latest`. Whether the rule should exempt a local overlay is a narrowing decision with an owner,
so it is reported rather than taken here.

## Risks / Trade-offs

Two requirements in these files now knowingly fail, and a reader auditing against them will find the same gaps. The
mitigation is that both are documented in the proposal with the file and line of every instance, so the next reader
inherits the analysis rather than rediscovering it, and neither gap is new: both predate this change by months.

Naming `v2.0.0` and `2.0.0` in a specification duplicates values that live in four charts and six build files, so the
next release has to touch all of them plus this requirement. That is the same trade the `postgres:17-alpine` pin
accepted earlier today, and the alternative wording is what allowed a mismatch to be unverifiable for this long.

The claim that the vanilla tree is gone was verified by a `find` that returned zero, and the claim about documentation
links by searching every markdown file for the string. Both are cheap to re-run and neither is protected by a test, so
a future reintroduction would be caught only by someone repeating the search.

## Migration Plan

Archive this change and confirm both merged specifications carry the corrected blocks, that the requirement this change
does not touch is unchanged, that no semicolon joining two clauses remains in either file, and that Auth0 appears in
neither. Rollback is `git checkout` on the two merged specifications, because nothing is built, deployed or migrated.

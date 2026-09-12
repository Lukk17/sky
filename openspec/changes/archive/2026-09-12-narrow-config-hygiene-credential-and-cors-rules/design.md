## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach rather than the content.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit. The CLI at version 1.11.0 performs that rewrite itself.

A delta replaces a requirement block by matching its header, so restating one requirement means standing behind every
fact inside it. Both blocks here carried scope errors beyond the rule being relaxed, and all of them are corrected in
the same delta.

The CLI refuses a MODIFIED block that drops a scenario name the merged requirement has, and it has no vocabulary for
renaming a scenario. Both scenario names here survive the correction, `Starting a service without required env vars`
and `Default-profile CORS`, so a rename of the requirement followed by a modification under the new name is available
and is what this change uses. Both headers had to move: one says credentials are never defaulted, which is now untrue
by design, and the other says the defaults are restrictive, which was always the wrong axis.

## Goals / Non-Goals

**Goals:**

- Narrow each rule to exactly what the owner decided, with the reasoning inside the requirement so the next reader
  finds a decision rather than an inconsistency.
- Write the credential exemption so it cannot be read wider than the `local` profile, which means naming the boundaries
  rather than trusting the reader to infer them.
- Bring the places the old rules did not reach into scope, so the capability stops being a rule about three files and
  becomes a rule about cross-origin configuration.
- Leave the capability naming its one real violation rather than four false ones.

**Non-Goals:**

- Any change to code or configuration. Both requirements move toward configuration that already ships.
- Fixing the object-store credential defaults in `sky-offer`'s default profile. The narrowed rule still forbids them.
  That is reported, and the fix is a configuration change nobody authorised here.
- Adding the startup check the credential requirement demands. That is a code change, and it is reported rather than
  quietly dropped from the requirement.
- Deleting the abandoned Kong experiment directory that holds the repository's one wildcard value. That belongs to
  `repo-hygiene`.
- The starter-hygiene requirement, the third in this capability, which is true as it stands.

## Decisions

Rename, then modify, rather than removing and adding. The situation differs from `api-versioning`, where a decision had
been reversed and the requirement recorded a mechanism nobody built. Here both rules are correct in substance and wrong
in scope, both keep their subject and their scenarios, and the headers move only because each had encoded the old scope
in its own title. A MODIFIED block is the honest verb for that, and both scenario names survive, so the CLI permits it.

Write the exemption as one exemption plus three boundaries, not as a softer rule. A rule reading that credential
defaults are discouraged outside the local profile would have been shorter and would have protected nothing, because
the next reader would weigh it rather than apply it. Naming the exemption, then naming the three ways it could be
widened and forbidding each, keeps the rule auditable: a reviewer can check each boundary against a file without a
judgement call. The third boundary, that the `local` profile is not activated in a deployed environment, is the one a
reader would otherwise miss, because the exemption attaches to a file rather than to an environment and a file is
portable.

Put the reasoning in the requirement rather than only here. The owner has had to restate the same reasoning several
times, which is the signal that it was living in conversation rather than in the record. Naming the JWT decoder that
verifies nothing and the gateway chain that permits everything, both already accepted under the same profile, makes the
argument checkable against the repository rather than a matter of taste, and it gives a future reader the comparison
that makes a defaulted development password the smaller relaxation rather than a new one.

Relax the cross-origin rule to the wildcard alone, and say inside the requirement why the old form was unsatisfiable.
The alternative, keeping the production-only default and creating the profile overlay the old rule assumed, was
rejected by the owner and would have been a configuration change across three services to satisfy a rule nobody was
failing in substance. What the requirement now records is that the permission to name development origins is a decision
rather than an oversight, which is the thing the old text got wrong about this repository.

Widen the cross-origin rule to reach configuration code, not just configuration files. `sky-notify` has no cross-origin
property at all: `WebSocketConfig` holds a hardcoded list, so the old rule, which spoke of the value in
`application.yaml`, did not cover the one service whose allow-list cannot be changed without a rebuild. A rule that
misses the least flexible case is the wrong rule. The same reasoning brings in the `cors-allow-origin` ingress
annotation, which is where the production policy actually lives.

Scope the rule to cross-origin configuration for a service or for an ingress, rather than to the whole repository. The
repository does hold one wildcard, the Kong developer-portal origin setting in the abandoned experiment under
`config/k8s/Xperimantal/`. Three options were weighed. Writing the rule to cover the whole repository would make it
false the moment it was read, which is the failure this pass exists to correct. Carving out that path by name would put
a dead directory into a behaviour contract and would keep it alive in the record. Scoping the rule to what it is
actually about, the cross-origin policy of a sky service and of the ingress in front of it, is true today and stays
true after the experiment is deleted. That is the option taken, and the file is named in the proposal so the finding is
not lost.

Keep the requirement demanding a loud startup failure on a missing credential, even though nothing provides it. Spring
Boot's `PropertySourcesPlaceholdersResolver` builds its placeholder helper with unresolvable placeholders ignored, read
directly out of the binder's own bytecode, so an unset `POSTGRES_PASSWORD` binds as the literal `${POSTGRES_PASSWORD}`
and fails at connection time rather than at startup. The repository already knows this: `sky-message`'s module guide
records the same behaviour and describes a startup check that existed to compensate, and that check is gone along with
the feature it guarded. No module carries one now. Weakening the requirement to describe the binder's behaviour would
trade a contract for a description and would remove the only record that the gap exists, so the requirement keeps the
MUST, the scenario keeps the expected outcome, and the gap is reported as a defect with an owner rather than absorbed.

## Risks / Trade-offs

The cross-origin narrowing is the one place in this pass where the specification was arguably better than the
implementation and is being brought down to it rather than the other way round. The concrete consequence, verified
rather than assumed: nothing in any profile file, compose file or chart sets `ACCESS_CONTROL_ALLOW_ORIGIN`, so the
committed default is what runs in the cluster, and the deployed services therefore answer cross-origin requests from
`http://localhost:4200`, `http://localhost:5777` and their own port as well as from the production frontend. That is
only reachable by a user who is already running something on those ports, and the owner has accepted it for a local
project, but it is a real widening of the deployed policy and it is now permitted by the rule rather than flagged by
it. The mitigation that would cost nothing in local convenience is an `ACCESS_CONTROL_ALLOW_ORIGIN` value in the Helm
values of the three services, which is a chart change and a separate decision.

Two production hostnames are in play and the requirement deliberately says nothing about which is right, which means it
cannot catch the disagreement. The three REST services default to `https://skycloud.luksarna.com`, the `sky-offer`
ingress annotation names `https://sky.luksarna.com`, and `sky-notify` hardcodes both. Leaving named origins out of the
rule is what makes the rule stable, and it is also what makes this invisible to it. Reported instead.

The credential exemption is held by review rather than by a test. Nothing fails a build when a default appears in a
file without a profile in its name, which is exactly how the two object-store defaults in `sky-offer` arrived. The
second scenario is written as an audit a person or a script can run, so the rule is at least mechanically checkable,
but nothing runs it today.

## Migration Plan

Archive this change and confirm `openspec/specs/spring-boot-hygiene/spec.md` carries both corrected blocks under their
new headers, that the starter-hygiene requirement is byte-identical to what the earlier punctuation change left, and
that the capability still validates. Rollback is `git checkout` on the one merged specification, because nothing is
built, deployed or migrated.

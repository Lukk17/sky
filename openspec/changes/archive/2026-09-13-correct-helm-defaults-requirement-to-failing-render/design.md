## Context

See proposal.md, Why, for the motivation. Three constraints shape the approach rather than the content.

A delta replaces a merged requirement by matching its header and swapping the whole block, so touching one line means
restating everything under that header, which in turn means every other claim inside the block has to be checked
against the charts as they stand rather than carried over on trust. That check is what turned up the two unnamed
categories, the namespace remedy and the five charts that render clean, and it is the reason the replacement block is
longer than the one it retires.

`openspec/specs/` has exactly one sanctioned writer, the archive step's spec sync. The merged file is never edited in
place, so the correction has to travel as a delta even though it touches one capability and no code.

The charts are read-only for this change. `helm template` renders locally and contacts no cluster, so it is the whole
of the evidence gathering and it changes nothing.

## Goals / Non-Goals

Goals:

- Replace a worked example that is now false with one that quotes what the command actually prints.
- Leave the requirement checkable: every claim in the restated block maps to a command someone can rerun.
- Say which mechanism holds each category out of the defaults, because the two in use are not the same mechanism and
  the difference is invisible from the requirement as written.

Non-Goals:

- Any chart, template or values file. The charts are already in the state the corrected requirement describes.
- The other two requirements in `helm-charts`. Both were re-checked and both hold, so neither is restated. The pinned
  tag requirement matches: all four service charts carry `tag: "v2.0.0"` and `pullPolicy: "IfNotPresent"`. The TLS
  secret requirement matches: every production overlay names `sky-tls-cert` and `dev-ssl-cert` appears in local
  overlays only, which is the context its own wording exempts.
- The `architecture` capability, which another agent holds this round.

## Decisions

Retire the requirement and add a replacement, rather than restating it under `## MODIFIED Requirements`. A restatement
was the intended route and the CLI refuses it: `openspec validate --strict` rejects a `MODIFIED` block that omits a
scenario name the merged spec still holds, because archive replaces the whole block and will not silently drop one.
The message is `MODIFIED "Helm charts have no environment-specific values in defaults" omits scenario(s) the current
spec still has: "Default values are obviously placeholders"`. The scenario being replaced cannot keep that name and
also be true, so `MODIFIED` cannot express this change at all. Three alternatives lost. Keeping the old scenario name
over a body that says the opposite is a false header, and a false header is the defect this change exists to remove.
Renaming the requirement and restating it does not help: the validator walks renames before comparing scenarios, so
the same rejection fires. Hand-editing the merged file afterwards is the unsanctioned route, because
`openspec/specs/` has exactly one writer.

Accept that the replacement lands at the end of the capability file. Archive appends an added requirement rather than
restoring the retired one's position, so the environment-defaults requirement moves from first to last and the other
two shift up. Their text is unchanged byte for byte, which was confirmed by running the archive against a copy of the
`openspec` tree in a scratch directory and diffing the result before doing it for real. Reordering the merged file by
hand to put it back would be a second, unsanctioned writer, and position carries no meaning the capability relies on.

Give the replacement a name that covers what it says. `Helm charts have no environment-specific values in defaults`
names a prohibition, and the requirement now carries a prohibition plus the enforcement that makes a missing overlay
fail at the command. `Chart defaults name no environment and a missing overlay fails the render` names both, so a
later reader matching on the header gets the whole of it.

Treat the enumerated list as closed and extend it, rather than reopening it with `such as`. Two things in the existing
sentence read as closure: the `and` before the final item, and the trailing clause that predicates something of every
member, which only means anything over a fixed set. An open list would also be unfalsifiable, so a reviewer could
never use it to judge a chart. The cost of closing it is that a seventh category has to be added here when one
appears, which is the same cost the sentence already carried.

Replace the trailing clause `each of which names a concrete environment's front door`. It was already loose for a
namespace and it is plainly untrue of a cross origin allow list, which names the browser origin rather than a front
door. `ties a rendered manifest to one concrete environment` is true of all six.

Add the mechanism as a normative sentence. `MUST NOT contain environment-specific literals` is satisfied by
`host: example.com`, which is exactly the weaker design the two commits removed, so the requirement has to say that
the default is empty and the template wraps it in `required`. Without that sentence the new scenario does not follow
from the requirement it sits under.

Carve the namespace out inside the list rather than dropping it from the list. It is still a literal a default must
not carry, and the remedy is simply different: a namespace is not something an overlay supplies, it comes from the
`helm` invocation, so the chart templates `{{ .Release.Namespace }}` and the template renders it through `tpl`.
Dropping it would leave the booking chart's templated offer address unexplained and invite the next reader to write
the namespace back in.

Quote helm's output verbatim, including the template path, the line and column, and the exit status. A paraphrase of
an error message cannot be checked, and the point of the scenario is that a person reading the terminal learns which
value they forgot.

Add the scenario for the charts that render cleanly. Five of the eleven top-level charts carry no environment-specific
value and exit zero with a full manifest set. Nothing in the capability said so, so the failing render reads as
universal, and the next person to add a chart would either add a pointless `required` or conclude the requirement was
already broken.

## Risks / Trade-offs

The quoted error string pins a template path, a line and a column. Any edit above line 19 of
`config/k8s/helm/service/sky-booking/templates/ingress.yaml` moves it and the scenario goes stale. Mitigation: the
scenario names the exact command, so the string is one render away from being regenerated, and the command is recorded
in tasks.md.

The chart list in the third scenario is a snapshot and a new chart changes it. Mitigation: the scenario says which
charts those are today and states the reason they render, so the reason survives even when the list moves.

Closing the enumerated list means a value in a seventh category is not covered until someone amends the requirement.
Mitigation: the mechanism sentence is category-independent, so a chart adding a `required` for a new kind of value is
consistent with the requirement before the list catches up.

Retiring a requirement header breaks any external reference to it. Mitigation: the only references are two archived
changes, `2026-06-25-helm-parameterize-cleanup` and `2026-09-12-correct-punctuation-in-cluster-delivery-specs`, and an
archive is a historical record that should keep naming the header it acted on.

## Migration Plan

Archive this change, take the sync the archive step performs itself, and confirm the merged file carries the
replacement requirement, no longer carries the retired one, and holds the other two byte-identical to their current
text. Rollback is `git checkout` on `openspec/specs/helm-charts/spec.md`, because nothing is built, deployed or
migrated.

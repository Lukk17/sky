## Why

The `helm-charts` capability carries a worked example the charts stopped honouring two commits ago. Its scenario
`Default values are obviously placeholders` expects `helm template` with no overlay to render manifests holding an
obviously fake value, `host: example.com`, so a person notices. `e2f4c41` and `4970536` replaced that with something
stronger: every environment-specific value now sits behind Helm's `required` with an empty default, so a render with
no overlay writes nothing and exits naming the value that is missing. The requirement is satisfied more strongly than
it was written, and the example under it is false. A reader taking it at face value would reintroduce the placeholder,
which is the weaker design the two commits deliberately removed.

## What Changes

- Retire the requirement `Helm charts have no environment-specific values in defaults` and replace it with
  `Chart defaults name no environment and a missing overlay fails the render`, which carries every prohibition the old
  one held. The retirement is what the OpenSpec CLI leaves available: a `MODIFIED` block may not drop a scenario name
  the merged spec still holds, and the scenario being replaced cannot keep its name and be true. The new name also
  covers what the requirement now says, which is a prohibition plus an enforcement mechanism.
- Expect the failing render, quoting the exact text helm prints, the exit status and the empty standard output,
  verified by running the render rather than by description.
- Extend the enumeration of forbidden literals. It reads as a closed list of four categories and two more are now held
  out of the defaults that it does not name: the identity provider issuer and redirect URL, and the cross origin allow
  list. Replace the trailing clause `each of which names a concrete environment's front door`, which is untrue of a
  namespace and of an allow list, with one that is true of every member.
- State the mechanism the requirement now rests on, because `MUST NOT contain environment-specific literals` is
  satisfied by a placeholder too. A value a chart needs in order to render MUST default to empty and MUST be wrapped
  in `required` at the point a template uses it.
- Acknowledge the namespace, which is the one named category not held behind `required`, because a namespace is not a
  value an overlay supplies. `sky-booking` templates `{{ .Release.Namespace }}` into the address it calls `sky-offer`
  on and renders it through `tpl`, so the address follows the release.
- Add a scenario for the charts that legitimately render with no overlay. Five of the eleven top-level charts carry no
  environment-specific value at all and exit 0 with a full manifest set, which nothing in the capability says today,
  so the failing render reads as universal when it is not.
- Add a scenario pinning the two-namespace render, which is the evidence behind the namespace clause.

Nothing here changes a chart, a template, a values file or any source. Every statement moves the specification onto
behaviour that already ships in `HEAD` and was verified by rendering.

## Capabilities

### New Capabilities

None. The capability already exists and keeps its `## Purpose`.

### Modified Capabilities

- `helm-charts`: one requirement is removed and one added in its place, covering the enumerated list of forbidden
  literals, the mechanism that holds them out of the defaults, and the scenarios under it.

## Impact

- Affected files: `openspec/specs/helm-charts/spec.md`, rewritten at archive time from the delta spec in this change.
- The replacement requirement lands at the end of the file, because archive appends an added requirement rather than
  restoring the retired one's position. The other two requirements keep their text byte for byte and their relative
  order.
- The other two requirements in that capability, on pinned image tags and on TLS secret names, are untouched. Both
  were re-checked against the charts and both still hold.
- No source, chart, template, values file, compose file, Bruno request or OpenAPI contract is touched, so there is
  nothing to build and nothing to deploy.
- Risk: low. The evidence is `helm template` output from the committed charts, reproducible from the commands recorded
  in `tasks.md`.

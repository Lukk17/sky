## Why

Commit 2c49a82 made the published path the path a service serves. Every `RewritePath` filter is gone from
`sky-gateway/src/main/resources/application.yaml`, and every `rewrite-target` and `use-regex` annotation is gone from
every API ingress in `config/k8s/helm/service/<service>/values.yaml` and its two overlays. The gateway now routes
`/api/v1/bookings/**`, `/api/v1/user/bookings/**`, `/api/v1/offers/**`, `/api/v1/search`, `/api/v1/owner/offers/**`,
`/api/v1/messages/**` and `/notifyWebsocket/**`, and each ingress serves the same paths at `pathType: Prefix` with no
rewrite. The swagger and api-docs ingresses keep their service prefix and their rewrite, because all three services
serve those two paths on the same addresses.

One merged sentence was falsified by that, in the other direction from the usual one. The versioning requirement gives
its reason for rejecting a header resolver as `the version is kept in the URL so the public addresses stay
/api/v1/...`, and adds that this left the frontend and every saved request working unchanged. Until this commit the
first half held of the address a service serves and not of the address a caller used. A caller wrote
`/offer/api/offers`, which carries no version segment at all, and the ingress rewrite supplied `/api/v1` behind it. So
the version this requirement keeps in the URL was invisible in the URL a caller actually held, which is the one place
it was being kept for. The second half held for the reason the sentence does not give: commit 94eb9b2 introduced the
`/api/v1` prefix and moved the rewrite target to `/api/v1/$2` in the same change, so the rewrite absorbed the new
prefix and the public addresses did not move. They moved now, and the saved requests in the Bruno collection moved
with them in the same commit.

Nothing states the property the commit established. The three merged specifications that could hold it say nothing
about it: `api-versioning` mentions the public address only in the clause above, `helm-charts` governs what a chart
holds out of its defaults and never mentions a path or a rewrite, and `api-contract` governs where a published
document comes from and explicitly assigns the addresses inside it to `api-versioning`. That gap is what let the
clause sit here for a week saying something no caller could observe.

## What Changes

- Modify the `Path-segment API versioning via the Spring Framework 7 native mechanism` requirement in
  `api-versioning`, in one sentence of its first paragraph. The header rejection and its reason stay. What is added is
  that the reason was true of a direct call and false of every route through the edge while the rewriting existed, why
  the frontend and the saved requests were left working at the time, and that the requirement added below is what
  makes the sentence hold of every route in. Every other sentence of the block, and all four of its scenarios, are
  carried byte-identical.
- Add a requirement to `api-versioning`, `The published address is the address the service serves`. It fixes that the
  path a caller sends is the path the service serves and that nothing in front of a service rewrites it, that the
  disjoint top-level resources the routing rests on are a property to preserve, that the swagger and api-docs
  ingresses are the one bounded exception, and the two things that follow: one published document describing the
  service at every hop, and an ingress authorization gate that no longer depends on how the controller orders two
  overlapping paths.

The argument for putting the new requirement in `api-versioning` rather than in `helm-charts`, in `api-contract` or in
a capability of its own is in design.md, because the brief asked for the argument rather than for a choice.

Nothing here changes code, a test, a chart, a compose file, a Bruno request, a published document or a module guide.

## Capabilities

### Modified Capabilities

- `api-versioning`: one requirement modified in one sentence, and one requirement added with five scenarios.

## Impact

- Affected files: `openspec/specs/api-versioning/spec.md`, rewritten at archive time from the delta in this change.
- No source file, test, chart, compose file, migration, Bruno request, published document or module guide is touched.
- Two agents are working in `docs/api/request` with `docs/api/README.md`, and in the service `application.yaml` files
  with `docs/api/openapi`, while this is written. Both trees were read for facts and neither was written to. No claim
  in the delta depends on the current content of either: the `servers` block of a published document is deliberately
  not referenced, and the scenario about one document describing every hop is written about operation paths alone.
- Risk: low on the modification, which is one sentence against a block checked line by line. Moderate on the addition,
  because it fixes an addressing rule that constrains every future endpoint. The part to read closely is the second
  paragraph, since a new top-level resource colliding with another service's is the one way to make the rule
  unaffordable, and the last paragraph, since it is the one that turns an accident of the old prefixes into a rule.

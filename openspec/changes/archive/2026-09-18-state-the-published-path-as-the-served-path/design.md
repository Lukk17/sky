## Context

The property to record is that the published path and the served path are one string, so the edge picks a service and
changes nothing else. It is visible in three places at once: the gateway route table in
`sky-gateway/src/main/resources/application.yaml`, the ingress path lists in `config/k8s/helm/service/*/values*.yaml`,
and the controller mappings the three services declare. A requirement that reaches only one of the three would let the
other two drift, and drifting apart is the exact failure the property prevents.

## Which capability owns it

Four homes were considered.

`helm-charts` was rejected. Its Purpose is keeping the charts deployable in more than one environment, and its three
requirements govern image tags, TLS secret naming and what a default `values.yaml` may hold. A path is not an
environment specific, so the rule would sit in that capability without being about its subject. The decisive objection
is reach: a chart requirement cannot say anything about the gateway, which is not a chart and ships no Helm chart at
all, so half the property would be unstated and the two halves would be free to disagree. That is how the local
gateway and the production ingress came to need a paired edit rule in a module guide in the first place.

`api-contract` was rejected, though it is the closest fit for one of the two consequences. Its Purpose says it governs
the document and assigns the addresses inside it, and the version literal that reaches them, to `api-versioning`. A
requirement about addresses would contradict that sentence, and a Purpose is not reachable from a delta, so the only
way to take that route is to hand-edit a merged file after the archive. The capability boundary it draws is also the
right one: `api-contract` answers where a document comes from, and this answers what an address is.

A new capability was rejected on the same boundary argument, not on size. A one-requirement capability is established
here rather than exceptional, so that was not the objection. The objection is that the requirement would have to be
named for the edge, and then `api-versioning` would keep two requirements about what address a resource has, the
plural-noun rule and the service-to-service namespace rule, while a third about what address a resource has lived
somewhere else. Splitting one subject across two files is what makes a contradiction survivable.

`api-versioning` was chosen. Three reasons, in the order they carry weight.

The rewriting was a versioning defect, not only an addressing one. A rewrite lets the version segment a caller sends
and the version segment a service serves be different strings, and under the old prefixes the caller's address carried
no version segment at all. A caller could therefore neither see which version it was on nor ask for another, which
defeats the reason this capability puts the version in the path rather than in a header. Fixing the addresses is what
makes path versioning observable to the caller it exists for.

Two of the three requirements already in this capability rule on what address a resource has rather than on the
version. The plural-noun requirement is about naming, and the service-to-service requirement forbids a second URL
namespace such as `/api/internal` and insists the address says what the resource is. A service-naming prefix that the
edge translates is exactly a second namespace for the same resource, so the new requirement finishes a rule this
capability already half carries. The one-line Purpose reads narrower than that, and the merged content is the better
evidence of what the capability owns.

The sentence being corrected and the requirement that makes it true then sit in one file, so a future edit to either
cannot silently contradict the other.

## What was deliberately left out

The mechanism by which ingress-nginx compiles locations is not in the delta. `config/k8s/helm/helm_README.md` and the
commit message both state that the swagger rows carrying `rewrite-target` and `use-regex` make ingress-nginx compile
every path on that host as a case-insensitive regex location, and that regex locations are first match over a list
sorted by descending path length. That claim is about a controller's template rather than about anything in this
repository, nothing here vendors that source, and no nginx container was run for this change. The property the delta
states instead is the one that can be checked here by reading the rendered path lists: two API paths on one host that
differ in whether they carry the oauth2-proxy annotations must not overlap. That rule is correct whichever way the
controller resolves an overlap, which is the point of stating it rather than the mechanism.

The `servers` block of a published document is not referenced either. It names one address today, the direct one, and
another agent is editing those files as this is written. The path set is the part that is the same at every hop, so
the scenario is written about operation paths and says only that what stands in front of the path differs.

## Why

Two of the three requirements in `spring-boot-hygiene` are stricter than the configuration this project deliberately
ships, so the capability reports a violation every time it is read and the real rule it was protecting is buried under
a false positive. The owner has decided both, and both decisions are narrowings rather than rewrites.

The requirement `No default credentials in committed config` forbids any profile configuration from carrying a
credential default. Three files carry one, and all three are local-profile files:
`sky-booking/src/main/resources/application-local.yaml`, `sky-offer/src/main/resources/application-local.yaml` and
`sky-message/src/main/resources/application-local.yaml` each default both `POSTGRES_USER` to `postgres` and
`POSTGRES_PASSWORD` to `local`. This project is for local use, its development credentials are deliberately committed,
and the `local` profile is already the repository's one relaxed branch: the same profile installs a JWT decoder that
verifies no signature, no issuer and no expiry, and `sky-gateway`'s `local` chain permits every exchange. A defaulted
development password is the smaller of the relaxations already accepted under that name. The decision is to exempt the
`local` profile explicitly, and to write the exemption so it cannot be read as licence for the default profile or for
any deployed environment.

The requirement `CORS defaults are restrictive` wants the committed default to name the production frontend alone, with
localhost relaxations living in a profile overlay. No such overlay exists. The default in each of the three REST
services names the production frontend plus three development origins, nothing in any profile file, compose file or
chart overrides `ACCESS_CONTROL_ALLOW_ORIGIN`, and the shape the requirement describes has never existed here. The
decision is to leave the configuration alone and relax the rule to forbid only a wildcard, which is the part that
actually matters and the part that holds everywhere today.

## What Changes

- Replace the credential requirement with one carrying a single named exemption for the `local` profile, the reasoning
  inside the requirement rather than left to be rediscovered, and three explicit boundaries so the exemption cannot
  grow: it reaches only a file whose name carries the `local` profile, it reaches no other profile, and the `local`
  profile is not to be activated in a deployed environment.
- Replace the cross-origin requirement with one that forbids a wildcard and says nothing about which named origins the
  list holds, and that covers every place this repository configures cross-origin access for a service rather than only
  the three `application.yaml` files: the WebSocket handshake allow-list in `sky-notify` is hardcoded in Java and the
  old rule did not reach it at all.
- Correct, in the same two blocks, the stale details inside them:
  - The credential rule named only a password-shaped key. The three exempt files default a username as well, and
    `sky-offer` defaults two object-store keys, so the rule has to speak of credentials rather than of passwords.
  - The cross-origin rule spoke of a profile overlay as though one existed. No service ships a cross-origin override in
    any profile file, so the sentence describing the overlay is removed rather than rephrased.
  - The cross-origin rule covered `application.yaml` only. `sky-notify` has no cross-origin property at all: its
    allow-list is a hardcoded `List.of(...)` in `WebSocketConfig`, and the ingress in front of `sky-offer` carries its
    own `cors-allow-origin` annotation. Both are now in scope of the rule they were always meant to be held to.

Nothing here changes code or configuration. Both requirements move toward configuration that already ships.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `spring-boot-hygiene`: the scope of the credential-default ban, and the strength and reach of the cross-origin rule.

## Impact

- Affected file: `openspec/specs/spring-boot-hygiene/spec.md`, rewritten at archive time from the delta in this change.
  Two of its three requirements are touched. The third, the starter-hygiene rule, is not.
- Two defects this pass found are reported rather than fixed, because fixing either is a code change nobody asked for
  in this scope:
  - `sky-offer/src/main/resources/application.yaml` defaults `S3_ACCESS_KEY` to `root` and `S3_SECRET_KEY` to
    `localdev` in the default profile, not in a profile file. The narrowed rule still forbids that, deliberately, so
    the capability now names one live violation instead of four false ones.
  - No service fails startup on a missing credential, which the requirement demands and keeps demanding. Spring Boot's
    property binder resolves placeholders with unresolvable ones ignored, so an unset `POSTGRES_PASSWORD` binds as the
    literal text `${POSTGRES_PASSWORD}` and surfaces later as a failed connection rather than as a startup error naming
    the property. No module carries a startup check to compensate. The requirement is left demanding the loud failure,
    because the specification is better than the implementation here and weakening it to match would lose the contract.
- One wildcard cross-origin value exists in the repository, `portal_cors_origins: '*'` in
  `config/k8s/Xperimantal/kong/kong-classic/kong-values.yaml`. It is the Kong developer-portal setting of an abandoned
  experiment that no script, chart or compose file references, and it is not a cross-origin policy for any sky service,
  so the rule is scoped to exclude it rather than written to be false on the day it is read. Deleting that directory is
  a repository-hygiene decision, not a change to this capability.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched, so there is nothing to build
  and nothing to deploy.
- Risk: low for the cross-origin narrowing as a statement of fact, and a real trade-off as a policy. See design.md.

## Context

See proposal.md Why for motivation. Current state: the Angular app logs in through Auth0 while services validate Keycloak JWTs from the `sky` realm, so no frontend token validates downstream and `SkyAuthService` token plumbing is an unwired stub. `sky-gateway` is a Spring Cloud Gateway WebFlux edge proxy with its own `local` permit all chain. In Kubernetes, `oauth2-proxy` (`keycloak-oidc`) already performs external auth with session cookies and forwards `Authorization` downstream. Services are stateless JWT resource servers with audience `sky-backend` and realm role conversion, and that interior stays untouched.

## Goals / Non-Goals

**Goals:**

- One issuer (`sky` realm) for browser and services in every environment.
- Tokens exist only server side at the gateway, never in the browser.
- Compose and local development behave like the cluster edge.
- Service side validation, error shapes, and 401 and 403 semantics unchanged.

**Non-Goals:**

- Sender constrained (DPoP or mTLS bound) tokens, designed as the follow up once the relay works.
- Changing service authorization rules, realm roles, or the audience value.
- Migrating stored user data or existing sessions, there are none worth keeping.

## Decisions

- `sky-gateway` carries the BFF with `spring-boot-starter-oauth2-client` plus `TokenRelay`, against adding a separate BFF service, because the edge proxy already fronts every local route and one fewer deployment means one fewer session owner.
- Server side sessions in the gateway's default in memory store, against an external store now, because every environment runs exactly one gateway replica and an external store ahead of a second replica is speculative infrastructure. The external store activates with the second replica.
- Short lived access tokens with server side refresh rotation, against long lived tokens, because theft blast radius shrinks to minutes and rotation is invisible to the browser.
- Frontend drops `@auth0/auth0-angular` entirely and derives login state from the session, against keeping the SDK as fallback, because two identity stacks is the defect being fixed.
- Exactly one session owner per environment, against layering `oauth2-proxy` and the gateway BFF in Kubernetes, because two session cookies produce double logins and competing logout semantics. The assignment is structural: `sky-gateway` ships no Helm chart, so in Kubernetes the owner can only be `oauth2-proxy`, while in compose and local development the gateway is the only edge and owns the session. Compose runs the gateway off the `local` profile today, so the compose file moves it to `default` with the `KEYCLOAK_*` variables pointed at the development realm.
- CSRF through SameSite plus Origin enforcement at the gateway, against synchronizer tokens in the first cut, because the frontend is served through the same gateway origin and Origin checking covers the cross site write case with no per form token plumbing.

## Risks / Trade-offs

- [Risk] Gateway becomes session stateful, losing the current stateless scale story → Mitigation is the external session store plus sticky free routing and short TTLs.
- [Risk] `oauth2-proxy` and gateway BFF both claiming the browser session in Kubernetes → Mitigation is the single owner rule in the spec with an explicit per environment decision recorded in tasks.
- [Risk] Silent refresh timing gaps cause one failing request per rotation window → Mitigation is proactive refresh ahead of expiry with single flight per session.
- [Risk] Logout propagation misses Keycloak or the proxy session, leaving a usable session elsewhere → Mitigation is front channel logout plus backchannel where configured, verified by a logout scenario in tasks.
- [Risk] WebSocket notify path needs session auth over an initial HTTP upgrade rather than a bearer header → Mitigation is authenticating the upgrade request through the session and scoping that decision explicitly in tasks.

## Migration Plan

Land behind the current tree with services untouched, cut the frontend from Auth0 to the session in the same change so no mixed mode ships, then switch each environment's session owner flag in order: compose, local development, Kubernetes. Rollback is the previous frontend build plus the previous gateway image, since services never change.

## Open Questions

None that change specs, approach, or tasks. DPoP binding scope is deferred to the follow up by design.

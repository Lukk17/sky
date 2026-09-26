## Context

Services in `apps/frontend/sky-view/src/app/services/` concatenate a per-service `*BaseAddress` with a path fragment from `apps/frontend/sky-view/src/environments/*`. Production fragments still name the retired prefixes, and the message fragments (`/send`, `/delete/`, `/received`, `/sent`) never matched the backend shapes either. The gateway on `http://localhost:5777` routes `/api/v1/bookings/**`, `/api/v1/user/bookings/**`, `/api/v1/offers/**`, `/api/v1/search`, `/api/v1/owner/offers/**`, `/api/v1/messages/**` and `/notifyWebsocket/**` with no rewrite. See `proposal.md` for motivation and `specs/frontend-api-integration/spec.md` for requirements.

## Goals / Non-Goals

Goals: one origin per environment, published paths everywhere, token on secured calls, socket through the edge. Keep the change mechanical: same components, same flows, new addresses.

Non-Goals: no Angular upgrade here (owned by `update-frontend-to-latest-angular`). No photo upload endpoints until the backend photo contract is rechecked. No auth library swap.

## Decisions

- Collapse the three base addresses into one `apiBaseUrl` per environment file, because per-service origins are what let the app bypass the edge. Keep `localDev` as the only file naming direct ports, so Gradle-run services stay reachable during development.
- Rewrite the message service methods rather than just the path strings, because send and delete differ in method plus shape (`POST /messages` with body, `DELETE /messages/{id}`), not only in prefix.
- Build the notify socket URL from the environment origin plus `/notifyWebsocket` instead of the hardcoded `ws://localhost:5554` string in `StompService`, so compose, cluster and local dev each resolve correctly. Rejected keeping a hardcoded host per environment because the origin already carries that fact.
- Keep the auth header logic in `sky-auth.service.ts` untouched in shape and only widen its coverage to every secured call, because the backend still validates the same Keycloak bearer token behind both edges.

## Risks / Trade-offs

- Risk: the cluster ingress requires the oauth2-proxy session while compose permits broadly, so a flow passing locally may redirect in the cluster. Mitigation is to verify each secured screen in both environments during the task pass.
- Risk: photo handling (`photoPath` form field) predates the object-store photo endpoints and may map to nothing. Mitigation is to leave photo calls out of this change and record the gap as a follow-up instead of guessing the contract.
- Risk: anonymous endpoints called with an expired token answer 401 from the resource-server chain even though they need none. Mitigation is to attach the token only when one is present and let anonymous calls go bare.

## Migration Plan

Land the environment plus service edits in one commit on top of the toolchain change, then run the Bruno collection against compose and the cluster to prove the paths. Rollback is the previous commit. No backend deploy is needed.

## Open Questions

None.

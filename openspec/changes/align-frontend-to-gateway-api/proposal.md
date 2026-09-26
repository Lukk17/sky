## Why

The imported frontend still calls the retired service-prefix addresses (`/offer/api/...`, `/booking/api/...`, `/msg/api/...`), which the gateway answers 404 since the `edge-paths-become-api-v1` change. Every authenticated screen is therefore broken against compose and against the cluster.

## What Changes

- Point all three environment files at one base address, the gateway origin (`http://localhost:5777` for compose, the deployed host for production), instead of per-service hosts and ports.
- Replace every retired path with the published `/api/v1/...` address the service serves: offers, owner offers, search, user bookings, bookings and the four message endpoints (`POST /messages`, `GET /messages/received`, `GET /messages/sent`, `DELETE /messages/{messageId}`).
- Route the notify WebSocket through the gateway (`/notifyWebsocket`) in deployed environments instead of dialling `sky-notify` directly.
- Align the message service calls with the real backend shapes (send becomes `POST /messages` with a body, delete becomes `DELETE /messages/{id}`).
- Send the Keycloak bearer token on every secured call through the gateway and ingress auth chain.
- **BREAKING**: drops support for the retired `/offer/api`, `/booking/api` and `/msg/api` prefixes in the frontend entirely.

## Capabilities

### New Capabilities

- `frontend-api-integration`: defines how `apps/frontend` addresses the backend through the single edge in every environment, which published paths it uses, and how auth and the notify socket travel.

### Modified Capabilities

None. Backend requirements (`api-versioning`, gateway routing) do not change, the frontend conforms to them.

## Impact

- Affected code: `apps/frontend/sky-view/src/environments/*`, `apps/frontend/sky-view/src/app/services/*` (`offer.service.ts`, `booking.service.ts`, `message.service.ts`, `StompService.ts`, `sky-auth.service.ts`), any component holding a hardcoded path.
- Systems: compose stack via `http://localhost:5777`, cluster via the deployed host. Direct per-service ports (5552 to 5555) are no longer used by the app.
- Depends on `update-frontend-to-latest-angular` for a working toolchain, but the path mapping is independent and may be implemented on the old toolchain first.

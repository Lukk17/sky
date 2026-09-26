## Purpose

Defines how the `apps/frontend` application reaches the backend through the single edge in every environment, so each screen calls the published address the service serves and behaves identically against compose and the cluster.

## ADDED Requirements

### Requirement: Single edge base address per environment
The frontend MUST address exactly one base origin per environment: the gateway origin under compose (`http://localhost:5777`), the deployed host in production, and direct service ports only under an explicit local development configuration. No production or default environment file may name a per-service port (5552 to 5555).

#### Scenario: Production build calls one origin
- **WHEN** the application is built with the production environment and a user opens the offers list
- **THEN** every API request goes to the deployed host origin and none targets a `5552` to `5555` port

#### Scenario: Compose run calls the gateway
- **WHEN** the application runs against the compose stack
- **THEN** every API request goes to `http://localhost:5777` and the browser network log shows no direct service port

### Requirement: Published versioned paths only
The frontend MUST call the published `/api/v1/...` addresses and MUST NOT call the retired `/offer/api`, `/booking/api` or `/msg/api` prefixes. The mapping MUST be: offers at `GET /api/v1/offers`, owned offers at `GET`, `POST` and `PUT /api/v1/owner/offers` plus `DELETE /api/v1/owner/offers/{offerId}`, search at `POST /api/v1/search`, user bookings at `GET /api/v1/user/bookings`, bookings at `POST /api/v1/bookings` plus `DELETE /api/v1/bookings/{bookingId}`, messages at `POST /api/v1/messages`, `GET /api/v1/messages/received`, `GET /api/v1/messages/sent` and `DELETE /api/v1/messages/{messageId}`.

#### Scenario: Retired prefix is gone
- **WHEN** any agent or human searches `apps/frontend/sky-view/src` for `/offer/api`, `/booking/api` or `/msg/api`
- **THEN** zero matches remain, excluding change archives

#### Scenario: Authenticated screens load against the gateway
- **WHEN** a signed-in user opens owned offers, user bookings, received messages and sent messages through the gateway
- **THEN** each list renders data from its `/api/v1/...` address with no 404 from the edge

### Requirement: Bearer token on secured calls
Every call to an endpoint the backend secures MUST carry the caller's Keycloak bearer token in the `Authorization` header, including the owner, booking and message endpoints. Anonymous endpoints (`GET /api/v1/offers`, `POST /api/v1/search`) MUST be callable without a token.

#### Scenario: Secured call carries identity
- **WHEN** a signed-in user opens their bookings through the gateway
- **THEN** the request carries a bearer token and the response is the user's bookings rather than 401

### Requirement: Notify socket travels through the edge
Deployed environments MUST open the notify WebSocket at the gateway-routed `/notifyWebsocket` path on the edge origin rather than dialling `sky-notify` directly. The subscription destination and payload shape stay unchanged.

#### Scenario: Notification arrives through the gateway
- **WHEN** a booking event fires while the user has the application open against compose
- **THEN** the notification arrives over the gateway WebSocket URL and is displayed

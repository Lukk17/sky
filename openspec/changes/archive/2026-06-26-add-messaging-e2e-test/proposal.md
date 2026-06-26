# messaging: e2e capability test proposal

## Why

The messaging service (sky-message, port 5553) is the only stateful service in the suite with no async
companion and no object storage, yet its core user-to-user flow has no end-to-end assertion that survives a
refactor. The existing Bruno requests assert field presence only (`res.body.id` isDefined) and run unauthenticated
in the captured `responses.json`, so they prove routing but not the authenticated round-trip. This test pins the
send / list-received / list-sent / delete flow for an authenticated user against the live gateway, asserting the
sender identity resolves from the Keycloak token and that a deleted message actually leaves the sender's sent page.

## What this will verify

- POST `/msg/api/messages` with a valid Bearer token returns HTTP 201 and a body carrying a server-assigned UUID
  `id`, `senderEmail` equal to `lukk@sky.dev`, the submitted `receiverEmail`, and the submitted canary `text`.
- GET `/msg/api/messages/received` returns HTTP 200 with a paginated body (a `content` array and a numeric
  `totalElements`).
- GET `/msg/api/messages/sent` returns HTTP 200 and the just-created message appears in the sender's sent page,
  matched by `id` and the canary `text` (persisted state in Postgres `sky.message`).
- DELETE `/msg/api/messages/{id}` returns HTTP 204 and the message no longer appears in the sender's sent page
  (persisted-state removal).

## Setup cost class

- [ ] 1: No state to reset, no fixtures, hits a single endpoint or MCP tool.
- [ ] 2: Single fixture upload OR vision-capable model OR PDF parsing.
- [x] 3: Single-service reset (e.g. Redis only).
- [ ] 4: Multi-service reset (DB + Redis + Qdrant + MinIO).
- [ ] 5: Seeded state + observation of an async background process.

This test resets one backing service only (Postgres `sky.message`) and uploads no fixture, so it is the cheapest
test in the suite to set up and runs first in a sweep.

## Fixtures needed

- None. The message body is inline canary text; no file is uploaded.

## Concurrency profile

- Mutates: Postgres `sky.message`, rows where `sender_email = lukk@sky.dev` (the canary messages this test creates
  and deletes).
- Conflicts with: none. No other test in the suite writes to `sky.message`.
- Serial: false.

## API client invocation

- Token: `docs/api/request/auth/get-token.yml` (Keycloak password grant), or the equivalent curl shown in the spec.
- Flow: `docs/api/request/message/send-message.yml`, `get-received-messages.yml`, `get-sent-messages.yml`,
  `delete-message.yml`. The spec drives the same endpoints with self-contained curl through the gateway so it is
  tool-portable.

## Number assignment

N reflects relative setup cost across the suite (lowest first). Messaging is the cheapest (single-service reset, no
fixture), so it takes the lowest prefix.

N: 1

## Next steps

After this proposal is approved:

- Generate `e2e/testing/1-messaging-test.md` from the `test-spec` artifact.
- Generate `e2e/testing/templates/1-messaging-tasks.template.md` from the `tasks-template` artifact.
- Each execution generates a `run` record under `e2e/testing/runs/`.

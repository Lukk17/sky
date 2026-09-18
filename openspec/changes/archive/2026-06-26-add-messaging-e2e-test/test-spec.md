# messaging: e2e test

## What this verifies

- POST `/msg/api/messages` with a valid Bearer token returns HTTP 201 and a body carrying a server-assigned UUID
  `id`, `senderEmail` equal to `lukk@sky.dev`, the submitted `receiverEmail`, and the submitted canary `text`.
- GET `/msg/api/messages/received` returns HTTP 200 with a paginated body (a `content` array and a numeric
  `totalElements`).
- GET `/msg/api/messages/sent` returns HTTP 200 and the just-created message appears in the sender's sent page,
  matched by `id` and the canary `text` (persisted state in Postgres `sky.message`).
- DELETE `/msg/api/messages/{id}` returns HTTP 204 and the message no longer appears in the sender's sent page
  (persisted-state removal).

## Prerequisites

Two checks: the Bruno CLI is installed and the gateway is reachable.

Check the Bruno CLI is installed.

```bash
bru --version
```

Expect a version number and exit code 0.

Check the gateway is reachable.

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:5777/actuator/health
```

Expect `200`.

## Reset state

None. The run below creates its own data and deletes it in the cleanup requests, so it is self-cleaning and
re-runnable.

## Run

One step: a single Bruno invocation from the collection directory.

```bash
cd docs/api/request && bru run auth message cleanup/delete-message.yml --env local --insecure
```

The auth folder mints the token, the message folder runs the create/read/assert requests with IDs chained
automatically by the collection's scripts, and the cleanup request deletes what was created. Bruno evaluates every
assertion in each request.

## Expected

The run summary reports Status PASS with all requests passed and all assertions passed: 17/17 assertions.

The assertion groups cover: the sent message returns HTTP 201 with a UUID `id`, `senderEmail` equal to
`lukk@sky.dev`, the submitted `receiverEmail`, and the canary `text`; the received page returns HTTP 200 with a
`content` array and a numeric `totalElements`; the sent page returns HTTP 200 with the created message present in
Postgres `sky.message`; and the cleanup delete returns HTTP 204.

## Fixtures

- None.

## Concurrency

- Mutates: Postgres `sky.message`, rows where `sender_email = lukk@sky.dev` (the canary messages this test creates
  and deletes).
- Conflicts with: none. No other test in the suite writes to `sky.message`.
- Serial: false.

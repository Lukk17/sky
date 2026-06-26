# messaging: e2e test

## What this verifies

- POST `/msg/api/messages` with a valid Bearer token returns HTTP 201 and a body carrying a server-assigned numeric
  `id`, `senderEmail` equal to `lukk@sky.dev`, the submitted `receiverEmail`, and the submitted canary `text`.
- GET `/msg/api/messages/received` returns HTTP 200 with a paginated body (a `content` array and a numeric
  `totalElements`).
- GET `/msg/api/messages/sent` returns HTTP 200 and the just-created message appears in the sender's sent page,
  matched by `id` and the canary `text` (persisted state in Postgres `sky.message`).
- DELETE `/msg/api/messages/{id}` returns HTTP 204 and the message no longer appears in the sender's sent page
  (persisted-state removal).

## Prerequisites

The runner needs curl and jq on PATH, a reachable gateway, and a Keycloak token for user lukk.

Check curl is installed.

```bash
curl --version
```

Expect a version banner and exit code 0.

Check jq is installed.

```bash
jq --version
```

Expect a version string such as `jq-1.7` and exit code 0.

Check the gateway is reachable through its public offer endpoint.

```bash
curl -s -o /dev/null -w '%{http_code}\n' 'http://localhost:5777/offer/api/offers?page=0&size=1'
```

Expect `200`.

Obtain a Keycloak access token for user lukk and export it.

```bash
export TOKEN=$(curl -sk -X POST https://keycloak.test:9443/realms/sky/protocol/openid-connect/token -d grant_type=password -d client_id=sky-backend -d client_secret=dev-only-change-in-prod -d username=lukk -d password=test1234 | jq -r .access_token)
```

Expect `$TOKEN` to be a non-empty JWT (three dot-separated segments). Verify with the next command.

```bash
printf '%s' "$TOKEN" | grep -Eq '^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$' && echo token-ok
```

Expect `token-ok`.

## Reset state

Remove any canary messages left by a previous aborted run so the sent page starts clean for this sender. This lists
the sender's sent messages and deletes those whose text carries the canary marker.

```bash
curl -s 'http://localhost:5777/msg/api/messages/sent?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq -r '.content[] | select(.text | startswith("SKY-E2E-MSG-CANARY")) | .id' | while read -r id; do curl -s -o /dev/null -X DELETE "http://localhost:5777/msg/api/messages/$id" -H "Authorization: Bearer $TOKEN"; done
```

## Run

Each step is one API call. Wait for the documented success status before moving to the next step.

1. Send a canary message and capture its id. Wait for HTTP 201.

```bash
export MSG_ID=$(curl -s -X POST 'http://localhost:5777/msg/api/messages' -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -H 'Accept: application/json' -d '{"text":"SKY-E2E-MSG-CANARY 7F3A automated e2e probe, please ignore.","receiverEmail":"owner@example.com"}' | jq -r .id)
```

2. List received messages. Wait for HTTP 200.

```bash
curl -s -w '\n%{http_code}\n' 'http://localhost:5777/msg/api/messages/received?page=0&size=20' -H "Authorization: Bearer $TOKEN"
```

3. List sent messages and confirm the created id is present. Wait for HTTP 200.

```bash
curl -s 'http://localhost:5777/msg/api/messages/sent?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$MSG_ID" '.content[] | select((.id|tostring) == $id)'
```

4. Delete the created message. Wait for HTTP 204.

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE "http://localhost:5777/msg/api/messages/$MSG_ID" -H "Authorization: Bearer $TOKEN"
```

5. List sent messages again and confirm the id is gone.

```bash
curl -s 'http://localhost:5777/msg/api/messages/sent?page=0&size=100' -H "Authorization: Bearer $TOKEN" | jq --arg id "$MSG_ID" '[.content[] | select((.id|tostring) == $id)] | length'
```

## Expected

Step 1 returns HTTP 201. The response body has a numeric `id`, `senderEmail` equal to `lukk@sky.dev`,
`receiverEmail` equal to `owner@example.com`, and `text` equal to the canary string sent. `MSG_ID` is a non-empty
number.

Step 2 returns HTTP 200. The body has a `content` array and a numeric `totalElements`.

Step 3 returns HTTP 200 and prints exactly one message object whose `id` equals `MSG_ID` and whose `text` is the
canary string, confirming the message persisted to the sender's sent collection in Postgres `sky.message`.

Step 4 returns HTTP 204.

Step 5 prints `0`, confirming the message was removed from the sender's sent collection (persisted-state removal).

## Fixtures

- None. The message text is inline canary content (`SKY-E2E-MSG-CANARY 7F3A`), not a file.

## Concurrency

- Mutates: Postgres `sky.message`, rows where `sender_email = lukk@sky.dev` (the canary messages this test creates
  and deletes).
- Conflicts with: none. No other test in the suite writes to `sky.message`.
- Serial: false.

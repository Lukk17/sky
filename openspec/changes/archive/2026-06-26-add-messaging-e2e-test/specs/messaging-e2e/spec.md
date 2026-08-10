## ADDED Requirements

### Requirement: Messaging capability has a behaviour-only e2e runbook

The suite MUST carry an immutable spec at `e2e/testing/1-messaging-test.md` and its run-record template at
`e2e/testing/templates/1-messaging-tasks.template.md` that drive the send, list-received, list-sent, and delete
flow for the authenticated user lukk@sky.dev through the gateway at `http://localhost:5777`. Assertions MUST be
observable behaviour only: HTTP status codes, response body content, and persisted state in Postgres `sky.message`.
The runbook MUST NOT assert on log substrings.

#### Scenario: Send then delete a message round-trips

- **WHEN** the runner sends a canary message as lukk@sky.dev and then deletes it by id
- **THEN** the create call returns HTTP 201 with a server-assigned UUID `id` and `senderEmail` equal to
  `lukk@sky.dev`, the message appears once in the sender's sent page matched by `id` and canary `text`, the delete
  returns HTTP 204, and a follow-up listing shows the message is no longer in the sender's sent page

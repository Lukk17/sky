## Context

See proposal.md, Why, for the motivation. Four constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops a
scenario name the merged requirement has. All six existing scenario names across the three restated requirements are
carried through unchanged, including the two `websocket` rejection scenarios that needed no edit at all but have to
travel with their block.

No build is run by this change and no broker is started, which bounds what the restatements may claim. Every value comes
from reading `sky-booking/src/main/resources/application.yaml`, `sky-offer/src/main/resources/application.yaml`,
`sky-notify/src/main/java/com/lukk/sky/notify/config/kafka/KafkaConsumerConfig.java`,
`sky-notify/src/main/java/com/lukk/sky/notify/adapters/inbound/KafkaListeners.java`,
`sky-notify/src/main/java/com/lukk/sky/notify/config/WebSocketAuthChannelInterceptor.java`,
`sky-notify/src/main/java/com/lukk/sky/notify/config/WebSocketConfig.java` and
`sky-common/src/main/java/com/lukk/sky/common/security/ResourceServerJwtAutoConfiguration.java`.

One requirement here is ahead of the code. That changes what this change is allowed to do to it, and the reasoning is in
Decisions.

## Goals / Non-Goals

**Goals:**

- Clear four semicolon-joined clauses without softening any rule they carried.
- Remove the last Auth0 reference from a live requirement, since the provider moved to Keycloak and the variable that
  names the issuer was deliberately made provider-neutral so the next move is a configuration change.
- State the audience check as conditional, because a requirement that lists a check which may not run teaches a reader
  to assume a guarantee the deployment may not have.
- Name the producers in scope, so the rule is auditable by counting rather than by interpretation.

**Non-Goals:**

- Weakening the producer requirement to the two properties that are set. See Decisions.
- Adding the producer configuration test the scenario names. That is test code, and this change touches none.
- The dead-letter routing requirement in `kafka-messaging` and the per-user delivery requirement in `websocket`.
  Neither carries a semicolon, so neither has to be restated, and restating a block obliges correcting every stale
  detail inside it. Both were read. The per-user one holds: the broker is enabled on `/queue` alone, the user
  destination prefix is `/user`, and the only send path is `convertAndSendToUser`. The dead-letter one carries a stale
  class name, reported in the proposal's Impact rather than corrected here, because correcting it properly means an
  evidence pass over its retry counts and its topic naming as well.
- The `local` profile's unverified decoder. See Risks.

## Decisions

Keep the five-property producer rule and report the gap. This is the same shape as the `@EmbeddedKafka` divergence that
`2026-09-12-correct-test-strategy-stack-and-coverage-gate` left standing earlier today, and it gets the same treatment,
because correcting a specification toward code that falls short of it deletes a rule rather than repairing a
description. The rule here protects something specific: with `retries`, `delivery.timeout.ms` and
`max.in.flight.requests.per.connection` unset, their values come from whatever the Kafka client ships as a default, so a
client upgrade can change how long a write is retried and how many requests may be in flight without any change in this
repository. Whether the current defaults happen to match the required values is not asserted either way here, because
establishing it would mean resolving the client and reading its documented defaults, and nothing in this repository
records them.

Name the three producers. "Every Kafka producer in the system" was correct and uncountable. The set is the publisher in
sky-booking, the publisher in sky-offer, and the dead-letter producer in sky-notify, and the third one matters most to
name because it is the one a reader would miss: it lives in a consumer configuration class, it exists to publish
failures, and it sets the same two properties and omits the same three.

Say the issuer is read from a provider-neutral variable rather than naming Keycloak. Naming Keycloak would be accurate
today and would make the requirement stale on the next provider change, which is precisely the failure the Auth0
wording represents. The variable name is the durable fact, and the comment in
`sky-notify/src/main/resources/application.yaml` says it was chosen for that reason.

Tie the audience check to its condition rather than dropping it. Dropping audience from the list would lose a real
guarantee for every deployment that does set an expected audience. Listing it unconditionally claims one that a
deployment without an audience does not have. The conditional form is the only one that is true in both deployments.

Keep the rejection scenarios untouched inside the restated `websocket` block. They are carried through character for
character. The expired-or-invalid one names a wrong audience as a rejection cause, which reads oddly beside a
conditional audience check, but it is consistent: where an audience is configured, a wrong one is a rejection, and
where none is, the scenario's other two causes still apply. Rewriting it would be a change nobody asked for in a
punctuation pass.

## Risks / Trade-offs

Leaving the producer requirement strict means `kafka-messaging` now knowingly fails against three producers. Anyone
auditing against it will find the same gap and may conclude the specification is stale again. Mitigation: the gap is
stated in the proposal with the file and line of every property that is set, so the next reader inherits the analysis
instead of redoing it.

The STOMP requirement is stated for the chain that runs outside the `local` profile. Under `local`, `sky-common`
registers an unverified decoder that accepts any well-formed token without checking signature, issuer or expiry, so the
validation this requirement describes does not happen there. That is deliberate and documented in the module, and the
four services run on the default profile even under compose, so the gap is structural rather than routine. It is not
written into the requirement because a security requirement that lists its own bypass in the same breath invites the
bypass to spread, and the right place for it is the module's own documentation, which already has it. Flagged here so
the omission is a decision rather than an oversight.

Naming the two listeners and their shared acknowledgement path pins a structural detail that a refactor could change
without changing behaviour. The requirement states the property that matters, which is that both reach the same
acknowledgement, rather than the method name that currently provides it.

## Migration Plan

Archive this change and confirm both merged specifications carry the corrected blocks, that the three requirements this
change does not touch are unchanged, that no semicolon joining two clauses remains in either file, and that Auth0
appears in neither. Rollback is `git checkout` on the two merged specifications, because nothing is built, deployed or
migrated.

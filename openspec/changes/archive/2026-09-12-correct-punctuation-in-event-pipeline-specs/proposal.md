## Why

The repository formatting rule forbids joining two independent clauses with a semicolon, and twenty lines across nine
merged specifications still do it. Four of those lines sit on one path: the notification pipeline that carries an event
from a publisher, through a topic, to a subscribed browser. `kafka-messaging` scenarios at lines 11, 18 and 22, and the
`websocket` scenario at line 11.

The punctuation is the occasion rather than the work. A delta replaces a requirement block by matching its header, so
fixing a scenario means restating its whole requirement, and restating a requirement means standing behind every fact
inside it. The three requirements touched here were therefore checked against the producer configuration, the consumer
container, the listener body and the STOMP interceptor, and the check turned up one stale fact, one overstatement and
one divergence the specification is right about.

The stale fact is the identity provider. The `websocket` requirement says the token is validated "using the same Auth0
issuer the REST API trusts". Auth0 is not in use and the variable is deliberately provider-neutral:
`sky-notify/src/main/resources/application.yaml` line 36 reads
`issuer-uri: ${OAUTH2_ISSUER_URI:https://keycloak.test:9443/realms/sky}`, and the comment above it at line 33 says the
name is provider-neutral on purpose. Auth0 appears in no live configuration file anywhere in the repository. The two
places it survives are a committed legacy secret and a comparison row in the root README, both outside this change.

The overstatement is the audience check. The same requirement lists audience alongside signature, expiry and issuer as
though all four always run. Audience validation is conditional:
`sky-common/src/main/java/com/lukk/sky/common/security/ResourceServerJwtAutoConfiguration.java` line 29 registers the
audience validator under `@ConditionalOnExpression("!'${OAUTH2_AUDIENCE:}'.isBlank()")`, so with no audience
configured there is no audience validator and the token is accepted without that check.

The divergence is the producer configuration, and here the specification is ahead of the code rather than behind it.
It requires five producer properties to be set explicitly and forbids relying on a default for any of them. Two are
set. `sky-booking/src/main/resources/application.yaml` lines 77 and 79 and
`sky-offer/src/main/resources/application.yaml` lines 92 and 94 set `acks: all` and `enable.idempotence: true` and
nothing else, and `sky-notify/src/main/java/com/lukk/sky/notify/config/kafka/KafkaConsumerConfig.java` lines 59 and 60
set the same two on the dead-letter producer. `retries`, `delivery.timeout.ms` and
`max.in.flight.requests.per.connection` are set by nobody. The scenario also promises a producer integration test
asserting the five values, and no test anywhere reads a producer configuration. The rule is not weakened to match: it
is restated with its force intact and the gap is reported.

## What Changes

- Restate the producer requirement with the punctuation fixed and the rule unchanged, naming the three producers that
  are in scope so "every Kafka producer in the system" is a countable set rather than a phrase.
- Report, without weakening the requirement, that three of its five properties are set by no producer and that the
  producer configuration test its scenario names does not exist. Both stay as requirements, because a durability
  setting left to a client default changes silently with a client upgrade, which is exactly what the rule exists to
  prevent.
- Restate the consumer requirement with the punctuation fixed in both scenarios. This one holds as written, and gains
  only the detail that two listeners share one acknowledgement path.
- Restate the STOMP requirement with the punctuation fixed, the Auth0 issuer replaced by the provider-neutral issuer
  the code actually reads, and the audience check stated as conditional on an audience being configured.
- Leave the three requirements in these files that carry no semicolon alone: the dead-letter routing requirement, the
  two scenarios of the per-user delivery requirement, and the two remaining STOMP rejection scenarios. One of them
  carries a separate stale detail, reported in Impact rather than fixed here.

No code, configuration file or test is touched, and no build is run. Every value comes from reading a committed file.

## Capabilities

### New Capabilities

None. Both capabilities already exist.

### Modified Capabilities

- `kafka-messaging`: punctuation in the producer scenario and in both consumer scenarios, plus the named set of
  producers in scope.
- `websocket`: punctuation in the valid-token scenario, the identity provider the issuer comes from, and the
  conditional nature of the audience check.

## Impact

- Affected files: `openspec/specs/kafka-messaging/spec.md` and `openspec/specs/websocket/spec.md`, both rewritten at
  archive time from the deltas in this change. Two of three requirements in the first and one of two in the second.
- Grouped together because they are one pipeline rather than two subjects. sky-offer publishes to `offerTopic-1`,
  sky-notify consumes it under a manual acknowledgement mode and pushes to the owning user's queue, and the `websocket`
  specification's own delivery scenario names `offerTopic-1` explicitly. A reviewer checking that an offset is not
  committed before the WebSocket emission succeeded is checking both capabilities in one pass.
- Reported rather than fixed, because the requirement carries no semicolon and this change has no evidence pass behind
  it: the dead-letter requirement in `kafka-messaging` names `JsonProcessingException` as a non-retryable exception,
  and the code registers `tools.jackson.core.JacksonException` instead, at
  `sky-notify/src/main/java/com/lukk/sky/notify/config/kafka/KafkaConsumerConfig.java` line 76. That is the Jackson 3
  migration leaving a Jackson 2 class name behind in a specification, and it wants its own correction.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected.
- Risk: low for the three corrections. The open item is the producer gap, which this change deliberately leaves as a
  failing contract rather than resolving it in either direction, because resolving it means either editing
  configuration in three modules and adding a test, or relaxing a durability rule, and neither belongs in a punctuation
  pass.

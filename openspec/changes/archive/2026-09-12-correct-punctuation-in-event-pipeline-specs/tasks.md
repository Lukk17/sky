## 1. Establish the punctuation scope

- [x] 1.1 Count the semicolon lines in `openspec/specs/kafka-messaging/spec.md` and `openspec/specs/websocket/spec.md`
  and verify the count is three and one
- [x] 1.2 Read each of the four lines and classify it as two independent clauses, a serial list separator or shell
  syntax, paying attention to the line where the semicolon follows `ack.acknowledge()` and could be mistaken for Java
  statement syntax
- [x] 1.3 Record which requirement block each line belongs to, since a scenario cannot be corrected without restating
  its requirement

## 2. Check the producer claims

- [x] 2.1 List every Kafka producer in the repository and verify the set is the publishers in sky-booking and sky-offer
  plus the dead-letter producer in sky-notify
- [x] 2.2 Record which of the five required properties each producer sets, with the file and line for each one found
- [x] 2.3 Verify the three properties not found are absent rather than set somewhere else, by searching every module
  main tree for each property name
- [x] 2.4 Search every module test tree for a test that reads a producer configuration, and record whether the test the
  scenario names exists
- [x] 2.5 Decide the handling: keep the rule at full force and report the gap, rather than restating the requirement
  down to the two properties that are set
- [x] 2.6 Establish whether the effective values of the three unset properties can be verified from this repository,
  and if not, assert nothing about them

## 3. Check the consumer claims

- [x] 3.1 Verify the listener container is configured with `AckMode.MANUAL_IMMEDIATE`, with the file and line
- [x] 3.2 Count the `@KafkaListener` methods in sky-notify and record which topics they consume
- [x] 3.3 Verify the acknowledgement happens after the WebSocket emission rather than before it, by reading the listener
  body and confirming no catch swallows the failure ahead of the acknowledge call
- [x] 3.4 Verify both listeners reach acknowledgement through the same path, so the requirement can say so

## 4. Check the STOMP claims

- [x] 4.1 Verify the interceptor is registered on the client-inbound channel rather than somewhere else
- [x] 4.2 Verify what the interceptor does on a CONNECT frame, and that it sets the principal on the session
- [x] 4.3 Record where the issuer comes from, with the file and line, and verify the variable name is provider-neutral
- [x] 4.4 Verify Auth0 appears in no live configuration file, and record the places it does survive so the claim is
  precise rather than absolute
- [x] 4.5 Verify whether the audience check is unconditional, by reading the registration condition of the audience
  validator in `sky-common`
- [x] 4.6 Read the requirement this change does not touch, `Notifications are delivered per-user, not broadcast`, and
  record whether it holds, without restating it
- [x] 4.7 Decide how to handle the `local` profile decoder, which does not perform the validation this requirement
  describes

## 5. Write the delta specifications

- [x] 5.1 Read both current merged specifications rather than assuming their content
- [x] 5.2 Write `specs/kafka-messaging/spec.md` and `specs/websocket/spec.md` under `## MODIFIED Requirements`, carrying
  each whole requirement block including the scenarios that need no edit
- [x] 5.3 Verify every requirement header and every existing scenario name matches the merged file character for
  character, so the archive step cannot drop a scenario
- [x] 5.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against a
  fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 5.5 Verify both deltas respect the wrap width of their merged files, which is one physical line per paragraph and
  per scenario bullet
- [x] 5.6 Run `openspec validate correct-punctuation-in-event-pipeline-specs --strict` and verify it reports no error

## 6. Archive and verify the merged files

- [x] 6.1 Archive the change and verify both merged specifications carry the corrected blocks
- [x] 6.2 Verify no semicolon joining two clauses remains in either merged file
- [x] 6.3 Verify Auth0 appears in neither merged file
- [x] 6.4 Verify the three untouched requirements are unchanged in their own text
- [x] 6.5 Verify `openspec validate --specs --strict` still reports eighteen passed and none failed
- [x] 6.6 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 7. Notes from the run

- Task 1.2 classification. All four lines join two independent clauses. The two that needed the closest reading were the
  `ack.acknowledge()` pair, where the semicolon sits outside the closing backtick and is punctuation rather than Java
  statement syntax. Had it been inside the code span it would have been neither a violation nor safe to touch.
- Task 2.2 found two of five properties set, in all three producers. `acks: all` and `enable.idempotence: true` at
  `sky-booking/src/main/resources/application.yaml` lines 77 and 79, the same pair at
  `sky-offer/src/main/resources/application.yaml` lines 92 and 94, and the same pair again as
  `ProducerConfig.ACKS_CONFIG` and `ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG` at
  `sky-notify/src/main/java/com/lukk/sky/notify/config/kafka/KafkaConsumerConfig.java` lines 59 and 60.
- Task 2.3 confirms the other three are absent everywhere. A search of every module main tree for `retries`,
  `delivery.timeout` and `max.in.flight` returns no configuration match.
- Task 2.4 found no such test. No test in any module reads `ProducerFactory.getConfigurationProperties()` or asserts a
  producer property, so the test the scenario names does not exist.
- Task 2.6 could not be settled from this repository. Whether the Kafka client defaults for the three unset properties
  match the required values is not recorded anywhere here, so the restated requirement and this note both decline to
  claim it either way.
- Task 3.2 found two listeners, consuming `offerTopic-1` and `bookingTopic-1` from
  `sky-common/src/main/java/com/lukk/sky/common/kafka/SkyTopics.java`.
- Task 3.3 holds. In `KafkaListeners.consume` the `ack.acknowledge()` call is the last statement of a `try` whose only
  companion is a `finally` that clears the correlation id. There is no `catch`, so a failure in `notifyClient`
  propagates and the acknowledgement is skipped, which is what the scenario describes.
- Task 3.4 holds. Both listener methods delegate to the one private `consume` method, so there is a single
  acknowledgement path.
- Task 4.4 is precise rather than absolute. Auth0 appears in no live configuration file. It survives in
  `config/k8s/secret/sealed/sealed-secrets.yaml` and `config/k8s/secret/secrets.yaml` as legacy key names, which
  `config/k8s/helm/helm_README.md` already documents as stale and needing a reseal, and as one comparison row in the
  root `README.md`. All three are outside this change.
- Task 4.5 found the audience check conditional, at
  `sky-common/src/main/java/com/lukk/sky/common/security/ResourceServerJwtAutoConfiguration.java` line 29, registered
  under `@ConditionalOnExpression("!'${OAUTH2_AUDIENCE:}'.isBlank()")`.
- Task 4.6 read `Notifications are delivered per-user, not broadcast` without restating it, and it holds.
  `WebSocketConfig` enables the simple broker on `/queue` alone and sets the user destination prefix to `/user`, and the
  only send path is `template.convertAndSendToUser(targetUser, "/queue/" + NOTIFY_DEST, message)` in `WebSocketService`.
  A broadcast `/topic/notify` destination is not merely unused, it is not a broker destination at all.
- Task 4.7 decided to leave the `local` profile bypass out of the requirement and state it in design.md instead. The
  reasoning is there under Risks.
- Task 6.4 holds. The diffs of both merged files show only the three restated blocks, plus the whitespace normalisation
  the archive step applies to the `## Purpose` and `## Requirements` headings and the trailing blank line.
- `openspec archive` emitted the same non-blocking warning as the changes archived before it today, that the proposal's
  Why section exceeds 1000 characters. Left as written, because each corrected claim needs the file and the line it was
  read from.

## 1. Establish the defect from the code before changing anything

- [x] 1.1 Read `BookingController` and `OfferApiController` in full and record every publication site, its payload and
  its `userInfo`, so the wire shape can be held identical
- [x] 1.2 Read `MessageController` and sky-notify's `KafkaListeners` and record whether either does the same thing, so
  the change is scoped to the services that actually carry the defect
- [x] 1.3 Establish whether the two services do it identically, and record the differences: three sites against two,
  a service method returning a confirmation string purely so the controller can publish it, and a delete payload
  formatted in the controller in one and returned from the service in the other
- [x] 1.4 Establish the transaction boundary of every one of the five sites from the annotations, including that
  `bookOffer` carries none and delegates to `BookingPersister`, so the ordering claim is read off the code
- [x] 1.5 Establish which existing tests assert a publication, and record that neither controller slice test does:
  the assertions live in `BookingIntegrationTest` and `OfferIntegrationTest`, and booking's delete has none at all

## 2. Move the call

- [x] 2.1 Replace `sendMessage(KafkaPayloadModel)` on both driven notification ports with methods named for the event,
  taking the DTO or the offer identifier
- [x] 2.2 Move the envelope, the timestamp and the serialisation into both outbound notification adapters
- [x] 2.3 Publish from `BookingServicePrimary.bookOffer` after the persister call, and from `removeBooking` in both
  branches, last in each
- [x] 2.4 Publish from `OfferServicePrimary.addOffer`, `editOffer` and `deleteOffer`, last in each
- [x] 2.5 Change `BookingService.removeBooking` to `void`, since the confirmation string had no other reader
- [x] 2.6 Strip the port, the `ObjectMapper`, the helper and the now-unused imports from both controllers

## 3. Move the tests, and prove they still bite

- [x] 3.1 Drop the notification mock from `BookingControllerTest`, `OfferApiControllerTest` and `OfferApiDocumentTest`,
  because the controller no longer has that collaborator
- [x] 3.2 Add to `BookingServicePrimaryTest` and `OfferServicePrimaryTest` an ordering assertion per publication site
  and a publishes-nothing assertion per rejection path
- [x] 3.3 Add the missing end-to-end assertion for the booking delete event, which no test covered before
- [x] 3.4 Delete all five publication calls, run `BookingIntegrationTest` and `OfferIntegrationTest`, watch five tests
  fail, and restore the calls

## 4. Add the rule and prove it bites

- [x] 4.1 Add the `adapters.inbound` to `domain.ports.outbound` rule to sky-booking and sky-offer, run both green
- [x] 4.2 Inject the notification port back into both controllers, run both red, read the failure and check that no
  older rule fires on it, then remove the injection
- [x] 4.3 Establish whether sky-message and sky-notify can carry the same rule, and record the answer in the
  requirement rather than adding a rule that matches nothing

## 5. Write the delta specification

- [x] 5.1 Extract the requirement block from the merged file so the header matches character for character
- [x] 5.2 Add the driven-port rule paragraph, naming which services carry it and why the other two do not
- [x] 5.3 Add the paragraph saying where a driven port is called from decides when the side effect happens, and that
  the move is recorded in the module guide
- [x] 5.4 Correct the `adapters.dto` deviation sentence, which named only `domain.service`
- [x] 5.5 Add one scenario for a controller publishing through a driven port
- [x] 5.6 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers, with a matcher first validated against a fixture holding an em dash, an en
  dash, an arrow and a bullet
- [x] 5.7 Run `openspec validate move-event-publication-into-the-domain-service --strict` and verify it reports no error

## 6. Update the module documentation

- [x] 6.1 Record in `sky-booking/AGENTS.md` and `sky-offer/AGENTS.md` where publication now happens, the new rule, and
  the ordering relationship before and after, as an accepted residual
- [x] 6.2 Update both `README.md` files where they describe the publication path
- [x] 6.3 Add the entry to both `CHANGELOG.md` files under the unreleased 2.0.0 entry

## 7. Verify

- [x] 7.1 Run `./gradlew build` from the repository root and verify it is green, including
  `jacocoTestCoverageVerification` at 0.90 line and 0.90 branch
- [x] 7.2 Rebuild and import only the two changed images, upgrade only those two releases, restart the rollouts, and
  verify both pods are healthy
- [x] 7.3 Run the Bruno collection and verify it still scores 19 of 19 requests and 97 of 97 assertions
- [x] 7.4 Create an offer with a known correlation id and read sky-notify's log for the received Kafka message, so the
  event is proved to come from the new place on the running cluster

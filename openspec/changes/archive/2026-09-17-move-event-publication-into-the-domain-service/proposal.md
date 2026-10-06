## Why

`BookingController` in sky-booking and `OfferApiController` in sky-offer each injected an outbound notification port,
built the Kafka envelope by hand and published their own events. An inbound adapter was reaching a driven port
directly, which is the same shape as a controller reaching a repository, and the repository case is already forbidden
outright. The predecessor change `enforce-architecture-guarantees-with-archunit` found this while writing that rule,
recorded it in its own run notes, and reported it rather than folding it in, because the repair changes when an event
fires and therefore changes tests.

The rule that exists closes one member of a family and leaves the rest open. A repository interface is one driven port
among several, all of them in `domain.ports.outbound`, so the notification port was never covered and a controller
could inject it with nothing objecting.

Publishing from the controller is not only a layering complaint. It puts the publication outside every boundary the
domain service establishes, so the event is ordered against nothing the domain does and is covered by no transaction
the service opens. The controller also had to serialise the payload itself and mint the envelope timestamp, which put
a second copy of the wire shape in an inbound adapter, next to the one the outbound adapter already owned.

## What Changes

- Move all five publication sites into the domain services: `bookOffer` and `removeBooking` in
  `BookingServicePrimary`, and `addOffer`, `editOffer` and `deleteOffer` in `OfferServicePrimary`.
- Replace the single `sendMessage(KafkaPayloadModel)` method on each driven notification port with methods named for
  the event, taking the DTO or the offer identifier, so the domain names what happened and the adapter keeps the
  envelope, the timestamp and the serialisation. `KafkaPayloadModel` leaves the domain of both services.
- Change `BookingService.removeBooking` from `String` to `void`. The confirmation string existed only so the
  controller could publish it, and leaving a return value nothing reads is what invites the call back into the
  controller.
- Add to sky-booking and sky-offer an ArchUnit rule that fails the build when a class under `adapters.inbound` depends
  on a class under `domain.ports.outbound`. Both were observed failing on a deliberate violation before being trusted.
- Correct the recorded `adapters.dto` deviation, which said the wire DTO is handled in `domain.service`, when it is
  also in the driving port that returns it and is now in the driven notification port that takes it.
- Record in the specification that where a driven port is called from decides when the side effect happens, and that
  moving such a call inward is recorded in the module guide of the service it moves in.

No wire payload changes. All five events carry the same `payload` and `userInfo` as before, which is what the
end-to-end tests that read the real Kafka record assert.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `architecture`: the requirement `Hexagonal layer separation is enforced by ArchUnit` gains the driven-port rule, a
  paragraph on what moving such a call means for ordering, a corrected deviation sentence and one scenario.

## Impact

- Affected specification: `openspec/specs/architecture/spec.md`, rewritten at archive time from the delta in this
  change. The requirement `Canonical package layout per service` is left byte-identical.
- Affected production source in sky-booking: `adapters/inbound/api/BookingController`,
  `domain/ports/inbound/BookingService`, `domain/ports/outbound/BookingNotificationService`,
  `domain/service/BookingServicePrimary` and `adapters/outbound/notification/BookingNotificationServicePrimary`.
- Affected production source in sky-offer: `adapters/inbound/api/OfferApiController`,
  `domain/ports/outbound/OfferNotificationService`, `domain/service/OfferServicePrimary` and
  `adapters/outbound/notification/OfferNotificationServicePrimary`.
- Affected tests: `architecture/HexagonalArchitectureTest` in both services gains one rule,
  `domain/service/BookingServicePrimaryTest` and `domain/service/OfferServicePrimaryTest` gain the publication
  assertions and the cases that must publish nothing, `BookingIntegrationTest` gains the delete-event assertion that
  never existed, and the three controller-level tests drop the notification mock the controller no longer has.
- Affected module guides and changelogs: `sky-booking/AGENTS.md`, `sky-booking/README.md`, `sky-booking/CHANGELOG.md`,
  `sky-offer/AGENTS.md`, `sky-offer/README.md` and `sky-offer/CHANGELOG.md`, under the unreleased 2.0.0 entry.
- No other module, no chart, no compose file, no migration, no Bruno request and no OpenAPI contract is touched.
  sky-common is unchanged: `KafkaNotificationPublisher` and `KafkaPayloadModel` were already the right shape and the
  move needed nothing from them.
- Risk: the ordering change carries it. Four of the five sites now publish inside the transaction that commits the
  change they announce, where they published after the commit before, so a commit failure in the instant after the
  send leaves an event naming a row that does not exist. The window is one commit wide, the direction of the trade is
  argued in the design document, and it is recorded as an accepted residual in both module guides beside the storage
  residual of the same family sky-offer already carries.

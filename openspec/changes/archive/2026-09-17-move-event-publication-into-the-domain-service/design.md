## Context

Two controllers published their own Kafka events. The question this change had to answer is not whether that is wrong,
which the predecessor change already settled, but what moving the call does to the relationship between publishing an
event and committing the change it announces. That relationship is different in the two services and different between
the two sites inside sky-booking, so it is written out here rather than summarised.

## Goals

- An inbound adapter reaches no driven port, in either service, with a rule that fails the build on the next attempt.
- The wire payload of all five events is unchanged, byte for byte.
- The ordering consequence is established from the code, stated plainly, and recorded where a contributor will read it.

## Non-Goals

- A transactional outbox. It is the correct answer to the residual this change accepts and it is a larger change with
  its own table, its own relay and its own failure modes. Nobody asked for one.
- An after-commit event listener. Same reason, and it would put a Spring eventing dependency into the domain that the
  allow list does not admit.
- Touching sky-message, sky-notify, sky-common or sky-gateway. `KafkaNotificationPublisher` and `KafkaPayloadModel`
  in sky-common already had the right shape and needed nothing.
- Splitting the wire DTO into a domain command or result type. That is the `adapters.dto` deviation, it is recorded,
  and resolving it is its own change.

## Decisions

### Where the transaction boundary already was, and what the move does to each site

The five sites do not move the same way, because the transaction boundaries were never uniform.

`BookingServicePrimary.bookOffer` carries no `@Transactional`. Its transaction lives one bean deeper, in
`BookingPersister.saveAndPublish`, which is a separate bean and therefore a real proxy boundary. A publication placed
in `bookOffer` after that call runs after the commit, exactly as the controller's did. This site's ordering is
unchanged.

`BookingServicePrimary.removeBooking` is `@Transactional` on the method. `OfferServicePrimary` is `@Transactional` on
the class, so `addOffer`, `editOffer` and `deleteOffer` are all transactional. In those four, a publication inside the
method runs inside the transaction, where the controller's ran after it committed. Before this change, a publish could
not precede a persistence failure, because persistence had already committed by the time the controller was reached:
the residual was a committed row with no event, which `KafkaNotificationPublisher` logs at warn and swallows. After
this change, the residual on those four is the mirror image: an event can be handed to the producer and the commit can
then fail, leaving an event naming a row that does not exist.

### Why that trade is accepted rather than engineered away

The window is one commit wide and the call sits last in each method, after every domain step including the event-store
append, so anything the domain rejects still publishes nothing. The alternative that closes it is an outbox or an
after-commit hook, both explicitly out of scope. sky-offer already carries an accepted residual of exactly this family
in its module guide, where the object-store delete runs inside the transaction that commits the row change, and the
same reasoning applies: the honest move is to state the window, keep the call last, and not reorder it earlier.

What the move buys in exchange is the thing the controller version could never have. The domain service now decides
whether the event fires, which means the decision sits next to the rules rather than one layer above them, and it is
ordered against the domain's own steps rather than against nothing.

### The port speaks events, not envelopes

`sendMessage(KafkaPayloadModel)` forced its caller to build the envelope, which is why the controller had to hold an
`ObjectMapper` and mint a timestamp. Replacing it with `publishCreated`, `publishEdited`, `publishDeleted` and
`publishRemoved` moves all three into the adapter and takes `KafkaPayloadModel` out of the domain of both services.

The alternative was to move the controller's `sendNotification` helper into the domain service verbatim, which would
have put `tools.jackson.databind` into `domain.service`. The recorded deviation permitting that package says in so
many words that no new code may rely on it, so the helper could not move as it stood. Passing the DTO instead pays the
older `adapters.dto` deviation, which the domain already carries in `domain.service` and in the driving port, rather
than widening the newer one, and that choice is now written into the requirement rather than left to be rediscovered.

Passing the domain entity instead of the DTO was considered and rejected for sky-offer specifically. `OfferDTO.photoUrl`
is derived per response by `OfferServicePrimary.toDto`, which asks `PhotoStorage` to presign the stored object. An
adapter mapping the entity itself would either drop that field, changing the wire payload, or call a second driven port
from inside a driven adapter.

### Why the rule is the package and not the type

The rule forbids `adapters.inbound` depending on `domain.ports.outbound`, rather than naming notification ports. A
package rule needs no edit when the sixth driven port arrives, and the package is already the thing the layout
requirement pins. It subsumes the existing repository rule, which is kept: a subsumed rule costs one extra line in a
failure report and gives a sharper message for the mistake it names, and the predecessor change made the same call.

## Risks and Mitigations

- An event published before a failed commit. Accepted, one commit wide, recorded in both module guides, call kept last
  in each method so nothing the domain rejects publishes.
- A silent regression to publishing nothing. Every one of the five sites is asserted end to end by a test that reads
  the real Kafka record through Testcontainers, and all five were watched failing with the publication removed.
- sky-message carrying no equivalent rule. Recorded in the requirement as a gap rather than written as enforced. The
  exposure is nil today, because the only driven port it holds is a repository the existing rule already covers.

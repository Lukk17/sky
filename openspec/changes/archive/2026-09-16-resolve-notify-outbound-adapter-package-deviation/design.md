## Context

Two facts about sky-notify are recorded in the specifications as things a reader should know rather than as things a
build enforces. One is the package `adapters.outbound.service`, carried as a deviation since the layout requirement
was corrected. The other is the WebSocket origin allow-list, described in the hygiene capability as a hardcoded list
in configuration code. Both are now settled in the same change, because both live in one small module and would
otherwise contend for the same build.

## Goals

- The package says what it adapts, and a rule fails the build the next time a name says nothing.
- The origin list is bound the way the three REST services bind theirs, with the deployed value coming from the chart.
- Every specification sentence this change makes untrue is corrected in the same change.

## Non-Goals

- Moving `NotificationPublisherPrimary` into a subpackage of its own. It sits directly under `adapters.outbound`, and
  whether an adapter that adapts nothing external belongs in a named subpackage is a separate question from the one
  this change answers.
- Adding a rule that every class under `adapters.outbound` sits in a subpackage. That rule would fail on
  `NotificationPublisherPrimary` today, so writing it is the same decision as the point above.
- Changing anything in the three REST services or in their charts, including their committed origin defaults.

## Decisions

### The name is the transport, not the concern

Two names fit. `websocket` names the transport, `notification` names the concern and is what sky-booking and sky-offer
call their outbound Kafka publisher. `notification` would collide in meaning with those two packages while adapting
something completely different, and the class it holds is a thin wrapper over `SimpMessagingTemplate` whose whole
content is the transport. `websocket` it is, which also makes the narrowed rule read as what it checks.

### The second rule forbids names that describe nothing, because it cannot check the opposite

The requirement asks for a subpackage named for what it adapts. No rule can judge whether a name describes a
transport, so the rule is written as the reachable half: no package under `adapters` may be named `service`, `impl` or
`util`. That admits a badly chosen name that is not on the list, and it closes the exact family of names that the
deviation came from. The specification says it is an approximation rather than implying the full sentence is enforced.

The narrowed `SimpMessagingTemplate` rule does the rest of the work for this module, because it pins the one class
that matters to the one package name.

### Both rules were run red before they were trusted

A rule whose selector matches nothing passes and reads like enforcement. Each rule was proved on a deliberate
violation, both injected in one run so each failure could be read on its own. A class under
`adapters/outbound/service` with no Spring dependency failed only the name rule, and a class directly under
`adapters/outbound` holding a `SimpMessagingTemplate` failed only the location rule. Both violations were removed.

### The origin test keeps proving a refusal, not a read

Feeding the bound property into the test would on its own prove only that the list was read. The test sets the
property to two origins that no environment uses, drives the acceptance cases from that same value, and asserts a 403
for two origins outside it. One of the two is `https://skycloud.luksarna.com`, which the deleted constant named, so a
list that survived anywhere in the code fails the test rather than passing it. Run against the previous
implementation, the suite fails four cases in both directions: it accepts an origin the property does not name and
refuses two it does.

### The committed default keeps the production frontend and drops the API host

The deleted constant named four origins. `https://sky.luksarna.com` is the frontend, `https://skycloud.luksarna.com`
is the API host, and the two localhost entries are the gateway and the development server. The default that replaces
it names the frontend and the two local origins, matching the shape the three REST services carry, and drops the API
host, which is not a browser origin for this stack. No per-service port is on any of the lists: in the cluster nothing
publishes one, and under compose a page calling its own service is same origin and never consults the list.

## Risks

- The chart value is required, so an environment without an overlay entry fails the render rather than starting on a
  default. That is the intended behaviour and it matches the three REST services, and it means a new environment needs
  the value written before it can install.
- An origin missing from the chart value is refused at the handshake, which surfaces to a user as a notification
  channel that never connects rather than as an error anywhere in the service log. The mitigation is that the value
  was rendered for all three environments and the deployed pod was exercised with an allowed origin and a refused one.

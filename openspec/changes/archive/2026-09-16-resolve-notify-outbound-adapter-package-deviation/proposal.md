## Why

The `architecture` capability records one present divergence rather than blessing it. sky-notify holds
`adapters.outbound.service`, a package named for neither a technology nor a concern, holding one class,
`WebSocketService`. Beside `domain.service` it reads as a second service package rather than as a driven adapter. The
requirement `Canonical package layout per service` asks every driven adapter to sit in a subpackage named for what it
adapts, the way `adapters.outbound.rest`, `adapters.outbound.notification` and `adapters.outbound.storage` do
elsewhere, so this one has been carried as a deviation to be resolved since 2026-09-12.

Nothing stopped it drifting in the first place. The rule that covers the class gates the whole of `adapters.outbound`
without looking at the subpackage name, so any name at all passes it. Renaming the package without changing the rule
leaves the next name free to drift the same way.

The same module carried a second thing the specification describes rather than enforces. `WebSocketConfig` passed a
list of four origins compiled into the class to both STOMP endpoints, so the deployed WebSocket endpoint accepted
`http://localhost:5777` and `http://localhost:4200` and no chart value could change that without a rebuild. The
requirement `Cross-origin configuration never allows every origin` in the `spring-boot-hygiene` capability names that
list as configuration code rather than a property, which stops being true the moment the list is bound from
`sky.crossOrigin.allowed` the way the three REST services already bind it.

## What Changes

- Rename `com.lukk.sky.notify.adapters.outbound.service` to `com.lukk.sky.notify.adapters.outbound.websocket` and
  move `WebSocketService` into it, so the package names the transport it adapts.
- Narrow the existing sky-notify rule over `SimpMessagingTemplate` from `adapters.outbound` to
  `adapters.outbound.websocket`, so the location rule now asserts the name that drifted.
- Add a sky-notify rule that fails the build when any package under `adapters` is named `service`, `impl` or `util`,
  which is the closest a rule can come to the specification sentence asking for a subpackage named for what it adapts.
- Bind sky-notify's allowed browser origins from `sky.crossOrigin.allowed`, overridden by
  `ACCESS_CONTROL_ALLOW_ORIGIN`, and supply the value from the Helm chart per environment.
- Remove the deviation paragraph from the layout requirement, drop the deviation from the audit scenario that lists
  it, and correct the hygiene requirement sentence that calls the sky-notify allow-list a hardcoded list in
  configuration code.

Both new rules were observed failing on a deliberate violation before they were trusted, and the violations were
removed again.

## Capabilities

### New Capabilities

None. Both capabilities already exist.

### Modified Capabilities

- `architecture`: the requirement `Canonical package layout per service`, its enforcement sentence, its recorded
  deviation and the scenario that audits the four services against the layout.
- `spring-boot-hygiene`: one clause of the requirement `Cross-origin configuration never allows every origin`, which
  describes how sky-notify holds its allow-list.

## Impact

- Affected specifications: `openspec/specs/architecture/spec.md` and `openspec/specs/spring-boot-hygiene/spec.md`,
  both rewritten at archive time from the deltas in this change. The requirement
  `Hexagonal layer separation is enforced by ArchUnit` is left byte-identical.
- Affected production source, all under `sky-notify/src/main/java/com/lukk/sky/notify`:
  `adapters/outbound/websocket/WebSocketService` moves from `adapters/outbound/service`,
  `adapters/outbound/NotificationPublisherPrimary` follows the import, and `config/WebSocketConfig` takes the origin
  list through its constructor instead of holding it as a constant.
- Affected configuration: `sky-notify/src/main/resources/application.yaml` gains the `sky.crossOrigin.allowed`
  default, and the chart under `config/k8s/helm/service/sky-notify` gains the value in all three values files plus
  the `ACCESS_CONTROL_ALLOW_ORIGIN` entry in the deployment template.
- Affected tests: `architecture/HexagonalArchitectureTest` gains one rule and narrows another,
  `config/WebSocketOriginIntegrationTest` drives its origins from the bound property and asserts a refusal for an
  origin the property does not name, and `adapters/outbound/websocket/WebSocketServiceTest` is new.
- Affected module guide: `sky-notify/AGENTS.md`, plus the module changelog entry for 2.0.0.
- No other service, no other chart, no compose file, no Bruno request and no OpenAPI contract is touched.
- Risk: low on the rename, which the compiler settles. The origin change is the one that can break a browser client,
  because an origin missing from the chart value is refused at the handshake rather than reported anywhere else, so
  the value was rendered for all three environments and the handshake was exercised against the deployed pod in both
  directions.

## MODIFIED Requirements

### Requirement: Cross-origin configuration never allows every origin
No cross-origin configuration this repository ships for a service, or for the ingress in front of one, may allow every origin. Each allowed-origin value MUST be an explicit list of named origins, MUST NOT be a wildcard, and MUST NOT be written as an origin pattern that a wildcard would satisfy. The rule MUST cover every place such a value is configured rather than only the ones that look like properties: the `sky.crossOrigin.allowed` value of each REST service together with the `ACCESS_CONTROL_ALLOW_ORIGIN` environment variable that overrides it, the `sky.crossOrigin.allowed` value of sky-notify, which the STOMP endpoints read for the WebSocket handshake and which was a list compiled into `WebSocketConfig` until it was bound like the other three, and the `cors-allow-origin` annotation on any ingress fronting a service. Which named origins the list holds is deliberately left open, and the committed default MAY name development origins alongside the production frontend, as all four services do today. That permission is deliberate, not tolerated: the earlier form of this rule required the default to name the production frontend alone and pushed localhost origins into a profile overlay, and no such overlay has ever existed here, so the rule could only have been satisfied by adding configuration nobody wanted and by leaving a local run unable to call its own API. The wildcard is the part that actually matters, because it is the only value that turns the allow-list from a decision into an absence of one, and forbidding it is the part that already holds everywhere.

#### Scenario: Default-profile CORS
- **WHEN** the application boots without an `ACCESS_CONTROL_ALLOW_ORIGIN` environment variable
- **THEN** the configured cross-origin value is the committed default, which names the production frontend and the development origins explicitly, and it is not a wildcard

#### Scenario: Auditing every cross-origin value for a wildcard
- **WHEN** an operator lists every cross-origin value this repository ships for a service or for an ingress in front of one, covering all four services and the ingress annotations in the Helm values
- **THEN** none of them is a wildcard or an origin pattern a wildcard would satisfy, and each one names its origins explicitly

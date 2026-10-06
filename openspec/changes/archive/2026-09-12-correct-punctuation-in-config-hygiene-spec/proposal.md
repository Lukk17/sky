## Why

The repository formatting rule forbids joining two independent clauses with a semicolon, and twenty lines across nine
merged specifications still do it. Four of those lines are in `spring-boot-hygiene`, more than in any other
specification: the requirement text at line 7, the audit scenario at line 11, the startup scenario at line 18 and the
requirement text at line 21. All three requirements in the file are affected, so all three are restated.

The punctuation is the occasion rather than the work. A delta replaces a requirement block by matching its header, so
fixing a line means restating its whole requirement, and restating a requirement means standing behind every fact
inside it. All three were checked against the build files and the configuration, and the check turned up two stale facts
and two divergences.

The first stale fact is a database that left a year of history behind. The startup scenario asks what happens when a
developer starts sky-booking without setting `MYSQL_PASS`. There is no MySQL and no such variable. The credentials are
`${POSTGRES_USER}` and `${POSTGRES_PASSWORD}`, read at `sky-booking/src/main/resources/application.yaml` lines 67 and
68 and at the same pair of lines in sky-offer and sky-message, both without a default, which is what makes the scenario
work at all.

The second stale fact is where the springdoc starter comes from. The requirement says services must declare exactly one
springdoc UI starter matching their stack. No service declares one. It is declared once, at
`buildSrc/src/main/kotlin/sky.web-conventions.gradle.kts` line 8, and a module receives it by applying that convention
plugin. Exactly three do, which are sky-booking, sky-offer and sky-message. sky-notify applies the Kafka conventions and
not the web ones, so it has no springdoc and no Swagger UI, which is correct for a service whose only HTTP surface is a
WebSocket handshake, and sky-gateway applies neither.

The first divergence is a credential default. The requirement forbids any profile configuration from carrying a default
for a credential. All three stateful services carry one: `password: ${POSTGRES_PASSWORD:local}` at
`sky-booking/src/main/resources/application-local.yaml` line 29,
`sky-offer/src/main/resources/application-local.yaml` line 24 and
`sky-message/src/main/resources/application-local.yaml` line 30. It is plainly deliberate, it matches the local compose
password, and it is still what the rule forbids.

The second divergence is the CORS default. The requirement wants the default to be the production frontend and wants
localhost relaxations to live in a profile overlay. The default is a four-entry list, the production frontend followed
by three localhost origins, at `sky-booking/src/main/resources/application.yaml` line 7 and the equivalent line in
sky-offer and sky-message. No overlay overrides it, so the localhost origins are not in an overlay at all, they are in
the default. The part of the rule that matters most holds: no CORS configuration anywhere uses a wildcard.

Neither divergence is resolved by rewriting the requirement that names it. Both rules are kept and both gaps reported.

## What Changes

- Restate the starters requirement with the punctuation fixed, the springdoc mechanism corrected to the convention
  plugin that actually carries it, and the reactive exception named, since sky-gateway deliberately runs on WebFlux
  through the Spring Cloud Gateway starter and must not be read as breaking the rule against WebFlux in a servlet
  service.
- Restate the credentials requirement with the punctuation fixed, with `MYSQL_PASS` replaced by `POSTGRES_PASSWORD`, and
  with its double negative repaired so it says plainly that a credential default is forbidden.
- Report, without weakening that requirement, the three local-profile credential defaults.
- Restate the CORS requirement with the punctuation fixed and with the profile named correctly, since the only
  non-default profile in this repository is `local` and no `dev` profile exists.
- Report, without weakening that requirement, that the localhost origins are in the default rather than in an overlay.

No code, build file or configuration file is touched, and no build is run. Every value comes from reading a committed
file.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `spring-boot-hygiene`: punctuation in all three requirements, the springdoc declaration mechanism, the credential
  environment variable name, and the profile name in the CORS requirement.

## Impact

- Affected file: `openspec/specs/spring-boot-hygiene/spec.md`, rewritten at archive time from the delta in this change.
  All three of its requirements are touched, which is why it is its own change rather than folded into a neighbour:
  every one of the three carries either a stale fact or a divergence needing its own judgement, and grouping it with
  another capability would have buried three decisions in one review.
- Kept separate from `api-versioning`, which would otherwise have been its natural partner as the other
  service-configuration capability. Two of that capability's three requirements describe a mechanism that was never
  built, which is a decision rather than a correction, so its three semicolons are reported to the owner and left in
  place.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected.
- Risk: low for the corrections. The two open items are left as failing contracts on purpose. Both are local-profile
  relaxations that look deliberate and harmless, and both are the same kind of decision the owner had to make about the
  embedded Kafka broker today, so both belong to an owner rather than to a punctuation pass.

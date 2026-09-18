## Context

See proposal.md, Why, for the motivation. Four constraints shape the approach.

`openspec/specs/` has one sanctioned writer, the archive step's spec rewrite, so a correction reaches a merged
specification as a delta rather than as a direct edit.

A delta replaces a requirement block by matching its header, and the archive step refuses a MODIFIED block that drops a
scenario name the merged requirement has. All three existing scenario names are carried through unchanged and no
scenario is added, so every block here is a restatement.

All three requirements in this capability carry a semicolon, so the whole file is restated. That is unusual among the
punctuation changes and it is the reason this capability is its own change: three independent judgements in one review
is already the limit, and pairing it with another capability would have pushed past it.

No build is run. Every value comes from reading the six module build files,
`buildSrc/src/main/kotlin/sky.web-conventions.gradle.kts`, `gradle/libs.versions.toml`, and the `application.yaml` and
`application-local.yaml` of the three stateful services.

## Goals / Non-Goals

**Goals:**

- Clear four semicolon-joined clauses without softening any rule they carried.
- Replace the last MySQL reference in a live requirement, because a scenario that names a variable no deployment reads
  cannot be executed by anyone, which makes it decoration rather than a check.
- Say where the springdoc starter comes from, because a contributor following the requirement as written would add a
  starter to a module build file that already has it transitively and would then have it twice.
- Keep sky-gateway's reactive stack from reading as a violation, since the WebFlux prohibition is about servlet modules
  and the gateway is the one module that is deliberately not one.

**Non-Goals:**

- Closing either divergence. Both are configuration edits in modules, and this change touches no configuration.
- Narrowing either rule to permit the local-profile relaxations it finds. See Decisions.
- The `api-versioning` capability, which would have been this one's natural partner. Its situation is reported to the
  owner, and the short version is in the proposal's Impact.

## Decisions

Keep the credential rule absolute and report the three local defaults. `password: ${POSTGRES_PASSWORD:local}` is
deliberate, it matches the local compose database, and nothing about a local run is endangered by it. It is also exactly
what the requirement forbids, and the requirement's phrasing is deliberately broad: it covers profile configuration
beside `application.yaml`, not only `application.yaml` itself. Narrowing it to exempt the `local` profile is the same
class of decision the owner took today about the embedded Kafka broker, and it has the same two possible answers, so it
is reported rather than taken. Worth noting for whoever takes it: the repository already treats `local` as a profile
where security is deliberately relaxed, since `sky-common` registers an unverified JWT decoder there, so an exemption
would be consistent with an existing pattern rather than a new precedent.

Keep the CORS rule as a default-versus-overlay split and report that the split is not how the configuration is laid
out. The rule is not a formality: the default list is what a production deployment serves if
`ACCESS_CONTROL_ALLOW_ORIGIN` is unset, and it currently includes three localhost origins. The practical exposure is
small, because a localhost origin only helps an attacker who already controls something on the victim's machine, and
the ingress enforces its own policy in front of the service. That is a reason the divergence is tolerable, not a reason
the rule is wrong.

Repair the double negative rather than preserving it. The old text read "No `application.yaml` or related profile config
MUST carry a default value", which literally says no file is obliged to carry one. The intended meaning is obvious from
the scenario beside it, and the restatement says it directly. This is the one place in this change where the wording
changes for readability rather than for a fact, and it changes no force.

Describe the springdoc rule by outcome and name the mechanism once. The testable property is that a REST service has
exactly one springdoc UI starter and a non-REST module has none. How that is achieved, which is one declaration in the
shared web convention plugin and three modules applying it, is named because without it the requirement sends a reader
to the wrong file. Naming the plugin does pin a build-level structure in a specification, which is the trade taken
deliberately: a reader who cannot find the declaration cannot audit the rule.

Name sky-gateway's starter exactly. The prohibition is on `spring-boot-starter-webflux` in a servlet module. The
gateway declares `spring-cloud-starter-gateway-server-webflux`, a different artifact, and it is reactive on purpose.
Without that sentence the next reader reconciles a WebFlux module against a rule forbidding WebFlux and has to go and
find out why it is allowed.

## Risks / Trade-offs

Two requirements in this file now knowingly fail, in five configuration files between them. Anyone auditing against them
will find the same five and may conclude the specification is stale again. Mitigation: each one is named in the proposal
with its file and line, so the next reader inherits the analysis, and both are flagged as owner decisions rather than as
defects to fix quietly.

Naming the convention plugin ties this requirement to the build layout, so moving the springdoc declaration would make
the requirement stale even though the outcome it describes still held. The outcome is stated first and the mechanism
second, so a reader who finds the mechanism moved still has the rule.

The claim that no CORS configuration uses a wildcard was established by searching the main source trees of all six
modules for a wildcard origin and finding only unrelated actuator exposure settings. It is not protected by a test.

## Migration Plan

Archive this change and confirm the merged specification carries all three corrected blocks, that no semicolon joining
two clauses remains, that MySQL appears nowhere in it, and that the credential and CORS rules are no weaker than they
were. Rollback is `git checkout` on the one merged specification, because nothing is built, deployed or migrated.

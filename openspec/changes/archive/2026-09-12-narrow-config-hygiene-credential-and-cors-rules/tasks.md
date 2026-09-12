## 1. Establish the credential evidence

- [x] 1.1 Enumerate every configuration file that could carry a credential default, by listing every
  `application*.y*ml` under `sky-booking/src`, `sky-offer/src`, `sky-message/src`, `sky-notify/src`, `sky-gateway/src`
  and `sky-common/src`, rather than trusting any module guide or any list given in the request
- [x] 1.2 Grep each of those files for `password`, `secret`, `credential`, `client-secret`, `token`, `passphrase`,
  `key` and `username`, and record every match that carries a default value after the colon inside `${...}`
- [x] 1.3 Verify the exempt set is exactly three files, `sky-booking/src/main/resources/application-local.yaml`,
  `sky-offer/src/main/resources/application-local.yaml` and `sky-message/src/main/resources/application-local.yaml`,
  and that each defaults both `POSTGRES_USER` to `postgres` and `POSTGRES_PASSWORD` to `local`, which is two keys per
  file rather than the one password the request named
- [x] 1.4 Verify `sky-notify/src/main/resources/application-local.yaml` carries no such default, which is consistent
  with it having no datastore
- [x] 1.5 Verify every default-profile `application.yaml` uses a bare `${POSTGRES_USER}` and `${POSTGRES_PASSWORD}`,
  and that `sky-gateway` uses a bare `${KEYCLOAK_CLIENT_ID}` and `${KEYCLOAK_CLIENT_SECRET}`
- [x] 1.6 Record the one violation outside the local profile: `sky-offer/src/main/resources/application.yaml` defaults
  `S3_ACCESS_KEY` to `root` and `S3_SECRET_KEY` to `localdev` in a file with no profile in its name
- [x] 1.7 Confirm the relaxations already accepted under the `local` profile, by reading
  `sky-common/src/main/java/com/lukk/sky/common/security/LocalSecurityAutoConfiguration.java` and its
  `UnverifiedJwtDecoder`, and the `@Profile("local")` branch of
  `sky-gateway/src/main/java/com/lukk/sky/gateway/config/SecurityConfig.java`, so the comparison written into the
  requirement is checkable rather than asserted
- [x] 1.8 Test the claim that a missing credential fails startup, by reading
  `PropertySourcesPlaceholdersResolver` out of the `spring-boot` jar on the current version line with `javap` and
  verifying it constructs `PropertyPlaceholderHelper` with the ignore-unresolvable flag set, and by grepping every
  module's main sources for any startup check on a resolved credential and verifying there is none

## 2. Establish the cross-origin evidence

- [x] 2.1 Verify the committed default of each REST service, recording that each names the production frontend plus
  the gateway port, the frontend development port and its own service port, and that the three differ only in that last
  entry
- [x] 2.2 Verify no profile file, compose file or chart overrides `ACCESS_CONTROL_ALLOW_ORIGIN`, by grepping `config`
  and every module's resources, so the claim that no overlay exists rests on a search rather than on the request
- [x] 2.3 Verify what each service actually applies the value to, by reading the three `CorsConfig` classes and
  confirming each calls `allowedOrigins` with the split list rather than `allowedOriginPatterns`
- [x] 2.4 Verify the fourth service, by reading
  `sky-notify/src/main/java/com/lukk/sky/notify/config/WebSocketConfig.java` and recording that its allow-list is a
  hardcoded `List.of(...)` of four named origins passed to `setAllowedOrigins`, with no property and no environment
  variable behind it
- [x] 2.5 Verify `sky-gateway` carries no cross-origin configuration at all, neither a property nor a `globalcors` block
- [x] 2.6 Search the whole repository for a wildcard cross-origin value and account for every hit, confirming the only
  one is the Kong developer-portal setting under `config/k8s/Xperimantal/`, that no script, chart or compose file
  references that directory, and that it is therefore not a cross-origin policy for any sky service
- [x] 2.7 Record the ingress reality, that only `sky-offer` carries `cors-allow-origin` annotations in its Helm values
  and that they name a different production hostname from the one the services default to, without editing either,
  because the Helm values are another agent's working area

## 3. Write the delta specification

- [x] 3.1 Confirm both scenario names survive the correction, so a rename plus a modification is available rather than
  a removal plus an addition
- [x] 3.2 Write `openspec/changes/narrow-config-hygiene-credential-and-cors-rules/specs/spring-boot-hygiene/spec.md`
  with a `## RENAMED Requirements` block carrying both FROM and TO pairs, and verify each FROM header matches the merged
  file character for character
- [x] 3.3 Write both blocks under `## MODIFIED Requirements` using the new headers, each carrying the entire
  requirement including the existing scenario under its existing name
- [x] 3.4 Verify the credential requirement states one exemption and three boundaries, and that none of the three can
  be satisfied by a reading that also admits the default profile or a deployed environment
- [x] 3.5 Verify the cross-origin requirement covers the three REST service values, the sky-notify hardcoded list and
  the ingress annotation, so every place found in section 2 is in scope of the rule
- [x] 3.6 Verify the delta holds no em dash, no en dash, no semicolon, and no bold or italic outside the `**WHEN**` and
  `**THEN**` markers the delta format requires, using a byte-exact matcher first validated against a fixture containing
  an em dash, an en dash, an arrow, a bullet and a box-drawing character
- [x] 3.7 Verify the delta respects the merged file's wrap width, which is one physical line per requirement paragraph
  and one per scenario line
- [x] 3.8 Run `openspec validate narrow-config-hygiene-credential-and-cors-rules --strict` and verify it reports no
  error

## 4. Archive and sync

- [x] 4.1 Archive the change, letting the CLI perform the merged-file rewrite, and verify
  `openspec/specs/spring-boot-hygiene/spec.md` carries both new headers and neither old one
- [x] 4.2 Verify the merged file's requirement count is unchanged at three, and that the starter-hygiene requirement
  and its scenario are byte-identical to what `2026-09-12-correct-punctuation-in-config-hygiene-spec` left, by diffing
  the block out of `git show HEAD:` against the rewritten file
- [x] 4.3 Verify the merged file still holds no semicolon joining two clauses
- [x] 4.4 Verify the change directory moved to
  `openspec/changes/archive/2026-09-12-narrow-config-hygiene-credential-and-cors-rules` and that `openspec list`
  reports it as no longer active
- [x] 4.5 Verify `git status` shows no file outside `openspec/changes` and `openspec/specs` that this change touched

## 5. Notes from the run

- Task 1.2 found more than the request described. The request named the database password in three local files. The
  three files each default a username as well, so six keys rather than three, and
  `sky-offer/src/main/resources/application.yaml` defaults two object-store keys in a file with no profile in its name.
  The narrowed rule covers the first six by exemption and still forbids the last two, so the capability now names one
  live violation instead of four false ones. The fix is a configuration change and is reported rather than made.
- Task 1.8 contradicted the requirement rather than confirming it. The requirement demands that a missing credential
  fail startup loudly, and nothing provides that: Spring Boot's `PropertySourcesPlaceholdersResolver` constructs its
  `PropertyPlaceholderHelper` with the ignore-unresolvable flag set, so an unset `POSTGRES_PASSWORD` binds as the
  literal `${POSTGRES_PASSWORD}` and surfaces as a failed connection instead. No module carries a startup check, and
  `sky-message`'s module guide still describes the one that did, along with the feature it guarded, both of which are
  gone from the source tree. The requirement keeps the MUST and the gap is reported, because the specification is better
  than the implementation here.
- Task 2.2 found that nothing overrides `ACCESS_CONTROL_ALLOW_ORIGIN` anywhere, not in a profile file, not in either
  compose file, not in any chart. So the committed default is also the deployed value, which makes the narrowing a real
  widening of the deployed cross-origin policy rather than only a documentation fix. Recorded in design.md under Risks.
- Task 2.4 found the rule's real gap. `sky-notify` has no cross-origin property at all: `WebSocketConfig` holds a
  hardcoded list of four origins, two of which are different production hostnames, `https://sky.luksarna.com` and
  `https://skycloud.luksarna.com`. The old rule spoke only of `application.yaml`, so the one allow-list that cannot be
  changed without a rebuild was outside it.
- Task 2.7 found that only `sky-offer` carries `cors-allow-origin` ingress annotations, that they name
  `https://sky.luksarna.com` while the three services default to `https://skycloud.luksarna.com`, and that
  `sky-booking` and `sky-message` carry no such annotation at all. Nothing was edited, because the Helm values are
  another agent's working area. Reported to the owner.
- The merged file's Purpose still says no credential is committed as a default, which the exemption now contradicts. A
  Purpose cannot be changed through a delta, so it is corrected by the separate change
  `correct-spec-purpose-statements`, which edits the merged file directly under `skip_specs`.
- `openspec archive` emitted one non-blocking warning, that the proposal's Why section exceeds 1000 characters. The
  section carries the evidence for two requirements. Left as written.
- Task 4.5 holds for this change. The working tree also carries another agent's in-flight Kafka producer work in
  `sky-booking`, `sky-offer` and `sky-notify`, none of it from here. The cross-origin and credential lines this pass
  relies on were re-read after those edits landed and none of them moved.

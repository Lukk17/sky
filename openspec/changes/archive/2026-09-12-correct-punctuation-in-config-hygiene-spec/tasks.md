## 1. Establish the punctuation scope

- [x] 1.1 Count the semicolon lines in `openspec/specs/spring-boot-hygiene/spec.md` and verify the count is four, the
  most of any specification in the repository
- [x] 1.2 Read each of the four lines and classify it as two independent clauses, a serial list separator or shell
  syntax, taking care over the two lines that read as three-item lists of rules rather than as two joined statements
- [x] 1.3 Record which requirement block each line belongs to, and confirm that all three requirements in the file are
  affected so the whole file is restated

## 2. Check the starter claims

- [x] 2.1 Search every module build file, every convention plugin and the version catalogue for
  `spring-boot-starter-data-rest` and verify it appears nowhere
- [x] 2.2 Search the same files for `webflux` and record every hit, so the one reactive module is named precisely rather
  than read as a violation
- [x] 2.3 Search the same files for `springdoc` and record where the UI starter is actually declared
- [x] 2.4 Record which modules apply the web convention plugin, and verify the set is exactly the three REST services
- [x] 2.5 Verify sky-notify and sky-gateway do not apply it, so the requirement can say a non-REST module has no
  springdoc starter rather than leaving it implied

## 3. Check the credential claims

- [x] 3.1 Record the database credential environment variable names from the `application.yaml` of all three stateful
  services, with the line of each
- [x] 3.2 Verify those references carry no default, which is what makes the startup-failure scenario work
- [x] 3.3 Search every profile configuration file in every module for a credential reference that does carry a default,
  and record every hit with its file and line
- [x] 3.4 Decide the handling for any hit: keep the rule and report, or narrow, and record why
- [x] 3.5 Verify no MySQL reference survives anywhere in the configuration, so the corrected variable name is the only
  one a reader will find

## 4. Check the CORS claims

- [x] 4.1 Record the default allowed-origins value of all three REST services, with the line of each
- [x] 4.2 Verify whether any CORS configuration uses a wildcard origin, which is the part of the rule that matters most
- [x] 4.3 Verify whether a profile overlay overrides the origins list, since the rule places localhost relaxations in an
  overlay
- [x] 4.4 Record which profiles exist in this repository, so the requirement names a profile that is real
- [x] 4.5 Decide the handling for the localhost origins in the default: keep the rule and report, or narrow, and record
  why

## 5. Write the delta specification

- [x] 5.1 Read the current merged specification rather than assuming its content
- [x] 5.2 Write `specs/spring-boot-hygiene/spec.md` under `## MODIFIED Requirements`, carrying all three whole
  requirement blocks
- [x] 5.3 Verify every requirement header and every existing scenario name matches the merged file character for
  character, so the archive step cannot drop a scenario
- [x] 5.4 Verify the delta holds no em dash, no en dash, no semicolon joining two clauses, and no bold or italic outside
  the `**WHEN**` and `**THEN**` markers the format requires, using a byte-exact matcher first validated against a
  fixture containing an em dash, an en dash, an arrow and a bullet
- [x] 5.5 Verify the delta respects the wrap width of the merged file, which is one physical line per paragraph and per
  scenario bullet
- [x] 5.6 Verify the restatement of the credential requirement is no weaker than the original despite the repaired
  double negative, by comparing the two clause by clause
- [x] 5.7 Run `openspec validate correct-punctuation-in-config-hygiene-spec --strict` and verify it reports no error

## 6. Archive and verify the merged file

- [x] 6.1 Archive the change and verify the merged specification carries all three corrected blocks
- [x] 6.2 Verify no semicolon joining two clauses remains in the merged file
- [x] 6.3 Verify MySQL appears nowhere in the merged file
- [x] 6.4 Verify the wildcard prohibition and the credential prohibition are both still present and still absolute
- [x] 6.5 Verify `openspec validate --specs --strict` still reports eighteen passed and none failed
- [x] 6.6 Verify `git status` shows no modified file outside `openspec/changes` and `openspec/specs`

## 7. Notes from the run

- Task 1.2 classification. All four lines were corrected. Two of them, the requirement text at line 7 and the audit
  scenario at line 11, are three-item lists whose items are independent clauses with no internal commas, so a semicolon
  was doing the work a numbered structure should do. Both were restated as a named set of three rules instead. This is
  the opposite call to the one made for `openspec/specs/architecture/spec.md` line 7, where the items do carry internal
  commas and the semicolons are a legitimate serial separator, which is why that line was left alone.
- Task 2.2 found one hit, `implementation(libs.spring.cloud.starter.gateway.server.webflux)` at
  `sky-gateway/build.gradle.kts` line 29. No module declares `spring-boot-starter-webflux`, so the prohibition holds and
  the reactive module is reactive through a different artifact.
- Task 2.3 and 2.4 found the starter declared once, at `buildSrc/src/main/kotlin/sky.web-conventions.gradle.kts` line 8,
  plus a deliberate `compileOnly` declaration in `sky-common/build.gradle.kts` line 33 that exists so the library can
  compile against springdoc without forcing it on a consumer. The modules applying `sky.web-conventions` are
  sky-booking, sky-offer and sky-message, exactly the three REST services.
- Task 2.5 holds. sky-notify applies `sky.spring-service-conventions` and `sky.kafka-conventions`, and sky-gateway
  applies only `sky.spring-service-conventions`, so neither receives springdoc.
- Task 3.1 and 3.2 hold. `username: ${POSTGRES_USER}` and `password: ${POSTGRES_PASSWORD}` at
  `sky-booking/src/main/resources/application.yaml` lines 67 and 68,
  `sky-offer/src/main/resources/application.yaml` lines 82 and 83, and
  `sky-message/src/main/resources/application.yaml` lines 59 and 60. None carries a default.
- Task 3.3 found three hits, all in the `local` profile: `password: ${POSTGRES_PASSWORD:local}` at
  `sky-booking/src/main/resources/application-local.yaml` line 29,
  `sky-offer/src/main/resources/application-local.yaml` line 24 and
  `sky-message/src/main/resources/application-local.yaml` line 30.
- Task 3.4 decided to keep the rule and report. The reasoning is in design.md under Decisions.
- Task 3.5 holds. No MySQL reference survives in any module configuration file.
- Task 4.1 found the default list at `sky-booking/src/main/resources/application.yaml` line 7,
  `sky-offer/src/main/resources/application.yaml` line 4 and `sky-message/src/main/resources/application.yaml` line 4,
  each reading the production frontend followed by `http://localhost:5777`, `http://localhost:4200` and the service's
  own port.
- Task 4.2 holds, which is the important half. No CORS configuration anywhere uses a wildcard origin. The only wildcard
  hits in the search were `management.endpoints.web.exposure.include: "*"` under the `local` profile of sky-offer and
  sky-message, which is actuator exposure rather than CORS.
- Task 4.3 found no overlay overriding the origins. The `local` profile files of all three services set no
  `sky.crossOrigin.allowed`, so the localhost origins exist only in the default.
- Task 4.4 found `local` to be the only non-default profile in the repository, plus `test` in the test source sets. No
  `dev` profile exists, so the requirement now names `local`.
- Task 5.6 holds. The original read "No `application.yaml` or related profile config MUST carry a default value", which
  as written says no file is obliged to carry one. The restatement says no such file may carry one, which is the meaning
  its own scenario already assumed, and it covers the same set of files.
- `openspec archive` emitted the same non-blocking warning as the changes archived before it today, that the proposal's
  Why section exceeds 1000 characters. Left as written, because each corrected claim needs the file and the line it was
  read from.

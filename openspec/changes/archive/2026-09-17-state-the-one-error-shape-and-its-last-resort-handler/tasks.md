## 1. Establish the behaviour from the code, not from the module guide

- [x] 1.1 Read `sky-common/src/main/java/com/lukk/sky/common/web/UnhandledExceptionResolver.java` and verify it
  implements `HandlerExceptionResolver` and `Ordered`, returns `Ordered.LOWEST_PRECEDENCE`, answers 500 as
  `application/problem+json`, sets `instance` from the request URI, carries no `Retry-After`, and builds its `detail`
  from a constant that names no exception type, message, class or package
- [x] 1.2 Verify in the same file that the failure is logged at error with the full stack, and confirm from
  `sky-booking/src/main/resources/logback-spring.xml` that the pattern renders `%X{correlationId}`, so the log line
  and the `X-Correlation-Id` header on the response join on one value
- [x] 1.3 Verify the registration route in
  `sky-common/src/main/java/com/lukk/sky/common/web/RestExceptionHandlerAutoConfiguration.java`: a `@Bean` under
  `@ConditionalOnMissingBean(UnhandledExceptionResolver.class)` on a class already listed in
  `AutoConfiguration.imports`, with no new entry in that file
- [x] 1.4 Verify the ordering claim against the framework source rather than against the module guide:
  `WebMvcConfigurationSupport.handlerExceptionResolver` sets the composite order to 0 (spring-webmvc 7.0.8, line 993),
  `DispatcherServlet.initHandlerExceptionResolvers` detects every `HandlerExceptionResolver` bean and sorts them, and
  `ControllerAdviceBean.getOrder` falls back to `Ordered.LOWEST_PRECEDENCE` (spring-web 7.0.8, line 158)
- [x] 1.5 Verify the premise that makes an advice unable to be last: the three service `GlobalExceptionHandler` classes
  declare no order, and `SkyRestExceptionHandler` and `SpringDataExceptionHandler` are both `@Order(0)`
- [x] 1.6 Verify the two non-REST consumers behave as the scenario says, from
  `sky-gateway/src/test/java/com/lukk/sky/gateway/config/ServletExceptionHandlingAbsentTest.java` and
  `sky-notify/src/test/java/com/lukk/sky/notify/config/UnhandledExceptionResolverInertTest.java`
- [x] 1.7 Verify the empty-body claim for 401 and 403 against `SkySecurityDefaults`, which installs no custom entry
  point or access denied handler, and against the `Unauthorized` and `Forbidden` responses in
  `docs/api/openapi/sky-message.openapi.yaml`
- [x] 1.8 Verify the bound: `CorrelationIdFilter` only reads a header and the shared chain answers 401 and 403 by
  status, so nothing in this repository throws inside the servlet filter chain today

## 2. Check every detail inside the requirement block being restated

- [x] 2.1 Check the `AutoConfiguration.imports` sentence against the file, and against
  `META-INF/spring.factories`, which holds one `FailureAnalyzer` that is not listed in the imports file and runs anyway
- [x] 2.2 Check the `@ConditionalOnMissingBean` sentence against all three shared handler beans, recording which type
  each one is keyed on
- [x] 2.3 Check the sentence requiring every shared auto-configuration to be guarded, against all eleven entries in the
  imports file, and record any that carry no condition
- [x] 2.4 Check the `compileOnly` sentence against `sky-common/build.gradle.kts`
- [x] 2.5 Check the six package names and the concern each is said to hold against
  `sky-common/src/main/java/com/lukk/sky/common/`
- [x] 2.6 Check the three scenarios being carried unchanged are still true, so carrying them verbatim is a finding
  rather than an omission

## 3. Write the delta specification

- [x] 3.1 Write `specs/sky-common/spec.md` with one `## ADDED Requirements` block and one `## MODIFIED Requirements`
  block, and verify the modified requirement header matches the merged file character for character
- [x] 3.2 Verify all five scenarios of the modified requirement are present, and that the three not being amended are
  byte-identical to the merged file
- [x] 3.3 Verify by sentence-level diff against the merged requirement that the only prose changes are the three the
  proposal names, and specifically that the sentence about guarding every auto-configuration is carried verbatim
- [x] 3.4 Verify the delta holds no em dash, no en dash, no semicolon and no bold or italic outside the `**WHEN**` and
  `**THEN**` markers, using a matcher first validated against a fixture holding an em dash, an en dash, an arrow and a
  bullet
- [x] 3.5 Verify the delta respects the merged file's wrap width, which is one physical line per paragraph and per
  scenario bullet
- [x] 3.6 Run `openspec validate state-the-one-error-shape-and-its-last-resort-handler --strict` and verify it reports
  no error

## 4. Archive and verify the merged result

- [x] 4.1 Archive the change and let the CLI perform the merged file rewrite
- [x] 4.2 Verify `openspec/specs/sky-common/spec.md` now holds two requirements, that the new one states the property
  before the constraint, and that the modified one carries the three corrections
- [x] 4.3 Verify the two earlier corrections to this capability survived: the module shape from
  `2026-09-12-correct-sky-common-module-shape` and the handler override route from
  `2026-09-12-add-sky-common-handler-override-route`
- [x] 4.4 Verify `openspec validate --specs --strict` reports no error, and that `openspec/changes` holds nothing but
  `archive`
- [x] 4.5 Verify `git status` shows no modified or added file outside `openspec/changes` and `openspec/specs`

## 5. Notes from the run

- The merged-file rewrite was performed by `openspec archive state-the-one-error-shape-and-its-last-resort-handler
  --yes`, which reported `+ 1 added` and `~ 1 modified` against `sky-common`. No merged specification was hand edited.
- Task 2.3 is checked because the check was run and its result recorded, and the result is a finding rather than
  a pass. It is the one finding this change deliberately does not write into the specification.
  Nine of the eleven auto-configurations listed in `AutoConfiguration.imports` carry `@ConditionalOnWebApplication`,
  `@ConditionalOnClass` or both. `CommonConfigPropertiesAutoConfiguration` and `StartupLogConfig` carry no condition at
  all, and `StartupLogConfig` imports `org.springframework.web.client.RestClient`, which `sky-common` declares
  `compileOnly`, so a consumer without spring-web would fail on it rather than silently do without it. Every consumer
  has spring-web today. This is the specification describing something better than what is built, so the sentence was
  carried through verbatim and the gap was raised with the owner rather than rewritten to match the code.
- Task 3.4 needed a matcher built and proved before it could be trusted. `grep -P` with `\x{2014}` exits 2 in this Git
  Bash with `character value in \x{} or \o{} is too large` and reports nothing, so a clean exit from it means nothing
  at all. The matcher used instead is the UTF-8 byte sequence of each dash, validated first against a five-line fixture
  holding an em dash, an en dash, an arrow and a bullet: it matched lines one and two and left the arrow, the bullet
  and the plain hyphen alone.
- Task 1.4 was checked against the Spring sources in the Gradle module cache rather than against the module guide,
  which is what the guide itself asks for. `WebMvcConfigurationSupport.handlerExceptionResolver` line 993 in
  spring-webmvc 7.0.8 registers the composite at order 0, and `ControllerAdviceBean.getOrder` line 158 in spring-web
  7.0.8 falls back to `Ordered.LOWEST_PRECEDENCE` for an advice that declares none. Both claims held.
- No Gradle build was run and no test was executed, by instruction. Every behavioural claim in the added requirement is
  either read from the source or already asserted by a test that shipped with `f895268`, and the tasks name which.

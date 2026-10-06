## 1. Establish what changed, from the build and the libraries rather than from a module guide

- [x] 1.1 Verify the three documents are generated output: read
  `buildSrc/src/main/kotlin/sky.openapi-conventions.gradle.kts` and confirm it writes
  `$rootDir/docs/api/openapi/<module>.openapi.yaml`, forks the service under the `local,openapi` profile pair, and
  wires `generateOpenApiDocs` into `build`
- [x] 1.2 Verify the three services carry a generation profile, and check it against the credential rule in
  `spring-boot-hygiene`: read `application-openapi.yaml` in sky-booking, sky-offer and sky-message and confirm none of
  them defaults a credential, since the fork runs `local,openapi` and `application-local.yaml` already carries them
- [x] 1.3 Verify the defect the change is about is real and present: confirm `sky.apiPrefix` is `/api/v1` in all three
  services and that every path key in all three documents under `docs/api/openapi/` reads `/api/1/...`
- [x] 1.4 Verify the mechanism that produced it, in springdoc rather than by inference:
  `PathVersionStrategy.updateOperationPath` in springdoc-openapi-starter-common 3.0.3 replaces the segment at the
  configured index with the version string it is passed, and `OpenApiResource.calculatePath` in
  springdoc-openapi-starter-webmvc-api 3.0.3 passes it `requestMappingInfo.getVersionCondition().getVersion()`
- [x] 1.5 Verify the runtime equivalence of `v1` and `1` in the Spring Framework source at
  `D:\Development\projekty-IT\spring-framework`, not from the brief: `SemanticApiVersionParser.parseVersion` calls
  `skipNonDigits` before matching `^(\d+)(\.(\d+))?(\.(\d+))?$`, so both parse to `Version(1, 0, 0)`, and
  `Version.equals` compares major, minor and patch
- [x] 1.6 Verify nothing downstream of the parse compares text: `DefaultApiVersionStrategy` runs the default, every
  `addSupportedVersion` value and every `addMappedVersion` value through `parseVersion`, and
  `VersionRequestCondition` parses the annotation literal through the strategy and matches on the parsed value
- [x] 1.7 Verify the state of the three controllers at the time of writing, so nothing here rests on a half-edited
  file: all three declare `@RequestMapping(path = "${sky.apiPrefix}", version = "1")`, which is the state that
  produced the published `/api/1/...` paths

## 2. Check every sentence of the api-versioning block being restated

- [x] 2.1 Check the configuration claim against
  `sky-common/src/main/java/com/lukk/sky/common/web/ApiVersioningAutoConfiguration.java`: it implements
  `WebMvcConfigurer` and configures versioning in `configureApiVersioning(ApiVersionConfigurer)`
- [x] 2.2 Check the resolver claim in the same file: `usePathSegment(1, predicate)` reads the second segment, and the
  predicate returns true only for a segment whose first character is `v` and whose second is a digit, so a resource
  name resolves no version
- [x] 2.3 Check the default and supported claims: `setDefaultVersion("1")` and `addSupportedVersions("1")`, and
  confirm against `ApiVersionConfigurer` that both names exist with those spellings
- [x] 2.4 Check that exactly one version is supported today, including through detection: `detectSupportedVersions`
  defaults to true, so a version appearing in a mapping joins the set, and only one version appears in any mapping
- [x] 2.5 Check the third scenario against the framework: `InvalidApiVersionException` extends `ResponseStatusException`
  with `HttpStatus.BAD_REQUEST`, so the invalid-version failure does carry 400
- [x] 2.6 Check the header sentence: no `useRequestHeader` call exists anywhere in the repository, and confirm which
  half of that sentence is still true, which is the frontend and the saved requests, and which half is not, which is
  the published API documents

## 3. Check every sentence of the sky-common block being restated

- [x] 3.1 Check the one-shape claim: all three service `GlobalExceptionHandler` classes return
  `org.springframework.web.ErrorResponse`, so every mapped failure answers as a problem detail
- [x] 3.2 Check the empty-body claim for 401 against a test rather than an assumption:
  `SkySecurityDefaultsTest.unauthorizedResponseHasAnEmptyBodyAndABearerChallenge` asserts 401, an empty body and a
  `WWW-Authenticate` value starting with `Bearer`
- [x] 3.3 Check the empty-body claim for 403 the same way, and against today's test results rather than by reasoning
  about the dispatcher: `MessageControllerTest.sendMessage_whenJwtHasNoRole_thenReturn403` asserts 403 with an empty
  body and `OfferApiControllerTest.getOwnedOffers_whenJwtHasNoRole_thenReturn403` asserts 403, and both ran green at
  15:37 and 15:40 today, after the last-resort resolver landed at 13:03
- [x] 3.4 Check that the header the sentence claims is really on the 403:
  `BearerTokenAccessDeniedHandler.handle` in spring-security-oauth2-resource-server 7.0.5 adds `WWW-Authenticate`,
  sets 403 and writes no body, and adds `error="insufficient_scope"` when the principal is an OAuth2 token
- [x] 3.5 Check what now documents that pair, which is the replacement the change is for:
  `sky-common/src/main/java/com/lukk/sky/common/openapi/ApiSecuredErrorResponses.java` declares both, and the text of
  both declarations appears verbatim in all three generated documents
- [x] 3.6 Check the second answer on status 403, which the clause does not currently mention: the shared annotation
  declares `application/problem+json` for it, and each service maps its own authorization exception to 403 with a
  problem detail
- [x] 3.7 Check the bound paragraph: `CorrelationIdFilter` only reads a header, mints one and sets it on the response,
  and `SkySecurityDefaults.filterChain` installs no custom entry point or access denied handler, so nothing in this
  repository throws inside the servlet filter chain today
- [x] 3.8 Check the ordering paragraph in the framework source: `ControllerAdviceBean.getOrder` falls back to
  `Ordered.LOWEST_PRECEDENCE` for an advice declaring no order, the three service advices declare none, and
  `UnhandledExceptionResolver` is a `HandlerExceptionResolver` bean returning `Ordered.LOWEST_PRECEDENCE`
- [x] 3.9 Check the answer paragraph against `UnhandledExceptionResolver`: 500, `application/problem+json`, `instance`
  from `request.getRequestURI()`, a fixed `DETAIL` constant naming no type, message, class or package, no
  `Retry-After` anywhere in the class, and `log.error` with the exception passed so the stack is logged
- [x] 3.10 Check the first scenario's `title` claim, which nothing in the resolver sets: `ProblemDetail.getTitle`
  returns the reason phrase of a resolvable status when no title was set, so the body reads `Internal Server Error`
- [x] 3.11 Check the last scenario's two halves still hold: both pinning tests exist, at
  `sky-notify/src/test/java/com/lukk/sky/notify/config/UnhandledExceptionResolverInertTest.java` and
  `sky-gateway/src/test/java/com/lukk/sky/gateway/config/ServletExceptionHandlingAbsentTest.java`
- [x] 3.12 Check `RestExceptionHandlerAutoConfiguration` still gates the bean on
  `@ConditionalOnWebApplication(SERVLET)` and `@ConditionalOnClass(ResponseEntityExceptionHandler.class)`, which is
  what makes that scenario true rather than a description

## 4. Decide where the generated-output fact belongs

- [x] 4.1 Read the Purpose and requirement list of `test-strategy`, `spring-boot-hygiene` and `repo-hygiene`, and
  record for each why it does or does not own a property about a published document
- [x] 4.2 Search every merged specification for a requirement that already covers the API documents, and confirm none
  does, so two requirements reference them and nothing defines them
- [x] 4.3 Confirm a capability holding one requirement is established here rather than an exception, by counting
  requirements per capability: four of the eighteen hold exactly one
- [x] 4.4 Record the decision and the argument in design.md, including the case against writing it anywhere

## 5. Write the delta specifications

- [x] 5.1 Extract both merged requirement blocks to a scratch file and build each delta from that extract, so every
  sentence not being changed is byte-identical rather than retyped
- [x] 5.2 Verify each modified requirement header matches the merged file character for character
- [x] 5.3 Verify by sentence-level diff that the only prose changes are the ones proposal.md names, and that all three
  existing scenarios of the versioning requirement and all four of the error requirement are carried unchanged
- [x] 5.4 Verify the deltas hold no em dash, no en dash, no semicolon and no bold or italic outside the `**WHEN**` and
  `**THEN**` markers, using a byte-exact matcher validated first against a fixture holding both dashes
- [x] 5.5 Verify the deltas keep the merged files' wrapping, which is one physical line per paragraph and per scenario
  bullet
- [x] 5.6 Run `openspec validate state-the-generated-api-contract-and-correct-what-it-falsified --strict` and verify it
  reports no error

## 6. Archive and verify the merged result

- [x] 6.1 Archive the change and let the CLI perform the merged-file rewrite
- [x] 6.2 Write the Purpose of the new `api-contract` specification by hand, because the archive leaves a placeholder
  there and no delta verb addresses a Purpose
- [x] 6.3 Verify `openspec validate --specs --strict` reports no error across all nineteen specifications, and that
  `openspec/changes` holds nothing but `archive`
- [x] 6.4 Verify `git status` shows no file changed outside `openspec/`, and read the diff of both merged
  specifications line by line to confirm nothing outside the intended edits moved

## 7. Notes from the run

- Task 3.3 is the one that was nearly got wrong by reasoning instead of measuring, and it is worth recording. Reading
  `DispatcherServlet.processHandlerException` suggests that a method-security denial thrown inside the dispatch would
  be resolved by the new last-resort resolver and answered as 500, because an empty `ModelAndView` counts as resolved,
  nothing before it handles `AuthorizationDeniedException`, `ResponseEntityExceptionHandler` does not list it, and it
  is a plain `RuntimeException` carrying no `ErrorResponse`. The measurement says otherwise. Two tests asserting 403
  with an empty body for a token with no realm role ran green at 15:37 and 15:40 today, and no `unhandled_exception`
  line appears in the captured output of either class, though it does appear in the precedence tests of the same run,
  so logging capture was working. The behaviour is therefore measured and the clause is true. The mechanism that keeps
  the denial away from the resolver was not traced to a source file and is left as an open question, worth one test in
  `sky-common` if the owner wants it pinned deliberately rather than held incidentally.
- Task 5.4 needed a matcher built and proved first. `grep -P` with `\x{2014}` is unusable in this Git Bash, so the
  matcher used is the UTF-8 byte sequence of each dash, validated against a fixture holding an em dash, an en dash, an
  arrow and a plain hyphen, and it matched only the two dash lines.
- Task 1.4 was checked in the sources jars from the Gradle module cache. Both springdoc classes were read rather than
  inferred from the output, though the output agrees with them: the version segment of every path in all three
  documents reads `1` while the services answer `v1`.
- Task 1.7 records a deliberate boundary. Another agent is editing the three controllers and the module guides while
  this change is written. Nothing here was taken from a controller as evidence of the end state, and nothing outside
  `openspec/` was written.
- No Gradle build was run and no test was executed. Running one would have compiled controllers another agent has open
  and would have contended for the daemon with their work. Every behavioural claim is read from source, from a library
  source jar, or from the test reports of the run that finished at 15:40 today.
- Nothing was committed.

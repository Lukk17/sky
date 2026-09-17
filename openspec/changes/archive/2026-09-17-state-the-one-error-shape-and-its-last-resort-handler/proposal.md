## Why

`sky-common` now ships `UnhandledExceptionResolver`, committed an hour ago as `f895268`, which answers 500 as an
RFC 9457 problem detail for anything every other handler declined, says nothing about the cause, and logs the failure
at error with the full stack. Before it, every deliberate failure in this repository already answered with a problem
detail and anything unanticipated fell through to Spring Boot's flat `{timestamp, status, error, path}` body, so one
API answered in two shapes and every client needed two parsers.

Nothing in the `sky-common` specification is contradicted by that, which is why this is an amendment and not a
correction. It is narrow in two ways that matter.

It never states the property the resolver exists to hold. No requirement anywhere under `openspec/specs/` names a
problem detail, in any capability, so the one error contract all four services share is written down in three
hand-written OpenAPI contracts and in a module guide and in no specification at all.

And it describes shared error handling entirely as advice: the `web` package holds "the exception handlers", the shared
REST exception handler is "an auto-configured bean rather than a base class", and a service that wants more handling
writes "a separate advice". Every word of that is true of the two shared advices. It is the wrong home for the class
just added, whose entire point is that it is not an advice, so a reader following the specification would put the next
one in the wrong place.

The decision that has to survive is why it is not one, and it is not a preference. `ExceptionHandlerExceptionResolver`
walks advices in order and returns from the first one whose handler matches the exception at all, so a catch-all in an
advice beats a more specific handler in a later one. The three service `GlobalExceptionHandler` classes declare no
order, and `ControllerAdviceBean.getOrder` resolves that to `Ordered.LOWEST_PRECEDENCE` (spring-web 7.0.8, line 158).
There is no order value below the lowest, so no second advice can be made to sort behind them, and a tie falls back to
bean discovery order, which is not a contract. An advice therefore cannot be last, and a catch-all advice here would
undo every 503, 502, 409 and 404 the services map. A `HandlerExceptionResolver` can be last:
`DispatcherServlet.initHandlerExceptionResolvers` detects every such bean and sorts them, and the entire advice set
lives inside the one `HandlerExceptionResolverComposite` that `WebMvcConfigurationSupport.handlerExceptionResolver`
registers at order 0 (spring-webmvc 7.0.8, line 993). That is structural rather than an ordering convention, and it is
the constraint a contributor has to meet before writing the next last-resort handler.

## What Changes

- Add one requirement to `sky-common` that states the property first, that an error this repository answers with a body
  carries one shape and only one, and then the constraint that keeps it true, that the last-resort handler sits outside
  the advice set and therefore cannot outrank a deliberate mapping. It also states the bound on the property, because a
  property stated without its bound is a property somebody will one day find false.
- Modify the existing single requirement so its `web` package clause admits a handler that is deliberately not an
  advice, and so the registration contract it states covers the mechanism this class actually uses.

Three things the evidence pass turned up inside the requirement block being restated, all corrected in the same delta.

- The `AutoConfiguration.imports` sentence is over-broad in two directions. It says a class not listed there "never runs
  in a consumer whatever annotations it carries". `UnhandledExceptionResolver` is not listed there and is a bean in all
  four services, because a `@Bean` on a class that is listed reaches a consumer through that entry. And
  `MissingCredentialFailureAnalyzer` is not listed there either and is registered in every consumer, because Spring
  Boot discovers a `FailureAnalyzer` through `META-INF/spring.factories`, which this module has carried since
  `1f6a850`. The contract is real and worth keeping, it is just a contract about auto-configuration classes.
- The override route says each shared handler "is declared `@ConditionalOnMissingBean` on the type it supplies". Two of
  the three are. `SkyRestExceptionHandler` is keyed on `ResponseEntityExceptionHandler`, its Spring supertype, at
  `sky-common/src/main/java/com/lukk/sky/common/web/RestExceptionHandlerAutoConfiguration.java` line 21. The difference
  is not cosmetic: it means a service that declares any bean extending `ResponseEntityExceptionHandler`, intending to
  add handling, silently takes the shared advice out of the context instead.
- The requirement covers three shared handler beans now rather than two, and the third is reached the same way, under
  `@ConditionalOnMissingBean(UnhandledExceptionResolver.class)` on line 28 of the same file.

One thing the evidence pass turned up and this change deliberately does not touch, reported instead. The requirement
says every shared auto-configuration "MUST be guarded so it contributes nothing to a consumer whose classpath does not
support it". Nine of the eleven listed in `AutoConfiguration.imports` carry `@ConditionalOnWebApplication`,
`@ConditionalOnClass` or both. `CommonConfigPropertiesAutoConfiguration` and `StartupLogConfig` carry no condition at
all, and `StartupLogConfig` imports `org.springframework.web.client.RestClient`, which this module declares
`compileOnly`. No consumer lacks spring-web today, so nothing is broken, but the specification is describing something
better than what is built and the fix would be a code change. The sentence is carried through verbatim and the gap is
raised rather than written away.

Nothing here changes code, and nothing here changes a test, a chart, a compose file, a Bruno request, an OpenAPI
contract or a module guide.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `sky-common`: one requirement added, covering the single error shape and the position of the last-resort handler that
  keeps it single. The existing requirement modified in three places, the registration contract, the override route,
  and the `web` package clause of its last scenario.

## Impact

- Affected file: `openspec/specs/sky-common/spec.md`, rewritten at archive time from the delta spec in this change.
- No source file, test, chart, compose file, migration, Bruno request, OpenAPI contract or module guide is touched.
- `sky-common/AGENTS.md` carries a full section on this class and was read as a lead rather than as evidence. Every
  claim it makes that this change relies on was re-established from the source tree, from the two Spring Framework
  7.0.8 source files named above, and from the tests that ship with `f895268`. The guide was found accurate on every
  point checked. It is deliberately not the text of the requirement: it explains a class to a contributor, and the
  requirement states what must hold for the next one.
- Risk: low for the modified requirement, where two corrections are annotations in one file and the third is a package
  clause. Higher for the added requirement, which states a property no specification has stated before, so its bound is
  the part to read closely: it is the failures that reach a service's request dispatcher, which is what the resolver
  can see.

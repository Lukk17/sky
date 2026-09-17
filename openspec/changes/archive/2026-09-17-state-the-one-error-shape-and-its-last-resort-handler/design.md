## Context

See proposal.md for why. The design question this change has to answer is not what the code does, which landed in
`f895268` and is not in dispute, but how much of it the specification takes on. `sky-common/AGENTS.md` already carries
a full section on the class, roughly eighty lines of it, and the tempting move is to lift that section into the
requirement. That would produce a requirement that goes stale the next time the class is refactored, and it would put
a contributor's explanation where a contract belongs.

## Goals

- The property is stated where a reader looking for the error contract will find it, which today is nowhere.
- The constraint that keeps the property true is stated as a constraint, so breaking it is a decision somebody has to
  argue against rather than a shortcut somebody takes without noticing.
- The requirement survives a rewrite of the class it protects.

## Non-Goals

- Restating the module guide. The guide explains one class to a contributor, the requirement states what must hold for
  the next one, and the two are deliberately different texts.
- Any code, test, chart, contract or guide change. The implementation already satisfies everything written here.
- A requirement about how the last-resort body is rendered, which converters write it, or how the correlation id
  reaches the log line. Those can all change without changing what a caller sees.

## Decisions

### Where the line between the specification and the module guide falls

The rule applied throughout: the specification carries what a caller can observe, plus the smallest amount of
mechanism without which the observable property cannot be kept. Everything else is the guide's.

What the specification therefore states, and why each item passes that test.

- The shape of an error body, as a caller sees it. Purely observable.
- The bound on that guarantee. Observable too, in the sense that matters: it is the one case where a caller sees
  something else, so leaving it out would make the requirement false rather than brief.
- Status 500, no `Retry-After`, no exception type, no message, no class, no package. All observable in a response.
- That the failure is logged at error with the stack, under the correlation id the response carries. Not observable to
  a caller, and stated anyway, because resolving an exception stops the container logging it. Without the obligation,
  a later refactor could delete the only record that the failure happened and no test of the response would notice.
- That the handler of last resort is not a controller advice, and is registered outside the advice set and sorted
  behind all of it. This is mechanism, and it is the one piece of mechanism the property cannot survive without. A
  catch-all inside the advice set changes what a caller sees on every mapped failure, and it changes it silently.

What the specification leaves to `sky-common/AGENTS.md`, each because the property survives it changing.

- The class name, the package it lives in, the auto-configuration that declares it, and the order constant.
- That the body is written through the converters of the application's own `RequestMappingHandlerAdapter`, taken
  lazily through an `ObjectProvider`, and what happens when no converter can write a problem detail.
- The `SpringDataExceptionHandler` rethrow case, which a resolver outside the advice set catches and no advice could.
  It is a strong argument for the position and it is an argument, so it belongs with the reasoning.
- That the correlation id is rendered by each service's `logback-spring.xml` pattern rather than put in the message by
  the resolver, and that the id would be gone by the time an `/error` dispatch ran.
- The residual reasoning about replacing `BasicErrorController`, and why that trade was not taken.

The requirement does name two framework types, `HandlerExceptionResolver` and `ResponseEntityExceptionHandler`. Both
earn it. The first is what makes "outside the advice set" checkable rather than a figure of speech, and the second is
the type the shared advice is keyed on, which a service has to know before it writes an advice of its own. The
specifications in this repository already work at that level: the Helm requirement names Helm's `required`, and this
same requirement already names `@ConditionalOnMissingBean` and `compileOnly`.

### Why a new requirement rather than one more clause on the existing one

The existing requirement answers what the shared library may hold and how a consumer may depend on it, which is what
its capability Purpose says. The error contract is a behavioural guarantee about four services, and it is true of them
whether or not the handler that keeps it true ships from a shared module. Folding it into a block that is already one
paragraph of eleven sentences would bury the one sentence this change exists to preserve, in the block a reader opens
to find out what may go in the module.

A separate requirement also gives the constraint its own header, which is the thing a contributor writing the next
last-resort handler will match against. That matters more than tidiness: the failure mode being guarded is somebody
reaching for the obvious tool, and an obvious tool is not stopped by a sentence in the middle of somebody else's
paragraph.

The cost is a cross-reference. The `(b)` clause of the module-shape requirement now points at the new requirement by
name rather than describing where the handler sits, so the two cannot drift into two accounts of one rule.

### Why the bound is in the requirement rather than in the guide

A property stated without its bound is a property somebody eventually finds false, and the person who finds it will be
holding a flat error body and a specification that says flat error bodies do not happen. They will then either file a
defect against code that is behaving as designed, or conclude the specification is unreliable. Naming the servlet
filter chain as the one uncovered path costs two sentences and removes both outcomes.

The alternative was to state the property absolutely and let the guide carry the residual. Rejected: the guide is not
what a reader consults to find out whether the contract holds.

### What the evidence pass changed inside the restated block

Three corrections, each listed in proposal.md with its file and line. Two of them are worth a word here on how they
were decided rather than what they say.

The `AutoConfiguration.imports` sentence could have been narrowed by simply deleting the absolute claim. It is kept
and qualified instead, because the claim is load bearing for auto-configuration classes and is exactly the discipline
that keeps a class from running in a consumer by accident. What it needed was a boundary, not a deletion, and the
boundary is now the thing that tells a contributor where a new shared bean and a new `FailureAnalyzer` each go.

The `@ConditionalOnMissingBean` keying could have been left alone, since it reads as right and only one of the three
beans contradicts it. It is corrected because the difference has a consequence a contributor can walk into: a service
adding a second advice that happens to extend `ResponseEntityExceptionHandler` would take the shared advice out of the
context rather than add to it, and would then also lose the framework error mappings it thought it was keeping.

## Risks and Mitigations

- A reader treats the bound as a licence to answer something else outside the dispatcher. Mitigated in the requirement
  text, which says in so many words that it is not, and by the bound naming one concrete path rather than a category.
- The property drifts from the three hand-written OpenAPI contracts, which `f895268` corrected to match. No test joins
  a contract to a requirement, so this is a real exposure and it is not new to this change. Mitigated only by the
  contracts and the requirement now saying the same thing, so a future divergence is visible to a reader of either.
- A later change adds a fifth service that maps requests and forgets the shared handling. The scenario about a
  consumer that is not a REST service covers the two current cases and does not cover that one, because nothing in
  this repository can enforce it today short of an ArchUnit rule in a module that has no test source of that kind.
  Recorded as a gap rather than written as enforced.

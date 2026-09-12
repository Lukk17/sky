## Why

The change archived as `2026-09-12-correct-sky-common-module-shape` corrected the `sky-common` requirement to say the
shared REST exception handler is an auto-configured bean rather than a base class for a service to extend. Its own
design.md decided to go one step further and name the route a service takes when it needs different behaviour, on the
reasoning that a prohibition without an alternative sends a contributor looking for one. The delta carried the
prohibition and omitted the route, so the merged requirement now forbids extending the handler and says nothing about
replacing it. The change's task 5.2 caught this and is recorded as not passing.

The route exists and is deliberate.
In `sky-common/src/main/java/com/lukk/sky/common/web/RestExceptionHandlerAutoConfiguration.java`, the bean method on
line 20 carries `@ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)` on line 19, and the bean method on
line 30 carries `@ConditionalOnMissingBean(SpringDataExceptionHandler.class)` on line 29. So a service
that declares its own bean of either type takes over, and the shared one does not compete with it. That is the sanctioned
override, and a reader of the specification currently cannot find it.

## What Changes

- Restate the `sky-common` requirement, extending only the clause about the REST exception handler, so it names the
  override route: a service replaces the shared handler by defining its own bean of the same type, and the shared one
  then does not register.
- Change nothing else in the requirement. Every other sentence and all four scenarios are carried over from the merged
  file unchanged, because they were corrected on evidence earlier today and nothing about them is in question.

Nothing here changes code.

## Capabilities

### New Capabilities

None. The capability already exists.

### Modified Capabilities

- `sky-common`: the exception handler clause of the single requirement gains the override route. No other clause and no
  scenario changes.

## Impact

- Affected file: `openspec/specs/sky-common/spec.md`, rewritten at archive time from the delta spec in this change.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is touched.
- Kept as its own change rather than folded back into `correct-sky-common-module-shape`, which is already archived.
  Reopening an archived change would rewrite a record of what was decided, and the defect was in that change rather than
  in the repository, so the honest shape is a second change that says so.
- Risk: low, and narrower than the change it follows. One clause is extended, four scenarios are carried verbatim, and
  the evidence is two annotations in one file.

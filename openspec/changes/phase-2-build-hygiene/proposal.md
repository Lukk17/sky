## Why

The build hygiene is good in pieces and not as a whole. `sky.jacoco-conventions` enforces
0.90 line and 0.90 branch per module, and every module except `sky-gateway` passes it,
but the gate is a per-module number: a module that drops to 0.89 fails and a module that
sits at 1.00 carries the whole composite on its back. There is no aggregate view, so a
release cannot state a number for the build as a whole and nobody can tell whether the
floor is being met by the weak modules or by the strong ones.

Java compilation does not emit parameter names. Every service keeps its own constructor
parameter names out of the bytecode, which is the one place the reflection-based code in
`sky-common` and the three controllers has to guess. It is one compiler flag, it is off
in every module, and it is on in none of the convention plugins.

Test logging is the last uninstrumented part of the test run. A failing test prints the
JUnit report and the stack trace, but a green run prints nothing at all, so the only way
to know a module's suite ran is to look at the exit code. That is fine in CI and useless
when a developer runs one module by hand.

## What Changes

- Add an aggregate JaCoCo task to the root build that merges the per-module XML reports
  and reports one line and one branch number for the whole composite, and fail the build
  when either drops below the per-module floor. The aggregate is a report and a gate, not
  a replacement for the per-module one: the per-module gate stays where it is and the
  aggregate adds a number on top of it.
- Add `-parameters` to the Java toolchain options in `sky.java-conventions`, so every
  module that applies it compiles with parameter names. `sky-gateway` applies the same
  plugin and gets it too.
- Add a test logging configuration that prints the class name and the method name of
  every test that runs, at INFO level, through the `test` task in `sky.java-conventions`.

No production class changes. No test changes. No source, chart, compose file, migration,
Bruno request or OpenAPI contract is touched.

## Capabilities

### New Capabilities

- `build-hygiene`: aggregate JaCoCo gate, `-parameters` on every Java compile, and test
  logging that names the class and method of every test that runs.

### Modified Capabilities

None.

## Impact

- Affected build files: the root `build.gradle.kts`, `buildSrc/src/main/kotlin/sky.java-conventions.kts`,
  and `buildSrc/src/main/kotlin/sky.jacoco-conventions.kts` if the aggregate lives there.
- Affected module guides: none, because the change is in shared build logic and every
  module inherits it.
- Risk: low. The aggregate reads reports the per-module gate already produces, the
  compiler flag is additive and changes no behaviour, and the logging change only adds
  output to a task that already ran.
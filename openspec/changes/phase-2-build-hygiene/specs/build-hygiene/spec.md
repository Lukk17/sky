## Aggregate JaCoCo Gate

### Aggregate coverage over the whole composite

**WHEN** the root `check` task runs

**THEN** the aggregate JaCoCo task merges the per-module XML reports the per-module gate
already produced, over the same filtered class set, and reports one line coverage and
one branch coverage number for the composite

**WHEN** the aggregate task runs

**THEN** it fails the build when the line coverage drops below 0.90 or the branch
coverage drops below 0.90, the same thresholds the per-module gate uses

**WHEN** a module produces an empty measured set

**THEN** the aggregate skips that module's report rather than counting it, so an empty
counter cannot drag the merged number down

### `-parameters` on every Java compile

**WHEN** any module compiles its Java sources

**THEN** every `JavaCompile` task carries `-parameters` in its compiler args, so
constructor and method parameters carry their names into the bytecode

### Test logging names every test that runs

**WHEN** the `test` task runs in any module

**THEN** every test that starts prints its fully qualified class name and its method name
on one line, whether it passes or fails
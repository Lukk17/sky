## Context

The repository runs one composite Gradle build over six modules. Three of the hygiene
gaps are in shared build logic and one is a missing aggregate; none of them is in a
module's own source.

## Goals

- One number for the whole composite that the per-module floor implies, computed from
  the reports the per-module gate already produces.
- Parameter names in bytecode for every module, from one place in the convention plugin.
- Every test that runs names itself in the log, so a green run is observable.

## Non-Goals

- Raising the per-module floor. 0.90 line and 0.90 branch stays where it is; the
  aggregate enforces the same number over the merged set rather than a higher one.
- Replacing the per-module gate with the aggregate. The per-module gate gives a module
  name when it fails; the aggregate gives a number. Both are kept.
- Touching `sky-gateway`'s coverage exemption. It stays disabled in its own build file,
  and the aggregate excludes that module's empty counter from the merged number by
  skipping modules whose measured set is empty, the same way the plugin already does.

## Decisions

### The aggregate is a root-level task, not a plugin

The per-module gate lives in `sky.jacoco-conventions`, which is applied per module and
names no module. An aggregate over the composite cannot live there, because the plugin
runs inside each module's own build and has no view of the other five. The root
`build.gradle.kts` is the one place that sees all six modules, so it is where the merge
and the gate go. It depends on every module's `jacocoTestCoverageVerification` task
through the composite's project dependencies, so it runs after the per-module gate has
already decided each module on its own.

### The aggregate reuses the same filter the reports use

`sky.jacoco-conventions` sets `classDirectories` from one filter on purpose: reporting
one set and enforcing another is how a gate ends up enforcing a number nobody agreed to.
The aggregate reads the per-module XML reports, which were already produced over that
filtered set, and merges them. It does not re-filter, because re-filtering here would
reintroduce the drift the per-module plugin avoids. If the filter changes, both move
together because the reports already moved.

### `-parameters` goes in `sky.java-conventions`, not per module

The flag is a toolchain option, and `sky.java-conventions` is the one plugin every module
applies. Adding it there means all six modules get it and no module can drift. It is
additive: it changes the bytecode and nothing else, and no existing test reads parameter
names out of reflection, so the test suite is unaffected.

### Test logging prints class and method at INFO

The `test` task already has a `testLogging` block in the convention plugin. The change
adds `showStandardStreams = false` is left alone and instead sets the events that get
printed: `testEvents = ["started", "failed", "passed"]` is not enough, because a green
run prints nothing under it either. The intent is that every test, passing or failing,
prints its fully qualified class name and its method name on one line. That is the
`"started"` event with `includeStandardStreams = false`, which is what JUnit's
`TestListener` emits for each test before it runs.

## Risks

- The aggregate adds a task to the root build that did not exist. It depends on six
  subproject tasks, so it runs after them and cannot mask a per-module failure, but it
  can fail for a reason of its own. It is wired into `check` alongside the per-module
  gates, so a failure names the aggregate and not a module.
- `-parameters` changes every produced class file. It is invisible to behaviour and to
  every test, but it changes the build output, so the first build after it lands
  recompiles everything.
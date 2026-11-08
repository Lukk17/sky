## 1. Establish the baseline before changing anything

- [ ] 1.1 Read `sky.jacoco-conventions.kts` in full and record the filter expression, the
      two thresholds and the task name it wires into `check`, so the aggregate enforces
      the same number over the same set
- [ ] 1.2 Read `sky.java-conventions.kts` in full and record the existing `JavaCompile`
      and `testLogging` configuration, so the two additions land beside what is there
      rather than replacing it
- [ ] 1.3 Run the per-module coverage gate on one module and read the XML report it
      produces, so the aggregate has a real input to merge rather than a guessed shape
- [ ] 1.4 Record which modules produce an empty measured set and which do not, so the
      aggregate skips the empty ones the way the plugin already does

## 2. Add `-parameters` and test logging to the convention plugin

- [ ] 2.1 Add `-parameters` to the `options.compilerArgs` of every `JavaCompile` task in
      `sky.java-conventions`, run one module's compile and confirm the flag is present
- [ ] 2.2 Add the test logging events so every test prints its class and method, run one
      module's test suite and confirm a passing test prints its name
- [ ] 2.3 Confirm no test broke and no source file changed, with a `git status` limited
      to `buildSrc/`

## 3. Add the aggregate JaCoCo task to the root build

- [ ] 3.1 Add a root-level task that depends on every module's coverage verification task
      and merges their XML reports into one aggregate report
- [ ] 3.2 Wire the aggregate gate into the root `check` task at the same 0.90 line and
      0.90 branch thresholds the per-module gate uses
- [ ] 3.3 Confirm the aggregate skips modules whose measured set is empty, so the
      empty counter in `sky-gateway` does not drag the number down
- [ ] 3.4 Run the full `check` from the repository root and confirm both gates report
      the same number over the same set

## 4. Verify and archive

- [ ] 4.1 Run `./gradlew build` from the repository root and verify it is green
- [ ] 4.2 Archive the change, letting the CLI rewrite the merged file
- [ ] 4.3 Verify `openspec validate --specs --strict` reports no error and that
      `openspec/changes` holds nothing but `archive`
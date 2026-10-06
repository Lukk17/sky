## Why

Every one of the eighteen merged specifications under `openspec/specs/` opens with the same placeholder, which the
archive step left behind when the capability was first created:

```text
TBD - created by archiving change <name>. Update Purpose after archive.
```

The repository rule is that documentation does not ship a placeholder, so eighteen files currently break it in the
same way. The cost is not only cosmetic. Purpose is the only part of a specification that says what the capability is
for, so without it a reader has to infer the scope from the requirement list, and two capabilities whose requirements
overlap (`database-schemas` and `db-migrations`, or `gradle-build` and `framework-version`) cannot be told apart at
all. The placeholder also names the change that created the capability, which points a reader at an archived proposal
rather than at the current contract.

## What Changes

- Replace the Purpose section of all eighteen merged specifications with one or two sentences saying what that
  capability governs and why it exists.
- Write each Purpose from the capability's own requirement list, so it describes the contract that is in force rather
  than the change that happened to introduce it.
- Name no implementation detail a Purpose would outlive. `database-schemas` is the clearest case: its requirements
  still say MySQL, the repository runs PostgreSQL, and a Purpose describing the concern (where a service's tables and
  its migration history live) stays true across that engine change instead of repeating a fact that is already wrong.

No requirement text changes, so no behaviour changes and nothing is built, tested or deployed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None at requirement level, which is why this change sets `skip_specs: true` in its `.openspec.yaml`.

A Purpose section is not a requirement and no delta verb addresses it. `openspec instructions specs` is explicit about
this: a delta for an existing capability must not carry a `## Purpose` section, because the merged specification
already has one and the delta's copy is ignored at archive time, and the documented route for changing an existing
Purpose, including a leftover placeholder, is to edit the merged specification directly. So this change carries no
delta specification, and the rewrite is a task rather than a delta.

## Impact

- Affected files: the `## Purpose` section of all eighteen `spec.md` files under `openspec/specs/`. No other line in
  any of them is touched.
- No source, chart, compose file, migration, Bruno request or OpenAPI contract is affected.
- Kept separate from `correct-stale-spec-requirements` on purpose. That change corrects two requirement blocks on
  evidence from the code, and folding eighteen prose rewrites into it would bury a correctness fix under cosmetics.
- Risk: low, and bounded by the fact that a Purpose is descriptive. It carries no MUST and nothing validates against
  it, so a poorly worded one misleads a reader without breaking a build. The guard is that each Purpose is written
  from that capability's own requirements.

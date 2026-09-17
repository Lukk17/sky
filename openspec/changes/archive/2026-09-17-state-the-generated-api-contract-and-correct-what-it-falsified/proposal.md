## Why

The three documents under `docs/api/openapi/` stopped being hand-written today. They are produced from the running
service by a Gradle task wired in a new `sky.openapi-conventions` convention plugin, and `build` depends on it in each
of the three REST services. Two merged requirements were written when those files were maintained by hand, and both
say something that is no longer true.

The first is in `api-versioning`. It pins the version declaration as `@RequestMapping(version = "1")` and gives no
reason for the literal, and it says that keeping the version in the URL left the published API documents working
unchanged. Both halves failed together. springdoc rewrites the version segment of every operation path with the
literal it finds in the mapping annotation, exactly as written, so `version = "1"` against the prefix `/api/v1`
published `/api/1/...` on every endpoint of all three documents while the services kept answering `/api/v1/...`. That
is verified here in two places rather than taken on trust. `PathVersionStrategy.updateOperationPath` in
springdoc-openapi-starter-common 3.0.3 replaces the segment at the configured index with the version string it is
handed, and `OpenApiResource.calculatePath` in springdoc-openapi-starter-webmvc-api 3.0.3 hands it
`requestMappingInfo.getVersionCondition().getVersion()`, which is the raw annotation value. The fix now landing in the
three controllers is `version = "v1"`, and it is runtime-equivalent: `SemanticApiVersionParser.parseVersion` calls
`skipNonDigits` before matching its pattern, so `v1` and `1` both parse to the same `Version`, and everything
`DefaultApiVersionStrategy` compares, the mapping condition, the configured default and the supported set, is that
parsed value rather than the text.

So the fact the specification failed to carry is not which literal to type. It is that the literal is load bearing at
all, because a published document is generated from it. A requirement that pins a literal and says nothing about why
cannot warn anybody, which is exactly what happened.

The second is in `sky-common`, in the requirement headed `One error shape, held by a last resort that is not an
advice`, merged earlier today. It says the empty-bodied 401 and 403 are what the hand-written contracts already
document. There are no hand-written contracts any more. The behaviour it describes is unchanged, so the reference has
to name what documents it now.

Nothing in any specification says the documents are generated at all, which is why both of those sentences could go
stale without anything noticing. Two merged requirements lean on the published documents and neither of them, nor any
other, says what those documents are.

## What Changes

- Add a capability, `api-contract`, holding one requirement: a published API document is produced from the service
  rather than maintained beside it, it is never edited directly, and the copy the repository keeps is the output the
  current source produces, so producing it again yields no difference. Why that belongs in a capability of its own
  rather than in `test-strategy`, `spring-boot-hygiene` or `repo-hygiene` is argued in design.md, because the question
  was asked and the answer is not obvious.
- Modify the `api-versioning` requirement so the declaration rule states the property first, that a handler declares
  the version it serves separately from the version a request asks for, and then the constraint that property now
  carries, that the declared literal must be the text the public path carries, with the reason it is load bearing. The
  sentence claiming the published documents were left working unchanged is corrected rather than deleted, because the
  clause about the frontend and the saved requests in it is still true and is still the reason the header resolver was
  rejected.
- Modify the `sky-common` requirement so the 401 and 403 clause names the shared response annotation that declares
  them, and says the published documents report that declaration rather than assert it independently.

Two things the evidence pass turned up inside the two restated blocks, both handled here.

- The `sky-common` clause covers only one of the two answers that share status 403. The empty-bodied one comes from
  the security filter chain, and a service raising an authorization failure of its own answers 403 with a problem
  detail, which sky-booking, sky-offer and sky-message all do and which the shared annotation carries in its own 403
  declaration as `application/problem+json`. A sentence saying so is added, because a reader taking the existing
  clause as the whole of 403 would read the domain answer as a second shape when it is the one shape.
- Every other sentence in both blocks was checked against the code and found true, so both are carried verbatim. The
  checks, and what each was run against, are in tasks.md.

Two things the evidence pass turned up that this change deliberately does not write into a specification, reported
instead.

- The `repo-hygiene` Purpose says the repository keeps nothing that a build or a run would regenerate anyway, and
  three tracked files now are exactly that. Its `Build outputs are gitignored` scenario also says `git status` reports
  a clean tree after a build, which is now conditional on the three documents being current. Neither is the subject of
  this change, a Purpose is not reachable from a delta at all, and both want the owner's decision rather than a guess,
  so both are raised rather than rewritten.
- The design document of the change archived this afternoon records as a live risk that the error requirement could
  drift from the three hand-written OpenAPI contracts, with no test joining them. That risk is now much smaller,
  because the contracts follow the annotations instead of being kept level by hand. It is recorded here rather than
  edited there, because an archived change is a record of what was decided then.

Nothing here changes code, a test, a chart, a compose file, a Bruno request, a published document or a module guide.

## Capabilities

### New Capabilities

- `api-contract`: where a published API document comes from, and what follows from it being produced rather than
  written.

### Modified Capabilities

- `api-versioning`: one requirement modified, in the declaration sentence and in the sentence about what the path
  version left working unchanged, with one scenario added that pins the declared literal to the published path.
- `sky-common`: one requirement modified, in one clause of its first paragraph.

## Impact

- Affected files: `openspec/specs/api-versioning/spec.md` and `openspec/specs/sky-common/spec.md`, rewritten at archive
  time from the deltas in this change, and a new `openspec/specs/api-contract/spec.md`. The Purpose of that new file is
  written by hand after the archive, because the archive step leaves a placeholder there and no delta verb addresses a
  Purpose.
- No source file, test, chart, compose file, migration, Bruno request, published document or module guide is touched.
- The three controllers are being changed to `version = "v1"` by another agent as this is written. Nothing here depends
  on that edit having landed. The requirement states which text must be declared, and the working tree at the time of
  writing still held `version = "1"` in all three, which is the state that produced the `/api/1/...` paths now sitting
  in the three documents.
- Risk: low on the two modifications, which come to one clause and two sentences against blocks checked line by line.
  Higher on the added capability, which states a property no specification has stated before. The part to read closely
  is its second paragraph, because that is where the property becomes checkable, and the check it names is a
  regeneration that yields no difference rather than anybody's judgement about whether a document looks right.

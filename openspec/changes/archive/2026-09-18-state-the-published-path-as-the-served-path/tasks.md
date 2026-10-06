## 1. Establish what commit 2c49a82 actually changed, from the files rather than from the brief

- [x] 1.1 Read `git show --stat 2c49a82` and its message, and confirm the file set is the gateway route table, the
  twelve service values files, the Bruno collection, the three e2e runbooks, four READMEs, the root `AGENTS.md`, two
  module guides and two gateway tests
- [x] 1.2 Confirm no `RewritePath` filter is left in `sky-gateway/src/main/resources/application.yaml`, in either of
  its two profile documents, and record the four route predicates each document now carries
- [x] 1.3 Confirm every API ingress across `config/k8s/helm/service/*/values.yaml`, `values-local.yaml` and
  `values-prod.yaml` carries a literal path at `pathType: Prefix` with no `rewrite-target` and no `use-regex`
- [x] 1.4 Confirm by a repository-wide grep that the only `rewrite-target` and `use-regex` annotations left in
  `config/k8s/helm` are the six swagger and api-docs pairs and the oauth2-proxy ingress's own `/oauth2` regex path
- [x] 1.5 Confirm the retired prefixes are gone rather than moved, by grepping `/offer/api`, `/booking/api` and
  `/msg/api` across the repository and finding them only in prose that describes their removal and in the gateway test
  that asserts they route nowhere
- [x] 1.6 Confirm the published path set from the controllers rather than from a guide: the mappings in
  `OfferApiController`, `BookingController` and `MessageController` under `@RequestMapping(path = "${sky.apiPrefix}")`,
  with `sky.apiPrefix` reading `/api/v1` in all three `application.yaml` files
- [x] 1.7 Confirm the gateway predicates cover that path set, including that `/api/v1/offers/**` and
  `/api/v1/owner/offers/**` match the bare collection path as well, which
  `SecurityConfigLocalProfileTest.publishedApiPath_whenNoCredentialsSupplied_thenReachesItsRouteInsteadOfBeingRejected`
  pins for all seven paths
- [x] 1.8 Confirm the ingress path set covers it too, since `pathType: Prefix` matches on segment boundaries, so
  `/api/v1/messages` covers `/messages/received`, `/messages/sent` and `/messages/{messageId}`
- [x] 1.9 Confirm the claim about the old shape from git rather than from the message: the parent commit had
  `rewrite-target: "/api/v1/$2"` on the public offer path and `rewrite-target: "/api/v1/owner/$2"` on the owner path,
  both regex and both with `use-regex`, so the two locations overlapped and only ordering separated them
- [x] 1.10 Confirm what a caller's saved request used to look like: the parent commit's
  `docs/api/request/offer/get-all-offers.yml` holds `{{baseUrl}}/offer/api/offers` and the current file holds
  `{{baseUrl}}/api/v1/offers`
- [x] 1.11 Confirm the history behind the clause being corrected: `git log -S` shows the `/api/v1/$2` rewrite target
  introduced by 94eb9b2, the same commit that introduced the `/api/v1` prefix, so the rewrite absorbed the prefix and
  the public addresses did not move then

## 2. Find every merged requirement the change could have falsified

- [x] 2.1 Grep `openspec/specs` for `/offer/api`, `/booking/api`, `/msg/api`, `rewrite`, `apiPrefix` and `stripped`,
  and read every hit, which is one paragraph of `helm-charts` matching only on the word `rewrites`
- [x] 2.2 Widen the grep to `ingress`, `/api/v1`, `gateway`, `edge`, `proxy`, `route`, `public address` and `5777`
  across all nineteen specifications, and read every hit rather than the matching line
- [x] 2.3 Read `api-versioning`, `api-contract`, `helm-charts` and `kubernetes-deployment` in full, since those four
  are where an addressing rule could live
- [x] 2.4 Read the three e2e capabilities in full, since their runbooks changed in the commit, and confirm each names
  the gateway at `http://localhost:5777` and no path, so none of them is falsified
- [x] 2.5 Check the `test-strategy` hit on `/api/v1/bookings`, which is a service-level test scenario and does not
  involve the edge
- [x] 2.6 Check the `spring-boot-hygiene` cross-origin requirement, which names the `cors-allow-origin` annotation on
  an ingress and no path, and is untouched by the commit
- [x] 2.7 Check `helm-charts` sentence by sentence against the twelve values files, confirming the six categories held
  out of defaults are still exactly six, that a path list is not one of them and names no environment, and that the
  three charts behind oauth2-proxy still keep the two auth annotations out of their defaults
- [x] 2.8 Check `api-contract` against the delta's wording, confirming its Purpose assigns addresses to
  `api-versioning`, which is what rules it out as the home for the new requirement

## 3. Check every sentence of the api-versioning block being restated

- [x] 3.1 Check the configuration and resolver claims against
  `sky-common/src/main/java/com/lukk/sky/common/web/ApiVersioningAutoConfiguration.java`: it implements
  `WebMvcConfigurer`, configures versioning in `configureApiVersioning`, calls `usePathSegment(1, predicate)`, and the
  predicate returns true only for a segment of at least two characters beginning with `v` followed by a digit
- [x] 3.2 Check the default and supported claims in the same file: `setDefaultVersion("1")` and
  `addSupportedVersions("1")`, so exactly one version is supported and it is the default
- [x] 3.3 Check the declaration claim in the three controllers: all three carry
  `@RequestMapping(path = "${sky.apiPrefix}", version = "v1")`, which is the literal the second paragraph requires
- [x] 3.4 Check the header sentence: no `useRequestHeader`, `useQueryParam` or `useMediaTypeParameter` call exists
  anywhere in the repository
- [x] 3.5 Check the second paragraph's claim about the published document against the generated files:
  `docs/api/openapi/sky-offer.openapi.yaml` carries `/api/v1/offers`, `/api/v1/offers/{offerId}/owner`,
  `/api/v1/owner/offers`, `/api/v1/owner/offers/{offerId}`, `/api/v1/owner/offers/{offerId}/photo` and `/api/v1/search`
- [x] 3.6 Check which half of the clause being corrected is true of what: `/api/v1/offers` was always what the service
  served, and the caller address `/offer/api/offers` carried no version segment, so the sentence described a direct
  call only
- [x] 3.7 Confirm the other two requirements of the capability are untouched by the commit: the plural-noun rule names
  no edge path, and the service-to-service rule's endpoint is called at `api/v1/offers/{id}/owner` built from
  `sky.offerOwnerEndpoint` in `sky-booking/src/main/resources/application.yaml`, which is a direct service call that
  never crossed an edge

## 4. Check every claim the added requirement makes

- [x] 4.1 The path equality claim, against the three sources it spans: the gateway predicates, the ingress path lists
  and the controller mappings all read `/api/v1/...` with the same segments
- [x] 4.2 The disjointness claim, by listing the top-level resources each service owns and confirming no two services
  share one: `offers`, `owner/offers` and `search` on sky-offer, `bookings` and `user/bookings` on sky-booking,
  `messages` on sky-message, `/notifyWebsocket` on sky-notify
- [x] 4.3 The swagger exception, by confirming all three services expose the springdoc UI and the api-docs on the same
  two paths, with no `springdoc.swagger-ui.path` and no `springdoc.api-docs.path` override in any of the three
  `application.yaml` files
- [x] 4.4 The header carve-out, against the gateway's non-local route table, which carries `TokenRelay=` on all four
  routes and no path filter, and against the `auth-response-headers` annotation in the service charts
- [x] 4.5 The overlap claim in the last paragraph, by string inspection of the rendered path lists rather than by
  reasoning about nginx: `/api/v1/offers` is not a prefix of `/api/v1/owner/offers` at a segment boundary or at any
  other position, and `/offer/api` was a prefix of `/offer/api/owner/offers`
- [x] 4.6 The observable scenarios against the tests that already pin three of them:
  `publishedApiPath_whenNoCredentialsSupplied_thenReachesItsRouteInsteadOfBeingRejected` covers the seven published
  paths, `retiredServicePrefix_whenRequested_thenMatchesNoRoute` covers the three retired prefixes, and
  `notifyWebsocketHandshake_whenSockJsInfoRequested_thenMatchesTheNotifyRoute` covers the passthrough route
- [x] 4.7 Record what no test in this repository pins and what therefore rests on the commit's own measurement against
  a running stack, which is the `instance` echo scenario, and keep that scenario because it is the observable form of
  a path arriving unchanged

## 5. Write and validate the delta

- [x] 5.1 Extract the merged requirement block to a scratch file and build the delta from that extract by a single
  string replacement, so every sentence not being changed is byte-identical rather than retyped
- [x] 5.2 Verify the modified requirement header matches the merged file character for character
- [x] 5.3 Verify by diff that only the one paragraph differs, and by sentence-level diff that the only change inside
  it is the intended one
- [x] 5.4 Verify the delta holds no em dash, no en dash, no semicolon, and no bold or italic outside the `**WHEN**`
  and `**THEN**` markers, matching the dashes by their UTF-8 byte sequences
- [x] 5.5 Verify the delta keeps the merged file's wrapping, which is one physical line per paragraph and per scenario
  bullet
- [x] 5.6 Run `openspec validate state-the-published-path-as-the-served-path --strict` and verify it reports no error

## 6. Archive and verify the merged result

- [x] 6.1 Archive the change and let the CLI perform the merged-file rewrite
- [x] 6.2 Verify `openspec validate --specs --strict` reports no error across all nineteen specifications, and that
  `openspec/changes` holds nothing but `archive`
- [x] 6.3 Verify `git status` shows no file changed outside `openspec/`, and read the diff of the merged specification
  line by line to confirm nothing outside the intended edits moved

## 7. Notes from the run

- The brief said the root `AGENTS.md` still describes the prefix as stripped at the ingress and asked for that to be
  reported rather than fixed. It is already fixed. Commit 2c49a82 rewrote that bullet itself, and the working tree
  holds the corrected text, so there is nothing to route.
- The brief also suggested `helm-charts` was the likeliest home for anything describing the rewrite arrangement.
  Nothing in it describes the arrangement at all. Its only match on the grep is the word `rewrites` inside a sentence
  about a `helm upgrade` overwriting a live release with a placeholder.
- Nothing was committed, no build was run, and no file outside `openspec/` was written.

## 1. Versioning config

- [ ] 1.1 Decide resolver: header `X-API-Version` primary, optional media-type fallback. Document in `sky-common`.
- [ ] 1.2 Add `ApiVersionConfig` (a `WebMvcConfigurer`) in each REST service. Could live in `sky-common` if the config is identical across services.
- [ ] 1.3 Set default version `"1"`, supported versions `["1"]`.
- [ ] 1.4 Verify springdoc-openapi 3.x renders version info in `/swagger-ui` and the OpenAPI JSON.

## 2. Annotate controllers

- [ ] 2.1 sky-booking: add `version = "1"` to `BookingController` class-level `@RequestMapping`.
- [ ] 2.2 sky-offer: add to `OfferApiController` and `OfferInternalController`.
- [ ] 2.3 sky-message: add to `MessageController`.

## 3. Path standardization

- [ ] 3.1 sky-offer:
  - `POST /api/owner/offer` → `POST /api/owner/offers`
  - `PUT /api/owner/offer` → `PUT /api/owner/offers/{id}` (move id into path while at it) — discuss in apply.
  - `DELETE /api/owner/offer/{offerId}` → `DELETE /api/owner/offers/{offerId}`
- [ ] 3.2 sky-message:
  - `POST /api/message` → `POST /api/messages`
  - `DELETE /api/message/{messageId}` → `DELETE /api/messages/{messageId}`
- [ ] 3.3 Verify `/api/home`, `/api/` hello endpoints — keep or remove. If keeping, version them too.
- [ ] 3.4 `POST /api/search` (sky-offer) — discuss in apply whether to convert to `GET /api/offers?q=...` (RESTful) or leave as-is. Default: leave; mark as a future v2 candidate.

## 4. Internal route alignment

- [ ] 4.1 `OfferInternalController` route, after `hexagonal-enforcement-archunit`, sits at `/api/internal/owner/offer/{id}`. Update to `/api/internal/owner/offers/{id}` for consistency.
- [ ] 4.2 Update `OfferRestClient` in sky-booking accordingly.
- [ ] 4.3 Update sky-booking integration test mockwebserver path expectations.

## 5. Tests

- [ ] 5.1 Every controller test calls the new path with `X-API-Version: 1` header.
- [ ] 5.2 Add a negative test: no version header → still works because default is "1".
- [ ] 5.3 Add a negative test: unsupported version (e.g., "2") → 400 or `NotAcceptableApiVersionException` per Spring Framework 7's default.

## 6. Documentation & artifacts

- [ ] 6.1 Update `config/postman-collection/sky.postman_collection.json` paths and add `X-API-Version: 1` header in collection-level defaults.
- [ ] 6.2 Update root `README.md` API section (if any) and `AGENTS.md` ## Architecture.
- [ ] 6.3 Note breaking change for sky-view frontend; coordinate.

## 7. Verify

- [ ] 7.1 `./gradlew test` green.
- [ ] 7.2 Newman against the updated collection — green.
- [ ] 7.3 Smoke locally: hit each new path with `X-API-Version: 1` — works. Hit old singular path — 404.
- [ ] 7.4 Swagger UI shows v1 grouping.

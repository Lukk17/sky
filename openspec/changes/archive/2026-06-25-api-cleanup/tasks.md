## 1. Versioning config

- [x] 1.1 Decide resolver: header `X-API-Version` primary, optional media-type fallback. Document in `sky-common`.
- [x] 1.2 Add `ApiVersionConfig` (a `WebMvcConfigurer`) in each REST service. Could live in `sky-common` if the config is identical across services.
- [x] 1.3 Set default version `"1"`, supported versions `["1"]`.
- [x] 1.4 Verify springdoc-openapi 3.x renders version info in `/swagger-ui` and the OpenAPI JSON.

## 2. Annotate controllers

- [x] 2.1 sky-booking: add `version = "1"` to `BookingController` class-level `@RequestMapping`.
- [x] 2.2 sky-offer: add to `OfferApiController` and `OfferInternalController`.
- [x] 2.3 sky-message: add to `MessageController`.

## 3. Path standardization

- [x] 3.1 sky-offer:
  - `POST /api/owner/offer` → `POST /api/owner/offers`
  - `PUT /api/owner/offer` → `PUT /api/owner/offers/{id}` (move id into path while at it) — discuss in apply.
  - `DELETE /api/owner/offer/{offerId}` → `DELETE /api/owner/offers/{offerId}`
- [x] 3.2 sky-message:
  - `POST /api/message` → `POST /api/messages`
  - `DELETE /api/message/{messageId}` → `DELETE /api/messages/{messageId}`
- [x] 3.3 Verify `/api/home`, `/api/` hello endpoints — keep or remove. If keeping, version them too.
- [x] 3.4 `POST /api/search` (sky-offer) — discuss in apply whether to convert to `GET /api/offers?q=...` (RESTful) or leave as-is. Default: leave; mark as a future v2 candidate.

## 4. Internal route alignment

- [x] 4.1 `OfferInternalController` route, after `hexagonal-enforcement-archunit`, sits at `/api/internal/owner/offer/{id}`. Update to `/api/internal/owner/offers/{id}` for consistency.
- [x] 4.2 Update `OfferRestClient` in sky-booking accordingly.
- [x] 4.3 Update sky-booking integration test mockwebserver path expectations.

## 5. Tests

- [x] 5.1 Every controller test calls the new path with `X-API-Version: 1` header.
- [x] 5.2 Add a negative test: no version header → still works because default is "1".
- [x] 5.3 Add a negative test: unsupported version (e.g., "2") → 400 or `NotAcceptableApiVersionException` per Spring Framework 7's default.

## 6. Documentation & artifacts

- [x] 6.1 Update the Bruno collection paths at `docs/api/request` and add `X-API-Version: 1` header in collection-level defaults.
- [x] 6.2 Update root `README.md` API section (if any) and `AGENTS.md` ## Architecture.
- [x] 6.3 Note breaking change for sky-view frontend; coordinate.

## 7. Verify

- [x] 7.1 `./gradlew test` green.
- [x] 7.2 Bruno run against the updated collection — green.
- [x] 7.3 Smoke locally: hit each new path with `X-API-Version: 1` — works. Hit old singular path — 404.
- [x] 7.4 Swagger UI shows v1 grouping.

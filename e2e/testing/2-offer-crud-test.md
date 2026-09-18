# offer-crud: e2e test

## What this verifies

- POST `/api/v1/owner/offers` returns HTTP 201 with a server-assigned UUID `id`, `ownerEmail` equal to
  `lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `price`, and `roomCapacity` echoed.
- GET `/api/v1/offers` (public) returns HTTP 200 with a paginated body (`content` array and numeric
  `totalElements`).
- POST `/api/v1/search` with the canary token returns HTTP 200 and the created offer appears in `content`
  (retrieval by `hotelName` LIKE).
- GET `/api/v1/owner/offers` returns HTTP 200 and the created offer appears in the owner's page.
- GET `/api/v1/offers/{id}/owner` returns HTTP 200 with body `lukk@sky.dev`.
- POST `/api/v1/owner/offers/{id}/photo` (multipart) returns HTTP 200 with a UUID `id` and a non-empty
  `photoUrl` presigned URL pointing at the `sky-offers` bucket (persisted state in the object store, plus the
  server-owned `photo_object_key` column of `public.offer`), and fetching that presigned URL returns HTTP 200 with
  the canary marker `SKY-OFFER-PHOTO-CANARY-4471` in the stored bytes, so the object in the bucket is this fixture
  rather than any other image. No response carries `photoPath`, the field a client once used to overwrite the key.
- PUT `/api/v1/owner/offers` returns HTTP 200, a follow-up read reflects the edited `hotelName` and `price`, and
  refetching `photoUrl` still returns the canary bytes, so the edit left the stored object where it was.
- POST on the same photo path again replaces the photo: HTTP 200 with a new `photoUrl` that fetches the canary, and
  the address of the object it replaced answers 404.
- DELETE `/api/v1/owner/offers/{id}/photo` returns HTTP 204 and the address it cleared answers 404, so the object
  left the bucket and not just the column.
- A final POST on the photo path restores a photo, so the teardown below has one left to take with it.
- DELETE `/api/v1/owner/offers/{id}` returns HTTP 204, the offer no longer appears in the owner's page
  (persisted-state removal from `public.offer`), and the photo it was still holding answers 404, so deleting the
  offer deletes its object too.

## Prerequisites

Three checks: the Bruno CLI is installed, the gateway is reachable, and the object store answers on the host under
the same hostname the presigned photo URL carries.

Check the Bruno CLI is installed.

```bash
bru --version
```

Expect a version number and exit code 0.

Check the gateway is reachable.

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:5777/actuator/health
```

Expect `200`.

Check the object store answers on the host under the hostname the presigned URL carries. The upload request
fetches the presigned `photoUrl` back and asserts the canary marker is in the stored bytes, so that URL has to
resolve from here as well as from inside the Docker network. `sky-offer` signs it against
`S3_PRESIGN_ENDPOINT`, and the compose stack leaves that empty so it falls back to `S3_ENDPOINT`,
`http://s3.localhost:9070`. One hostname is enough here because `.localhost` resolves to `127.0.0.1` on the host and
an `extra_hosts` entry maps the same name to the host gateway inside the network. A cluster needs the two addresses
set separately, see [config/k8s/helm/helm_README.md](../../config/k8s/helm/helm_README.md).

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://s3.localhost:9070/
```

Expect `200`. Anything else means the store is down or the hostname does not resolve, and the photo round-trip
assertions fail with `presigned URL not fetchable: <reason>` rather than with a wrong status.

## Reset state

None. The run below creates its own data and deletes it in the cleanup requests, so it is self-cleaning and
re-runnable.

## Run

One step: a single Bruno invocation from the collection directory.

```bash
cd docs/api/request && bru run auth offer cleanup/delete-offer.yml --env local --insecure
```

The auth folder mints the token, the offer folder runs the create/read/assert requests and the whole photo lifecycle
(upload, edit, replace, delete, restore) with IDs chained automatically by the collection's scripts, and the cleanup
request deletes what was created. Bruno evaluates every assertion in each request.

## Expected

The run summary reports Status PASS with all requests passed and all assertions passed. The assertion total is
deliberately not stated here: it moves whenever the collection grows, so the runner records the total it actually saw
in the run file instead.

The assertion groups cover: the created offer returns HTTP 201 with a UUID `id`, `ownerEmail` equal to
`lukk@sky.dev`, and the submitted `hotelName`, `city`, `country`, `price`, and `roomCapacity`; the public offer list
returns HTTP 200 with a `content` array and a numeric `totalElements`; the search returns the offer by `hotelName`
LIKE; the owner page returns the offer (persisted to Postgres `public.offer`); the owner lookup returns HTTP 200 with
body `lukk@sky.dev`; the photo upload returns HTTP 200 with a UUID `id` and a non-empty presigned `photoUrl`
referencing the `sky-offers` bucket, with the object key in the server-owned `offer.photo_object_key` and absent from
every response body, and a follow-up fetch of that URL returns HTTP 200 with the canary marker in the stored bytes;
the edit returns HTTP 200, a follow-up read reflects the renamed `hotelName` and reprice, and the photo still fetches
the canary; the photo replace returns a new `photoUrl` that fetches the canary while the replaced address answers
404; the photo delete returns HTTP 204 and its address answers 404; the restore upload returns HTTP 200; and the
cleanup delete returns HTTP 204 (persisted-state removal from `public.offer`, and the object it held answers 404,
because the offer delete removes the stored photo with the row).

## Fixtures

- `e2e/fixtures/offer-photo.png`: a 400x200 PNG carrying the canary marker `SKY-OFFER-PHOTO-CANARY-4471` in a
  `tEXt` chunk (keyword `Comment`) placed directly after `IHDR`. The chunk leaves the 8-byte PNG signature and the
  image data untouched, so the upload endpoint, which sniffs magic bytes with
  `URLConnection.guessContentTypeFromStream` rather than trusting the filename, still detects `image/png` and
  accepts it. The marker is what makes the photo assertion specific: a presigned `photoUrl` comes back for any
  image, whereas the marker names this one file. `docs/api/request/offer/upload-photo.yml` posts this file itself,
  through the relative path `../../../e2e/fixtures/offer-photo.png`, which Bruno resolves against the collection
  root and allows to leave it, so the repository holds one copy of the image and there is no drift to guard
  against.

## Concurrency

- Mutates: `public.offer` in the `sky` database, rows owned by `lukk@sky.dev`; object-store bucket `sky-offers`,
  under the `offers/{offerId}/` prefix, where the run creates three objects and deletes all three, so it leaves the
  bucket as it found it.
- Conflicts with: `3-booking-flow-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `public.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.

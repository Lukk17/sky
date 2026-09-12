# booking-flow: e2e test

## What this verifies

- POST `/offer/api/owner/offers` returns HTTP 201 with a server-assigned UUID `id` and `ownerEmail` equal to
  `lukk@sky.dev` (the offer this flow books against).
- The offer folder's whole photo lifecycle passes as part of this run, because the run includes that folder. The
  detail belongs to `2-offer-crud-test.md` and is not restated here.
- POST `/booking/api/bookings` with `{offerId, dateToBook}` returns HTTP 201 with a server-assigned UUID `id`,
  the booked `offerId`, a `bookedDate`, and `bookingUser` equal to `lukk@sky.dev`.
- GET `/booking/api/user/bookings` returns HTTP 200 and the created booking appears in the user's page (persisted
  state in Postgres `public.booking`).
- GET `/offer/api/offers/{offerId}/owner` returns HTTP 200 with body `lukk@sky.dev` (the owner sky-booking resolves
  internally).
- DELETE `/booking/api/bookings/{bookingId}` returns HTTP 204 and the booking no longer appears in the user's page
  (persisted-state removal).
- DELETE `/offer/api/owner/offers/{offerId}` returns HTTP 204 (cleanup of the seeded offer).

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
cd docs/api/request && bru run auth offer booking cleanup/delete-booking.yml cleanup/delete-offer.yml --env local --insecure
```

The auth folder mints the token, the offer and booking folders run the create/read/assert requests with IDs chained
automatically by the collection's scripts, and the cleanup requests delete the booking and then the seeded offer.
Bruno evaluates every assertion in each request.

## Expected

The run summary reports Status PASS with all requests passed and all assertions passed. The assertion total is
deliberately not stated here: it moves whenever the collection grows, so the runner records the total it actually saw
in the run file instead.

The assertion groups cover: the seeded offer returns HTTP 201 with a UUID `id` and `ownerEmail` equal to
`lukk@sky.dev`; the offer folder carries the whole photo lifecycle, so its assertions are part of this run too
(`photoUrl` contains `X-Amz-Algorithm`, fetching it returns HTTP 200 with the canary marker in the stored bytes, the
edit leaves the object alone, and each replaced or deleted object's address answers 404); the booking returns HTTP 201
with a UUID `id`, the booked `offerId`, a `bookedDate` carrying `2027-08-15`, and
`bookingUser` equal to `lukk@sky.dev` (and a Kafka event consumed by sky-notify); the user bookings page returns the
booking (persisted to Postgres `public.booking`); the owner lookup returns HTTP 200 with body `lukk@sky.dev`; and the
two cleanup deletes return HTTP 204, removing the booking from Postgres `public.booking` and the seeded offer from
Postgres `public.offer`.

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

- Mutates: Postgres `public.booking`, rows where `booking_user = lukk@sky.dev`; Postgres `public.offer`, the seeded
  canary offer owned by `lukk@sky.dev` (created and deleted here); object-store bucket `sky-offers`, under the
  `offers/{offerId}/` prefix, where the photo lifecycle creates three objects and deletes all three, so the run
  leaves the bucket as it found it; Kafka, booking and offer events this flow produces and sky-notify consumes.
- Conflicts with: `2-offer-crud-test.md`, which also creates and deletes offers owned by `lukk@sky.dev` in
  `public.offer`. The overlapping Mutates already forces serialisation; this names it for clarity.
- Serial: false.

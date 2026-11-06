# SkyView

Front-end for the Sky web application (Angular 22).

-----------

## Prerequisites

Node `^22.22.3`, `^24.15.0` or `>=26.0.0` (local development runs Node 26), then install from this directory:

```powershell
npm ci
```

```sh
npm ci
```

-----------

## Run the dev server

All serves listen on `http://localhost:4200/`. Pick the configuration that matches the backend under test:

| Command | Environment file | Backend | Auth |
|---|---|---|---|
| `npm start` | `src/environments/environment.ts` | deployed host | Keycloak bearer token |
| `npm run localDev` | `src/environments/environment.localDev.ts` | `localhost:5777` gateway | session cookie after gateway login |

```powershell
npm start
```

```powershell
npm run localDev
```

`npm start` is plain `ng serve`. `npm run localDev` is `ng serve --configuration=localDev --hmr`, so it also enables hot module reload. The same three launches exist as IDE run configurations under `.run/` (`ng serve`, `ng serve HotReload`, `localDev`); open the repository root in the IDE so the `$PROJECT_DIR$`-relative `package.json` path resolves.

The default environment still calls the retired `/offer/api`, `/booking/api` and `/msg/api` prefixes, so only `localDev` against locally running services returns data until the gateway alignment change lands.

### Auth (BFF session)

Login goes through the gateway (`/oauth2/authorization/keycloak`), not Keycloak directly. The header shows login state from `GET /api/session`; a 401 on any other API call redirects to the `/auth` screen, which starts gateway login and returns to the saved path afterwards. The header logout button POSTs to gateway `/logout` and returns to `/home`. No access, ID, or refresh token is stored in the browser; the session lives in an HttpOnly gateway cookie and mutating calls carry the `X-XSRF-TOKEN` header from `/api/session` automatically. Anonymous screens (offer list, search, `/home`) work without login; booking, messaging, and owner screens require a session.

----------

## Generated API client

The REST client in `src/app/api-generated` is generated from the shared contract at [docs/api/contract/openapi.yaml](../../../docs/api/contract/openapi.yaml) with OpenAPI Generator (generator `typescript-angular`, package name `sky-api-client`). It is committed, and [config/scripts/check-client-freshness.sh](../../../config/scripts/check-client-freshness.sh) regenerates it and fails when the committed copy differs.

Pagination is typed: the services return `OfferPage`, `BookingPage` and `MessagePage` instead of a generic page. Search is `GET /api/v1/search` with a required `term` query parameter. Authentication and session handling are outside the contract: login, the gateway session cookie and CSRF are handled by the application's auth layer, not by the generated client.

----------

## Compile prod app

```powershell
npm run build -- --configuration=production
```

```sh
npm run build -- --configuration=production
```

The bundle lands in `dist/sky-view`. The `kaios` and `op_mini` browserslist warnings during the build are benign and the output is unaffected.

----------

## Lint

ESLint with angular-eslint:

```powershell
npm run lint
```

```sh
npm run lint
```

----------

## Unit tests

Karma plus Jasmine, headless Chrome:

```powershell
npm test -- --watch=false
```

```sh
npm test -- --watch=false
```

Specs live beside the sources as `*.spec.ts`, the entry is `src/test.ts`, configuration in `src/karma.conf.js`. Plain `npm test` keeps watching.

----------

## End-to-end tests

Playwright. The command starts its own dev server on port 4200, so no manual serve is needed:

```powershell
npm run e2e
```

```sh
npm run e2e
```

Specs live in `e2e/`, configuration in `playwright.config.ts`. First run on a machine downloads the Chromium headless shell via:

```powershell
npx playwright install chromium
```

----------

## Updating dependencies

Dependency versions can be found on:  
https://www.npmjs.com/

When updating keep in mind that before each version of package you can insert symbol:

| Symbol | Description                                                                                                                                            |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| ^      | The project will work with any version of the dependency that is greater than or equal to the specified version.                                       |
| ~      | The project will work with any version of the dependency that is greater than or equal to the specified version, but less than the next major version. |

Safest way is to use just a version number. But then to update you need to manually change versions in `package.json`

Updating each dependency to latest:

```shell
npm update @angular/core@latest
```

To update all, change its versions in `package.json` and run
```shell
npm install
```
sometimes force install is required:
```shell
npm install --force
```

To automatically update packages (with ~ or ^ versions) and change it version in `package.json`
```shell
npm update --save
```

Angular majors move together: `@angular/*`, `@angular-devkit/build-angular` and `@angular/cli` must share the major, with the TypeScript, `zone.js`, `rxjs` and `@types/node` ranges that major requires. Major upgrades go one major at a time via `ng update`.

---------

## Adding new environment

in `angular.json` under  
`projects.sky-view.architect.build.configurations`  
add

```
"localDev": {
  "fileReplacements": [
    {
      "replace": "src/environments/environment.ts",
      "with": "src/environments/environment.localDev.ts"
    }
  ]
}
```

and under  
`projects.sky-view.architect.serve.configurations`  
add
```
"localDev": {
  "buildTarget": "sky-view:build:localDev"
}
```

in `package.json` under `scripts` add:
```
"localDev": "ng serve --configuration=localDev --hmr"
```

---------

## Seed data

The backend seed lives in [../../../config/seed/](../../../config/seed/) under config/. It creates the 4 demo users, 12 offers, 21 bookings, 24 messages, and one photo per owned offer. Compose or k3d must be running first so Keycloak and the edge answer.

```powershell
$env:TLS_INSECURE=1; node ../../../config/seed/seed.mjs
```

```sh
TLS_INSECURE=1 node ../../../config/seed/seed.mjs
```

---------

## Troubleshooter

`X [ERROR] Could not resolve "zone.js/dist/zone-error"`  
that import was removed with `zone.js` 0.15. Delete the `zone.js/dist/zone-error` import from the environment file instead of reinstalling anything.

`ERROR Error: NG0301`  
Clear cache:
```shell
rm .angular/cache
```

Port 4200 already in use (dev server or a previous `npm run e2e` webServer still holds it)  
stop the other server or serve on another port:
```shell
npx ng serve --port 4300
```

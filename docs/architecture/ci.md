# CI and releases (`.github/workflows`)

Workflows, signing and reproducible builds. Moved from CLAUDE.md.

- `checks.yml` (every push to `main`, every pull request): `./gradlew allTests`, uploads test
  reports on failure.
- `nightly.yml` (daily 02:00 UTC, skipped without new commits): signed nightly APKs for FOSS and
  Play, web app deployed to `grocy-nightly.patrickzedler.com` (build id = commit SHA, see
  `docs/architecture/updates.md`; GitHub Pages only keeps `deploy/github-pages`, which points the
  former address there and retires the worker installed at it), a reproducibility check that
  builds the unsigned FOSS release twice and compares bytes, then replaces the `nightly` GitHub release.
- `release.yml` (tag `v*`, must equal `v` + `grocy.versionName`): job `web` builds the web app
  (build id = tag commit, no version suffix); job `release` runs `allTests`, then signed FOSS APK,
  `Grocy-web-<tag>.zip` (without source maps) and SHA256SUMS on the GitHub release, Play and Wear
  AABs as workflow artifacts; job `docker` then pushes `ghcr.io/<owner>/grocy-kmp-web` (`1.2.3`,
  `1.2`, `latest`; pre-releases only by version) for amd64 and arm64; job `pages` deploys it to
  `grocy.patrickzedler.com`.
- Web hosting: Cloudflare Pages, projects `grocy-web` (release) and `grocy-web-nightly`, each with
  its custom domain; two origins, so release and nightly never share logins, settings or the
  database (`patzly.github.io` was shared with every other Pages site of the account, which could
  read the API keys in `localStorage`). `apps/web/deploy/cloudflare-pages.sh` (secrets
  `CLOUDFLARE_API_TOKEN` with Pages: Edit, `CLOUDFLARE_ACCOUNT_ID`) drops the source maps, adds
  `404.html` (without it Pages answers missing files with the app) and the `_headers` of
  `cloudflare-headers.sh` (the headers of `nginx.conf`; Pages joins a header two rules set, so
  the content-hashed files are listed one by one), deploys with wrangler and checks the
  deployment with `check-site.sh`. Pages redirects `index.html` to the directory: `sw.js` stores
  responses unredirected, browsers refuse redirected ones for navigations.
- Web image (`apps/web/docker`): the distribution in `nginxinc/nginx-unprivileged` (port 8080,
  pinned by digest, Dependabot updates it). `nginx.conf`: gzip, `no-cache` for everything except
  the content-hashed `<20 hex>.wasm/.js` (a year, immutable), the manifest's type, `nosniff`,
  `no-referrer`, `frame-ancestors 'none'`; no CSP beyond that, the app connects to any Grocy
  server. `apps/web/.dockerignore` sends only the two inputs. URLs use a hash
  (`docs/architecture/web-navigation.md`), so no fallback to `index.html` is needed. `apps/web/docker/check.sh`
  (in `nightly.yml` and `release.yml`; locally `ENGINE=podman`) builds and runs the image after the
  distribution and checks it with `apps/web/deploy/check-site.sh`: page, Wasm type, caching,
  gzip, manifest type, missing source maps.
- Signing reads `RELEASE_*` / `NIGHTLY_*` env vars (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`); without them builds are unsigned, never debug-signed.
- Reproducible FOSS builds matter (F-Droid/IzzyOnDroid): `vcsInfo` and the dependency info block
  are disabled. Do not add build inputs that vary per machine, path or time.
- Actions are pinned by commit SHA; Dependabot updates them weekly.

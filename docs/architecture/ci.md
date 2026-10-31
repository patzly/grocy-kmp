# CI and releases (`.github/workflows`)

Workflows, signing and reproducible builds. Moved from CLAUDE.md.

- `checks.yml` (every push to `main`, every pull request): `./gradlew allTests`, uploads test
  reports on failure.
- `nightly.yml` (daily 02:00 UTC, skipped without new commits): signed nightly APKs for FOSS and
  Play, web app deployed to GitHub Pages (build id = commit SHA, see `docs/architecture/updates.md`), a reproducibility check that builds the unsigned FOSS
  release twice and compares bytes, then replaces the `nightly` GitHub release.
- `release.yml` (tag `v*`, must equal `v` + `grocy.versionName`): runs `allTests`, then signed
  FOSS APK + SHA256SUMS on the GitHub release, Play and Wear AABs as workflow artifacts.
- Signing reads `RELEASE_*` / `NIGHTLY_*` env vars (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD`); without them builds are unsigned, never debug-signed.
- Reproducible FOSS builds matter (F-Droid/IzzyOnDroid): `vcsInfo` and the dependency info block
  are disabled. Do not add build inputs that vary per machine, path or time.
- Actions are pinned by commit SHA; Dependabot updates them weekly.

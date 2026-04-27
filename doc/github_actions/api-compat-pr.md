# API Compatibility PR Workflow

This document explains the purpose and behavior of `.github/workflows/api-compat-pr.yml`.

## Purpose

The workflow checks public API compatibility for toolkit modules in pull requests.

It does this by:
- discovering which Gradle projects expose `apiCheck`
- filtering out non-releasable/app-style modules
- running one `apiCheck` per module in parallel
- uploading discovery logs and per-module reports

This helps catch public API changes that are not reflected in checked-in `.api` files.

## When It Runs

It runs on `pull_request` for these events:
- `opened`
- `synchronize`
- `reopened`
- `ready_for_review`

It skips draft PRs (`if: github.event.pull_request.draft == false`).

## High-Level Flow

1. `discover` job
   - Runs `./gradlew --dry-run apiCheck --console=plain --no-daemon`
   - Parses task paths like `:module:apiCheck` and `:toolkit:core:apiCheck`
   - Validates candidate modules against real Gradle project paths from `./gradlew -q projects`
   - Excludes app-style modules (`*-app`) and known non-publishable modules
   - Emits JSON output (`modules_json`) for matrix execution

2. `run-api-check` job
   - Builds a dynamic matrix from `modules_json`
   - Runs `./gradlew :<module>:apiCheck --stacktrace --no-daemon`
   - Uses `fail-fast: false` so all module results are visible
   - Uploads module API/build reports as artifacts

## Why There Is a Fallback

If dry-run discovery finds no `apiCheck` tasks, fallback scanning checks `toolkit/*/api/*.api`.

Important safety behavior:
- fallback candidates are resolved against real Gradle project paths
- unresolved or ambiguous candidates are excluded
- this avoids matrix entries that would fail with `Project not found`

A warning is emitted when fallback is used.

## Security Notes (Public Repo)

- The workflow uses `ubuntu-latest` (GitHub-hosted runners).
- Permissions are minimal: `contents: read`.

For public repos, this is safer than running untrusted PR code on self-hosted runners.

## Concurrency Behavior

`concurrency` is enabled with:
- `group: api-compat-pr-${{ github.event.pull_request.number || github.ref }}`
- `cancel-in-progress: true`

This cancels older runs for the same PR when new commits are pushed.

## Artifacts

- `api-compat-discovery-log`: dry-run discovery log
- `api-compat-<module>`: per-module API and report outputs

## Local Verification

From repo root:

```zsh
ruby -e "require 'yaml'; YAML.load_file('.github/workflows/api-compat-pr.yml'); puts 'YAML parse OK'"
./gradlew --dry-run apiCheck --console=plain --no-daemon
./gradlew :authentication:apiCheck --stacktrace --no-daemon
```

## Troubleshooting

- `modules_json` is `[]`
  - Check the discovery log artifact
  - Confirm modules actually expose `apiCheck`
  - Verify fallback warning and excluded reasons in job summary

- `Project not found` errors
  - Should be prevented by current project-path resolution logic
  - If seen, inspect `discover` summary for unresolved candidate mapping

- Frequent fallback usage
  - Indicates dry-run parsing did not find expected `apiCheck` task lines
  - Review Gradle output format and regex assumptions in `discover`


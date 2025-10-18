# Triage Plan: Issue #1582 — Don’t add lockfiles to samples

- Link: https://github.com/elide-dev/elide/issues/1582
- Status: Open
- Priority: Medium-High (developer DX, cross-platform stability)
- Confidence: High (well-scoped change in CLI sample packaging/templates)

## Context
Sample projects embedded in the CLI currently include lockfiles (e.g., `package-lock.kdl`, possibly other ecosystem lockfiles). These can break installs or have encoding issues across environments, as noted in the issue. We should avoid shipping lockfiles inside sample templates.

## Proposed Solution
1. Remove explicit lockfiles from sample template sources:
   - Delete `package-lock.kdl` from:
     - packages/cli/src/projects/ktweb/
     - packages/cli/src/projects/web-static-worker/
   - Ensure no other lockfiles are present in other sample directories.

2. Update the sample manifest (used by the CLI) so it no longer references lockfiles:
   - Edit: packages/cli/src/projects/samples.json
   - For the `web-static-worker` entry, remove `"package-lock.kdl"` from `files`.

3. Harden sample packaging to exclude common lockfiles so future additions don’t slip in:
   - Edit: packages/cli/build.gradle.kts — in the `Zip` tasks that pack samples (around packSample* and `from(path) { ... }`), add excludes for:
     - `package-lock.json`, `pnpm-lock.yaml`, `yarn.lock`, `package-lock.kdl`
     - `Cargo.lock`, `poetry.lock`, `Pipfile.lock`, `Gemfile.lock`
     - Gradle dependency locks: `**/gradle/dependency-locks/**`
     - Any other known ecosystem lockfiles if applicable.

4. Keep default .gitignore allowing Elide’s own lock (`.dev/elide.lock*`) in user projects; this is separate from template lockfiles and should not ship pre-populated in samples.

## Acceptance Criteria
- Running `elide init <template>` for web-related samples produces no lockfiles in the new project tree.
- Packaged resources under `META-INF/elide/samples/` (zips or files) do not contain lockfiles.
- All existing CLI tests pass; if tests reference removed files, update them accordingly.

## Verification Plan
- Build the CLI and repack samples:
  - gradle task path: packages/cli:processResources (depends on `packSamples`)
- Manually inspect the produced jar’s `META-INF/elide/samples/` content to confirm no lockfiles.
- Create projects locally:
  - `elide init ktweb` and `elide init web-static-worker`
  - Confirm no lockfiles present.
  - Run `elide install` and `elide build` to ensure smooth install without missing lockfile assumptions.

## Risk / Considerations
- Some samples previously relied on `package-lock.kdl` to pin versions; removing it means the first `elide install` will resolve fresh. This aligns with the issue intent and should be acceptable.
- Ensure no code path expects the lockfile to exist on first run.

## Implementation Checklist
- [ ] Remove `packages/cli/src/projects/ktweb/package-lock.kdl`
- [ ] Remove `packages/cli/src/projects/web-static-worker/package-lock.kdl`
- [ ] Update `packages/cli/src/projects/samples.json` to remove the lockfile from `web-static-worker/files`
- [ ] Update `packages/cli/build.gradle.kts` sample `Zip` excludes to cover common lockfiles
- [ ] Build and verify resources
- [ ] Run targeted CLI tests and sample init smoke tests

## Why this issue
- Importance: Improves out-of-the-box experience and reduces cross-platform breakage.
- Difficulty: Low-to-moderate; limited to CLI templates and packaging logic.
- Confidence: High; clearly defined steps, limited blast radius, straightforward verification.


---
type: "agent_requested"
description: "ALWAYS LOAD THESE RULES WHILE DEVELOPING ON ELIDE REPOS"
---

### Testing guidance for Elide
- Prefer targeted JVM tests with native tasks excluded for fast iteration.
- If a test fails, open `build/reports/tests/**/index.html` for details; cite the failing assertion.
- Use `--rerun-tasks` to bypass up‑to‑date.
- Widen scope only after the smallest test is green: single test → module tests → make test → make check.

---

### Known Elide pitfalls (CI and platform)
- Classpath resource casing (Linux CI is case‑sensitive). Ensure resource paths match exact case (e.g., `elide:project.pkl`).
- Native/sqlite friction: full or CLI tests can pull sqlite native libs; prefer targeted JVM tests first.
- Branch names: avoid parentheses/spaces or other shell‑sensitive characters.
- After schema/Pkl changes, ensure generated resources appear under `build/resources/**` and are readable at runtime.

---

### Elide patterns and conventions

Pkl union Listing default (ensures `new {}` amends intended branch):
```pkl
packages: Listing<MavenPackageDependency> = new Listing<MavenPackageDependency> {
  default = (index) -> new MavenPackageSpec {}
}
```

Built‑in Pkl module path (lowercase to match classpath resource):
```kotlin
// In ElidePackageManifestCodec.kt
// rewrite first line in built‑in module to: amends "elide:project.pkl"
```

Commit hygiene for API changes:
- Separate commit titled `chore(api): update ABI pins` for the `apiDump` output.
- Functional changes should avoid formatting churn; run formatting separately if CI requires.

Per‑issue workflow (team preference):
- Create a tasklist; start with “Investigate/Triage/Understand the problem”.
- Implement a focused test that reproduces the issue; land minimal fix.
- Open PR; then branch from `main` for the next issue.

---

### Preferred modules and paths (orientation)
- Tooling related Pkl/manifest code: `packages/tooling/**`.
- Builder integration points: `packages/builder/**`.
- GraalVM/native tasks: `packages/graalvm/**` (exclude during fast loops as shown above).

---

### Elide‑specific escalation heuristics
Ask for senior guidance (Dario/Nat) when:
- The first sanity run fails in a non‑obvious way or indicates architectural constraints.
- You suspect classpath/resource casing or Gradle task wiring issues.
- You need confirmation on ownership between `tooling`, `builder`, and `runtime` modules.

Provide:
- Problem statement and hypothesis; failing test name and command.
- Key file paths and lines touched; stacktrace excerpt.
- Two concrete next‑step options and a preferred choice.

---

### Definition of Done on Elide
- A targeted test (or tests) reproduces the issue and now passes locally with exit code 0.
- Minimal diff fix; ABI pins updated separately if public API changed.
- Commands used for verification are listed in the PR description.
- Any resource casing risks or native interactions are called out explicitly.


---

### Linting and Formatting (Detekt/Spotless)
- For fast inner loops, exclude detekt and spotless tasks; focus on logic and tests.
- Before opening a PR (or when CI requires), run formatter and linter separately.

Quick excludes in fast loops:
```sh
./gradlew :packages:tooling:test --tests 'pkg.ClassTest' --rerun-tasks \
  -x detekt -x spotlessApply -x spotlessCheck --no-daemon --console=plain --stacktrace
```

Format, then lint before PR:
```sh
./gradlew spotlessApply --no-daemon --console=plain --stacktrace
./gradlew detekt --no-daemon --console=plain --stacktrace
```

Commit hygiene:
- Keep functional changes separate from formatting/lint churn:
  - `chore(format): apply spotless`
  - `chore(lint): address detekt findings`
- Prefer localized lint fixes touching only edited files; avoid repo-wide refactors unless requested.


---

### CI matrix expectations (what typically gates merges)
- Pull requests (on.pr.yml):
  - Gating when applicable:
    - pr-test → job.test.yml → Tests: All Suites (fails PR on test failures)
    - Gradle Wrapper validation (checks.gradle-wrapper.yml)
  - Advisory (non-blocking; continue-on-error):
    - API Check (checks.apicheck.yml)
    - Formatting (checks.formatting.yml) – label `ci:fmt-ignore` can explicitly ignore
    - Detekt (checks.detekt.yml)
    - Dependency Graph + Dependency Review (dependency-review continues on error)
    - Clippy (only when natives are in scope)
- Push/merge to main/stable (on.push.yml):
  - Gating: Build (job.build.yml), then Test (job.test.yml) – Test depends on Build
- Practical guidance:
  - Locally prioritize unit tests and compilation; run API pin updates when public API changes.
  - Formatting/Detekt can be deferred to pre‑PR, but keep their fixes in separate `chore` commits.

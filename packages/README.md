## Elide Packages

### `dev.elide` ([API docs](https://v3.docs.elide.dev/kotlin/html/))

This directory contains the main framework source code for each distinct package, with each shown below:

| **Package**                       | Artifact                        | Platforms            | Summary                                                   |
|-----------------------------------|---------------------------------|----------------------|-----------------------------------------------------------|
| [`core`][8] ([docs][18])          | `dev.elide:elide-core`          | Multiplatform        | Core, pure-Kotlin utilities                               |
| [`uuid`][24] ([docs][25])         | `dev.elide:elide-uuid`          | Multiplatform        | Cross-platform UUID bindings                              |
| [`base`][1] ([docs][11])          | `dev.elide:elide-base`          | Multiplatform        | Base library (annotations, x-plat code)                   |
| [`model`][6] ([docs][16])         | `dev.elide:elide-model`         | Multiplatform        | Cross-platform data modeling and RPC API layer            |
| [`test`][10] ([docs][20])         | `dev.elide:elide-test`          | Multiplatform        | Cross-platform testing utilities                          |
| [`ssr`][22] ([docs][23])          | `dev.elide:elide-ssr`           | Multiplatform        | Type bindings for SSR / server flows                      |
| [`rpc`][7] ([docs][17])           | `dev.elide:elide-rpc`           | Multiplatform        | Multiplatform code for RPC services and dispatch          |
| [`ssg`][34] ([docs][35])          | `dev.elide:elide-ssg`           | JVM/Native           | Static Site Generation (SSG) tooling, used by the bundler |
| [`frontend`][2] ([docs][12])      | `dev.elide:elide-frontend`      | JavaScript (Browser) | Baseline frontend code, lib bindings (proto, gRPC)        |
| [`graalvm`][3] ([docs][13])       | `dev.elide:elide-graalvm`       | JVM/Native           | JVM integration code for GraalVM support                  |
| [`graalvm-py`][32] ([docs][33])   | `dev.elide:elide-graalvm-py`    | Python               | Python runtime tooling and env code for guest VMs         |
| [`graalvm-js`][4] ([docs][14])    | `dev.elide:elide-graalvm-js`    | JavaScript (SSR)     | JS tooling and env code for in-VM JS SSR                  |
| [`graalvm-react`][5] ([docs][15]) | `dev.elide:elide-graalvm-react` | JavaScript (SSR)     | React-specific JS VM env extensions and tooling           |
| [`server`][9] ([docs][19])        | `dev.elide:elide-server`        | JVM/Native           | Server-side JVM application library base                  |
| [`bom`][28] ([docs][29])          | `dev.elide:elide-bom`           | Gradle Catalog       | Version catalog / BOM for use with Gradle and Maven       |
| [`platform`][30] ([docs][31])     | `dev.elide:elide-platform`      | Gradle Platform      | Version alignment platform                                |
| [`cli`][26] ([docs][27])          | Shipped as native binary        | Native               | CLI distribution of the Elide runtime                     |

## Reports

All packages are scanned with [Sonar](https://sonarcloud.io/project/overview?id=elide-dev_v3) and coverage is tracked in
[Codecov](https://app.codecov.io/gh/elide-dev/elide). Here is a running static report:

### Coverage

Core modules shoot for full, 100% coverage.

[![codecov box](https://codecov.io/gh/elide-dev/elide/branch/v3/graphs/tree.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/elide)
[![codecov sunburst](https://codecov.io/gh/elide-dev/elide/branch/v3/graphs/sunburst.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/elide)

### Static analysis

The Sonar Quality Gate will fail if duplication, vulnerabilities, or technical debt exceed reasonable limits on a given
PR or branch.

<table border="0">
    <tbody>
        <tr>
            <td rowspan=6>
                <img src="https://sonarcloud.io/api/project_badges/quality_gate?project=elide-dev_v3" alt="Sonar quality gate" />
            </td>
            <td>
                <b>Stats</b>
            </td>
            <td>
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=ncloc" alt="Lines of code" />
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=coverage" alt="Coverage" />
            </td>
        </tr>
        <tr>
            <td>
                <b>Quality</b>
            </td>
            <td>
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=reliability_rating" alt="Reliability rating" />
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=bugs" alt="Bugs" />
            </td>
        </tr>
        <tr>
            <td>
                <b>Security</b>
            </td>
            <td>
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=security_rating" alt="Security Rating" />
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=vulnerabilities" alt="Vulnerabilities" />
            </td>
        </tr>
        <tr>
        </tr>
        <tr>
            <td>
                <b>Maintainability</b>
            </td>
            <td>
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=sqale_rating" alt="Maintainability rating" />
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=code_smells" alt="Bugs" />
            </td>
        </tr>
        <tr>
            <td>
                <b>Duplication</b>
            </td>
            <td>
                <img src="https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=duplicated_lines_density" alt="Duplication density" />
            </td>
        </tr>
    </tbody>
</table>

## Dependency management

Library dependencies are declared in the [`elide` version catalog](../gradle/elide.versions.toml), and should be
consumed from Kotlin accessors generated from that catalog. Dependencies are monitored by GitHub's
[`dep-review` bot](https://github.com/actions/dependency-review-action), by [Dependabot](https://github.com/dependabot),
and by [Renovate](https://github.com/renovatebot/renovate) (see
[_Dependency Dashboard_](https://github.com/elide-dev/v3/issues/8) for more information).

License checks are handled by [Licensebat](https://licensebat.com/). Additional OWASP security checks are performed by Snyk and OWASP's own tools.

### Interactive upgrades

Run `update-jsdeps` or `update-jdeps` to run an interactive upgrade for NPM or Maven deps, respectively.

### Dependency locking

Elide extensively locks and verifies dependencies. Gradle is configured to [verify dependency hashes and signatures][21], so please make sure to update these
files if dependencies change, and to verify your update is legitimate to the best of your abilities.

After dependency changes, locks can be updated with `make relock-deps`.

[1]: ./base
[2]: ./frontend
[3]: ./graalvm
[4]: ./graalvm-js
[5]: ./graalvm-react
[6]: ./model
[7]: ./rpc
[8]: ./core
[9]: ./server
[10]: ./test
[11]: https://v3.docs.elide.dev/kotlin/html/packages/base/index.html
[12]: https://v3.docs.elide.dev/kotlin/html/packages/frontend/index.html
[13]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm/index.html
[14]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm-js/index.html
[15]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm-react/index.html
[16]: https://v3.docs.elide.dev/kotlin/html/packages/model/index.html
[17]: https://v3.docs.elide.dev/kotlin/html/packages/rpc-js/index.html
[18]: https://v3.docs.elide.dev/kotlin/html/packages/core/index.html
[19]: https://v3.docs.elide.dev/kotlin/html/packages/server/index.html
[20]: https://v3.docs.elide.dev/kotlin/html/packages/test/index.html
[21]: https://docs.gradle.org/7.4.2/userguide/dependency_verification.html#sub:enabling-verification
[22]: ./ssr
[23]: https://v3.docs.elide.dev/kotlin/html/packages/ssr/index.html
[24]: ./uuid
[25]: https://v3.docs.elide.dev/kotlin/html/packages/uuid/index.html
[26]: ./cli
[27]: https://v3.docs.elide.dev/kotlin/html/packages/cli/index.html
[28]: ./bom
[29]: https://v3.docs.elide.dev/kotlin/html/packages/bom/index.html
[30]: ./platform
[31]: https://v3.docs.elide.dev/kotlin/html/packages/platform/index.html
[32]: ./graalvm-py
[33]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm-py/index.html
[34]: ./ssg
[35]: https://v3.docs.elide.dev/kotlin/html/packages/ssg/index.html

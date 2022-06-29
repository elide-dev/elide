
## Elide Packages
### `dev.elide.v3` ([API docs](https://v3.docs.elide.dev/kotlin/html/))

This directory contains the main framework source code for each distinct package. Packages are organized in a flat
directory structure.

| **Package**                       | Artifact                     | Platforms            | Summary                                            |
|-----------------------------------|------------------------------|----------------------|----------------------------------------------------|
| [`base`][1] ([docs][11])          | `dev.elide.v3:base`          | Multiplatform        | Base library (annotations, x-plat code)            |
| [`frontend`][2] ([docs][12])      | `dev.elide.v3:frontend`      | JavaScript (Browser) | Baseline frontend code, lib bindings (proto, gRPC) |
| [`graalvm`][3] ([docs][13])       | `dev.elide.v3:graalvm`       | JVM/Native           | JVM integration code for GraalVM support           |
| [`graalvm-js`][4] ([docs][14])    | `dev.elide.v3:graalvm-js`    | JavaScript (SSR)     | JS tooling and env code for in-VM JS SSR           |
| [`graalvm-react`][5] ([docs][15]) | `dev.elide.v3:graalvm-react` | JavaScript (SSR)     | React-specific JS VM env extensions and tooling    |
| [`model`][6] ([docs][16])         | `dev.elide.v3:model`         | Multiplatform        | Cross-platform data modeling and RPC API layer     |
| [`rpc-js`][7] ([docs][17])        | `dev.elide.v3:rpc-js`        | JavaScript           | JavaScript code for client-side RPC dispatch       |
| [`rpc-jvm`][8] ([docs][18])       | `dev.elide.v3:rpc-jvm`       | JVM/Native           | JVM code for server-side RPC dispatch, gRPC Web    |
| [`server`][9] ([docs][19])        | `dev.elide.v3:server`        | JVM/Native           | Server-side JVM application library base           |
| [`test`][10] ([docs][20])         | `dev.elide.v3:test`          | Multiplatform        | Cross-platform testing utilities                   |

## Reports

All packages are scanned with [Sonar](https://sonarcloud.io/project/overview?id=elide-dev_v3) and coverage is tracked in
[Codecov](https://app.codecov.io/gh/elide-dev/v3). Here is a running static report:

### Coverage

Core modules shoot for full, 100% coverage.

[![codecov box](https://codecov.io/gh/elide-dev/v3/branch/v3/graphs/tree.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/v3)
[![codecov sunburst](https://codecov.io/gh/elide-dev/v3/branch/v3/graphs/sunburst.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/v3)


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
[7]: ./rpc-js
[8]: ./rpc-jvm
[9]: ./server
[10]: ./test
[11]: https://v3.docs.elide.dev/kotlin/html/packages/base/index.html
[12]: https://v3.docs.elide.dev/kotlin/html/packages/frontend/index.html
[13]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm/index.html
[14]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm-js/index.html
[15]: https://v3.docs.elide.dev/kotlin/html/packages/graalvm-react/index.html
[16]: https://v3.docs.elide.dev/kotlin/html/packages/model/index.html
[17]: https://v3.docs.elide.dev/kotlin/html/packages/rpc-js/index.html
[18]: https://v3.docs.elide.dev/kotlin/html/packages/rpc-jvm/index.html
[19]: https://v3.docs.elide.dev/kotlin/html/packages/server/index.html
[20]: https://v3.docs.elide.dev/kotlin/html/packages/test/index.html
[21]: https://docs.gradle.org/7.4.2/userguide/dependency_verification.html#sub:enabling-verification


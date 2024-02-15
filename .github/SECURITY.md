# Security Policy: Elide Project

> Version: `1.0`

The Elide project and team take security very seriously; a big point of Elide's existence is a lack of isolation in
other runtimes.

Security issues are addressed promptly, and we continuously enhance project security where at all possible.

## Supported Versions

The Elide project is still early, but we intend to support the latest release and immediate past release.
Once the project hits a level of stability suitable for a `1.0` version we will update this document and issue an
LTS release.

**Current version support matrix:**

| Version         | Supported                                   |
| --------------- | ------------------------------------------- |
| `v3-alpha4-*`   | :white_check_mark:                          |
| `v3-alpha3-*`   | :white_check_mark: (Immediate past release) |
| `v2` and before | :x: No support (ancient)                    |

## Reporting a Vulnerability

**We use GitHub issues to track vulnerabilities.** [Click here][9] to report a new issue.
If you need to report a vulnerability privately, please use the email address on our main GitHub organization page
(`apps` at `elide` dot `cloud`).

If you need to provide secure information or your report needs to be encrypted, please use our PGP key, as listed on
public key servers at the same email address.

The Elide team will respond to vulnerabilities promptly. We will work with you to understand the scope of the issue and
confirm the vulnerability.

Once confirmed, we will work to address the issue and release a patch as soon as possible, including backporting to the
latest release and immediate past release.

Other (older) releases may receive backports on a case-by-case basis.

## Security Advisories

We will publish security advisories for any vulnerabilities that we address.

These advisories will be published on our GitHub organization page and will be linked to from this document;
the main `elide` repository will also have a link to this document.

**At this time, no security advisories have been announced.**

## Supply Chain Security

Elide leverages [dependency verification][1] and [dependency locking][2] for all supported tooling; in any case, we
select the minimum set of high-quality dependencies necessary to deliver a feature.

In most cases, Elide only depends on standard libraries and language-endorsed extensions.

### Dependency Verification and Locking

Elide employs Gradle for dependency assurance, with `SHA-256` and `PGP` used for hashing and signing.

### Attestations and Signing

Elide ships with [SLSA attestations][3] for all modules, and embeds an SBOM with each binary artifact.
Library releases are signed with PGP and published to Maven Central; all releases are additionally registered with
[Sigstore][4].

Container image bases carry SLSA attestations and are registered with Sigstore.

## Continuous Updates

Elide pins to the latest versions of all dependencies, by default, modulo (1) known vulnerabilities and (2) support for
current features. Every attempt is made to use only stable dependencies; sometimes this is not possible with the speed
at which development occurs on Elide.

Elide employs Dependabot and Renovate for automated dependency updates, and we continuously monitor for and adopt new
releases of all software Elide depends on.

## Upstream Policies

Elide is a meta-framework and runtime.

When used as a **library or framework**, the bulk of Elide's functionality is implemented by [Micronaut][5] and
[Netty][6]. When used as a **runtime**, Elide is built on top of [GraalVM][10].

You can find their security policies [here][7] (Micronaut), [here][8] (Netty), and [here][11] (GraalVM), respectively.

[1]: https://docs.gradle.org/current/userguide/dependency_verification.html
[2]: https://docs.gradle.org/current/userguide/dependency_locking.html
[3]: https://slsa.dev/
[4]: https://www.sigstore.dev/
[5]: https://micronaut.io/
[6]: https://netty.io/
[7]: https://github.com/micronaut-projects/micronaut-core/security/policy
[8]: https://github.com/netty/netty/security/policy
[9]: https://github.com/elide-dev/elide/issues/new
[10]: https://www.graalvm.org/
[11]: https://github.com/oracle/graal/security/policy

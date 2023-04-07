
Elide support for Bazel is experimental at this time. There is a set of rules which can be pulled into your project. The
rules currently are not well documented, but provide support for the following:

- **Running tests using Elide.** This uses Elide as a runtime, similar to Node JS-style tests.
- **Running app binaries using Elide.** Targets built otherwise with Bazel can be run using Elide as a runtime.

<br />

##### Planned features

Anticipated features for eventual release w.r.t. Bazel tooling for Elide include:

- **Full parity with Gradle.** There are not yet rules for building application components, similar to the
  [Gradle plugin](/tools/gradle). These are planned for the future.
- **Packaged Maven targets for use with Bazel.** Structured dependency targets for Elide libraries would be reall
  useful and is planned for the future.

Please file an issue on the [main repo](https://github.com/elide-dev/elide) if you need additional Bazel features -- we
are looking for feedback on what is needed.

<br />

<a id="install"></a>

#### Installing the Bazel Rules for Elide

The Bazel rules are currently included with the [`runtime`](https://github.com/elide-dev/runtime) repository. At a later
date, these rules will be moved to their own repository; stay tuned for updates on that repo.

Near the beginning of your `WORKSPACE` file:
```starlark
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

ELIDE_RUNTIME_VERSION = "main"    # recommended: pin to a commit hash
ELIDE_RUNTIME_FINGERPRINT = None  # update after pin

http_archive(
    name = "elide_runtime",
    sha256 = ELIDE_RUNTIME_FINGERPRINT,
    strip_prefix = "runtime-%s" % ELIDE_RUNTIME_VERSION,
    urls = ["https://github.com/elide-dev/runtime/archive/%s.tar.gz" % (ELIDE_RUNTIME_VERSION)],
)
```

Near the end of your `WORKSPACE` file:
```starlark
# elide

load("@elide_runtime//tools/defs/elide:bindist.bzl", "elide_bindist_repository")

elide_bindist_repository(
    name = "elide",
)
```

<br />

##### Pinning a version of the Elide CLI

By default, the latest available version of the Elide tool is used (which was available at the time of the commit to
`runtime`). To pin to a specific version, amend your `WORKSPACE` file:

```starlark
# ... everything from above ...

elide_bindist_repository(
    name = "elide",
    version = "1.0-v3-alpha3-b6",
)
```

<br />

<a id="usage"></a>

#### Usage from Bazel

Coming soon.

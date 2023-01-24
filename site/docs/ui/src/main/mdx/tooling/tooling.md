
Elide is designed to be used as a [library][1], a [runtime][2], or both. Consequently, integration exists for build
tooling such as [Gradle][3], [Bazel][4], and Node-based style toolchains.

<br />

#### Tooling for use as a library

When building an app with Elide as a framework, you will want to add each library you want to use as a dependency. Elide
supports use as a framework **from JVM languages**, so Maven coordinates are available for each component.

Additionally, POM dependencies (for Maven) and version catalog dependencies (for Gradle) are available to keep versions
in sync and avoid bloating your build files with dependencies.

See the [Gradle][3] or [Bazel][4] guides for more information.

<br />

#### Tooling as a runtime

To install the Elide CLI, see the [Getting Started][5] guide.


[1]: /library
[2]: /runtime
[3]: /tools/gradle
[4]: /tools/bazel
[5]: /getting-started

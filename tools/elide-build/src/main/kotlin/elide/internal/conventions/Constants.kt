/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.internal.conventions

import org.gradle.api.Project

/** Convention values used across all extensions. */
public object Constants {
  /**
   * The name of the property used to indicate that the project is running in CI.
   * @see isCI
   */
  internal const val CI_FLAG = "elide.ci"

  /** Property names and env variables related to credentials. */
  internal object Credentials {
    /** Environment variable: path to the Google Cloud credentials file. */
    const val GOOGLE = "GOOGLE_APPLICATION_CREDENTIALS"

    /** Environment variable: username to be used when publishing packages. */
    const val PUBLISH_USER = "PUBLISH_USER"

    /** Environment variable: token to be used when publishing packages. */
    const val PUBLISH_TOKEN = "PUBLISH_TOKEN"

    /** Environment variable: username to be used when authenticating to GitHub. */
    const val GITHUB_ACTOR = "GITHUB_ACTOR"

    /** Environment variable: token to be used when authenticating to GitHub. */
    const val GITHUB_TOKEN = "GITHUB_TOKEN"

    /** Property: username to be used when publishing Maven packages. */
    const val MAVEN_USER = "elide.publish.repo.maven.username"

    /** Property: password to be used when publishing Maven packages. */
    const val MAVEN_PASSWORD = "elide.publish.repo.maven.password"
  }

  /** General publishing conventions. */
  internal object Publishing {
    /** Property: whether authentication should be used for Maven repositories. */
    const val MAVEN_AUTH_REQUIRED = "elide.publish.repo.maven.auth"

    /** Property: URL of the Maven repo to be used for publishing. */
    const val MAVEN_REPO_URL = "elide.publish.repo.maven"
  }

  /** Remote repositories used for publishing, etc. */
  internal object Repositories {
    /** Project-specific Maven repository on GitHub. */
    const val GITHUB_MAVEN = "https://maven.pkg.github.com/elide-dev/elide"

    /** Docker container registry. */
    const val PKG_DOCKER = "https://us-docker.pkg.dev"
  }

  /** Values for internal test conventions. */
  internal object Tests {
    /** Sets the threshold at which tests are considered as 'slow'. */
    const val SLOW_TEST_THRESHOLD = 30_000L

    /** Maximum number of forks allowed during parallel test execution. */
    const val MAX_PARALLEL_FORKS = 4
  }

  /** Properties and conventions related to versioning. */
  internal object Versions {
    /** Property: defines the Kotlin language version used in the project. */
    const val KOTLIN = "versions.kotlin.language"

    /** Property: defines the version of the Kotlin SDK. */
    const val KOTLIN_SDK = "versions.kotlin.sdk"

    /** Constant: default Kotlin SDK version if no other version is defined. */
    const val KOTLIN_SDK_PIN = "2.0.0-Beta3"

    /** Constant: pinned version of AtomicFU. */
    const val ATOMICFU = "0.23.2"

    /** Constant: pinned version of Groovy. */
    const val GROOVY = "4.0.18"

    /** Constant: pinned version of OpenTelemetry. */
    const val OPENTELEMETRY = "1.32.0"

    /** Constant: default Kotlin version. */
    const val KOTLIN_DEFAULT = "2.0"

    /** Property: defines the target JVM version. */
    const val JVM_TARGET = "versions.java.target"

    /** Property: JVM bytecode target if no other version is specified. */
    const val JVM_DEFAULT = "17"

    /** GraalVM metadata repository version. */
    const val GRAALVM_METADATA = "0.3.6"

    /** Pinned Protobuf version. */
    const val PROTOBUF = "3.21.11"

    /** Pinned Jupiter (JUnit5) version. */
    const val JUPITER = "5.10.2"

    /** Static Netty version. */
    const val NETTY = "4.1.101.Final"

    /** Guava version. */
    const val GUAVA = "33.0.0-jre"

    /** gRPC version. */
    const val GRPC = "1.61.1"

    /** GraalVM version. */
    const val GRAALVM = "23.1.2"
  }

  /** Kotlin conventions. */
  internal object Kotlin {
    /** Property: whether to treat all warnings as errors. */
    const val STRICT_MODE = "strictMode"

    /** Enumerates the target platforms for JavaScript projects. */
    enum class JavaScriptTarget {
      BROWSER,
      NODE_JS,
    }
  }

  internal object Build {
    /** Property: whether to build and bundle documentation from project sources, defaults to "true". */
    const val BUILD_DOCS = "buildDocs"

    /** Property: Whether to enable dependency locking. */
    const val LOCK_DEPS = "elide.lockDeps"
  }

  /** Static library configuration values. */
  public object Elide {
    /** Major library version. */
    private const val MAJOR_VERSION = "1.0.0"

    /** Major library version tag. */
    private const val VERSION_TAG = "alpha7"

    /** Version string for the library. */
    public const val VERSION: String = "$MAJOR_VERSION-$VERSION_TAG"

    /** Maven group shared by Elide artifacts. */
    public const val GROUP: String = "dev.elide"

    /** Maven group shared by tooling projects. */
    public const val SUBSTRATE_GROUP: String = "dev.elide.tools"

    /** Compiler args to include in all Kotlin targets. */
    private val BaseCompilerArgs = listOf(
      "-Xcontext-receivers",
      "-Xskip-prerelease-check",
      "-Xexpect-actual-classes",
    )

    /** Compiler args to include in Kotlin JVM targets. */
    internal val JvmCompilerArgs = BaseCompilerArgs.plus(
      listOf(
        "-no-stdlib",
        "-Xjvm-default=all",
        "-Xjsr305=strict",
      ),
    )

    /** Compiler args to include in Kotlin JS targets. */
    internal val JsCompilerArgs = BaseCompilerArgs.plus(
      listOf(
        "-Xgenerate-dts",
      ),
    )

    /** Compiler args to include in Kotlin JVM targets which use `kapt`. */
    internal val KaptCompilerArgs = BaseCompilerArgs.plus(
      listOf(
        "-no-stdlib",
        "-Xallow-unstable-dependencies",
        "-Xemit-jvm-type-annotations",
        "-Xjvm-default=all",
        "-Xjsr305=strict",
      ),
    )

    /** Compiler args to include in KMP targets. */
    internal val KmpCompilerArgs = BaseCompilerArgs
  }
}

/** Whether this [Project] is running in CI, as indicated by the [CI_FLAG][Constants.CI_FLAG] property. */
internal val Project.isCI get() = findProperty(Constants.CI_FLAG)?.toString().toBoolean()

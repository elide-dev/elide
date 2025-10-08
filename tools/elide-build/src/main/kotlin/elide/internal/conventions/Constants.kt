/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
import elide.internal.conventions.linting.KtlintConventions.RuleCategory.CODE
import elide.internal.conventions.linting.KtlintConventions.ktRuleset
import elide.internal.conventions.linting.KtlintConventions.ktlintDisable
import elide.internal.conventions.linting.KtlintConventions.ktlintRule

/** Convention values used across all extensions. */
public object Constants {
  /**
   * The name of the property used to indicate that the project is running in CI.
   * @see isCI
   */
  internal const val CI_FLAG = "elide.ci"

  /** Property names and env variables related to credentials. */
  internal object Credentials {
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

    /** Property: Whether to enable Sigstore when publishing. */
    const val ENABLE_SIGSTORE = "enableSigstore"

    /** Property: Whether to enable GPG signing when publishing. */
    const val ENABLE_SIGNING = "enableSigning"
  }

  /** Remote repositories used for publishing, etc. */
  internal object Repositories {
    /** Project-specific Maven repository on GitHub. */
    const val GITHUB_MAVEN = "https://maven.pkg.github.com/elide-dev/elide"
  }

  /** Values for internal test conventions. */
  internal object Tests {
    /** Sets the threshold at which tests are considered as 'slow'. */
    const val SLOW_TEST_THRESHOLD = 30_000L

    /** Maximum number of forks allowed during parallel test execution. */
    const val MAX_PARALLEL_FORKS = 2
  }

  /** Properties and conventions related to versioning. */
  internal object Versions {
    /** Property: defines the Kotlin language version used in the project. */
    const val KOTLIN = "versions.kotlin.language"

    /** Constant: default Kotlin SDK version if no other version is defined. */
    const val KOTLIN_SDK_PIN = "2.2.20"

    /** Constant: pinned version of Kotlin Coroutines. */
    const val COROUTINES = "1.10.2"

    /** Constant: pinned version of AtomicFU. */
    const val ATOMICFU = "0.29.0"

    /** Constant: pinned version of BouncyCastle. */
    const val BOUNCYCASTLE = "1.82"

    /** Constant: pinned version of Groovy. */
    const val GROOVY = "4.0.18"

    /** Constant: defines the Jacoco version in use. */
    const val JACOCO = "0.8.13"

    /** Constant: pinned version of Buf. */
    const val BUF = "1.28.1"

    /** Constant: pinned version of OpenTelemetry. */
    const val OPENTELEMETRY = "1.32.0"

    /** Constant: default Kotlin version. */
    const val KOTLIN_DEFAULT = "2.1"

    /** Property: defines the target JVM version. */
    const val JVM_TARGET = "versions.java.target"

    /** Property: defines the target JVM version for the toolchain. */
    const val JVM_TOOLCHAIN = "versions.java.toolchain"

    /** Property: JVM bytecode target if no other version is specified. */
    const val JVM_DEFAULT = "21"

    /** Property: JVM bytecode target expected for the toolchain. */
    const val JVM_TOOLCHAIN_DEFAULT = "24"

    /** GraalVM metadata repository version. */
    const val GRAALVM_METADATA = "0.3.26"

    /** Version to pin for Diktat. */
    const val DIKTAT = "2.0.0"

    /** Version to pin for ktlint. */
    const val KTLINT = "1.1.1"

    /** Google Java Format version to use. */
    const val GOOGLE_JAVA_FORMAT = "1.28.0"

    /** Version to pin for eslint. */
    const val ESLINT = "8.56.0"

    /** Version to pin for Prettier. */
    const val PRETTIER = "3.2.5"

    /** Pinned Protobuf version. */
    const val PROTOBUF = "4.29.3"

    /** Static Netty version. */
    const val NETTY = "4.2.1.Final"

    /** Guava version. */
    const val GUAVA = "33.5.0-jre"

    /** gRPC version. */
    const val GRPC = "1.62.2"

    /** JLine version. */
    const val JLINE = "3.28.0"

    /** Okio version. */
    const val OKIO = "3.15.0"

    /** GraalVM version. */
    const val GRAALVM = "25.0.0"
  }

  /** Kotlin conventions. */
  internal object Kotlin {
    /** Property: whether to treat all warnings as errors. */
    const val STRICT_MODE = "strictMode"

    /** Property: alternate switch for strictness. */
    const val STRICT_MODE_ALT = "elide.strict"

    /** Property: whether to build for all targets. */
    const val ALL_TARGETS = "elide.allTargets"

    /** Property: whether to build Kotlin native targets. */
    const val KNATIVE = "elide.kotlinNative"

    /** Property: active build mode. */
    const val BUILD_MODE = "elide.buildMode"
  }

  /** Build properties and other top-level constants. */
  internal object Build {
    /** Property: whether to build and bundle documentation from project sources, defaults to "true". */
    const val BUILD_DOCS = "buildDocs"

    /** Property: Whether to enable dependency locking. */
    const val LOCK_DEPS = "elide.lockDeps"
  }

  /** Linting settings and parameters. */
  @Suppress("unused", "SameParameterValue") internal object Linting {
    /** Ktlint overrides to apply globally. */
    val ktlintOverrides: Map<String, String> = ktRuleset(
      ktlintRule(CODE, "style", "intellij_idea"),
      ktlintDisable("annotation"),
      ktlintDisable("wrapping"),
      ktlintDisable("no-wildcard-imports"),
      ktlintDisable("if-else-wrapping"),
      ktlintDisable("value-argument-comment"),
      ktlintDisable("value-argument-comment"),
      ktlintDisable("type-argument-comment"),
      ktlintDisable("multiline-expression-wrapping"),
      ktlintDisable("trailing-comma-on-call-site"),
      ktlintDisable("trailing-comma-on-declaration-site"),
      ktlintDisable("paren-spacing"),
      ktlintDisable("property-naming"),
      ktlintDisable("filename"),
    )

    /** Ktlint overrides to apply to Gradle scripts. */
    val ktlintOverridesKts: Map<String, String> = ktlintOverrides.plus(mapOf())
  }

  /** Static library configuration values. */
  public object Elide {
    /** Version string for the library. */
    public const val VERSION: String = "1.0.0-beta10-rc6"

    /** Maven group shared by Elide artifacts. */
    public const val GROUP: String = "dev.elide"

    /** Maven group shared by tooling projects. */
    public const val SUBSTRATE_GROUP: String = "dev.elide.tools"

    /** Compiler args to include in all Kotlin targets. */
    private val BaseCompilerArgs = listOf(
      "-Xexpect-actual-classes",
      "-Xsuppress-version-warnings",
      "-Xconsistent-data-class-copy-visibility",
      "-Xvalue-classes",
      "-Xinline-classes",
      "-Xabi-stability=stable",
      "-Xannotation-default-target=param-property",
    )

    /** Compiler args to include in Kotlin JVM targets. */
    public val JvmCompilerArgs: List<String> = BaseCompilerArgs.plus(
      listOf(
        "-no-stdlib",
        "-no-reflect",
        "-Xjvm-default=all",
        "-Xjsr305=strict",
        "-Xvalidate-bytecode",
        "-Xsam-conversions=indy",
        "-Xno-receiver-assertions",
        "-Xno-param-assertions",
        "-Xlambdas=indy",
        "-Xenhance-type-parameter-types-to-def-not-null",
        "-Xemit-jvm-type-annotations",
        "-Xassertions=jvm",
        "-Xstring-concat=indy-with-constants",
      ),
    )

    /** Compiler args to include in Kotlin JS targets. */
    internal val JsCompilerArgs = BaseCompilerArgs.plus(
      listOf(
        "-Xgenerate-dts",
      ),
    )

    /** Compiler args to include in Kotlin JVM targets which use `kapt`. */
    internal val KaptCompilerArgs = JvmCompilerArgs.plus(
      listOf(
        "-Xallow-unstable-dependencies",
      ),
    )

    /** Compiler args to include in KMP targets. */
    internal val KmpCompilerArgs = BaseCompilerArgs
  }
}

/** Whether this [Project] is running in CI, as indicated by the [CI_FLAG][Constants.CI_FLAG] property. */
internal val Project.isCI get() = findProperty(Constants.CI_FLAG)?.toString().toBoolean()

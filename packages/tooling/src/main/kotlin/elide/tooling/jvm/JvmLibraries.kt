/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.tooling.jvm

import java.nio.file.Path
import elide.tooling.Classpath

/**
 * # JVM Libraries
 *
 * Describes coordinates and versions for built-in libraries which ship with Elide.
 */
public object JvmLibraries {
  // @TODO: don't hard-code any of this
  public const val ELIDE_VERSION: String = "1.0.0-beta8"
  public const val EMBEDDED_KOTLIN: String = "2.2.20"

  public const val EMBEDDED_JUNIT_VERSION: String = "5.13.1"
  public const val EMBEDDED_JUNIT_PLATFORM_VERSION: String = "1.13.1"
  public const val EMBEDDED_APIGUARDIAN_VERSION: String = "1.1.2"
  public const val EMBEDDED_OPENTEST_VERSION: String = "1.3.0"
  public const val EMBEDDED_KOTLINX_HTML_VERSION: String = "0.12.0"
  public const val EMBEDDED_KOTLINX_CSS_VERSION: String = "2025.7.7"
  public const val EMBEDDED_KOTLINX_IO_VERSION: String = "0.8.0"
  public const val EMBEDDED_COROUTINES_VERSION: String = "1.10.2"
  public const val EMBEDDED_SERIALIZATION_VERSION: String = "1.9.0"
  public const val EMBEDDED_REDACTED_VERSION: String = "1.14.1"
  public const val APIGUARDIAN_API: String = "org.apiguardian:apiguardian-api"
  public const val JUNIT_JUPITER_API: String = "org.junit.jupiter:junit-jupiter-api"
  public const val JUNIT_JUPITER_ENGINE: String = "org.junit.jupiter:junit-jupiter-engine"
  public const val JUNIT_PLATFORM_ENGINE: String = "org.junit.platform:junit-platform-engine"
  public const val JUNIT_PLATFORM_COMMONS: String = "org.junit.platform:junit-platform-commons"
  public const val JUNIT_PLATFORM_CONSOLE: String = "org.junit.platform:junit-platform-console"
  public const val JUNIT_JUPITER_PARAMS: String = "org.junit.jupiter:junit-jupiter-params"
  public const val OPENTEST: String = "org.opentest4j:opentest4j"
  public const val KOTLIN_TEST: String = "org.jetbrains.kotlin:kotlin-test"
  public const val KOTLIN_TEST_JUNIT5: String = "org.jetbrains.kotlin:kotlin-test-junit5"
  public const val KOTLINX_HTML: String = "org.jetbrains.kotlinx:kotlinx-html-jvm"
  public const val KOTLINX_CSS: String = "org.jetbrains.kotlinx:kotlinx-css-jvm"
  public const val KOTLINX_IO: String = "org.jetbrains.kotlinx:kotlinx-io-jvm"
  public const val KOTLINX_IO_BYTESTRING: String = "org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm"
  public const val KOTLINX_COROUTINES: String = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm"
  public const val KOTLINX_COROUTINES_TEST: String = "org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm"
  public const val KOTLINX_SERIALIZATION: String = "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm"
  public const val KOTLINX_SERIALIZATION_JSON: String = "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm"
  public const val ELIDE_BASE: String = "dev.elide:elide-core-jvm"
  public const val ELIDE_CORE: String = "dev.elide:elide-base-jvm"
  public const val ELIDE_TEST: String = "dev.elide:elide-test-jvm"

  internal val baseCoordinates = arrayOf(
    jarNamed("kotlin-stdlib"),
    jarNamed("kotlin-reflect"),
  )

  internal val baseKotlinxCoordinates = arrayOf(
    KOTLINX_HTML to EMBEDDED_KOTLINX_HTML_VERSION,
    KOTLINX_CSS to EMBEDDED_KOTLINX_CSS_VERSION,
    KOTLINX_IO to EMBEDDED_KOTLINX_IO_VERSION,
    KOTLINX_IO_BYTESTRING to EMBEDDED_KOTLINX_IO_VERSION,
  )

  internal val elideCoordinates = arrayOf(
    ELIDE_BASE,
    ELIDE_CORE,
  )

  internal val elideTestCoordinates = arrayOf(
    ELIDE_TEST,
  )

  public val testCoordinates: Array<Pair<String, String>> = arrayOf(
    OPENTEST to EMBEDDED_OPENTEST_VERSION,
    JUNIT_JUPITER_API to EMBEDDED_JUNIT_VERSION,
    JUNIT_JUPITER_PARAMS to EMBEDDED_JUNIT_VERSION,
    JUNIT_JUPITER_ENGINE to EMBEDDED_JUNIT_VERSION,
    JUNIT_PLATFORM_ENGINE to EMBEDDED_JUNIT_PLATFORM_VERSION,
    JUNIT_PLATFORM_COMMONS to EMBEDDED_JUNIT_PLATFORM_VERSION,
    JUNIT_PLATFORM_CONSOLE to EMBEDDED_JUNIT_PLATFORM_VERSION,
    KOTLIN_TEST to EMBEDDED_KOTLIN,
    KOTLIN_TEST_JUNIT5 to EMBEDDED_KOTLIN,
    KOTLINX_COROUTINES_TEST to EMBEDDED_COROUTINES_VERSION,
    KOTLINX_SERIALIZATION to EMBEDDED_SERIALIZATION_VERSION,
    KOTLINX_SERIALIZATION_JSON to EMBEDDED_SERIALIZATION_VERSION,
    APIGUARDIAN_API to EMBEDDED_APIGUARDIAN_VERSION,
  )

  public fun jarNamed(name: String): String = buildString {
    append(name)
    append('.')
    append("jar")
  }

  public fun jarNameFor(coordinate: String, version: String): String {
    val parts = coordinate.split(":")
    require(parts.size == 2) { "Invalid built-in coordinate: $coordinate" }
    return "${parts[1]}-$version.jar"
  }

  public fun elideJarNameFor(coordinate: String, version: String): String {
    return jarNameFor(coordinate, version).removePrefix("elide-")
  }

  public fun resolveJarFor(path: Path, coordinate: String, version: String): Path {
    return resolveJarFor(path, jarNameFor(coordinate, version))
  }

  public fun resolveJarFor(path: Path, name: String): Path {
    return path
      .resolve("kotlin")
      .resolve(EMBEDDED_KOTLIN)
      .resolve("lib")
      .resolve(name)
  }

  public fun resolveElideJarFor(path: Path, coordinate: String, elideVersion: String): Path {
    return resolveJarFor(path, jarNameFor(coordinate, elideVersion))
  }

  public fun builtinClasspath(
    path: Path,
    elideVersion: String = ELIDE_VERSION,
    tests: Boolean = false,
    kotlin: Boolean = true,
    kotlinx: Boolean = true,
    elide: Boolean = true,
  ): Classpath {
    return Classpath.from(
      buildList {
        if (elide) {
          addAll(elideCoordinates.map { resolveElideJarFor(path, it, elideVersion) })
          if (tests) {
            addAll(elideTestCoordinates.map { resolveElideJarFor(path, it, elideVersion) })
          }
        }
        if (kotlin || tests) addAll(
          baseCoordinates.map {
            resolveJarFor(path, it)
          },
        )
        if (kotlinx) baseKotlinxCoordinates.forEach { (coordinate, version) ->
          add(resolveJarFor(path, coordinate, version))
        }
        if (tests) {
          addAll(
            testCoordinates.map { (coordinate, version) ->
              resolveJarFor(path, coordinate, version)
            },
          )
        }
      },
    )
  }
}

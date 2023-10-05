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

package elide.internal.conventions.project

public object Projects {
  /** Sample code modules. */
  public val samples: List<String> = listOf(
    ":samples:server:hellocss",
    ":samples:server:helloworld",
    ":samples:fullstack:basic:server",
    ":samples:fullstack:react:server",
    ":samples:fullstack:ssr:server",
    ":samples:fullstack:react-ssr:server",
  )

  /** Kotlin MPP modules. */
  public val multiplatformModules: List<String> = listOf(
    "base",
    "core",
    "model",
    "rpc",
    "ssr",
    "test",
    "runtime",
    "proto:proto-core",
  )

  /** Server-side only modules. */
  public val serverModules: List<String> = listOf(
    "graalvm",
    "graalvm",
    "graalvm-js",
    "graalvm-jvm",
    "graalvm-kt",
    "graalvm-llvm",
    "graalvm-py",
    "graalvm-rb",
    "graalvm-react",
    "proto:proto-capnp",
    "proto:proto-flatbuffers",
    "proto:proto-kotlinx",
    "proto:proto-protobuf",
    "server",
  )

  /** Packages which are not in use at this time. */
  public val disabledPackages: List<String> = listOf(
    ":packages:ssg",
  )

  /** Browser-side only modules. */
  public val frontendModules: List<String> = listOf(
    "frontend",
    "graalvm-js",
    "graalvm-react",
  )

  /** Modules which should not be exported for documentation. */
  public val noDocModules: Set<String> = sortedSetOf(
    "cli",
    "frontend",
    "graalvm",
    "graalvm-js",
    "graalvm-jvm",
    "graalvm-kt",
    "graalvm-llvm",
    "graalvm-py",
    "graalvm-rb",
    "graalvm-react",
    "graalvm-wasm",
    "proto-capnp",
    "proto-core",
    "proto-flatbuffers",
    "proto-kotlinx",
    "proto-protobuf",
    "app",
    "ui",
    "processor",
    "runtime",
    "rpc",
    "model",
  )

  /** Modules which should not be reported on for testing. */
  public val noTestModules: List<String> = listOf(
    "bom",
    "platform",
    "packages",
    "processor",
    "reports",
    "bundler",
    "samples",
    "site",
    "ssg",
    "docs",
    "model",
    "benchmarks",
    "frontend",
    "graalvm-js",
    "graalvm-react",
    "test",
  )

  /** All library modules which are published. */
  public val publishedModules: List<String> = listOf(
    // Library Packages
    "base",
    "core",
    "frontend",
    "graalvm",
    "graalvm-js",
    "graalvm-jvm",
    "graalvm-kt",
    "graalvm-llvm",
    "graalvm-py",
    "graalvm-rb",
    "graalvm-react",
    "model",
    "proto:proto-core",
    "proto:proto-capnp",
    "proto:proto-flatbuffers",
    "proto:proto-kotlinx",
    "proto:proto-protobuf",
    "rpc",
    "server",
    "ssg",
    "ssr",
    "test",
    "runtime",
  ).map { ":packages:$it" }.plus(
    listOf(
      // Tools
      "processor",
    ).map { ":tools:$it" },
  ).filter {
    !disabledPackages.contains(it)
  }

  /** All subproject modules which are published. */
  public val publishedSubprojects: List<String> = emptyList()

  /** All publishable targets. */
  public val allPublishTargets: List<String> = publishedModules.plus(
    publishedSubprojects,
  )
}

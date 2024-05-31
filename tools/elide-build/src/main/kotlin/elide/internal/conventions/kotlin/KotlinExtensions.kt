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

package elide.internal.conventions.kotlin

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory.DependencyNotationSupplier
import org.gradle.kotlin.dsl.accessors.runtime.addDependencyTo
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

/** A wrapper around the [KotlinMultiplatformExtension], used to configure [dependencies] in a KMP project. */
public class DependenciesScope internal constructor(
  internal val extension: KotlinMultiplatformExtension,
  internal val project: Project
)

/** Configure a Kotlin Multiplatform project's dependencies. */
public fun Project.dependencies(block: DependenciesScope.() -> Unit) {
  DependenciesScope(
    extensions.getByType(KotlinMultiplatformExtension::class.java),
    this,
  ).apply(block)
}

public fun DependenciesScope.add(configuration: String, spec: String) {
  project.dependencies.apply {
    this.add(configuration, spec)
  }
}

public fun DependenciesScope.add(configuration: String, spec: DependencyNotationSupplier) {
  project.dependencies.apply {
    this.add(configuration, spec)
  }
}

/** Configure dependencies for the `commonMain` source set. */
public fun DependenciesScope.common(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("commonMain", block)
}

/** Configure dependencies for the `commonTest` source set. */
public fun DependenciesScope.commonTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("commonTest", block)
}

/** Configure dependencies for the `jvmMain` source set. */
public fun DependenciesScope.jvm(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jvmMain", block)
}

/** Configure dependencies for the `jvmTest` source set. */
public fun DependenciesScope.jvmTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jvmTest", block)
}

/** Configure dependencies for the `jsMain` source set. */
public fun DependenciesScope.js(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jsMain", block)
}

/** Configure dependencies for the `wasmJsMain` source set. */
public fun DependenciesScope.wasm(block: KotlinDependencyHandler.() -> Unit) {
  if (!project.isWasmDisabled()) {
    sourceSetDependencies("wasmJsMain", block)
  }
}

/** Configure dependencies for the `wasmJstest` source set. */
public fun DependenciesScope.wasmTest(block: KotlinDependencyHandler.() -> Unit) {
  if (!project.isWasmDisabled()) {
    sourceSetDependencies("wasmJsTest", block)
  }
}

/** Configure dependencies for the `wasmWasiMain` source set. */
public fun DependenciesScope.wasi(block: KotlinDependencyHandler.() -> Unit) {
  if (!project.isWasmDisabled()) {
    sourceSetDependencies("wasmWasiMain", block)
  }
}

/** Configure dependencies for the `wasmWasiTest` source set. */
public fun DependenciesScope.wasiTest(block: KotlinDependencyHandler.() -> Unit) {
  if (!project.isWasmDisabled()) {
    sourceSetDependencies("wasmWasiTest", block)
  }
}

/** Configure dependencies for the `jsTest` source set. */
public fun DependenciesScope.jsTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jsTest", block)
}

/** Configure dependencies for the `nativeMain` source set. */
public fun DependenciesScope.native(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("nativeMain", block)
}

/** Configure dependencies for the `nativeTest` source set. */
public fun DependenciesScope.nativeTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("nativeTest", block)
}

/** Resolve and configure a Kotlin Source Set with the given [name][sourceSetName]. */
private inline fun DependenciesScope.sourceSetDependencies(
  sourceSetName: String,
  crossinline block: KotlinDependencyHandler.() -> Unit
) {
  extension.sourceSets.getByName(sourceSetName).dependencies { block() }
}

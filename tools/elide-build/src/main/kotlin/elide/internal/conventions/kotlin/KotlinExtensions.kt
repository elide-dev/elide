package elide.internal.conventions.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

/** A wrapper around the [KotlinMultiplatformExtension], used to configure [dependencies] in a KMP project. */
@JvmInline public value class DependenciesScope internal constructor(
  internal val extension: KotlinMultiplatformExtension
)

/** Configure a Kotlin Multiplatform project's dependencies. */
public fun Project.dependencies(block: DependenciesScope.() -> Unit) {
  DependenciesScope(extensions.getByType(KotlinMultiplatformExtension::class.java)).apply(block)
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
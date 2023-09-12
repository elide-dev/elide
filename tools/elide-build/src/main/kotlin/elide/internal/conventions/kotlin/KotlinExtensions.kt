package elide.internal.conventions.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

@JvmInline public value class DependenciesScope(internal val extension: KotlinMultiplatformExtension)

public fun Project.dependencies(block: DependenciesScope.() -> Unit): Unit = afterEvaluate {
  DependenciesScope(extensions.getByType(KotlinMultiplatformExtension::class.java)).apply(block)
}

public fun DependenciesScope.common(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("commonMain", block)
}

public fun DependenciesScope.commonTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("commonTest", block)
}

public fun DependenciesScope.jvm(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jvmMain", block)
}

public fun DependenciesScope.jvmTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jvmTest", block)
}

public fun DependenciesScope.js(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jsMain", block)
}

public fun DependenciesScope.jsTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("jsTest", block)
}

public fun DependenciesScope.native(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("nativeMain", block)
}

public fun DependenciesScope.nativeTest(block: KotlinDependencyHandler.() -> Unit) {
  sourceSetDependencies("nativeTest", block)
}

private inline fun DependenciesScope.sourceSetDependencies(
  sourceSetName: String,
  crossinline block: KotlinDependencyHandler.() -> Unit
) {
  extension.sourceSets.getByName(sourceSetName).dependencies { block() }
}
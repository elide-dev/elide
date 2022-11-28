@file:Suppress("TooManyFunctions")

package dev.elide.buildtools.gradle.plugin.tasks

import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_BASE
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_CATALOG
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_CONVENTION
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_FRONTEND
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_GRAALVM
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_GRAALVM_JS
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_GRAALVM_REACT
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_MODEL
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_PLATFORM
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_PROCESSOR
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_PROTO
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_SERVER
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_SUBSTRATE
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.DEPENDENCY_TEST
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.ELIDE_LIB_VERSION
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import java.util.EnumSet

/** Installs dependencies within Elide plugin projects. */
@Suppress("unused")
internal object ElideDependencies {
    /** Default Group ID for library dependencies. */
    private const val libraryGroup = "dev.elide"

    /** Default Group ID for tooling dependencies. */
    private const val toolsGroup = "dev.elide.tools"

    /** Enumerates supported dependency targets (Gradle configurations) for Elide dependency injection. */
    internal enum class DependencyTarget constructor (
        internal val sourceSet: String? = null,
        internal val kmppSourceSet: String? = null,
    ) {
        /** Inject the dependency into the KSP processor set. */
        KSP,

        /** The dependency should be installed as a Java platform. */
        PLATFORM,

        /** The dependency should be installed as a version catalog. */
        CATALOG,

        /** Inject the dependency into a Multiplatform Test dependency, falling back to JVM compile-time. */
        TEST(sourceSet = "test", kmppSourceSet = "commonTest"),

        /** Inject the dependency into the applicable main target. */
        MAIN(sourceSet = "kotlin", kmppSourceSet = "commonMain"),
    }

    /** Enumerates types of dependency visibility. */
    internal enum class DependencyVisibility constructor (internal val config: String) {
        /** The dependency should propagate transitively. */
        API("api"),

        /** The dependency should be considered internal. */
        IMPLEMENTATION("implementation"),

        /** The dependency should only be available at compile time. */
        COMPILE("compileOnly"),

        /** The dependency should only be available at runtime. */
        RUNTIME("runtimeOnly"),
    }

    /** Describes dependency platforms supported by Kotlin. */
    internal enum class DependencyPlatform {
        /** The target works on any platform. */
        ANY,

        /** The target is JVM-only. */
        JVM,

        /** The target is JS-only. */
        JS,
    }

    /** Types of injectable dependencies. */
    internal enum class DependencyType {
        /** The dependency is a package that is part of the Elide library. */
        LIBRARY,

        /** The dependency is a package that is used within the build. */
        TOOLING,
    }

    /** Object surface for something which describes a dependency. */
    internal interface DependencySpec {
        /** Artifact ID for this dependency. */
        val artifactId: String

        /** Group ID for this dependency. */
        val groupId: String

        /** Target where this dependency should be deposited via injection. */
        val target: DependencyTarget

        /** Visibility setting for this dependency; determines transitive propagation. */
        val visibility: DependencyVisibility

        /** Set of platforms which should be supported by a given dependency. */
        val platforms: EnumSet<DependencyPlatform> get() = EnumSet.allOf(DependencyPlatform::class.java)
    }

    /** Object surface for something which describes a dependency. */
    interface LibraryDependencySpec : DependencySpec {
        /** @return Group ID to use for library dependencies. */
        override val groupId get() = libraryGroup
    }

    /** Enumerates injectable library packages. */
    internal enum class Package(
        private val spec: String,
        private val config: DependencyVisibility,
        private val type: DependencyTarget,
        private val supports: EnumSet<DependencyPlatform> = EnumSet.allOf(DependencyPlatform::class.java),
    ) : LibraryDependencySpec {
        /** Base package package. */
        BASE(
            spec = DEPENDENCY_BASE,
            config = DependencyVisibility.API,
            type = DependencyTarget.MAIN
        ),

        /** Model package package. */
        MODEL(
            spec = DEPENDENCY_MODEL,
            config = DependencyVisibility.API,
            type = DependencyTarget.MAIN
        ),

        /** Universal testing package. */
        TEST(
            spec = DEPENDENCY_TEST,
            config = DependencyVisibility.API,
            type = DependencyTarget.TEST,
        ),

        /** Protocol Buffers package. */
        PROTO(
            spec = DEPENDENCY_PROTO,
            config = DependencyVisibility.API,
            type = DependencyTarget.MAIN,
            supports = EnumSet.of(DependencyPlatform.JVM),
        ),

        /** Server-side package. */
        SERVER(
            spec = DEPENDENCY_SERVER,
            config = DependencyVisibility.IMPLEMENTATION,
            type = DependencyTarget.MAIN,
            supports = EnumSet.of(DependencyPlatform.JVM),
        ),

        /** GraalVM package for server-side use. */
        GRAALVM(
            spec = DEPENDENCY_GRAALVM,
            config = DependencyVisibility.IMPLEMENTATION,
            type = DependencyTarget.MAIN,
            supports = EnumSet.of(DependencyPlatform.JVM),
        ),

        /** Frontend package. */
        FRONTEND(
            spec = DEPENDENCY_FRONTEND,
            config = DependencyVisibility.IMPLEMENTATION,
            type = DependencyTarget.MAIN,
            supports = EnumSet.of(DependencyPlatform.JS),
        ),

        /** GraalVM JS package for server-side embedded use with SSR (i.e. compiles to Node). */
        GRAALVM_JS(
            spec = DEPENDENCY_GRAALVM_JS,
            config = DependencyVisibility.IMPLEMENTATION,
            type = DependencyTarget.MAIN,
            supports = EnumSet.of(DependencyPlatform.JS),
        ),

        /** GraalVM JS (React) package for server-side embedded use with SSR (i.e. compiles to Node). */
        GRAALVM_REACT(
            spec = DEPENDENCY_GRAALVM_REACT,
            config = DependencyVisibility.IMPLEMENTATION,
            type = DependencyTarget.MAIN,
            supports = EnumSet.of(DependencyPlatform.JS),
        );

        /** @inheritDoc */
        override val target: DependencyTarget get() = type

        /** @inehritDoc */
        override val visibility: DependencyVisibility get() = config

        /** @inheritDoc */
        override val artifactId: String get() = spec.substringAfter(":")

        /** @inheritDoc */
        override val groupId: String get() = spec.ifBlank { libraryGroup }.substringBefore(":")

        /** @inheritDoc */
        override val platforms: EnumSet<DependencyPlatform> get() = supports
    }

    /**
     * Represents an Elide dependency with injection configuration.
     *
     * @param name Debug name / label for this dependency.
     * @param artifact Artifact name for this dependency.
     * @param group Artifact group ID for this dependency.
     * @param type Type of target where this dependency should be injected.
     * @param config Determines visibility of the dependency.
     */
    internal sealed class ElideDependency(
        private val name: String,
        private val artifact: String,
        private val group: String,
        private val type: DependencyTarget,
        private val config: DependencyVisibility,
    ) : DependencySpec {
        override val artifactId: String get() = artifact
        override val groupId: String get() = group
        override val target: DependencyTarget get() = type
        override val visibility: DependencyVisibility get() = config

        /** Injectable package which is part of the Elide Library suite as part of a common module. */
        data class CommonLibrary internal constructor(private val pkg: Package) : ElideDependency(
            name = pkg.name.lowercase(),
            artifact = pkg.artifactId,
            group = pkg.groupId,
            type = pkg.target,
            config = pkg.visibility,
        )

        /** Injectable package which is part of the Elide Library suite as part of a JVM module. */
        data class JVMLibrary internal constructor(private val pkg: Package) : ElideDependency(
            name = pkg.name.lowercase(),
            artifact = pkg.artifactId,
            group = pkg.groupId,
            type = pkg.target,
            config = pkg.visibility,
        )

        /** Injectable package which should be added to the Kotlin Symbol Processing processor classpath. */
        data class AnnotationProcessor internal constructor(
            private val name: String,
            private val artifact: String,
            private val group: String,
        ) : ElideDependency(
            name = name,
            artifact = artifact,
            group = group,
            type = DependencyTarget.KSP,
            config = DependencyVisibility.API, // @TODO(sgammon): investigate scoping this sensitively
        ) {
            internal companion object {
                /** @return Annotation processor specification from the provided artifact [spec] string. */
                @JvmStatic internal fun fromSpec(spec: String): AnnotationProcessor = AnnotationProcessor(
                    name = spec.substringAfter(':'),
                    artifact = spec.substringAfter(':'),
                    group = spec.substringBefore(':'),
                )
            }
        }
    }

    /** Static dependency mappings for Elide libraries. */
    internal object Libraries {
        /** `base` library. */
        val BASE = ElideDependency.CommonLibrary(Package.BASE)

        /** `test` library. */
        val TEST = ElideDependency.CommonLibrary(Package.TEST)

        /** `model` library. */
        val MODEL = ElideDependency.CommonLibrary(Package.MODEL)

        /** `proto` library. */
        val PROTO = ElideDependency.JVMLibrary(Package.PROTO)

        /** `server` library. */
        val SERVER = ElideDependency.JVMLibrary(Package.SERVER)
    }

    /** Static dependency mappings for Elide tooling. */
    internal object Tools {
        /** KSP processor. */
        val PROCESSOR = ElideDependency.AnnotationProcessor.fromSpec(DEPENDENCY_PROCESSOR)

        /** Gradle Java Platform. */
        val PLATFORM = ElideDependency.AnnotationProcessor.fromSpec(DEPENDENCY_PLATFORM)

        /** Gradle Version Catalog. */
        val CATALOG = ElideDependency.AnnotationProcessor.fromSpec(DEPENDENCY_CATALOG)

        /** Gradle substrate. */
        val SUBSTRATE = ElideDependency.AnnotationProcessor.fromSpec(DEPENDENCY_SUBSTRATE)

        /** Gradle convention plugins. */
        val CONVENTION = ElideDependency.AnnotationProcessor.fromSpec(DEPENDENCY_CONVENTION)
    }

    /** Third-party declaration namespace. */
    internal object ThirdParty {
        /** Third-party Gradle plugins. */
        internal object Plugins {
            /** Kotlin Symbol Processing. */
            internal object KSP {
                /** Task name for KSP. */
                internal const val task: String = "kspKotlin"

                /** Plugin ID for KSP. */
                internal const val pluginId = "com.google.devtools.ksp"

                /** Configuration name to resolve in non-KMPP modules. */
                internal const val configNameNonKmpp = "ksp"
            }

            /** Kotlin Multiplatform Plugin. */
            internal object KotlinMultiplatform {
                /** Plugin ID for KMPP. */
                internal const val pluginId = "org.jetbrains.kotlin.multiplatform"
            }

            /** Kotlin JS plugin. */
            internal object KotlinJS {
                /** Plugin ID for KotlinJS. */
                internal const val pluginId = "org.jetbrains.kotlin.js"
            }

            /** Kotlin JVM plugin. */
            internal object KotlinJVM {
                /** Plugin ID for Kotlin JVM. */
                internal const val pluginId = "org.jetbrains.kotlin.jvm"
            }
        }
    }

    /**
     * Resolve a KSP version to use which should be compatible with the current Kotlin compiler.
     *
     * @return KSP version to use.
     */
    @Suppress("MagicNumber")
    private fun resolveKspVersion(): String {
        val current = KotlinVersion.CURRENT
        return "$current-" + when {
            current.isAtLeast(1, 8) -> "1.0.8"
            current.isAtLeast(1, 7, 20) -> "1.0.8"
            current.isAtLeast(1, 6, 21) -> "1.0.6"
            current.isAtLeast(1, 6, 20) -> "1.0.5"
            current.isAtLeast(1, 6, 10) -> "1.0.4"
            current.isAtLeast(1, 6, 0) -> "1.0.2"
            current.isAtLeast(1, 5, 31) -> "1.0.1"
            current.isAtLeast(1, 5, 30) -> "1.0.0"
            else -> throw IllegalStateException("KSP is not supported for Kotlin version $current")
        }
    }

    /**
     * Resolve the Gradle [Configuration] that should be used to inject the provided dependency [spec], based on the
     * active set of plugins and configurations within the receiver [Project].
     *
     * If the configuration cannot be resolved, an error is thrown.
     *
     * @receiver Project which should be resolved against.
     * @param spec Dependency specification.
     * @return Resolved configuration.
     */
    private fun Project.resolveConfigurationForSpec(spec: DependencySpec): Configuration = when {
        // if we're dealing with Kotlin Multi-platform, we'll need to pull a different source-set.
        plugins.hasPlugin(ThirdParty.Plugins.KotlinMultiplatform.pluginId) -> {
            // next up, determine the type of source set to apply to
            val sourceSetName = when (spec.target) {
                // if it's a KSP dependency, we need to add it as a processor to the common source set.
                DependencyTarget.KSP -> {
                    TODO("not yet implemented")
                }

                // if it's a catalog dependency, we need to add it via the Gradle API.
                DependencyTarget.CATALOG -> {
                    TODO("not yet implemented")
                }

                // if it's a platform dependency, we need to add it via the JVM source set.
                DependencyTarget.PLATFORM -> {
                    TODO("not yet implemented")
                }

                else -> spec.target.kmppSourceSet ?: error(
                    "Failed to resolve source set name for dependency target '${spec.target.name}'"
                )
            }

            val sourceSets = project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
            val sourceSet = (sourceSets.getByName(sourceSetName) as DefaultKotlinSourceSet)
            configurations.getByName(when (spec.visibility) {
                DependencyVisibility.API -> sourceSet.apiConfigurationName
                DependencyVisibility.COMPILE -> sourceSet.compileOnlyConfigurationName
                DependencyVisibility.IMPLEMENTATION -> sourceSet.implementationConfigurationName
                DependencyVisibility.RUNTIME -> sourceSet.runtimeOnlyConfigurationName
            })
        }

        // otherwise, we can assume there is a single Kotlin source-set.
        else -> when (spec.target) {
            DependencyTarget.KSP -> configurations.getByName(
                ThirdParty.Plugins.KSP.configNameNonKmpp
            )

            DependencyTarget.CATALOG -> {
                TODO("not yet implemented")
            }

            DependencyTarget.PLATFORM -> {
                TODO("not yet implemented")
            }

            DependencyTarget.TEST,
            DependencyTarget.MAIN -> configurations.getByName(
                spec.visibility.config
            )
        }
    }

    /**
     * Create a Gradle [Dependency] from the provided [spec].
     *
     * @receiver Dependency handler for the target project.
     * @param spec Specification for the injected dependency.
     * @return Created dependency.
     */
    private fun DependencyHandler.create(ext: ElideExtension, spec: DependencySpec): Dependency {
        val artifactId = spec.artifactId
        val groupId = spec.groupId
        val version = ext.version.getOrElse(ELIDE_LIB_VERSION)
        return create("$groupId:$artifactId:$version")
    }

    /**
     * Install a dependency within the provided [spec] of source-set/configuration.
     *
     * @receiver Project which should accept the new dependency.
     * @param spec Specification for the dependency to install.
     */
    private fun Project.installDependency(spec: DependencySpec) {
        if (logger.isDebugEnabled) {
            logger.debug("[Elide]: Dependency '${spec.groupId}:${spec.artifactId}' requested")
        }
        val extension = extensions.getByType(ElideExtension::class.java)
        val configuration: Configuration = resolveConfigurationForSpec(spec)

        // check for an existing dependency
        val existing = configuration.dependencies.find {
            it.group == spec.groupId &&
            it.name == spec.artifactId
        }

        // no such dependency exists; add it to the configuration
        if (existing == null) {
            logger.lifecycle("[Elide]: Providing dependency '${spec.groupId}:${spec.artifactId}'")

            configuration.dependencies.add(
                dependencies.create(extension, spec)
            )
        } else if (logger.isDebugEnabled) {
            logger.debug("[Elide]: Dependency '${spec.groupId}:${spec.artifactId}' already provided")
        }
    }

    /**
     * Install a statically-configured set of [deps] within the receiver [Project].
     *
     * @receiver Project which should accept the new dependency entries.
     * @param deps Dependencies to install into the project.
     * @return Current project, for chaining.
     */
    private fun Project.install(vararg deps: ElideDependency): Project {
        deps.forEach {
            installDependency(it)
        }
        return this
    }

    /**
     * Install the Kotlin Symbol Processing (KSP) Gradle plugin and apply it to the receiving [Project].
     *
     * @receiver Project to which the KSP plugin should be applied.
     * @return Current project, for chaining.
     */
    fun Project.installApplyKSP(): Project {
        if (!pluginManager.hasPlugin(ThirdParty.Plugins.KSP.pluginId) &&
            plugins.findPlugin(ThirdParty.Plugins.KSP.pluginId) == null) {
            // KSP needs to be resolved and by version, and then applied to the project.
            pluginManager.apply(ThirdParty.Plugins.KSP.pluginId)
        }
        return project
    }

    /**
     * Install the Elide Java Platform constraint dependency into the receiving [Project].
     *
     * @receiver Project to which the dependency should be applied.
     * @return Current project, for chaining.
     */
    internal fun Project.installJavaPlatform(): Project {
        return this // @TODO(sgammon): not yet implemented
//        return installVersionCatalog().install(
//            Tools.PLATFORM,
//        )
    }

    /**
     * Install the Elide frontend base dependency set.
     *
     * @receiver Project to which the dependency should be applied.
     * @return Current project, for chaining.
     */
    internal fun Project.installFrontend(): Project {
        return this // @TODO(sgammon): not yet implemented
    }

    /**
     * Install libraries which apply universally to all Kotlin targets.
     *
     * @receiver Project to which the dependency should be applied.
     * @return Current project, for chaining.
     */
    internal fun Project.installCommonLibs(): Project {
        return install(
            Libraries.BASE,
        )
    }

    /**
     * Install libraries which apply universally to all Kotlin targets.
     *
     * @receiver Project to which the dependency should be applied.
     * @return Current project, for chaining.
     */
    internal fun Project.installServerLibs(): Project {
        return installCommonLibs().install(
            Libraries.SERVER,
        )
    }

    /**
     * Install libraries which apply universally to all Kotlin test targets.
     *
     * @receiver Project to which the dependency should be applied.
     * @return Current project, for chaining.
     */
    internal fun Project.installCommonTestLibs(): Project {
        return installCommonLibs().install(
            Libraries.TEST,
        )
    }

    /**
     * Install the Elide Version Catalog into the receiving [Project].
     *
     * @receiver Project to which the version catalog should be installed.
     * @return Current project, for chaining.
     */
    internal fun Project.installVersionCatalog(): Project {
        return install(
            Tools.CATALOG,
        )
    }

    /**
     * Install KSP and the Elide KSP processor artifact into the receiving [Project].
     *
     * @receiver Project which should accept the new dependencies and plugin.
     * @return KSP execution task, or `null` if one could not be located.
     */
    internal fun Project.installElideProcessor() {
        installApplyKSP().installCommonLibs().install(
            // KSP processor (to produce app manifests)
            Tools.PROCESSOR,
        )
    }
}

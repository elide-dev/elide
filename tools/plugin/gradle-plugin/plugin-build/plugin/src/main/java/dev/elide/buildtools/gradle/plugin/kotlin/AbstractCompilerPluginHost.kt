package dev.elide.buildtools.gradle.plugin.kotlin

import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.cfg.ElideKotlinPluginsHandler
import dev.elide.buildtools.gradle.plugin.cfg.ElidePluginConfig.ELIDE_LIB_VERSION
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Host plugin for Gradle which installs a Kotlin compiler plugin provided by Elide.
 *
 * @param pluginId Kotlin plugin ID.
 * @param groupId Group ID of the artifact containing the compiler plugin.
 * @param artifactId Artifact ID of the compiler plugin.
 * @param version Plugin version to resolve.
 * @param O Concrete plugin options type.
 */
internal abstract class AbstractCompilerPluginHost<O : ElideKotlinPluginsHandler.PluginHandler> protected constructor (
    private val pluginId: String,
    private val groupId: String = defaultPluginGroup,
    private val artifactId: String = "$pluginId-plugin",
    private val version: String = ELIDE_LIB_VERSION,
) : KotlinCompilerPluginSupportPlugin {
    internal companion object {
        // Default plugin group.
        internal const val defaultPluginGroup: String = "dev.elide.tools.kotlin.plugin"
    }

    /** Plugin configuration, delivered via Gradle. */
    protected lateinit var config: ElideKotlinPluginsHandler

    /** Plugin-specific options. */
    protected lateinit var options: O

    /** @inheritDoc */
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    /** @inheritDoc */
    override fun getCompilerPluginId(): String = pluginId

    /** @inheritDoc */
    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = groupId,
        artifactId = artifactId,
        version = version,
    )

    /** @inheritDoc */
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(ElideExtension::class.java)
        config = extension.kotlinPluginOptions
        options = resolve(config)

        return project.provider {
            configure(extension, project)
        }
    }

    /**
     * Resolve plugin-specific configuration blocks from the provided [config] handler.
     *
     * @param config Gradle script configuration for Elide Kotlin plugins.
     * @return Plugin-specific configuration.
     */
    abstract fun resolve(config: ElideKotlinPluginsHandler): O

    /**
     * Configure this plugin host based on the current [config] and [options], targeted with the provided [extension]
     * and Gradle [project].
     *
     * @param extension Elide Gradle plugin extension.
     * @param project Target project.
     * @return List of plugin invocation options.
     */
    abstract fun configure(extension: ElideExtension, project: Project): List<SubpluginOption>
}

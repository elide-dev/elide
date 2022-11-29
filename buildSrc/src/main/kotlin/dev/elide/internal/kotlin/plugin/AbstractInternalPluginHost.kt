package dev.elide.internal.kotlin.plugin

import dev.elide.internal.ElideInternalPlugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/** Base host plugin class for Kotlin plugins provided by the Elide build. */
abstract class AbstractInternalPluginHost<O: ElideInternalPluginsExtension.PluginConfig> protected constructor (
  private val project: Project,
  private val pluginId: String,
  private val version: String,
  private val groupId: String = defaultPluginGroup,
  private val artifactId: String = "$pluginId-plugin",
) : KotlinCompilerPluginSupportPlugin {
  internal companion object {
    // Default plugin group.
    internal const val defaultPluginGroup: String = "dev.elide.tools.kotlin.plugin"
  }

  /** Plugin configuration, delivered via Gradle. */
  protected val config: ElideInternalPluginsExtension = project.extensions.getByType(
    ElideInternalPluginsExtension::class.java
  )

  /** Plugin-specific options. */
  protected val options: O by lazy { resolve(config) }

  /** @inheritDoc */
  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return options.enabled.get()
  }

  /** @inheritDoc */
  override fun getCompilerPluginId(): String {
    return pluginId
  }

  /** @inheritDoc */
  override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
    groupId = groupId,
    artifactId = artifactId,
    version = version,
  )

  /** @inheritDoc */
  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val extension = project.extensions.getByType(ElideInternalPlugin.ElideInternalExtension::class.java)
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
  abstract fun resolve(config: ElideInternalPluginsExtension): O

  /**
   * Configure this plugin host based on the current [config] and [options], targeted with the provided [extension]
   * and Gradle [project].
   *
   * @param extension Elide Gradle plugin extension.
   * @param project Target project.
   * @return List of plugin invocation options.
   */
  abstract fun configure(extension: ElideInternalPlugin.ElideInternalExtension, project: Project): List<SubpluginOption>
}

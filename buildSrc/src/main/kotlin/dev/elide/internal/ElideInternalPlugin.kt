package dev.elide.internal

import dev.elide.internal.kotlin.plugin.ElideInternalPluginsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Internal build plugin which applies Elide-provided Kotlin plugins. */
class ElideInternalPlugin : Plugin<Project> {
  /** @inheritDoc */
  override fun apply(target: Project) {
    target.extensions.create(ElideInternalExtension.EXTENSION_NAME, ElideInternalExtension::class.java)
  }

  /** Extension for internal build configuration. */
  abstract class ElideInternalExtension @Inject constructor (project: Project) {
    companion object {
      // Name of the extension within build scripts.
      const val EXTENSION_NAME = "elideInternal"
    }

    /** Library and tooling version. */
    public val version: AtomicReference<String> = AtomicReference(null)

    /** Kotlin plugin configuration. */
    public val kotlinPlugins: ElideInternalPluginsExtension = project.extensions.create(
      "kotlinPlugins",
      ElideInternalPluginsExtension::class.java,
    )
  }
}

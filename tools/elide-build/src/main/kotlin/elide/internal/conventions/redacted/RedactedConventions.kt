package elide.internal.conventions.redacted

import dev.zacsweers.redacted.gradle.RedactedPluginExtension
import org.gradle.api.Project

internal fun Project.configureRedactedPlugin() {
  extensions.getByType(RedactedPluginExtension::class.java).apply {
    enabled.set(true)
    replacementString.set("●●●●")
    redactedAnnotation.set("elide.annotations.data.Sensitive")
  }
}
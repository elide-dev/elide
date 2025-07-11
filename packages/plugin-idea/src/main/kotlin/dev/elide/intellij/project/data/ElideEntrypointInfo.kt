package dev.elide.intellij.project.data

import dev.elide.intellij.Constants

sealed interface ElideEntrypointInfo {
  val displayName: String
  val descriptiveName: String
  val baseCommand: String
  val value: String

  data class Script(
    override val displayName: String,
    override val value: String,
  ) : ElideEntrypointInfo {
    override val baseCommand: String = Constants.Commands.RUN
    override val descriptiveName: String = "'$displayName' script"
  }

  data class MainEntrypoint(
    override val value: String
  ) : ElideEntrypointInfo {
    override val displayName: String = stripQualifiedEntrypoint(value)
    override val baseCommand: String = Constants.Commands.RUN
    override val descriptiveName: String = "$displayName entrypoint"

    companion object {
      private fun stripQualifiedEntrypoint(value: String): String {
        // for paths, only display the filename and extension
        return value.substringAfterLast("/")
      }
    }
  }

  data class JvmMainEntrypoint(
    override val value: String,
  ) : ElideEntrypointInfo {
    override val displayName: String = stripQualifiedEntrypoint(value)
    override val baseCommand: String = Constants.Commands.RUN
    override val descriptiveName: String = "$displayName JVM entrypoint"

    companion object {
      private fun stripQualifiedEntrypoint(value: String): String {
        return value.substringAfterLast(".")
      }
    }
  }
}

val ElideEntrypointInfo.fullCommandLine: String
  get() = "$baseCommand $value"

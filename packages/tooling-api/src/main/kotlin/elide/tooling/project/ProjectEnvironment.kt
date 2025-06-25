package elide.tooling.project

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.runtime.EnvVar

/** Environment settings applied to the project. */
@JvmRecord @Serializable
public data class ProjectEnvironment private constructor(
  @Transient public val vars: Map<String, EnvVar> = sortedMapOf(),
) {
  public companion object {
    /** @return Project environment wrapping the provided [map] of env vars. */
    @JvmStatic public fun wrapping(map: Map<String, EnvVar>): ProjectEnvironment = ProjectEnvironment(vars = map)
  }
}

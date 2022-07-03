package elide.runtime.graalvm

import kotlinx.serialization.Serializable


/**
 * Runtime configuration for the GraalVM JavaScript engine.
 *
 * @param entry Entrypoint file which should cap the rendered script.
 * @param artifacts Artifacts to load for runtime use.
 */
@Serializable internal data class JsRuntimeConfig(
  val entry: String,
  val artifacts: List<JsRuntimeArtifact>,
) {
  /**
   * Defines a single artifact which is loaded for runtime use.
   *
   * @param name Name (path) of the artifact within the embedded runtime resource root.
   */
  @Serializable data class JsRuntimeArtifact(
    val name: String
  )
}

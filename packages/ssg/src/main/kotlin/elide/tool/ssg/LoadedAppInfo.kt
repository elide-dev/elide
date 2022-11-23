package elide.tool.ssg

import tools.elide.meta.AppManifest
import java.net.URL

/**
 * Info describing a loaded Elide application.
 *
 * @param target Path to the loaded JAR or HTTP server.
 * @param httpMode Whether to operate in HTTP mode.
 * @param classpath Classpath associated with the loaded JAR.
 * @param manifest Loaded application manifest.
 * @param params Original compiler parameters.
 * @param eligible Whether there are any pre-compilable endpoints.
 */
public data class LoadedAppInfo(
  val target: URL,
  val httpMode: Boolean,
  val classpath: String?,
  val manifest: AppManifest,
  val params: SiteCompilerParams,
  val eligible: Boolean,
)

package elide.tool.ssg

import io.micronaut.http.HttpRequest
import java.net.URL

/**
 * Describes an asset detected after parsing an HTML response.
 *
 * @param url Absolute URL of the asset.
 * @param request HTTP request which yielded the asset.
 * @param type Type of the asset.
 */
public data class DetectedArtifact(
  val url: URL,
  val request: HttpRequest<*>,
  val type: StaticContentReader.ArtifactType,
)

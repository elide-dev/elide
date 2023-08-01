/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.tool.ssg

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import tools.elide.meta.Endpoint
import tools.elide.meta.EndpointType
import java.net.URL
import java.nio.ByteBuffer
import java.util.SortedSet

/**
 * Internal payload for a single static site fragment, associated with the HTTP request that produced it.
 *
 * @param parent Parent endpoint which produced this fragment, if applicable.
 * @param explicitUrl Known explicit URL for this fragment; only available for crawled fragments.
 * @param expectedType Expected type; only available for crawled fragments.
 * @param request HTTP request which produced this site fragment.
 * @param endpoint HTTP endpoint specification for this site fragment.
 * @param response HTTP response produced for this fragment.
 * @param content Parsed data which should be saved as file content.
 * @param discovered Set of additionally-discovered transitive static fragment specs to resolve.
 */
public sealed class StaticFragment(
  internal open val parent: Endpoint? = null,
  internal open val explicitUrl: URL? = null,
  internal open val expectedType: EndpointType? = null,
  internal open val endpoint: Endpoint? = null,
  public val request: HttpRequest<*>,
  public val response: HttpResponse<*>,
  public val content: ByteBuffer,
  public val discovered: List<StaticFragmentSpec>,
) {
  /** @return Base path for this fragment (i.e. prefix path, if any). If none, an empty string is returned. */
  public abstract fun basePath(): String

  /** @return Tail path for this fragment (i.e. postfix path/file path). If none, an empty string is returned. */
  public abstract fun tailPath(): String?

  /** @return Types known to be produced by this endpoint, as simple MIME-type strings. */
  public abstract fun produces(): Set<String>

  /** @return Type of endpoint specified by this structure. */
  public abstract fun endpointType(): EndpointType

  /** [StaticFragment] which originates from an endpoint. */
  public class EndpointFragment(
    request: HttpRequest<*>,
    override val endpoint: Endpoint,
    response: HttpResponse<*>,
    content: ByteBuffer,
    discovered: List<StaticFragmentSpec>,
  ) : StaticFragment(
    request = request,
    endpoint = endpoint,
    content = content,
    response = response,
    discovered = discovered,
  ) {
    override fun basePath(): String = endpoint.base

    override fun tailPath(): String? = endpoint.tail.ifBlank { null }

    override fun produces(): SortedSet<String> = endpoint.producesList.toSortedSet()

    override fun endpointType(): EndpointType = endpoint.type
  }

  /** [StaticFragment] which originates from asset detection. */
  public class SynthesizedFragment(
    override val explicitUrl: URL,
    expectedType: EndpointType,
    request: HttpRequest<*>,
    response: HttpResponse<*>,
    content: ByteBuffer,
    discovered: List<StaticFragmentSpec>,
  ) : StaticFragment(
    parent = null,
    explicitUrl = explicitUrl,
    request = request,
    expectedType = expectedType,
    content = content,
    response = response,
    discovered = discovered,
  ) {
    override fun basePath(): String = ""

    override fun tailPath(): String {
      return explicitUrl.path
    }

    override fun produces(): Set<String> = when (expectedType) {
      EndpointType.PAGE -> sortedSetOf("text/html")
      EndpointType.API -> sortedSetOf("application/json")
      EndpointType.ASSET -> when {
        explicitUrl.path.endsWith(".css") -> sortedSetOf("text/css")
        explicitUrl.path.endsWith(".js") -> sortedSetOf("application/javascript")
        else -> emptySet()
      }
      else -> emptySet()
    }

    override fun endpointType(): EndpointType = expectedType ?: EndpointType.ENDPOINT_TYPE_UNSPECIFIED
  }

  public companion object {
    /** Create a [StaticFragment] from a known endpoint. */
    @JvmStatic public fun fromEndpoint(
      spec: StaticFragmentSpec,
      response: HttpResponse<*>,
      content: ByteBuffer,
      discovered: List<StaticFragmentSpec>,
    ): EndpointFragment = EndpointFragment(
      spec.request(),
      spec.endpoint(),
      response,
      content,
      discovered,
    )

    /** Create a [StaticFragment] from a detected (crawled) endpoint. */
    @JvmStatic public fun fromDetectedArtifact(
      artifact: DetectedArtifact,
      response: HttpResponse<*>,
      content: ByteBuffer,
      discovered: List<StaticFragmentSpec>,
    ): StaticFragment = SynthesizedFragment(
      explicitUrl = artifact.url,
      request = artifact.request,
      response = response,
      content = content,
      discovered = discovered,
      expectedType = when (artifact.type) {
        StaticContentReader.ArtifactType.IMAGE,
        StaticContentReader.ArtifactType.SCRIPT,
        StaticContentReader.ArtifactType.TEXT,
        StaticContentReader.ArtifactType.FONT,
        StaticContentReader.ArtifactType.STYLE,
        -> EndpointType.ASSET
      },
    )
  }
}

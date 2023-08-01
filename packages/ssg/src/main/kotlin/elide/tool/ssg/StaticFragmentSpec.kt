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
import tools.elide.meta.Endpoint

/**
 * Payload class for an expected site fragment.
 *
 * @param request Request generated for this endpoint.
 * @param endpoint Endpoint specification that generated this request.
 */
public sealed class StaticFragmentSpec(
  internal open val request: HttpRequest<*>,
  internal open val endpoint: Endpoint? = null,
) {
  public companion object {
    /** @return [StaticFragmentSpec] from a known endpoint. */
    @JvmStatic public fun fromEndpoint(
      request: HttpRequest<*>,
      endpoint: Endpoint,
    ): EndpointSpec = EndpointSpec(
      request = request,
      endpoint = endpoint,
    )

    /** @return [StaticFragmentSpec] from a known endpoint. */
    @JvmStatic public fun fromDetectedArtifact(
      request: HttpRequest<*>,
      artifact: DetectedArtifact,
    ): SynthesizedSpec = SynthesizedSpec(
      request,
      artifact,
    )
  }

  /** @return Generated request for this fragment. */
  public abstract fun request(): HttpRequest<*>

  /** @return Endpoint info, if available, or throws. */
  public abstract fun endpoint(): Endpoint

  /** Spec for a known endpoint, determined via application manifest. */
  public data class EndpointSpec(
    override val request: HttpRequest<*>,
    override val endpoint: Endpoint,
  ) : StaticFragmentSpec(request, endpoint) {
    override fun request(): HttpRequest<*> = request

    override fun endpoint(): Endpoint = endpoint
  }

  /** Spec for a known endpoint, determined via application manifest. */
  public data class SynthesizedSpec(
    override val request: HttpRequest<*>,
    internal val artifact: DetectedArtifact,
    internal val expectedType: StaticContentReader.ArtifactType? = null,
  ) : StaticFragmentSpec(request, null) {
    override fun request(): HttpRequest<*> = request

    override fun endpoint(): Endpoint = error(
      "Cannot acquire endpoint for synthesized spec"
    )

    internal companion object {
      /** Spawn a synthesized request spec from an HTTP request. */
      @JvmStatic internal fun fromRequest(
        base: HttpRequest<*>,
        request: HttpRequest<*>,
        expectedType: StaticContentReader.ArtifactType,
      ): SynthesizedSpec = SynthesizedSpec(
        request = request,
        artifact = DetectedArtifact(
          url = request.uri.toURL(),
          request = base,
          type = expectedType,
        ),
        expectedType = expectedType,
      )
    }
  }
}

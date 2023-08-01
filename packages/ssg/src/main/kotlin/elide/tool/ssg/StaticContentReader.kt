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
import java.nio.ByteBuffer

/**
 * # SSG: Static Content Reader
 *
 * Defines an interface for an object which is responsible for (1) converting HTTP responses to persisted string content
 * for a given SSG handler, and (2) parsing such content for references to discovered URLs which should be made eligible
 * for further processing.
 *
 * The static content reader is used by the [AppStaticCompiler] after a request has been resolved to an HTTP response.
 * Typically, the string body of the response is persisted to disk as the result.
 *
 * Some HTTP references in the resulting body may be eligible for further processing, so these are parsed as HTML (where
 * applicable), and then handed up to the [SiteCompiler] for further action.
 */
public interface StaticContentReader {

  /** Types of supported web artifacts for detection. */
  public enum class ArtifactType {
    /** A link to an image asset. */
    IMAGE,

    /** A link to a stylesheet asset. */
    STYLE,

    /** A link to a script asset. */
    SCRIPT,

    /** A link to a font asset. */
    FONT,

    /** A link to a generic text asset. */
    TEXT,
  }

  /**
   * Consume an HTTP response body to determine whether it should be eligible for further parsing; this is only the case
   * if the body is non-empty and contains HTML content.
   *
   * If the body should be parsed, `true` is returned as the first parameter, otherwise `false`. The second parameter
   * contains the extracted body content as a raw set of bytes.
   *
   * @param response Response to consume and determine parsing eligibility for.
   * @return Whether the body should be parsed, and a byte buffer of the extracted body content.
   */
  public fun consume(response: HttpResponse<ByteArray>): Pair<Boolean, ByteBuffer>

  /**
   * Parse an HTTP response body to discover any artifacts which should be made eligible for further processing; assets
   * supported for crawling are described by [ArtifactType].
   *
   * This method does not determine whether assets are eligible for actual download (i.e. they must also be same-origin
   * references, depending on setting). All matching asset type URLs are expected to be returned by this method.
   *
   * @param request HTTP request that produced this response.
   * @param response Response for which we are parsing body content.
   * @param content Raw body content from the response, which we should parse as HTML.
   * @return Detected artifacts to add to the processing queue.
   */
  public fun parse(request: HttpRequest<*>, response: HttpResponse<ByteArray>, content: ByteBuffer):
    List<DetectedArtifact>
}

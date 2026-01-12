/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js

import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # Fetch: Body Mixin
 *
 * Defines the Body mixin from the Fetch API specification, which provides methods for reading
 * the body of a Request or Response as various formats.
 *
 * From MDN:
 * "The Body mixin of the Fetch API represents the body of the response/request, allowing you to
 * declare what its content type is and how it should be handled."
 *
 * Both [FetchRequest] and [FetchResponse] implement this mixin to provide consistent body
 * reading capabilities.
 *
 * ## Why This Matters
 *
 * While `fetch()` works for outbound requests, server-side code receiving requests via the
 * fetch handler pattern (`export default { fetch(request) { ... } }`) needs to parse incoming
 * request bodies. Without `request.json()`, developers must manually read the body stream
 * and parse JSON, which is error-prone and inconsistent with the Fetch API spec.
 *
 * Similarly, `url.searchParams.get()` is essential for parsing query parameters in GET requests,
 * which is the primary way to pass data in REST APIs.
 *
 * &nbsp;
 *
 * ## Specification
 *
 * See also:
 * - [MDN: Body](https://developer.mozilla.org/en-US/docs/Web/API/Body)
 * - [Fetch Standard: Body mixin](https://fetch.spec.whatwg.org/#body-mixin)
 */
@API public interface FetchBody {
  /**
   * ## Body: body
   *
   * A [ReadableStream] of the body contents.
   *
   * From MDN:
   * "The body read-only property of the Request/Response interface contains a ReadableStream
   * with the body contents that have been added to the request/response."
   */
  @get:Polyglot public val body: ReadableStream?

  /**
   * ## Body: bodyUsed
   *
   * A boolean indicating whether the body has been read.
   *
   * From MDN:
   * "The bodyUsed read-only property of the Request/Response interface is a boolean value that
   * indicates whether the request/response body has been read yet."
   */
  @get:Polyglot public val bodyUsed: Boolean

  /**
   * ## Body: arrayBuffer()
   *
   * Returns a promise that resolves with an ArrayBuffer representation of the body.
   *
   * From MDN:
   * "The arrayBuffer() method of the Request/Response interface takes a Request/Response stream
   * and reads it to completion. It returns a promise that resolves with an ArrayBuffer."
   *
   * @return A JsPromise that resolves to an ArrayBuffer containing the body data.
   */
  @Polyglot public fun arrayBuffer(): JsPromise<Any>

  /**
   * ## Body: blob()
   *
   * Returns a promise that resolves with a Blob representation of the body.
   *
   * From MDN:
   * "The blob() method of the Request/Response interface takes a Request/Response stream and
   * reads it to completion. It returns a promise that resolves with a Blob."
   *
   * @return A JsPromise that resolves to a Blob containing the body data.
   */
  @Polyglot public fun blob(): JsPromise<Blob>

  /**
   * ## Body: formData()
   *
   * Returns a promise that resolves with a FormData representation of the body.
   *
   * From MDN:
   * "The formData() method of the Request/Response interface takes a Request/Response stream
   * and reads it to completion. It returns a promise that resolves with a FormData object."
   *
   * @return A JsPromise that resolves to a FormData object.
   */
  @Polyglot public fun formData(): JsPromise<Any>

  /**
   * ## Body: json()
   *
   * Returns a promise that resolves with the result of parsing the body text as JSON.
   *
   * From MDN:
   * "The json() method of the Request/Response interface takes a Request/Response stream and
   * reads it to completion. It returns a promise which resolves with the result of parsing
   * the body text as JSON."
   *
   * @return A JsPromise that resolves to the parsed JSON value.
   */
  @Polyglot public fun json(): JsPromise<Any>

  /**
   * ## Body: text()
   *
   * Returns a promise that resolves with a text representation of the body.
   *
   * From MDN:
   * "The text() method of the Request/Response interface takes a Request/Response stream and
   * reads it to completion. It returns a promise that resolves with a String."
   *
   * @return A JsPromise that resolves to a String containing the body text.
   */
  @Polyglot public fun text(): JsPromise<String>
}

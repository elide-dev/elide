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

package elide.runtime.intrinsics.js

import elide.runtime.intrinsics.js.err.TypeError
import elide.vm.annotations.Polyglot

/**
 * # JavaScript: `URL`
 *
 * The `URL` class is universally supported across JavaScript engines and browser implementations, and behaves similarly
 * in each case; it is used for parsing well-formed URLs and extracting their constituent parts.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * "The URL interface is used to parse, construct, normalize, and encode URLs. It works by providing properties which
 * allow you to easily read and modify the components of a URL. You normally create a new URL object by specifying the
 * URL as a string when calling its constructor, or by providing a relative URL and a base URL. You can then easily read
 * the parsed components of the URL or make changes to the URL. If a browser doesn't yet support the URL() constructor,
 * you can access a URL object using the Window interface's URL property. Be sure to check to see if any of your target
 * browsers require this to be prefixed."
 *
 * &nbsp;
 *
 * ## Specification compliance
 *
 * The `URL` constructor and object layout are defined as part of the
 * [WhatWG URL Specification](https://url.spec.whatwg.org/). Elide's implementation of `URL` is based on Java's
 * standard [`java.net.URI`](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/net/URI.html) class and
 * is (experimentally) compliant with the WhatWG URL Specification.
 *
 * ### Special behavior
 *
 * URLs are usable in various other places in the Web APIs, and also have a custom stringification function to make them
 * easy to use when printing or logging. Simply calling `.toString()` on a `URL` object produces a string with the
 * absolute URL.
 *
 * &nbsp;
 *
 * ### Further reading
 *
 * See also:
 * - [MDN: `URL`](https://developer.mozilla.org/en-US/docs/Web/API/URL)
 * - [WhatWG URL Specification](https://url.spec.whatwg.org/)
 * - [`java.net.URI` (JDK19)](https://docs.oracle.com/en/java/javase/19/docs/api/java.base/java/net/URI.html)
 *
 * ## Support for other standards
 *
 * `URL` objects are a common primitive used by other, higher-order JavaScript ecosystem standards. Most notably, `URL`
 * objects can be used with the Fetch API. URLs can be passed to the constructor for `Request`.
 */
public interface URL : java.io.Serializable {
  /**
   * ## URL: Constructors
   *
   * This interface defines the surface area of expected constructors for standard `URL` objects. According to spec, URL
   * objects must be constructable from a plain `string` or from another `URL`. Empty URLs cannot be constructed; if the
   * provided `string` is an invalid URL, it is rejected with an error.
   *
   * [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/URL) documents the combined constructor as:
   *
   * "The URL() constructor returns a newly created URL object representing the URL defined by the parameters. If the
   * given base URL or the resulting URL are not valid URLs, the JavaScript TypeError exception is thrown."
   */
  public interface URLConstructors {
    /**
     * ### Constructor: From [URL]
     *
     * Construct a copy of the provided `URL` object. Since the `URL` is already parsed, we can easily copy it without
     * validating, as it was validated when it was first constructed.
     *
     * Relative URLs are not allowed via this constructor.
     *
     * @see create to create a URL from a relative URL, with the `base` parameter.
     * @see create to create a URL from a string.
     * @param url URL to create a copy of.
     * @return Copied URL object.
     */
    @Polyglot public fun create(url: URL): URL

    /**
     * ### Constructor: From String
     *
     * Construct a new `URL` object from the provided `string`. If the provided `string` is not a valid URL, an error is
     * thrown. The resulting URL can be considered validated and well-formed.
     *
     * @see create To create a URL from another URL.
     * @param url String to create a structured/parsed URL from.
     * @return Parsed URL object.
     * @throws TypeError If the provided `string` is not a valid URL.
     */
    @Throws(TypeError::class)
    @Polyglot public fun create(url: String): URL

    /**
     * ### Constructor: From String, with Base URL as String
     *
     * Construct a new `URL` object from the provided `string`. If the provided `string` is not a valid URL, an error is
     * thrown. The resulting URL can be considered validated and well-formed.
     *
     * If [url] is relative, [base] is parsed first and used to resolve the effective absolute URL.
     *
     * @see create To create a URL from another URL.
     * @param url String to create a structured/parsed URL from.
     * @param base Base URL to use if [url] is relative.
     * @return Parsed URL object.
     * @throws TypeError If the provided `string` is not a valid URL.
     */
    @Throws(TypeError::class)
    @Polyglot public fun create(url: String, base: String): URL
  }

  /**
   * ## URL: Static Methods
   *
   * Describes the static API surface available on the standard `URL` constructor. These methods are available for class
   * execution without an object context.
   */
  public interface URLStaticMethods {
    /**
     * Create a new `URL` object which references the provided [File] or [Blob] object.
     *
     * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/createObjectURL):
     * "The URL.createObjectURL() static method creates a string containing a URL representing the object given in the
     * parameter. The URL lifetime is tied to the document in the window on which it was created. The new object URL
     * represents the specified File object or Blob object. To release an object URL, call revokeObjectURL()."
     *
     * @see createObjectURL to create a URL from a blob.
     * @see revokeObjectURL to revoke a created object URL.
     * @param file File to create a temporary URL reference for.
     * @return URL reference for the provided resource.
     */
    @Polyglot public fun createObjectURL(file: File): URL

    /**
     * Create a new `URL` object which references the provided [File] or [Blob] object.
     *
     * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/createObjectURL):
     * "The `URL.createObjectURL()` static method creates a string containing a URL representing the object given in the
     * parameter. The URL lifetime is tied to the document in the window on which it was created. The new object `URL`
     * represents the specified `File` object or `Blob` object. To release an object URL, call `revokeObjectURL()`."
     *
     * @see createObjectURL to create a URL from a file.
     * @see revokeObjectURL to revoke a created object URL.
     * @param blob Blob to create a temporary URL reference for.
     * @return URL reference for the provided resource.
     */
    @Polyglot public fun createObjectURL(blob: Blob): URL

    /**
     * Revoke a previously-issued temporary URL reference to a [File] or [Blob] object.
     *
     * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/revokeObjectURL):
     * "The `URL.revokeObjectURL()` static method releases an existing object `URL` which was previously created by
     * calling `URL.createObjectURL()`. Call this method when you've finished using an object `URL` to let the browser
     * know not to keep the reference to the file any longer."
     *
     * @see createObjectURL to create a URL from a file or blob.
     * @param url URL which was previously created via [createObjectURL], which should be revoked.
     */
    @Polyglot public fun revokeObjectURL(url: URL)
  }

  /**
   * ### URL: `hash`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/hash):
   * "The hash property of the URL interface is a string containing a '#' followed by the fragment identifier of the
   * URL. The fragment is not percent-decoded. If the URL does not have a fragment identifier, this property contains an
   * empty string — `""`."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-hash)
   * - MDN: [URL.hash](https://developer.mozilla.org/en-US/docs/Web/API/URL/hash)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.hash;
   * ← "#hello"
   * ```
   */
  @get:Polyglot public val hash: String

  /**
   * ### URL: `host`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/host):
   * "The host property of the URL interface is a string containing the host, that is the hostname, and then, if the
   * port of the URL is nonempty, a ':', followed by the port of the URL."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-host)
   * - MDN: [URL.host](https://developer.mozilla.org/en-US/docs/Web/API/URL/host)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * console.log(url.host);
   * "elide.dev"
   *
   * // `host` includes any non-standard port:
   * const url = new URL("https://elide.dev:123/?one=two#hello");
   * url.host;
   * ← "elide.dev:123"
   * ```
   */
  @get:Polyglot public val host: String

  /**
   * ### URL: `hostname`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/hostname):
   * "The hostname property of the URL interface is a string containing the domain name of the URL."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-hostname)
   * - MDN: [URL.hostname](https://developer.mozilla.org/en-US/docs/Web/API/URL/hostname)
   * - Differs from [host] because it never contains a port
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.hostname;
   * ← "elide.dev"
   * ```
   */
  @get:Polyglot public val hostname: String

  /**
   * ### URL: `href`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/href):
   * "The href property of the URL interface is a string containing the whole URL."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-href)
   * - MDN: [URL.href](https://developer.mozilla.org/en-US/docs/Web/API/URL/href)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.href;
   * ← "https://elide.dev/?one=two#hello"
   * ```
   */
  @get:Polyglot public val href: String

  /**
   * ### URL: `origin`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/origin):
   * "The origin read-only property of the URL interface returns a string containing the Unicode serialization of the
   * origin of the represented URL.
   *
   * The exact structure varies depending on the type of URL:
   * - For http or https URLs, the scheme followed by '://', followed by the domain, followed by ':', followed by the
   *   port (the default port, 80 and 443 respectively, if explicitly specified).
   * - For file: URLs, the value is browser dependent.
   * - For blob: URLs, the origin of the URL following blob: will be used. For example, "blob:https://mozilla.org" will
   *   be returned as "https://mozilla.org"."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Always read-only
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-origin)
   * - MDN: [URL.origin](https://developer.mozilla.org/en-US/docs/Web/API/URL/origin)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.origin;
   * ← "https://elide.dev"
   * ```
   */
  @get:Polyglot public val origin: String

  /**
   * ### URL: `password`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/password):
   * "The password property of the URL interface is a string containing the password specified before the domain name.
   * If it is set without first setting the username property, it silently fails."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - **Username property must always be set first**
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-password)
   * - MDN: [URL.password](https://developer.mozilla.org/en-US/docs/Web/API/URL/password)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://hello:cool@elide.dev/?one=two#hello");
   * url.password;
   * ← "cool"
   * ```
   */
  @get:Polyglot public val password: String

  /**
   * ### URL: `pathname`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/pathname):
   * "The pathname property of the URL interface represents a location in a hierarchical structure. It is a string
   * constructed from a list of path segments, each of which is prefixed by a / character. If the URL has no path
   * segments, the value of its pathname property will be the empty string."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-pathname)
   * - MDN: [URL.pathname](https://developer.mozilla.org/en-US/docs/Web/API/URL/pathname)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/hello?one=two#hello");
   * url.pathname;
   * ← "/hello"
   * ```
   */
  @get:Polyglot public val pathname: String

  /**
   * ### URL: `port`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/port):
   * "The port property of the URL interface is a string containing the port number of the URL."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-port)
   * - MDN: [URL.port](https://developer.mozilla.org/en-US/docs/Web/API/URL/port)
   *
   * #### Example value:
   * ```
   * // for a standard port:
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.port;
   * ← 443
   *
   * // for a non-standard port:
   * const url = new URL("https://elide.dev:123/?one=two#hello");
   * url.port;
   * ← 123
   * ```
   */
  @get:Polyglot public val port: Int?

  /**
   * ### URL: `protocol`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/protocol):
   * "The protocol property of the URL interface is a string representing the protocol scheme of the URL, including the
   * final ':'."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Always includes the final `:` character
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-protocol)
   * - MDN: [URL.protocol](https://developer.mozilla.org/en-US/docs/Web/API/URL/protocol)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.protocol;
   * ← "https:"
   * ```
   */
  @get:Polyglot public val protocol: String

  /**
   * ### URL: `search`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/search):
   * "The search property of the URL interface is a search string, also called a query string, that is a string
   * containing a '?' followed by the parameters of the URL."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - [searchParams] is usually a better alternative
   * - Always includes the initial `?` value
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-search)
   * - MDN: [URL.search](https://developer.mozilla.org/en-US/docs/Web/API/URL/search)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.search;
   * ← "?one=two"
   * ```
   *
   * @see searchParams for a more convenient way to access the query parameters.
   */
  @get:Polyglot public val search: String

  /**
   * ### URL: `searchParams`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/searchParams):
   * "The searchParams readonly property of the URL interface returns a URLSearchParams object allowing access to the
   * GET decoded query arguments contained in the URL."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Always read-only
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#interface-urlsearchparams)
   * - MDN: [URL.searchParams](https://developer.mozilla.org/en-US/docs/Web/API/URL/searchParams)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://elide.dev/?one=two#hello");
   * url.searchParams.get('one');
   * ← "two"
   * ```
   */
  @get:Polyglot public val searchParams: URLSearchParams

  /**
   * ### URL: `username`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/username):
   * "The username property of the URL interface is a string containing the username specified before the domain name."
   *
   * &nbsp;
   *
   * #### Notes & further reading
   * - Spec: [URL Standard](https://url.spec.whatwg.org/#dom-url-username)
   * - MDN: [URL.username](https://developer.mozilla.org/en-US/docs/Web/API/URL/username)
   *
   * #### Example value:
   * ```
   * const url = new URL("https://hello:cool@elide.dev/?one=two#hello");
   * url.username;
   * ← "hello"
   * ```
   */
  @get:Polyglot public val username: String

  /**
   * Convert the URL to a JSON-compatible string; in practice, the return result is the same value as [toString].
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/toJSON):
   * "The toJSON() method of the URL interface returns a string containing a serialized version of the URL, although in
   * practice it seems to have the same effect as URL.toString()."
   *
   * @return Absolute string URL.
   */
  @Polyglot public fun toJSON(): String

  /**
   * Generate a string representation of this `URL` object.
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/URL/toString):
   * "The URL.toString() stringifier method returns a string containing the whole URL. It is effectively a read-only
   * version of URL.href."
   *
   * @return Absolute string URL.
   */
  @Polyglot override fun toString(): String
}

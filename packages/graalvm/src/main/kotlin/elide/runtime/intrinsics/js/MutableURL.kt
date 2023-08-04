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

import elide.vm.annotations.Polyglot

/**
 * # JavaScript: `URL` (Mutable)
 *
 * The `URL` class is universally supported across JavaScript engines and browser implementations, and behaves similarly
 * in each case; it is used for parsing well-formed URLs and extracting their constituent parts. This interface extends
 * the built-in [URL] intrinsic interface with mutable properties, where supported by spec.
 *
 * &nbsp;
 *
 * ## Mutable properties
 *
 * Nearly all properties of `URL` objects are "mutable," meaning they can be assigned a new value. Under the hood, the
 * URL controlled by an Elide URL intrinsic is immutable, and modifications are atomic. From the point of view of the JS
 * guest code, the URL is mutable in-place, even though a new URL is created for each mutation.
 *
 * The following properties are **never** mutable, because they are read-only by spec:
 * - [origin]. This property is calculated from the current URL value.
 * - [searchParams]. This property is likewise calculated from the current URL value.
 *
 * @see URL for information about URL objects in general, both mutable and immutable, and a full documentation of fields
 *   and methods available on each URL object.
 */
public interface MutableURL : URL {
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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var hash: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var host: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var hostname: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var href: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var password: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var pathname: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var port: Int?

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var protocol: String

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
   * **Note:** This property is present on a mutable version of the `URL` interface.
   *
   * @see searchParams for a more convenient way to access the query parameters.
   */
  @get:Polyglot @set:Polyglot public override var search: String

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
   *
   * **Note:** This property is present on a mutable version of the `URL` interface.
   */
  @get:Polyglot @set:Polyglot public override var username: String
}

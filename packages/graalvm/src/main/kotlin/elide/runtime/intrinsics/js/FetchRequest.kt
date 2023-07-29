package elide.runtime.intrinsics.js

import elide.vm.annotations.Polyglot

/**
 * # Fetch: Request.
 *
 * Specifies the interface defined by the Fetch API Specification, "Request," which represents an HTTP request created
 * or received by the JavaScript runtime. Fetch requests model regular HTTP concepts such as the HTTP method, path, and
 * parameters, augmenting with structure from classes like [FetchHeaders] and [URLSearchParams].
 *
 * From MDN:
 * "The Request interface of the Fetch API represents a resource request. You can create a new Request object using the
 * `Request()` constructor, but you are more likely to encounter a Request object being returned as the result of
 * another API operation, such as a service worker `FetchEvent.request`."
 *
 * &nbsp;
 *
 * ## Use as execution inputs
 *
 * [FetchRequest]-shaped objects may be used with VM execution inputs in order to invoke a guest VM interface each time
 * a request is received ("server-style invocation"). There are multiple implementations of [FetchRequest] from a server
 * perspective.
 *
 * &nbsp;
 *
 * ## Use from a guest context
 *
 * Fetch requests can be created in applicable guest VM environments, such as JavaScript via the `Request(...)`
 * constructor, available globally. An instantiated `Request` can then be passed to `fetch` to initiate an HTTP request
 * (host permissions permitting).
 */
public interface FetchRequest {
  /** Default values applied to [FetchRequest] interfaces. */
  public object Defaults {
    /** Default `cache` value. */
    public const val DEFAULT_CACHE: String = "default"

    /** Default `credentials` value. */
    public const val DEFAULT_CREDENTIALS: String = "omit"

    /** Default `method` value. */
    public const val DEFAULT_METHOD: String = "GET"

    /** Default `mode` value. */
    public const val DEFAULT_MODE: String = "server"

    /** Default `priority` value. */
    public const val DEFAULT_PRIORITY: String = "auto"

    /** Default `redirect` value. */
    public const val DEFAULT_REDIRECT: String = "follow"

    /** Default `referrer` value. */
    public const val DEFAULT_REFERRER: String = "client"

    /** Default `referrerPolicy` value. */
    public const val DEFAULT_REFERRER_POLICY: String = "no-referrer"
  }

  /**
   * ## Request: Body.
   *
   * Specifies, if any, a [ReadableStream] which holds the body of a [FetchRequest]. Body data may only be provided when
   * the [method] for the request allows a body, such as `POST`, `PUT`, and so forth.
   *
   * From MDN:
   * "The read-only body property of the Request interface contains a ReadableStream with the body contents that have
   * been added to the request. Note that a request using the GET or HEAD method cannot have a body and null is returned
   * in these cases."
   *
   * See also: [MDN, Request.body](https://developer.mozilla.org/en-US/docs/Web/API/Request/body).
   */
  @get:Polyglot public val body: ReadableStream?

  /**
   * ## Request: Body usage.
   *
   * If the [body] for this request has already been consumed, this property must return `true`; if `false`, the [body]
   * is still buffered and may be queried.
   *
   * From MDN:
   * "The read-only bodyUsed property of the Request interface is a boolean value that indicates whether the request
   * body has been read yet."
   *
   * See also: [MDN, Request.bodyUsed](https://developer.mozilla.org/en-US/docs/Web/API/Request/bodyUsed).
   */
  @get:Polyglot public val bodyUsed: Boolean

  /**
   * ## Request: Caching.
   *
   * Specifies the cache mode for the request. The default value is `default`, which indicates that the browser or
   * executing engine should use sensible and safe defaults.
   *
   * From MDN:
   * "The cache read-only property of the Request interface contains the cache mode of the request. It controls how the
   * request will interact with the browser's HTTP cache."
   *
   * See also: [MDN, Request.cache](https://developer.mozilla.org/en-US/docs/Web/API/Request/cache).
   */
  @get:Polyglot public val cache: String get() = Defaults.DEFAULT_CACHE

  /**
   * ## Request: Credentials mode.
   *
   * Specifies whether user credentials should be sent with the request. The default value is `omit`, which indicates
   * that user credentials (or any other credentials) should be omitted. Server-side, there are no "default" rules for
   * authorization material present on a request; what you send is what is sent, so this property does nothing.
   *
   * From MDN:
   * "The credentials read-only property of the Request interface indicates whether the user agent should send or
   * receive cookies from the other domain in the case of cross-origin requests."
   *
   * See also: [MDN, Request.credentials](https://developer.mozilla.org/en-US/docs/Web/API/Request/credentials).
   */
  @get:Polyglot public val credentials: String get() = Defaults.DEFAULT_CREDENTIALS

  /**
   * ## Request: Destination.
   *
   * Describes the destination or use profile for the fetched data in a given fetch request/response cycle.
   *
   * From MDN:
   * "The destination read-only property of the Request interface returns a string describing the type of content being
   * requested. The string must be one of the [following values:] `audio`, `audioworklet`, `document`, `embed`, `font`,
   * `frame`, `iframe`, `image`, `manifest`, `object`, `paintworklet`, `report`, `script`, `sharedworker`, `style`,
   * `track`, `video`, `worker` or `xslt` strings, or the empty string, which is the default value."
   *
   * See also: [MDN, Request.destination](https://developer.mozilla.org/en-US/docs/Web/API/Request/destination).
   */
  @get:Polyglot public val destination: String

  /**
   * ## Request: Headers.
   *
   * Provides a typed view of fetch headers on top of this request, via [FetchHeaders], which behaves as a specialized
   * JavaScript-compatible multi-map. Multiple header values can be set per header key, if desired, which can be safely
   * combined into a comma-separated header value group upon render.
   *
   * From MDN:
   * "The headers read-only property of the Request interface contains the Headers object associated with the request."
   *
   * See also: [MDN, Request.headers](https://developer.mozilla.org/en-US/docs/Web/API/Request/headers).
   */
  @get:Polyglot public val headers: FetchHeaders

  /**
   * ## Request: Integrity.
   *
   * Provides a calculated integrity hash value which is expected to be found when calculating a similar value for any
   * data returned from a `Request` cycle. The `integrity` value is expected to be well-formed, with a prefix that
   * describes the algorithm used to calculate the hash.
   *
   * By default, no value is specified in a request constructed server-side. If a DOM request which yields a fetch
   * request has an integrity value specified, it is populated here.
   *
   * From MDN:
   * "The integrity read-only property of the Request interface contains the subresource integrity value of the
   * request."
   *
   * See also: [MDN, Request.integrity](https://developer.mozilla.org/en-US/docs/Web/API/Request/integrity).
   */
  @get:Polyglot public val integrity: String? get() = null

  /**
   * ## Request: Method.
   *
   * Provides the request HTTP method name associated with this request (defaulting to `GET`). This value may be any of
   * the standard HTTP method names, or a custom method name, as documented/applicable.
   *
   * From MDN:
   * "The method read-only property of the Request interface contains the request's method (GET, POST, etc.)"
   *
   * See also: [MDN, Request.method](https://developer.mozilla.org/en-US/docs/Web/API/Request/method).
   */
  @get:Polyglot public val method: String get() = Defaults.DEFAULT_METHOD

  /**
   * ## Request: Mode.
   *
   * Specifies the operating mode for this request, which is derived from the source of the request and applicable
   * security context. This property is typically not used server-side.
   *
   * From MDN:
   * "The mode read-only property of the Request interface contains the mode of the request (e.g., cors, no-cors,
   * same-origin, navigate or websocket.) This is used to determine if cross-origin requests lead to valid responses,
   * and which properties of the response are readable."
   *
   * See also: [MDN, Request.mode](https://developer.mozilla.org/en-US/docs/Web/API/Request/mode).
   */
  @get:Polyglot public val mode: String get() = Defaults.DEFAULT_MODE

  /**
   * ## Request: Priority.
   *
   * Specifies the operating priority for this request, where applicable/supported. This property gives the developer a
   * chance to express the relative priority of a given request, or to understand the estimated priority for an incoming
   * request.
   *
   * From MDN:
   * "The priority read-only property of the Request interface contains the hinted priority of the request relative to
   * other requests."
   *
   * See also: [MDN, Request.priority](https://developer.mozilla.org/en-US/docs/Web/API/Request/priority).
   */
  @get:Polyglot public val priority: String get() = Defaults.DEFAULT_PRIORITY

  /**
   * ## Request: Redirects.
   *
   * Describes how redirects are to be handled in response to this request. Only makes sense when expressed on an
   * outbound fetch request instance.
   *
   * From MDN:
   * "The redirect read-only property of the Request interface contains the mode for how redirects are handled."
   *
   * See also: [MDN, Request.redirect](https://developer.mozilla.org/en-US/docs/Web/API/Request/redirect).
   */
  @get:Polyglot public val redirect: String get() = Defaults.DEFAULT_REDIRECT

  /**
   * ## Request: Referrer.
   *
   * Indicates the referrer value for an outgoing request in a browser. Typically not used server-side.
   *
   * From MDN:
   * "The referrer read-only property of the Request interface is set by the user agent to be the referrer of the
   * Request. (e.g., client, no-referrer, or a URL.)"
   *
   * See also: [MDN, Request.referrer](https://developer.mozilla.org/en-US/docs/Web/API/Request/referrer).
   */
  @get:Polyglot public val referrer: String? get() = Defaults.DEFAULT_REFERRER

  /**
   * ## Request: Referrer Policy.
   *
   * Indicates the referrer-policy which is active for this request. Typically not used server-side.
   *
   * From MDN:
   * "The referrerPolicy read-only property of the Request interface returns the referrer policy, which governs what
   * referrer information, sent in the Referer header, should be included with the request."
   *
   * See also: [MDN, Request.referrerPolicy](https://developer.mozilla.org/en-US/docs/Web/API/Request/referrerPolicy).
   */
  @get:Polyglot public val referrerPolicy: String get() = Defaults.DEFAULT_REFERRER_POLICY

  /**
   * ## Request: URL.
   *
   * Indicates the request URL, to the best of operating runtime knowledge. In all instances, an absolute string URL is
   * expected as the return result.
   *
   * From MDN:
   * "The url read-only property of the Request interface contains the URL of the request."
   *
   * See also: [MDN, Request.url](https://developer.mozilla.org/en-US/docs/Web/API/Request/url).
   */
  @get:Polyglot public val url: String
}

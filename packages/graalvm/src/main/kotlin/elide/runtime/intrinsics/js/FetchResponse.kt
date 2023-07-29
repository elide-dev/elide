package elide.runtime.intrinsics.js

import elide.vm.annotations.Polyglot

/**
 * # Fetch: Response.
 *
 * Describes the structure of an HTTP response provided via the Fetch sub-system, which implements the WhatWG Fetch API
 * Standard, in both browser JavaScript engines and server-side JavaScript engines. The Fetch system and specification
 * are designed to make HTTP data fetches easy in JavaScript environments, and stand as a modern replacement for the
 * famous `XMLHttpRequest` API in browsers, and the `http`/`https` layers in NodeJS.
 *
 * Fetch responses model normal HTTP response structures, including a status code/status text, response headers, and a
 * response body, as applicable. Not all HTTP responses carry a response body. Responses can be used to model content
 * originating from a guest VM script (in the case of a generated or synthesized response, potentially in response to
 * input, such as an HTTP request), or used to model a response to originated requests, for example, when fetching data
 * from inside a VM and accessing the resulting response.
 *
 * From MDN:
 * "The Response interface of the Fetch API represents the response to a request. You can create a new Response object
 * using the Response() constructor, but you are more likely to encounter a Response object being returned as the result
 * of another API operation—for example, a service worker FetchEvent.respondWith, or a simple fetch()."
 *
 * &nbsp;
 *
 * ## Use as execution output
 *
 * The [FetchResponse] interface is used, along with the [FetchRequest] interface, to model server-style invocation for
 * the JavaScript VM. In this case, a [FetchResponse] instance is expected as the output for a VM execution run, which
 * is seeded with a [FetchRequest] instance provided by the outer host server runtime.
 *
 * Multiple concrete [FetchResponse] implementations exist; each originate from a given server implementation.
 *
 * &nbsp;
 *
 * ## Use from a guest context
 *
 * Responses can be created from within a JavaScript guest, or received from within a guest context when executing a
 * fetched [FetchRequest] (host permissions permitting).
 */
public interface FetchResponse {
  /** Default values applied to [FetchResponse] interfaces. */
  public object Defaults {
    /** Default value for `type`. */
    public const val DEFAULT_TYPE: String = "basic"

    /** Default value for `redirected`. */
    public const val DEFAULT_REDIRECTED: Boolean = false

    /** Default value for `status`. */
    public const val DEFAULT_STATUS: Int = 200

    /** Default value for `status` when creating a redirect. */
    public const val DEFAULT_STATUS_REDIRECT: Int = 302

    /** Default value for `statusText`. */
    public const val DEFAULT_STATUS_TEXT: String = "OK"

    /** Default value for `url`. */
    public const val DEFAULT_URL: String = ""
  }

  /**
   * ## Fetch Response: Factory.
   *
   * Describes the layout of constructors available to create new [FetchResponse] implementation objects, either via
   * host-side code or, where supported, via guest-side code. Some of these constructors are driven by spec requirements
   * whereas others are provided for host-side convenience. See individual constructor docs for more details.
   *
   * See also:
   * - Fetch constructors on MDN: https://developer.mozilla.org/en-US/docs/Web/API/Response/Response
   */
  public interface Factory<Impl> where Impl : FetchResponse {
    /**
     * ## Fetch Response: Error.
     *
     * Creates a new [FetchResponse] instance (of type [Impl]) which indicates a network-related error state.
     *
     * From MDN:
     * "The error() method of the Response interface returns a new Response object associated with a network error."
     *
     * See also: [MDN, Response.error](https://developer.mozilla.org/en-US/docs/Web/API/Response/error).
     */
    public fun error(): Impl

    /**
     * ## Fetch Response: Redirect.
     *
     * Creates a new [FetchResponse] instance (of type [Impl]) which redirects for a given pair of URLs.
     *
     * From MDN:
     * "The redirect() method of the Response interface returns a Response resulting in a redirect to the specified
     * URL."
     *
     * See also: [MDN, Response.redirect](https://developer.mozilla.org/en-US/docs/Web/API/Response/redirect).
     *
     * @param url URL which should ultimately be consulted (after applying this redirect) to produce a response.
     * @param status Status to provide within this redirect response. Defaults to `302`, by spec.
     * @return Redirect response instance.
     */
    public fun redirect(url: String, status: Int = Defaults.DEFAULT_STATUS_REDIRECT): Impl
  }

  /**
   * ## Response: Body.
   *
   * Provides access to a [ReadableStream] which expresses body data enclosed with this response, as applicable and
   * where supported; if no stream can be produced for this response, `null` may be returned.
   *
   * From MDN:
   * "The body read-only property of the Response interface is a ReadableStream of the body contents."
   *
   * See also: [MDN, Response.body](https://developer.mozilla.org/en-US/docs/Web/API/Response/body).
   */
  @get:Polyglot public val body: ReadableStream?

  /**
   * ## Response: Body status.
   *
   * Indicates whether the [ReadableStream] provided by [body] has been consumed for this request.
   *
   * From MDN:
   * "The bodyUsed read-only property of the Response interface is a boolean value that indicates whether the body has
   * been read yet."
   *
   * See also: [MDN, Response.bodyUsed](https://developer.mozilla.org/en-US/docs/Web/API/Response/bodyUsed).
   */
  @get:Polyglot public val bodyUsed: Boolean

  /**
   * ## Response: Headers.
   *
   * Provides a typed view of headers associated with this HTTP response, via [FetchHeaders], which behaves as a
   * specialized JavaScript-compatible multi-map. Multiple header values can be expressed per header key, if desired,
   * which can be safely combined into a comma-separated header value group upon render.
   *
   * From MDN:
   * "The headers read-only property of the Response interface contains the Headers object associated with the
   * response."
   *
   * See also: [MDN, Response.headers](https://developer.mozilla.org/en-US/docs/Web/API/Response/headers).
   */
  @get:Polyglot public val headers: FetchHeaders

  /**
   * ## Response: OK.
   *
   * Indicates whether this response terminated with an "ok" status. If the response terminated with an HTTP status
   * which was between `200` and `299`, inclusive, then this returns `true`. For statuses `400` and above, this value
   * is always `false`.
   *
   * From MDN:
   * "The ok read-only property of the Response interface contains a Boolean stating whether the response was successful
   * (status in the range 200-299) or not."
   *
   * See also: [MDN, Response.ok](https://developer.mozilla.org/en-US/docs/Web/API/Response/ok).
   */
  @get:Polyglot public val ok: Boolean get() = status in 200..299

  /**
   * ## Response: Redirection.
   *
   * Indicates whether this response was redirected.
   *
   * From MDN:
   * "The read-only redirected property of the Response interface indicates \[whether] the response is the result of a
   * request you made which was redirected."
   *
   * See also: [MDN, Response.redirected](https://developer.mozilla.org/en-US/docs/Web/API/Response/redirected).
   */
  @get:Polyglot public val redirected: Boolean get() = Defaults.DEFAULT_REDIRECTED

  /**
   * ## Response: Status.
   *
   * Provides the HTTP status code which this response terminated with. HTTP status codes are typically expressed from
   * the various HTTP specifications, although custom codes are possible.
   *
   * From MDN:
   * "The status read-only property of the Response interface contains the HTTP status codes of the response."
   *
   * See also: [MDN, Response.status](https://developer.mozilla.org/en-US/docs/Web/API/Response/status).
   */
  @get:Polyglot public val status: Int get() = Defaults.DEFAULT_STATUS

  /**
   * ## Response: Status text.
   *
   * Provides the terminal HTTP status text which corresponds with the terminal [status] code for this response.
   *
   * From MDN:
   * "The statusText read-only property of the Response interface contains the status message corresponding to the HTTP
   * status code in Response.status."
   *
   * See also: [MDN, Response.statusText](https://developer.mozilla.org/en-US/docs/Web/API/Response/statusText).
   */
  @get:Polyglot public val statusText: String get() = Defaults.DEFAULT_STATUS_TEXT

  /**
   * ## Response: Type.
   *
   * Provides the type of response expressed by this object. Available options:
   * - `basic`: Normal, same origin response, with all headers exposed except "Set-Cookie".
   * - `cors`: Response was received from a valid cross-origin request. Certain headers and the body may be accessed.
   * - `error`: Network error. No useful information describing the error is available. The Response's status is 0,
   *   headers are empty and immutable. This is the type for a Response obtained from Response.error().
   * - `opaque`: Response for "no-cors" request to cross-origin resource. Severely restricted.
   * - `opaqueredirect`: The fetch request was made with redirect: "manual". The Response's status is 0, headers are
   *    empty, body is null and trailer is empty.
   *
   * From MDN:
   * "The type read-only property of the Response interface contains the type of the response."
   *
   * See also: [MDN, Response.type](https://developer.mozilla.org/en-US/docs/Web/API/Response/type).
   */
  @get:Polyglot public val type: String get() = Defaults.DEFAULT_TYPE

  /**
   * ## Response: URL.
   *
   * Provides the URL which produced this response, in absolute form, at the final resting destination for the response
   * (factoring in redirects, as applicable).
   *
   * From MDN:
   * "The url read-only property of the Response interface contains the URL of the response. The value of the url
   * property will be the final URL obtained after any redirects."
   *
   * See also: [MDN, Response.url](https://developer.mozilla.org/en-US/docs/Web/API/Response/url).
   */
  @get:Polyglot public val url: String get() = Defaults.DEFAULT_URL
}

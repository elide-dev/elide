package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.annotations.core.Polyglot
import elide.runtime.gvm.internals.intrinsics.js.url.URLIntrinsic
import elide.runtime.intrinsics.js.*
import io.micronaut.http.HttpRequest
import kotlinx.coroutines.jdk9.awaitSingle
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.net.http.HttpRequest as JavaNetHttpRequest

/** Implements an intrinsic for the Fetch API `Request` object. */
internal class FetchRequestIntrinsic internal constructor (
  targetUrl: URLIntrinsic.URLValue,
  targetMethod: String = FetchRequest.Defaults.DEFAULT_METHOD,
  requestHeaders: FetchHeaders = FetchHeadersIntrinsic.empty(),
  private val bodyData: InputStream? = null,
) : FetchMutableRequest {
  /** TBD. */
  internal companion object Factory : FetchMutableRequest.RequestFactory<FetchMutableRequest> {
    /**
     * TBD.
     */
    @JvmStatic override fun forRequest(request: JavaNetHttpRequest): FetchMutableRequest {
      return FetchRequestIntrinsic(
        targetUrl = URLIntrinsic.URLValue.fromURL(request.uri()),
        targetMethod = request.method(),
        requestHeaders = FetchHeaders.fromPairs(request.headers().map().flatMap {
          it.value.map { value ->
            it.key to value
          }
        }),
        ByteArrayInputStream(
          runBlocking {
            request.bodyPublisher().orElse(null)?.awaitSingle()?.array() ?: ByteArray(0)
          }
        )
      )
    }

    /**
     * TBD.
     */
    @JvmStatic override fun forRequest(request: HttpRequest<*>): FetchMutableRequest {
      return FetchRequestIntrinsic(
        targetUrl = URLIntrinsic.URLValue.fromURL(request.uri),
        targetMethod = request.method.name,
        requestHeaders = FetchHeaders.fromPairs(request.headers.asMap().entries.flatMap {
          it.value.map { value ->
            it.key to value
          }
        }),
        bodyData = request.getBody(InputStream::class.java).orElse(null),
      )
    }
  }

  /** Construct a new `Request` from a plain string URL. */
  @Polyglot constructor (url: String) : this(targetUrl = URLIntrinsic.URLValue.fromString(url))

  /** Construct a new `Request` from a Fetch API spec `URL` object. */
  @Polyglot constructor (url: URLIntrinsic.URLValue) : this(targetUrl = url)

  /** Construct a new `Request` from a Java Fetch API spec `URL` object. */
  constructor (url: URL) : this(targetUrl = URLIntrinsic.URLValue.fromString(url.toString()))

  /** Construct a new `Request` from another request (i.e. make a mutable or non-mutable copy). */
  @Polyglot constructor (request: FetchRequest) : this (
    targetUrl = URLIntrinsic.URLValue.fromString(request.url),
    targetMethod = request.method,
    requestHeaders = request.headers,
  )

  // Atomic indicator of whether the request body has been consumed.
  private val bodyConsumed: AtomicBoolean = AtomicBoolean(false)

  // Target URL to be fetched.
  private val targetUrl: AtomicReference<URLIntrinsic.URLValue> = AtomicReference(targetUrl)

  // Target HTTP method to invoke.
  private val targetMethod: AtomicReference<String> = AtomicReference(targetMethod)

  // Request headers to enclose.
  private val requestHeaders: AtomicReference<FetchHeaders> = AtomicReference(requestHeaders)

  // -- Interface: Mutable HTTP Request -- //

  /** @inheritDoc */
  @get:Polyglot @set:Polyglot override var headers: FetchHeaders
    get() = requestHeaders.get()
    set(subject) = requestHeaders.set(subject)

  /** @inheritDoc */
  @get:Polyglot @set:Polyglot override var url: String
    get() = targetUrl.get().toString()
    set(newTarget) = targetUrl.set(URLIntrinsic.URLValue.fromString(newTarget))

  /** @inheritDoc */
  @get:Polyglot @set:Polyglot override var method: String
    get() = targetMethod.get()
    set(value) = targetMethod.set(value)

  // -- Interface: Immutable HTTP Request -- //

  /** @inheritDoc */
  @get:Polyglot override val body: ReadableStream? get() {
    if (bodyData == null) return null
    if (!bodyConsumed.get()) bodyConsumed.set(true)
    return ReadableStream.wrap(bodyData)
  }

  /** @inheritDoc */
  @get:Polyglot override val bodyUsed: Boolean get() = bodyConsumed.get()

  /** @inheritDoc */
  @get:Polyglot override val destination: String get() = targetUrl.get().toString()
}

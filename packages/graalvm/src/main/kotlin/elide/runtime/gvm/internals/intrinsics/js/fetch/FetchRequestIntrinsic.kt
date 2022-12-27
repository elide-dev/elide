package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Implements an intrinsic for the Fetch API `Request` object. */
internal class FetchRequestIntrinsic internal constructor (
  targetUrl: String,
  targetMethod: String = FetchRequest.Defaults.DEFAULT_METHOD,
  requestHeaders: FetchHeaders = FetchHeadersIntrinsic.empty(),
) : FetchMutableRequest {
  /** Construct a new `Request` from a plain string URL. */
  @Polyglot constructor (url: String) : this(targetUrl = url)

  /** Construct a new `Request` from a Fetch API spec `URL` object. */
  @Polyglot constructor (url: URL) : this(targetUrl = url.toString())

  /** Construct a new `Request` from another request (i.e. make a mutable or non-mutable copy). */
  @Polyglot constructor (request: FetchRequest) : this (
    targetUrl = request.url,
    targetMethod = request.method,
    requestHeaders = request.headers,
  )

  // Atomic indicator of whether the request body has been consumed.
  private val bodyConsumed: AtomicBoolean = AtomicBoolean(false)

  // Target URL to be fetched.
  private val targetUrl: AtomicReference<String> = AtomicReference(targetUrl)

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
    get() = targetUrl.get()
    set(newTarget) = targetUrl.set(newTarget)

  /** @inheritDoc */
  @get:Polyglot @set:Polyglot override var method: String
    get() = targetMethod.get()
    set(value) = targetMethod.set(value)

  // -- Interface: Immutable HTTP Request -- //

  /** @inheritDoc */
  @get:Polyglot override val body: ReadableStream get() = TODO("Not yet implemented")

  /** @inheritDoc */
  @get:Polyglot override val bodyUsed: Boolean get() = bodyConsumed.get()

  /** @inheritDoc */
  @get:Polyglot override val destination: String get() = targetUrl.get()
}

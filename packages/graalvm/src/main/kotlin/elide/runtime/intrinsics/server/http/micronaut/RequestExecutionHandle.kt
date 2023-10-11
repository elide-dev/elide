package elide.runtime.intrinsics.server.http.micronaut

import io.micronaut.core.type.Argument
import io.micronaut.core.type.ReturnType
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse

/**
 * Build a new [RequestExecutionHandle] that checks incoming arguments, providing type-safe invocations of the
 * specified [handle].
 *
 * Unlike with [executionHandle], handles created by this method do check for argument type and count: exactly one
 * argument must be provided on invocation, and it must be an [HttpRequest<*>][HttpRequest]; if the conditions are not
 * met, an [IllegalStateException] will be thrown.
 *
 * @param handle A function to be invoked by this handle.
 * @return A type-safe [RequestExecutionHandle] delegating to the provided [handle] function.
 */
internal inline fun requestExecutionHandle(
  crossinline handle: (HttpRequest<*>) -> HttpResponse<*>
): RequestExecutionHandle {
  return RequestExecutionHandle { args ->
    // unpack and validate arguments
    val request = args.singleOrNull() ?: error("Request handles require exactly one argument (found ${args.size}).")
    check(request is HttpRequest<*>) { "Request handles require an HTTP Request as argument, found $request" }

    // execute the lambda with the smart-cast request
    handle(request)
  }
}

/**
 * A specialized [LambdaExecutionHandle] implementation with pre-defined type information for HTTP request handlers,
 * allowing its use in a route builder. Micronaut will provide an [HttpRequest] with a raw string body for routes using
 * this type of handle.
 *
 * Note that this class does not provide any type safety, it is merely a convenience over manually specifying the
 * correct argument and return type metadata for Micronaut.
 *
 * To create new type-safe handles, use the [requestExecutionHandle] function.
 */
internal class RequestExecutionHandle(
  block: (arguments: Array<out Any?>) -> HttpResponse<*>
) : LambdaExecutionHandle<HttpResponse<*>>(
  returns = GENERIC_HANDLER_RETURN_TYPE,
  arguments = GENERIC_HANDLER_ARGUMENTS,
  block = block,
) {
  internal companion object {
    /**
     * A pre-computed array with a single argument: [HttpRequest<*>][HttpRequest], which Micronaut will provide with a
     * raw body that can be read by the handler.
     */
    private val GENERIC_HANDLER_ARGUMENTS: Array<Argument<*>> = arrayOf(
      Argument.of(HttpRequest::class.java, Argument.VOID),
    )

    /** A static, generic [ReturnType] for request handlers: [HttpResponse<*>][HttpResponse]. */
    private val GENERIC_HANDLER_RETURN_TYPE: ReturnType<HttpResponse<*>> = ReturnType.of(
      HttpResponse::class.java,
      Argument.VOID,
    )
  }
}
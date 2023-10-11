package elide.runtime.intrinsics.server.http.micronaut

import io.micronaut.core.type.Argument
import io.micronaut.core.type.ReturnType
import io.micronaut.inject.ExecutableMethod
import io.micronaut.inject.MethodExecutionHandle
import java.lang.reflect.Method

/**
 * Build a new [LambdaExecutionHandle] by inferring the return type metadata from the reified [type parameter][R], and
 * using the provided [arguments] information. The handle will delegate execution to the [block] argument.
 *
 * @param arguments Type information for the arguments consumed by the [block]; note that this does not provide any
 * guarantees about the actual arguments that will be passed on invocations.
 * @param block A block of code to be called when this handle is executed.
 *
 * @return A new [LambdaExecutionHandle] configured with the provided parameters.
 */
internal inline fun <reified R> executionHandle(
  arguments: Array<Argument<*>> = emptyArray(),
  noinline block: (arguments: Array<out Any?>) -> R,
): LambdaExecutionHandle<R> = LambdaExecutionHandle(
  returns = ReturnType.of(R::class.java),
  arguments = arguments,
  block = block,
)

/**
 * Build a new [LambdaExecutionHandle] from that [returns] a specific type and accepts the provided [arguments]. The
 * handle will delegate execution to the [block] argument.
 *
 * @param returns The type of value returned by this handle, must match the generic [R] type parameter.
 * @param arguments Type information for the arguments consumed by the [block]; note that this does not provide any
 * guarantees about the actual arguments that will be passed on invocations.
 * @param block A block of code to be called when this handle is executed.
 *
 * @return A new [LambdaExecutionHandle] configured with the provided parameters.
 */
internal fun <R> executionHandle(
  returns: ReturnType<R>,
  arguments: Array<Argument<*>> = emptyArray(),
  block: (arguments: Array<out Any?>) -> R,
): LambdaExecutionHandle<R> = LambdaExecutionHandle(
  returns = returns,
  arguments = arguments,
  block = block,
)

/**
 * A [MethodExecutionHandle] and [ExecutableMethod] implementation meant to be used with anonymous (lambda) functions.
 *
 * This class is meant for use with Elide's guest code engines to allow building Micronaut routes without explicit
 * controller types and annotations.
 *
 * Note that this implementation provides no runtime guarantees about the arguments passed to the anonymous function,
 * the [arguments] array is required by Micronaut's internal mechanisms in order to build the pipeline, and it is not
 * used to validate incoming arguments. The same applies to the return type information.
 *
 * Use the [executionHandle] top-level functions to construct new instances with inferred parameters and return types.
 */
internal open class LambdaExecutionHandle<R>(
  private val returns: ReturnType<R>,
  private val arguments: Array<Argument<*>>,
  private val block: (arguments: Array<out Any?>) -> R,
) : MethodExecutionHandle<Any, R>, ExecutableMethod<Any, R> {
  override fun getDeclaringType(): Class<Any> = STUBBED_DECLARING_TYPE
  override fun getMethodName(): String = STUBBED_METHOD_NAME
  override fun getArguments(): Array<Argument<*>> = arguments
  override fun getReturnType(): ReturnType<R> = returns
  override fun getExecutableMethod(): ExecutableMethod<Any, R> = this
  override fun getTargetMethod(): Method? = null
  override fun getTarget(): Any? = null

  override fun invoke(vararg arguments: Any?): R {
    // delegate to the user lambda
    return block(arguments)
  }

  override fun invoke(instance: Any?, vararg arguments: Any?): R {
    // delegate to the anonymous 'invoke' call
    return invoke(arguments = arguments)
  }

  internal companion object {
    /** A placeholder name for the method represented by the execution handle. */
    private const val STUBBED_METHOD_NAME: String = "anonymous_lambda"

    /** A placeholder class to be used as "declaring type" for the execution handle. */
    private val STUBBED_DECLARING_TYPE: Class<Any> = Any::class.java
  }
}
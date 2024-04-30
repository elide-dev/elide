package elide.embedded.native

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.assertEquals

/**
 * Execute the provided [block] inside a confined [Arena], safely closing it in case of an exception, and returning
 * the result of the invocation.
 */
inline fun <R> withArena(block: Arena.() -> R): R {
  return Arena.ofConfined().use(block)
}

/** Allocate a [MemorySegment] to hold a [pointer value][ValueLayout.ADDRESS]. */
fun Arena.allocatePointer(): MemorySegment {
  return allocate(ValueLayout.ADDRESS)
}

/** Read this segment as a [pointer][ValueLayout.ADDRESS] at the specified [offset]. */
fun MemorySegment.pointerValue(offset: Long = 0): MemorySegment {
  return get(ValueLayout.ADDRESS, offset)
}

/**
 * Assert that the specified [op] results in an integer result code of 0, indicating success. The [Any] return type
 * for the lambda is required to work around Panama's untyped function handles.
 */
fun assertNativeSuccess(message: String? = null, op: () -> Any) {
  assertEquals(
    expected = 0,
    actual = op(),
    message = message,
  )
}

/**
 * Assert that the specified [op] results in an integer result code of 0, indicating success. The [Any] return type
 * for the lambda is required to work around Panama's untyped function handles.
 *
 * In addition to the initial assertion, a [callback wrapped as a native upcall][NativeCallbackAdapter.nativeCallback]
 * will be passed to the lambda, which will trigger a deferred assertion when invoked. The callback will be accept a
 * single integer argument and returns void.
 */
context(Arena) suspend fun assertNativeSuccessSuspending(message: String? = null, op: (MemorySegment) -> Any) {
  val result = CompletableDeferred<Int>()
  val callback = NativeCallbackAdapter.nativeCallback { result.complete(it) }

  assertEquals(
    expected = 0,
    actual = op(callback),
    message = "Expected deferred operation to be accepted${if (message != null) ": $message" else ""}",
  )

  assertEquals(
    expected = 0,
    actual = result.await(),
    message = "Expected deferred operation to complete successfully${if (message != null) ": $message" else ""}",
  )
}

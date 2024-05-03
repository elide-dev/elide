/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded.native

import java.lang.foreign.*
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import elide.embedded.native.NativeCallbackAdapter.Companion.nativeCallback

/**
 * An adapter used to create Panama upcalls for Kotlin lambas. The wrapped [call] is executed by the [invoke] method.
 * Use the [nativeCallback] factory to obtain a pointer to an upcall stub which can be used to call the wrapped
 * lambda.
 *
 * ```kotlin
 * withArena {
 *   // prepare the upcall stub
 *   val callback: MemorySegment = nativeCallback { result -> println("callback invoked with code $result") }
 *
 *   // pass the stub to a native function
 *   myNativeFunctionHandle(callback)
 * }
 * ```
 */
internal class NativeCallbackAdapter private constructor(private val call: (Int) -> Unit) {
  /**
   * Invoke the wrapped [lambda][call] using the provided [result] code. This method is meant to be invoked via
   * reflection only, as part of an upcall stub.
   */
  fun invoke(result: Int) {
    call(result)
  }

  internal companion object {
    /**
     * Cached method handle for [invoke], obtained through reflection. Using [MethodHandle.bindTo] will create a new
     * instance, allowing this value to be reused.
     */
    private val baseHandle: MethodHandle by lazy {
      MethodHandles.lookup().unreflect(NativeCallbackAdapter::class.java.getMethod("invoke", Int::class.java))
    }

    /**
     * Allocate a new upcall stub wrapping the provided [block], which can be passed to native code as a function
     * pointer. Note that this function requires an [Arena] since a pointer needs to be allocated.
     *
     * ```kotlin
     * withArena {
     *   // prepare the upcall stub
     *   val callback: MemorySegment = nativeCallback { result -> println("callback invoked with code $result") }
     *
     *   // pass the stub to a native function
     *   myNativeFunctionHandle(callback)
     * }
     * ```
     */
    context(Arena) fun nativeCallback(block: (Int) -> Unit): MemorySegment {
      val boundHandle = baseHandle.bindTo(NativeCallbackAdapter(block))

      return Linker.nativeLinker().upcallStub(
        /*target = */boundHandle,
        /*function = */FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT),
        /*arena = */this@Arena,
      )
    }
  }
}

/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2

import org.graalvm.polyglot.Source
import java.util.concurrent.locks.ReentrantLock
import elide.runtime.core.EntrypointRegistry

/**
 * Manages thread-local or shared instances of handler stack types for HTTP intrinsics. Use [withStack] to access an
 * instance for the calling thread.
 *
 * Stack instances can be used to represent HTTP handling pipelines that dispatch guest code, requiring synchronization
 * to avoid concurrent context access. This class ensures no two concurrent threads access the same stack at once.
 *
 * If the [entrypointProvider] has no active record when [withStack] is called for the first time, the manager will
 * default to a shared stack instance guarded by a reentrant lock; otherwise, it will use thread-local stack instances
 * that are lazily initialized.
 */
public abstract class GuestHandlerStackManager<S> {
  private interface StackAccessor<S> {
    fun acquire(): S
    fun release(held: S)
  }

  /**
   * Uses per-thread stack instances via [ThreadLocal]. New instances are configured with [initializeStack] lazily on
   * [acquire].
   */
  private inner class ThreadLocalStackAccessor : StackAccessor<S> {
    private val localStack: ThreadLocal<S> = ThreadLocal()

    override fun acquire(): S {
      localStack.get()?.let { return it }

      val stack = newStack()
      localStack.set(stack)

      initializeStack(stack, entrypointProvider.acquire()!!)
      return stack
    }

    override fun release(held: S) {
      // noop
    }
  }

  /**
   * Uses a shared stack instance guarded by a reentrant lock. The instance is not passed to [initializeStack] as it
   * is configured by the code that owns the stack manager.
   */
  private inner class SharedStackAccessor : StackAccessor<S> {
    private val lock = ReentrantLock()
    val stack by lazy { newStack() }

    override fun acquire(): S {
      lock.lock()
      return stack
    }

    override fun release(held: S) {
      lock.unlock()
    }
  }

  /** Lazy accessor implementation, allows late registration of the entrypoint. */
  private val accessor by lazy {
    if (entrypointProvider.available) ThreadLocalStackAccessor() else SharedStackAccessor()
  }

  /** Entrypoint provider used to resolve the source handed to [initializeStack] in thread-local mode. */
  protected abstract val entrypointProvider: EntrypointRegistry

  /**
   * Initialize a [stack] instance in the current thread, given the current active [entrypoint]. Implementations can
   * use this method to re-evaluate the guest source and store thread-local values in the stack for later use.
   *
   * This method will only be called if the [entrypointProvider] has an available record.
   */
  protected abstract fun initializeStack(stack: S, entrypoint: Source)

  /**
   * Create a new unconfigured stack instance.
   *
   * Initialization is deferred until the instance is requested, at which point [initializeStack] will be called if
   * needed to configure the stack on the calling thread.
   */
  protected abstract fun newStack(): S

  /**
   * Acquire a stack instance for the current thread (or a shared one if local stacks are not allowed) and execute an
   * operation against it. If applicable, the instance is released after [block] finishes so it can be used by another
   * thread.
   */
  public fun <R> withStack(block: (S) -> R): R {
    val stack = accessor.acquire()
    return try {
      block(stack)
    } finally {
      accessor.release(stack)
    }
  }
}

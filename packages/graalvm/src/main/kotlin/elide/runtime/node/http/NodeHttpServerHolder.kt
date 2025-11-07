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

package elide.runtime.node.http

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.core.RuntimeLatch
import elide.runtime.exec.ContextAware
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.exec.PinnedContext
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.js.node.NodeHttpServerApplication
import elide.runtime.http.server.js.node.NodeHttpServerRequest
import elide.runtime.http.server.js.node.NodeHttpServerResponse
import elide.runtime.http.server.netty.HttpApplicationStack

internal class NodeHttpServerHolder(
  private val executor: ContextAwareExecutor,
  private val runtimeLatch: RuntimeLatch,
  private val entrypoint: Source
) : NodeHttpServerApplication(executor) {
  @Volatile private var stack: HttpApplicationStack? = null

  private val localInstance = ContextLocal<NodeHttpServerInstance>()
  private val state = AtomicInteger(STATE_IDLE)

  @ContextAware internal fun registerLocalInstance(instance: NodeHttpServerInstance) {
    // workaround for CLI entrypoint execution: currently the CLI does not use
    // the context-aware executor to evaluate sources, so we can't register here
    if (!executor.onDispatchThread) return
    executor.setContextLocal(localInstance, instance)
  }

  @ContextAware internal fun bind(options: HttpApplicationOptions) {
    if (!state.compareAndSet(STATE_IDLE, STATE_STARTING)) return
    val newStack = HttpApplicationStack.bind(this, options)
    stack = newStack

    runtimeLatch.retain()
    newStack.onClose.whenComplete { _, _ -> runtimeLatch.release() }

    state.set(STATE_STARTED)
    if (executor.onDispatchThread) localInstance.current()?.onStarted()
  }

  @ContextAware internal fun stop(): Boolean {
    if (!state.compareAndSet(STATE_STARTED, STATE_STOPPING)) return false

    val pin = PinnedContext.current()
    stack?.close()?.whenComplete { _, _ ->
      state.set(STATE_STOPPED)
      executor.submit(pin) { localInstance.current()?.onStopped() }
    } ?: return false

    stack = null
    return true
  }

  @ContextAware override fun dispatch(request: NodeHttpServerRequest, response: NodeHttpServerResponse) {
    resolveInstance().onCallReceived(request, response)
  }

  @ContextAware private fun resolveInstance(): NodeHttpServerInstance {
    localInstance.current()?.let { return it }
    Context.getCurrent().eval(entrypoint)
    return localInstance.current()
      ?: error("Internal error: expected a server instance to be registered after evaluating entrypoint")
  }

  private companion object {
    private const val STATE_IDLE = 0
    private const val STATE_STARTING = 1
    private const val STATE_STARTED = 2
    private const val STATE_STOPPING = 3
    private const val STATE_STOPPED = 4
  }
}

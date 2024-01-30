/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.impl

import java.util.TreeSet
import kotlinx.atomicfu.atomic
import elide.annotations.Context
import elide.annotations.Singleton
import elide.embedded.api.Capability
import elide.embedded.api.EmbeddedRuntime
import elide.embedded.api.EmbeddedRuntime.EmbeddedDispatcher
import elide.embedded.api.InstanceConfiguration
import elide.embedded.api.NativeCall

/**
 * # Embedded Runtime: V1 Implementation
 *
 * Implements access to an [EmbeddedRuntime] interface powered by Elide. Host applications are expected to initialize,
 * configure, and start the runtime, at which point applications can be installed, and requests can be dispatched to
 * them through this API.
 */
@Singleton @Context public class EmbeddedRuntimeImpl : EmbeddedRuntime {
  // Whether the embedded runtime has completed the initialization step.
  private val initialized = atomic(false)

  // Whether the embedded runtime has been configured.
  private val configured = atomic(false)

  // Whether the embedded runtime is currently running.
  private val running = atomic(false)

  // Set of capabilities declared by the host application.
  private val capabilities = TreeSet<Capability>()

  // Active configuration installed by the host application.
  private val activeConfig = atomic<InstanceConfiguration?>(null)

  override val isConfigured: Boolean get() = configured.value
  override val isRunning: Boolean get() = running.value

  private inline fun <R> requireConfigured(crossinline op: () -> R): R {
    require(configured.value) {
      "Embedded runtime is not configured"
    }
    return op.invoke()
  }

  private inline fun <R> requireNotConfigured(crossinline op: () -> R): R {
    require(!configured.value) {
      "Embedded runtime is already configured"
    }
    return op.invoke()
  }

  private inline fun <R> withActive(crossinline op: () -> R): R {
    require(running.value) {
      "Embedded runtime is not running"
    }
    return op.invoke()
  }

  override fun initialize() {
    require(!initialized.value) {
      "Cannot initialize embedded runtime more than once"
    }
    initialized.value = true
  }

  override fun enable(capability: Capability): Unit = requireNotConfigured {
    capabilities.add(capability)
  }

  override fun configure(config: InstanceConfiguration): Unit = requireNotConfigured {
    activeConfig.value = config
    configured.value = true
  }

  override fun start(): Unit = requireConfigured {
    running.value = true
  }

  override fun dispatcher(): EmbeddedDispatcher = withActive {
    object : EmbeddedDispatcher {
      override suspend fun handle(call: NativeCall) {
        TODO("Not yet implemented")
      }
    }
  }
}

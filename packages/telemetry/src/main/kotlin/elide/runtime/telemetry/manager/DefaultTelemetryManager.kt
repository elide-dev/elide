/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.telemetry.manager

import java.util.concurrent.Executors
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.telemetry.Event
import elide.runtime.telemetry.client.TelemetryClient

// Default implementation of `TelemetryManager`.
@Singleton
internal class DefaultTelemetryManager @Inject constructor (private val client: TelemetryClient) : TelemetryManager {
  private val exec = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("elide-telemetry-", 0).factory())
  private val dispatcher = exec.asCoroutineDispatcher()
  private val telemetryEnabled = atomic(true)

  override fun client(): TelemetryClient = client

  override fun enableTelemetry() {
    telemetryEnabled.compareAndSet(expect = false, update = true)
  }

  override fun disableTelemetry() {
    telemetryEnabled.compareAndSet(expect = true, update = false)
  }

  override fun <E : Event> deliver(event: E) {
    runBlocking(dispatcher) {
      if (telemetryEnabled.value) {
        client().deliver(event).await()
      }
    }
  }
}

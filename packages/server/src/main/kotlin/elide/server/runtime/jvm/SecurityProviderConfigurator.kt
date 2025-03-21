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

package elide.server.runtime.jvm

import java.util.concurrent.atomic.AtomicBoolean

/** Initializes JVM security providers at server startup. */
@Suppress("UtilityClassWithPublicConstructor")
public object SecurityProviderConfigurator {
  private val ready = AtomicBoolean(false)

  // Register security providers at JVM startup time.
  @JvmStatic @Synchronized private fun registerProviders() {
    // Security.insertProviderAt(
    //  org.bouncycastle.jce.provider.BouncyCastleProvider(),
    //  0,
    // )
  }

  /**
   * Initialize security providers available statically; this method is typically run at server startup.
   */
  @JvmStatic public fun initialize() {
    if (!ready()) {
      ready.compareAndSet(false, true)
      registerProviders()
    }
  }

  /**
   * Indicate whether security providers have initialized.
   */
  @JvmStatic public fun ready(): Boolean {
    return ready.get()
  }
}

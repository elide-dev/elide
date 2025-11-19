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
package elide.runtime.http.server

import java.util.concurrent.atomic.AtomicReference
import elide.annotations.Singleton
import elide.runtime.http.server.netty.HttpApplicationStack

@Singleton public class HttpServerEngine {
  public fun interface Provider {
    public fun bind(application: HttpApplication<CallContext>, options: HttpApplicationOptions): HttpApplicationStack
  }

  private val engineProvider = AtomicReference<Provider>()

  public fun use(provider: Provider) {
    check(engineProvider.compareAndSet(null, provider)) { "Engine already has a registered provider" }
  }

  public fun <C : CallContext> bind(
    application: HttpApplication<C>,
    options: HttpApplicationOptions
  ): HttpApplicationStack {
    @Suppress("UNCHECKED_CAST")
    return engineProvider.get()?.bind(application as HttpApplication<CallContext>, options)
      ?: HttpApplicationStack.bind(application, options)
  }
}

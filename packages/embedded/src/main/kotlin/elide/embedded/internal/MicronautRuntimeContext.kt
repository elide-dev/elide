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

package elide.embedded.internal

import io.micronaut.context.BeanContext
import io.micronaut.runtime.Micronaut
import org.graalvm.nativeimage.ImageInfo
import elide.embedded.*

/**
 * An [EmbeddedRuntimeContext] implementation backed by Micronaut's DI container.
 */
internal class MicronautRuntimeContext private constructor(
  override val configuration: EmbeddedConfiguration,
  private val beanContext: BeanContext,
) : EmbeddedRuntimeContext {
  override val appRegistry: EmbeddedAppRegistry by lazyBean()
  override val dispatcher: EmbeddedCallDispatcher by lazyBean()
  override val codec: EmbeddedCallCodec by lazyBean()

  private inline fun <reified T> lazyBean(): Lazy<T> {
    return lazy { beanContext.getBean(T::class.java) }
  }

  internal companion object {
    /** Environment for the embedded runtime. */
    private const val ENV_EMBEDDED = "embedded"

    /** Environment used when running from a GraalVM native image. */
    private const val ENV_NATIVE = "native"

    /** Environment used when running in JVM mode (e.g. tests). */
    private const val ENV_JVM = "jvm"

    /** Resolve the most specific environment (excluding the "embedded") in which the runtime is executing. */
    private fun resolveEnvironment(): String {
      if (ImageInfo.inImageCode()) return ENV_NATIVE
      return ENV_JVM
    }

    /**
     * Create and start a new Micronaut context with the specified configuration. The [config] object will be made
     * available as a singleton bean.
     */
    internal fun create(config: EmbeddedConfiguration): MicronautRuntimeContext {
      val appContext = Micronaut.build()
        .banner(false)
        .deduceEnvironment(false)
        .environments(ENV_EMBEDDED, resolveEnvironment())
        .singletons(config)
        .start()

      return MicronautRuntimeContext(config, appContext)
    }
  }
}

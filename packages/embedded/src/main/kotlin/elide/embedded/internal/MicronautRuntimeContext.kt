package elide.embedded.internal

import io.micronaut.context.BeanContext
import io.micronaut.runtime.Micronaut
import org.graalvm.nativeimage.ImageInfo
import elide.embedded.EmbeddedConfiguration
import elide.embedded.EmbeddedAppRegistry
import elide.embedded.EmbeddedRuntimeContext

/**
 * An [EmbeddedRuntimeContext] implementation backed by Micronaut's DI container.
 */
internal class MicronautRuntimeContext private constructor(
  override val configuration: EmbeddedConfiguration,
  private val beanContext: BeanContext,
) : EmbeddedRuntimeContext {
  override val appRegistry: EmbeddedAppRegistry by lazy { beanContext.getBean(EmbeddedAppRegistry::class.java) }

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
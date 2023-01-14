@file:Suppress("UnstableApiUsage")

package dev.elide.build.jvm.toolchain

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.jvm.toolchain.JavaToolchainResolverRegistry
import javax.inject.Inject

/** Plugin for GraalVM toolchain auto-provisioning. */
abstract class GraalVMToolchainPlugin : Plugin<Settings> {
  @Inject protected abstract fun getToolchainResolverRegistry(): JavaToolchainResolverRegistry

  /** @inheritDoc */
  override fun apply(settings: Settings) {
    settings.extensions.findByType(GraalVMToolchainExtension::class.java)
      ?: settings.extensions.create("elide-graalvm-toolchain", GraalVMToolchainExtension::class.java)
    settings.plugins.apply("jvm-toolchain-management")
    val registry = getToolchainResolverRegistry()
    registry.register(GraalVMToolchainResolver::class.java)
  }
}

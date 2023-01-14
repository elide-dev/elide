@file:Suppress("UnstableApiUsage")

package dev.elide.build.jvm.toolchain

import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import java.net.URI
import java.util.*
import javax.inject.Inject

/** Resolves/auto-provisions GraalVM toolchains. */
abstract class GraalVMToolchainResolver @Inject constructor (
  private val settings: Settings
) : JavaToolchainResolver {
  /** @inheritDoc */
  override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
    val ext = settings.extensions.findByType(GraalVMToolchainExtension::class.java)
      ?: return Optional.empty()

    return Optional.of(JavaToolchainDownload {
      ext.renderDownloadUrl().toURI()
    })
  }
}

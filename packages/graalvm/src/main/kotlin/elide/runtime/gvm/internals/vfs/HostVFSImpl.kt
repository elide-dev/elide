/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.vfs

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestIOConfiguration

/**
 * # VFS: Host.
 *
 * Coming soon.
 */
@Requires(property = "elide.gvm.vfs.enabled", value = "true")
@Requires(property = "elide.gvm.vfs.mode", value = "HOST")
internal class HostVFSImpl private constructor (
  config: EffectiveGuestVFSConfig,
  backing: FileSystem,
) : AbstractDelegateVFS<HostVFSImpl>(config, backing) {
  /**
   * Private constructor.
   *
   * @param config Effective VFS configuration to apply and enforce.
   */
  private constructor (
    config: EffectiveGuestVFSConfig,
  ) : this (
    config,
    FileSystems.getDefault(),
  )

  /**
   * ## Host VFS: Builder.
   *
   * Coming soon.
   */
  @Suppress("unused") internal data class Builder (
    override var readOnly: Boolean = GuestVFSPolicy.DEFAULT_READ_ONLY,
    override var root: String = ROOT_SYSTEM_DEFAULT,
    override var policy: GuestVFSPolicy = GuestVFSPolicy.DEFAULTS,
    override var workingDirectory: String = DEFAULT_CWD,
    override var caseSensitive: Boolean = GuestIOConfiguration.DEFAULT_CASE_SENSITIVE,
    override var enableSymlinks: Boolean = GuestIOConfiguration.DEFAULT_SYMLINKS,
  ) : VFSBuilder<HostVFSImpl> {
    /** Factory for creating new [Builder] instances. */
    companion object BuilderFactory : VFSBuilderFactory<HostVFSImpl, Builder> {
      /** Whether to default to using temp-space. */
      const val DEFAULT_USE_TEMP = false

      /** @inheritDoc */
      override fun newBuilder(): Builder = Builder()

      /** @inheritDoc */
      override fun newBuilder(builder: Builder): Builder = Builder().apply {
        readOnly = builder.readOnly
        root = builder.root
        policy = builder.policy
        workingDirectory = builder.workingDirectory
        caseSensitive = builder.caseSensitive
        enableSymlinks = builder.enableSymlinks
      }
    }

    /** @inheritDoc */
    override fun build(): HostVFSImpl {
      return HostVFSImpl(EffectiveGuestVFSConfig(
        readOnly = readOnly,
        root = root,
        policy = policy,
        workingDirectory = workingDirectory,
        caseSensitive = caseSensitive,
        supportsSymbolicLinks = enableSymlinks,
        bundle = emptyList(),
      ))
    }
  }

  /**
   * ## Host VFS: Factory.
   *
   * Coming soon.
   */
  internal companion object HostVFSFactory : VFSFactory<HostVFSImpl, Builder> {
    /** @inheritDoc */
    override fun create(): HostVFSImpl = Builder.newBuilder().build()

    /** @inheritDoc */
    override fun create(configurator: Builder.() -> Unit): HostVFSImpl = Builder.newBuilder().apply {
      configurator.invoke(this)
    }.build()

    /** @inheritDoc */
    override fun create(config: EffectiveGuestVFSConfig): HostVFSImpl = HostVFSImpl(config)

    /** @inheritDoc */
    override fun create(builder: VFSBuilder<HostVFSImpl>): HostVFSImpl = builder.build()
  }

  /** Factory bridge from Micronaut-driven configuration to a host-based VFS implementation. */
  @Factory internal class HostVFSConfigurationFactory {
    /**
     * TBD.
     */
    @Bean @Singleton internal fun spawn(ioConfig: GuestIOConfiguration): HostVFSImpl {
      // convert to effective VFS config
      val config = withConfig(ioConfig)

      // prepare a builder
      return Builder.newBuilder().apply {
        readOnly = config.readOnly
        root = config.root
        policy = config.policy
        workingDirectory = config.workingDirectory
        caseSensitive = config.caseSensitive
        enableSymlinks = config.supportsSymbolicLinks
      }.build()
    }
  }

  // Logger.
  private val logging: Logger = Logging.of(HostVFSImpl::class)

  /** @inheritDoc */
  override fun logging(): Logger = logging

  /** @inheritDoc */
  override fun allowsHostFileAccess(): Boolean = true

  /** @inheritDoc */
  override fun allowsHostSocketAccess(): Boolean = true
}

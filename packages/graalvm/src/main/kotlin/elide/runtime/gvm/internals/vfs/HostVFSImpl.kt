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

package elide.runtime.gvm.internals.vfs

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import java.util.concurrent.atomic.AtomicReference
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestIOConfiguration
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl.BundleInfo
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl.VfsObjectInfo

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
  private var realCwd: AtomicReference<String> = AtomicReference(System.getProperty("user.dir")),
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
    override var deferred: Boolean = false,  // no-op
    override var bundleMapping: MutableMap<Int, BundleInfo> = mutableMapOf(),  // no-op
    override var registry: MutableMap<String, VfsObjectInfo> = mutableMapOf(),  // no-op
    override var readOnly: Boolean = GuestVFSPolicy.DEFAULT_READ_ONLY,
    override var root: String = ROOT_SYSTEM_DEFAULT,
    override var policy: GuestVFSPolicy = GuestVFSPolicy.DEFAULTS,
    override var workingDirectory: String = DEFAULT_CWD,
    override var caseSensitive: Boolean = GuestIOConfiguration.DEFAULT_CASE_SENSITIVE,
    override var enableSymlinks: Boolean = GuestIOConfiguration.DEFAULT_SYMLINKS,
    var hostScope: Path? = null,
  ) : VFSBuilder<HostVFSImpl> {
    /** Factory for creating new [Builder] instances. */
    companion object BuilderFactory : VFSBuilderFactory<HostVFSImpl, Builder> {
      /** Whether to default to using temp-space. */
      const val DEFAULT_USE_TEMP = false

      override fun newBuilder(): Builder = Builder()

      override fun newBuilder(builder: Builder): Builder = Builder().apply {
        readOnly = builder.readOnly
        root = builder.root
        policy = builder.policy
        workingDirectory = builder.workingDirectory
        caseSensitive = builder.caseSensitive
        enableSymlinks = builder.enableSymlinks
      }
    }

    /**
     * Set the host directory scope within which this file system should be allowed to operate.
     *
     * @param scope Path to resolve as the scope.
     * @return This builder.
     */
    fun setScope(scope: Path? = null): Builder = apply {
      this.hostScope = scope?.toAbsolutePath()?.toRealPath()
    }

    /**
     * Set the host directory scope within which this file system should be allowed to operate.
     *
     * @param scope Path string to resolve as the scope.
     * @return This builder.
     */
    fun setScope(scope: String): Builder = apply {
      this.hostScope = Path.of(scope).toAbsolutePath().toRealPath()
    }

    override fun build(): HostVFSImpl {
      val resolvedHostScope: Path? = hostScope?.let {
        val target = it.toFile()
        if (!target.exists()) {
          if (target.canWrite()) {
            target.mkdirs()
          } else throw IOException(
            "Cannot initialize host VFS with non-writable path"
          )
        }
        it
      }

      return HostVFSImpl(EffectiveGuestVFSConfig(
        readOnly = readOnly,
        root = root,
        policy = policy,
        workingDirectory = workingDirectory,
        caseSensitive = caseSensitive,
        supportsSymbolicLinks = enableSymlinks,
        bundle = emptyList(),
        scope = resolvedHostScope,
      ))
    }
  }

  /**
   * ## Host VFS: Factory.
   *
   * Coming soon.
   */
  internal companion object HostVFSFactory : VFSFactory<HostVFSImpl, Builder> {
    override fun create(): HostVFSImpl = Builder.newBuilder().build()

    override fun create(configurator: Builder.() -> Unit): HostVFSImpl = Builder.newBuilder().apply {
      configurator.invoke(this)
    }.build()

    override fun create(config: EffectiveGuestVFSConfig): HostVFSImpl = HostVFSImpl(config)

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

  // Base path for all host paths, if set.
  private val hostPath: Path? by lazy {
    config.scope?.toAbsolutePath()?.toRealPath()
  }

  // Logger.
  override val logging: Logger by lazy {
    Logging.of(HostVFSImpl::class)
  }

  override fun allowsHostFileAccess(): Boolean = true

  override fun allowsHostSocketAccess(): Boolean = true

  private fun Path.relativeFromBase(path: Path, trim: String? = null): Path {
    val pathAsString = path.toString()
    val cleaned = when {
      pathAsString == "/" -> path
      path.startsWith(this) -> path  // already scoped to host
      path.isAbsolute && trim != null -> if (path.startsWith(trim)) {
        Path.of(pathAsString.drop(trim.length))
      } else {
        val subject = path.toList()
        val base = Path.of(trim).toList()

        Path.of(path.asSequence().drop(
          subject.zip(base).takeWhile { (a, b) -> a == b }.size
        ).joinToString(File.separator))
      }

      path.isAbsolute -> resolve(Path.of(path.toString().drop(1)))
      else -> resolve(path)
    }
    return cleaned
  }

  private fun <R> maybeWithScopedPath(path: Path, op: (path: Path) -> R): R {
    val real = realCwd.get()
    return hostPath?.let {
      op(it.relativeFromBase(path, trim = real))
    } ?: op(path)
  }

  override fun checkPolicy(request: AccessRequest): AccessResponse = hostPath?.let {
    maybeWithScopedPath(request.path) {
      super.checkPolicy(request.copy(path = it))
    }
  } ?: super.checkPolicy(request)

  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) =
    maybeWithScopedPath(path) {
      super.checkAccess(it, modes, *linkOptions)
    }

  override fun readStream(path: Path, vararg options: OpenOption): InputStream = maybeWithScopedPath(path) {
    super.readStream(it, *options)
  }

  override fun writeStream(path: Path, vararg options: OpenOption): OutputStream = maybeWithScopedPath(path) {
    super.writeStream(it, *options)
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel = maybeWithScopedPath(path) {
    super.newByteChannel(it, options, *attrs)
  }

  override fun newDirectoryStream(
    dir: Path,
    filter: Filter<in Path>,
  ): DirectoryStream<Path> = maybeWithScopedPath(dir) {
    super.newDirectoryStream(it, filter)
  }

  override fun readAttributes(
    path: Path,
    attributes: String,
    vararg options: LinkOption,
  ): MutableMap<String, Any> = maybeWithScopedPath(path) {
    super.readAttributes(it, attributes, *options)
  }

  override fun setAttribute(
    path: Path,
    attribute: String,
    value: Any,
    vararg options: LinkOption,
  ) = maybeWithScopedPath(path) {
    super.setAttribute(it, attribute, value, *options)
  }

  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) = maybeWithScopedPath(dir) {
    super.createDirectory(it, *attrs)
  }

  override fun createLink(link: Path, existing: Path) {
    require(hostPath == null) { "Cannot create links within a scoped host VFS." }
    super.createLink(link, existing)
  }

  override fun createSymbolicLink(link: Path, target: Path, vararg attrs: FileAttribute<*>?) {
    require(hostPath == null) { "Cannot create links within a scoped host VFS." }
    super.createSymbolicLink(link, target, *attrs)
  }

  override fun readSymbolicLink(link: Path): Path = maybeWithScopedPath(link) {
    super.readSymbolicLink(it)
  }

  override fun delete(path: Path) = maybeWithScopedPath(path) {
    super.delete(it)
  }

  override fun copy(source: Path, target: Path, vararg options: CopyOption?) = maybeWithScopedPath(source) { src ->
    maybeWithScopedPath(target) { tgt ->
      super.copy(src, tgt, *options)
    }
  }

  override fun move(source: Path, target: Path, vararg options: CopyOption?) = maybeWithScopedPath(source) { src ->
    maybeWithScopedPath(target) { tgt ->
      super.move(src, tgt, *options)
    }
  }

  override fun getEncoding(path: Path): Charset = maybeWithScopedPath(path) {
    super.getEncoding(it)
  }

  override fun getMimeType(path: Path): String? = maybeWithScopedPath(path) {
    super.getMimeType(it)
  }

  override fun isSameFile(path1: Path, path2: Path, vararg options: LinkOption): Boolean = maybeWithScopedPath(path1) {
    maybeWithScopedPath(path2) { other ->
      super.isSameFile(it, other, *options)
    }
  }

  override fun setCurrentWorkingDirectory(currentWorkingDirectory: Path) {
    maybeWithScopedPath(currentWorkingDirectory) {
      super.setCurrentWorkingDirectory(it)
    }
  }

  override val host: Boolean get() = true
  override val compound: Boolean get() = false
  override val virtual: Boolean get() = false
  override val supportsSymlinks: Boolean get() = config.supportsSymbolicLinks
  override val writable: Boolean get() = !config.readOnly
  override val deletable: Boolean get() = writable
}

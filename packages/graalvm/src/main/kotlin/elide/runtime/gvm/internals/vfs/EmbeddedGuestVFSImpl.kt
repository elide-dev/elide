package elide.runtime.gvm.internals.vfs

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Feature
import com.google.common.jimfs.Jimfs
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestIOConfiguration
import elide.util.UUID
import elide.vfs.Filesystem
import io.micronaut.context.annotation.Bean
import java.io.File
import java.io.RandomAccessFile
import java.net.URI
import java.nio.file.FileSystem

/**
 * # VFS: Embedded.
 *
 * This VFS implementation uses an in-memory file-system, loaded from a VFS filesystem bundle file. Bundle files can be
 * plain tarballs, compressed tarballs, or they can make use of Elide's optimized production bundle format. In each case
 * the embedded VFS implementation will load the bundle, initialize a Jimfs file-system with the decoded and verified
 * contents, and then satisfy guest I/O requests with the resulting file-system.
 *
 * ## Bundle formats
 *
 * VFS bundle files can be plain tarballs, compressed tarballs, or they can make use of Elide's optimized production
 * bundle format. Plain or compressed tarballs are ideal for quick development cycles, and Elide's production bundle
 * format is optimal for production use.
 *
 * ### Plain/compressed tarballs
 *
 * Plain and compressed tarballs are supported by the embedded VFS implementation. The bundle file is expected to be a
 * regular TAR file, with a `.tar` extension, unless compression is in use, in which case the file extension is expected
 * to be `.tar.gz`. The tarball models the root of the file system.
 *
 * ### Elide's bundle format
 *
 * The bundle format used internally by Elide is also based on a plain (un-compressed) tarball, with a `.evfs` extension
 * and the following structure:
 *
 * - `metadata.cfb`: Compressed Flatbuffer with metadata about the enclosed file-system
 * - `fsdata.tar.lz4`: LZ4-compressed tarball containing the actual file-system data
 * - `artifacts.json.gz`: GZIP-compressed JSON file containing a list of artifacts used to build the bundle
 *
 * The `metadata.cfb` file explains the file tree to the runtime, and the `fsdata.tar.lz4` file contains the actual data
 * for each file. `artifacts.json.gz` is not used by the runtime, and is only present for easy debugging. Because a
 * bundle is a valid tarball, it can be extracted with standard tools, or in OS GUIs by renaming to the `.tar`
 * extension.
 *
 * @param config Effective VFS configuration which should be applied to this instance.
 * @param backing Backing file-system implementation which should receive allowable I/O calls.
 * @param tree File-system tree to use for this instance; only present if operating with an Elide-formatted bundle.
 * @param databag File-system data. Always a de-compressed tarball (either unpacked from an Elide bundle, or directly).
 */
internal class EmbeddedGuestVFSImpl private constructor (
  config: EffectiveGuestVFSConfig,
  backing: FileSystem,
  private val tree: Filesystem?,
  private val databag: RandomAccessFile?,
) : AbstractBackedGuestVFS<EmbeddedGuestVFSImpl>(config, backing) {
  // Logger.
  private val logging: Logger = Logging.of(EmbeddedGuestVFSImpl::class)

  /**
   * Private constructor.
   *
   * This constructor is called from the internal constructor which is provided with an [EffectiveGuestVFSConfig]. The
   * configuration is translated into a Jimfs configuration and then built into a backing file-system.
   *
   * @param name Name to use for the file-system target.
   * @param config Effective VFS configuration to apply.
   * @param fsConfig Generated Jimfs configuration, from [config].
   * @param tree Embedded VFS file-system metadata, loaded from the bundle file (as applicable).
   * @param databag Embedded VFS file-system data, loaded from the bundle file (as applicable).
   */
  private constructor(
    name: String,
    config: EffectiveGuestVFSConfig,
    fsConfig: Configuration,
    tree: Filesystem?,
    databag: RandomAccessFile?,
  ) : this (
    config,
    buildFs(name, fsConfig),
    tree,
    databag,
  )

  /**
   * Internal constructor: From configuration.
   *
   * This constructor is called by builders and factories ([Builder] and [Factory], precisely), to build a new VFS
   * instance.
   *
   * @param config Effective VFS configuration to apply.
   * @param tree Embedded VFS file-system metadata, loaded from the bundle file (as applicable).
   * @param databag Embedded VFS file-system data, loaded from the bundle file (as applicable).
   */
  internal constructor(
    config: EffectiveGuestVFSConfig,
    tree: Filesystem?,
    databag: RandomAccessFile?,
  ) : this (
    "elide-${UUID.random()}",
    config,
    config.buildFs(config).build(),
    tree,
    databag,
  )

  /**
   * Builder to configure and spawn new embedded VFS implementations.
   *
   * Builders can be filled out property-wise, or via builder methods.
   *
   * @param readOnly Whether the file-system should be considered read-only (regardless of backing read-only status).
   * @param caseSensitive Whether the file-system should be considered case-sensitive.
   * @param enableSymlinks Whether to enable support for symbolic links.
   * @param root Root directory of the file-system.
   * @param workingDirectory Working directory of the file-system.
   * @param policy Policy to apply to moderate guest I/O access to the file-system in question.
   * @param compression Compression mode to apply for the selected [bundle] file.
   * @param bundle Bundle file to load the file-system from.
   * @param path Path to the bundle file, as a regular host-filesystem path, or `classpath:`-prefixed URI for a resource
   *   which should be fetched from the host app class-path.
   * @param file File to load as the bundle-file, if one happens to be on hand already.
   */
  @Suppress("unused") internal data class Builder (
    override var readOnly: Boolean = true,
    override var caseSensitive: Boolean = true,
    override var enableSymlinks: Boolean = false,
    override var root: String = ROOT_SYSTEM_DEFAULT,
    override var workingDirectory: String = DEFAULT_CWD,
    override var policy: GuestVFSPolicy = GuestVFSPolicy.DEFAULTS,
    internal var compression: Compression = Compression.DEFAULT,
    internal var bundle: Pair<Filesystem, RandomAccessFile>? = null,
    internal var path: URI? = null,
    internal var file: File? = null,
  ) : VFSBuilder<EmbeddedGuestVFSImpl> {
    /** TBD. */
    internal companion object Factory : VFSBuilderFactory<EmbeddedGuestVFSImpl, Builder> {
      /** @inheritDoc */
      override fun newBuilder(): Builder = Builder()

      /** @inheritDoc */
      override fun newBuilder(builder: Builder): Builder = builder.copy()
    }

    /** @inheritDoc */
    override fun setReadOnly(readOnly: Boolean): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.readOnly = readOnly
      return this
    }

    /** @inheritDoc */
    override fun setCaseSensitive(caseSensitive: Boolean): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.caseSensitive = caseSensitive
      return this
    }

    /** @inheritDoc */
    override fun setEnableSymlinks(enableSymlinks: Boolean): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.enableSymlinks = enableSymlinks
      return this
    }

    /** @inheritDoc */
    override fun setRoot(root: String): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.root = root
      return this
    }

    /** @inheritDoc */
    override fun setWorkingDirectory(workingDirectory: String): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.workingDirectory = workingDirectory
      return this
    }

    /** @inheritDoc */
    override fun setPolicy(policy: GuestVFSPolicy): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.policy = policy
      return this
    }

    /**
     * Set the [compression] mode to use with the attached bundle, if known; if one is not provided, a sensible
     * heuristic will be applied to detect compression.
     *
     * @see compression to set this value as a property.
     * @param compression Compression mode to use.
     * @return This builder.
     */
    fun setCompression(compression: Compression): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.compression = compression
      return this
    }

    /**
     * Set the [bundle] to use directly (pre-loaded from some data source).
     *
     * Note that setting this property force-unsets [path] and [file].
     *
     * @see bundle to set this value as a property.
     * @param bundle [Filesystem] and [RandomAccessFile] pair to use as the bundle.
     * @return This builder.
     */
    fun setBundle(bundle: Pair<Filesystem, RandomAccessFile>): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.bundle = bundle
      this.path = null
      this.file = null
      return this
    }

    /**
     * Set the [path] to load the bundle file from; can be a regular file-path, or a `classpath:`-prefixed path to load
     * a resource from the host app classpath.
     *
     * Note that setting this property force-unsets [bundle] and [file].
     *
     * @see path to set this value as a property.
     * @param path URI to the bundle file to load.
     * @return This builder.
     */
    fun setBundlePath(path: URI): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.path = path
      this.bundle = null
      this.file = null
      return this
    }

    /**
     * Set the [file] to load the bundle data from; expected to be a valid and readable regular file, which is a
     * tarball, a compressed tar-ball, or a bundle in Elide's internal format.
     *
     * Note that setting this property force-unsets [bundle] and [path].
     *
     * @see file to set this value as a property.
     * @param file File to load bundle data from.
     * @return This builder.
     */
    fun setBundleFile(file: File): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.file = file
      this.bundle = null
      this.path = null
      return this
    }

    /** @inheritDoc */
    override fun build(): EmbeddedGuestVFSImpl = create(EffectiveGuestVFSConfig.fromBuilder(this))
  }

  /** Factory to create new embedded VFS implementations. */
  internal companion object EmbeddedVFSFactory : VFSFactory<EmbeddedGuestVFSImpl, Builder> {
    /** Default in-memory filesystem features. */
    private val defaultFeatures = listOf(
      Feature.FILE_CHANNEL,
      Feature.SECURE_DIRECTORY_STREAM,
    )

    /** Default in-memory filesystem attribute views. */
    private val defaultViews: Array<String> = arrayOf(
      "basic",
    )

    /** Calculate a set of supported [Feature]s for a new in-memory filesystem instance. */
    private fun EffectiveGuestVFSConfig.supportedFeatures(): Array<Feature> = defaultFeatures.plus(
      if (this.supportsSymbolicLinks) {
        listOf(
          Feature.LINKS,
          Feature.SYMBOLIC_LINKS
        )
      } else {
        emptyList()
      }
    ).toTypedArray()

    /** Calculate a set of supported attribute view names for a new in-memory filesystem instance. */
    @Suppress("UnusedReceiverParameter")
    private fun EffectiveGuestVFSConfig.attributeViews(): Array<String> = defaultViews

    /** @return [Jimfs] instance configured with the receiver's settings. */
    private fun EffectiveGuestVFSConfig.buildFs(config: EffectiveGuestVFSConfig): Configuration.Builder =
      Configuration.unix()
        .toBuilder()
        .setRoots(config.root)
        .setWorkingDirectory(config.workingDirectory)
        .setSupportedFeatures(*supportedFeatures()).let { builder ->
          val views = attributeViews()
          if (views.size > 1) {
            builder.setAttributeViews(views.first(), *(views.drop(1).toTypedArray()))
          } else {
            builder.setAttributeViews(views.first())
          }
          builder
        }

    /** @return [Jimfs] instance configured with the receiver's settings. */
    @JvmStatic private fun buildFs(name: String, config: Configuration): FileSystem = Jimfs.newFileSystem(
      name,
      config,
    )

    /** @return Bundle pair loaded from the provided [file]. */
    @JvmStatic private fun loadBundleFromFile(file: File?): Pair<Filesystem, RandomAccessFile>? {
      if (file == null) return null
      TODO("not yet implemented")
    }

    /** @return Bundle pair loaded from the provided [URI]. */
    @JvmStatic private fun loadBundleFromURI(path: URI?): Pair<Filesystem, RandomAccessFile>? {
      if (path == null) return null
      TODO("not yet implemented")
    }

    /** @return Resolve bundle input data from the provided [builder]. */
    @JvmStatic private fun resolveBundle(builder: Builder): Pair<Filesystem, RandomAccessFile>? {
      return when {
        builder.bundle != null -> builder.bundle
        builder.file != null -> loadBundleFromFile(builder.file)
        builder.path != null -> loadBundleFromURI(builder.path)
        else -> null
      }
    }

    /** @return VFS configuration spawned from a builder. */
    private fun EffectiveGuestVFSConfig.Companion.fromBuilder(builder: Builder): EffectiveGuestVFSConfig {
      return EffectiveGuestVFSConfig(
        readOnly = builder.readOnly,
        caseSensitive = builder.caseSensitive,
        supportsSymbolicLinks = builder.enableSymlinks,
        policy = builder.policy,
        root = builder.root,
        workingDirectory = builder.workingDirectory,
      )
    }

    /** @inheritDoc */
    override fun create(): EmbeddedGuestVFSImpl = EmbeddedGuestVFSImpl(
      EffectiveGuestVFSConfig.DEFAULTS,
      null,
      null,
    )

    /** @inheritDoc */
    override fun create(config: EffectiveGuestVFSConfig): EmbeddedGuestVFSImpl = EmbeddedGuestVFSImpl(
      config,
      null,
      null,
    )

    /** @inheritDoc */
    override fun create(builder: VFSBuilder<EmbeddedGuestVFSImpl>): EmbeddedGuestVFSImpl =
      builder.build()

    /** @inheritDoc */
    override fun create(configurator: Builder.() -> Unit): EmbeddedGuestVFSImpl {
      return Builder.newBuilder().apply {
        configurator.invoke(this)
      }.build()
    }
  }

  /** Factory bridge from Micronaut-driven configuration to the Embedded VFS implementation. */
  @Factory internal class EmbeddedVFSConfigurationFactory {
    /**
     * TBD.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun initializeMemoryFS(fs: FileSystem, tree: Filesystem, bundle: RandomAccessFile) {
      TODO("not yet implemented")
    }

    /** Construct from a Micronaut-driven configuration. */
    internal fun withConfig(ioConfig: GuestIOConfiguration): EffectiveGuestVFSConfig {
      return EffectiveGuestVFSConfig.withPolicy(
        policy = ioConfig.policy,
        caseSensitive = ioConfig.caseSensitive,
        supportsSymbolicLinks = ioConfig.symlinks,
        root = ioConfig.root,
        workingDirectory = ioConfig.workingDirectory,
      )
    }

    /**
     * TBD.
     */
    @Bean @Singleton internal fun spawn(ioConfig: GuestIOConfiguration): EmbeddedGuestVFSImpl {
      // generate an effective configuration
      val config = withConfig(ioConfig)

      // prepare a builder according to the provided configuration
      val builder = Builder.newBuilder().apply {
        readOnly = config.readOnly
        caseSensitive = config.caseSensitive
        enableSymlinks = config.supportsSymbolicLinks
        policy = config.policy
        root = config.root
        workingDirectory = config.workingDirectory
        if (config.bundle != null) {
          path = config.bundle
        }
      }

      // resolve the filesystem tree and data-bag based on the settings provided to the builder
      val (tree, databag) = when (val bundle = resolveBundle(builder)) {
        null -> null to null
        else -> bundle
      }

      // resolve a jimfs configuration to use
      val fsConfig = config.buildFs(config)
      val memoryFS = Jimfs.newFileSystem("elide-${UUID.random()}", fsConfig.build())
      if (tree != null && databag != null) {
        initializeMemoryFS(memoryFS, tree, databag)
      }

      return EmbeddedGuestVFSImpl(
        config,
        memoryFS,
        tree,
        databag,
      )
    }
  }

  /** @inheritDoc */
  override fun logging(): Logger = logging

  /** @inheritDoc */
  override fun checkPolicy(request: AccessRequest): AccessResponse {
    TODO("Not yet implemented")
  }
}

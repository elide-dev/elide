package elide.runtime.gvm.internals.vfs

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Feature
import com.google.common.jimfs.Jimfs
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestIOConfiguration
import elide.util.UUID
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Requires
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import tools.elide.crypto.HashAlgorithm
import tools.elide.vfs.TreeEntry
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.io.path.toPath

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
 * @param tree Tree of resolved file-system metadata.
 */
@Requires(property = "elide.gvm.vfs.enabled", notEquals = "false")
@Requires(property = "elide.gvm.vfs.mode", notEquals = "HOST")
internal class EmbeddedGuestVFSImpl private constructor (
  config: EffectiveGuestVFSConfig,
  backing: FileSystem,
  private val tree: FilesystemInfo,
) : AbstractDelegateVFS<EmbeddedGuestVFSImpl>(config, backing) {
  /** Static settings for the embedded VFS implementation. */
  private object Settings {
    /** File digest algorithm to use. */
    const val fileDigestAlgorithm: String = "SHA-256"

    /** Hash algorithm to use (symbolic) for file digests. */
    val fileDigest: HashAlgorithm = HashAlgorithm.SHA256
  }

  /** Enumerates supported embedded VFS bundle formats. */
  private enum class BundleFormat {
    /** Regular (non-compressed) tarball. */
    TARBALL,

    /** Compressed tarball. */
    TARBALL_COMPRESSED,

    /** Elide's internal bundle format. */
    ELIDE_INTERNAL,
  }

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
   * @param tree Tree of resolved file-system metadata.
   */
  private constructor(
    name: String,
    config: EffectiveGuestVFSConfig,
    fsConfig: Configuration,
    tree: FilesystemInfo,
  ) : this (
    config,
    buildFs(name, fsConfig),
    tree,
  )

  /**
   * Internal constructor: From configuration.
   *
   * This constructor is called by builders and factories ([Builder] and [Factory], precisely), to build a new VFS
   * instance.
   *
   * @param config Effective VFS configuration to apply.
   */
  internal constructor(
    config: EffectiveGuestVFSConfig,
  ) : this (
    "elide-${UUID.random()}",
    config,
    config.buildFs().build(),
    FilesystemInfo.getDefaultInstance(),
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
    internal var bundle: Pair<FilesystemInfo, FileSystem>? = null,
    internal var path: URI? = null,
    internal var file: File? = null,
  ) : VFSBuilder<EmbeddedGuestVFSImpl> {
    /** TBD. */
    companion object Factory : VFSBuilderFactory<EmbeddedGuestVFSImpl, Builder> {
      /** @inheritDoc */
      override fun newBuilder(): Builder = Builder()

      /** @inheritDoc */
      override fun newBuilder(builder: Builder): Builder = builder.copy()
    }

    /**
     * Set the [bundle] to use directly (pre-loaded from some data source).
     *
     * Note that setting this property force-unsets [path] and [file].
     *
     * @see bundle to set this value as a property.
     * @param bundle [FilesystemInfo] and [FileSystem] pair to use as the bundle.
     * @return This builder.
     */
    fun setBundle(bundle: Pair<FilesystemInfo, FileSystem>): VFSBuilder<EmbeddedGuestVFSImpl> {
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
    override fun build(): EmbeddedGuestVFSImpl {
      val config = EffectiveGuestVFSConfig.fromBuilder(this)
      val fsConfig = config.buildFs()
      val (tree, bundle) = when (val bundle = resolveBundle(this, fsConfig)) {
        null -> null to null
        else -> bundle
      }

      return if (tree != null && bundle != null) {
        EmbeddedGuestVFSImpl(
          config,
          bundle,
          tree,
        )
      } else create(config)
    }
  }

  /** Factory to create new embedded VFS implementations. */
  companion object EmbeddedVFSFactory : VFSFactory<EmbeddedGuestVFSImpl, Builder> {
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
    internal fun EffectiveGuestVFSConfig.buildFs(): Configuration.Builder =
      Configuration.unix()
        .toBuilder()
        .setRoots(this.root)
        .setWorkingDirectory(this.workingDirectory)
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

    /** @return Compression-wrapped [file] input stream for a given bundle entry, in the provided [format]. */
    @Suppress("unused", "UNUSED_PARAMETER")
    @JvmStatic private fun fileCompression(file: InputStream, format: BundleFormat? = null): InputStream {
      return file  // @TODO(sgammon): inner entry compression
    }

    /** @return Filled-out file record builder for the provided archive entry. */
    @JvmStatic private fun fileForEntry(entry: ArchiveEntry, offset: Long): tools.elide.vfs.File.Builder {
      val file = FileRecord.newBuilder()
      file.name = entry.name
      file.size = entry.size
      file.offset = offset
      return file
    }

    /** Write the provided file data at the provided [entry] path using [memoryFS]. */
    @JvmStatic private fun writeFileToMemoryFS(
      entry: ArchiveEntry,
      memoryFS: FileSystem,
      filedata: ByteArray,
      builder: tools.elide.vfs.File.Builder,
    ) {
      val path = memoryFS.getPath(entry.name)
      val md = MessageDigest.getInstance(Settings.fileDigestAlgorithm)
      memoryFS.provider().newOutputStream(path, StandardOpenOption.CREATE).use { buf ->
        md.update(filedata)
        buf.write(filedata)
      }

      val fingerprint = md.digest()
      builder.setFingerprint(tools.elide.vfs.File.FileFingerprint.newBuilder()
        .setUncompressed(tools.elide.vfs.File.Fingerprint.newBuilder()
          .setAlgorithm(Settings.fileDigest)
          .setHash(ByteString.copyFrom(fingerprint))))
    }

    /** @return [FilesystemInfo] metadata generated from a regular tarball. */
    @JvmStatic private fun metadataForTarballDirectory(
      folder: ArchiveEntry,
      tarball: ArchiveInputStream,
      memoryFS: FileSystem,
      prefix: String,
    ): Pair<tools.elide.vfs.Directory.Builder, ArchiveEntry> {
      // generate a builder for the directory
      val builder = DirectoryRecord.newBuilder()
      builder.name = if (folder.name.endsWith("/")) {
        folder.name.dropLast(1)
      } else {
        folder.name
      }.split("/").last()

      // create the directory within the in-memory FS
      val folderPath = memoryFS.getPath(folder.name)
      memoryFS.provider().createDirectory(folderPath)

      var entry = tarball.nextEntry
      while (entry != null) {
        if (entry.name.startsWith(prefix)) {
          // we're still inside this directory
          entry = if (entry.isDirectory) {
            // recurse into subdirectory
            val (subdir, next) = metadataForTarballDirectory(entry, tarball, memoryFS, entry.name)
            builder.addChildren(TreeEntry.newBuilder().setDirectory(subdir))
            next
          } else {
            // generate file builder
            val fileBuilder = fileForEntry(entry, tarball.bytesRead)
            val filedata = ByteArray(entry.size.toInt())
            tarball.read(filedata)

            // add file
            writeFileToMemoryFS(
              entry,
              memoryFS,
              filedata,
              fileBuilder,
            )

            // index via builder and grab next entry
            builder.addChildren(TreeEntry.newBuilder().setFile(fileBuilder))
            tarball.nextEntry
          }
        } else {
          // we are no longer inside the directory. return the entry and builder.
          break
        }
      }
      return builder to entry
    }

    /** @return [FilesystemInfo] metadata generated from a regular tarball. */
    @JvmStatic private fun metadataForTarball(tarball: ArchiveInputStream, memoryFS: FileSystem): FilesystemInfo {
      val fs = FilesystemInfo.newBuilder()
      val root = FileTreeEntry.newBuilder()
      val rootDir = DirectoryRecord.newBuilder()
      var offset = 0L
      rootDir.name = "/"

      var entry = tarball.nextEntry
      while (entry != null) {
        // provision a new generic tree entry, which carries either a file or directory but never both
        val fsEntry = FileTreeEntry.newBuilder()
        if (entry.isDirectory) {
          // recursively drives the stream until a non-matching entry, which is returned as `next`.
          val (dir, next) = metadataForTarballDirectory(
            entry,
            tarball,
            memoryFS,
            entry.name,
          )
          fsEntry.setDirectory(dir)
          rootDir.addChildren(fsEntry)
          entry = next
        } else {
          val file = fileForEntry(entry, offset)
          offset += entry.size

          // read the file into the buffer
          if (!tarball.canReadEntryData(entry)) {
            throw IOException("Cannot read entry data for ${entry.name}")
          } else {
            val filedata = ByteArray(entry.size.toInt())
            tarball.read(filedata)

            writeFileToMemoryFS(
              entry,
              memoryFS,
              filedata,
              file,
            )
          }

          val lastmod = entry.lastModifiedDate
          if (lastmod != null) {
            val instant = lastmod.toInstant()
            file.setModified(Timestamp.newBuilder()
              .setSeconds(instant.epochSecond)
              .setNanos(instant.nano))
          }
          fsEntry.setFile(file)
          rootDir.addChildren(fsEntry)

          // grab next entry
          entry = tarball.nextEntry
        }
      }
      root.setDirectory(rootDir)
      fs.setRoot(root)
      return fs.build()
    }

    /** @return [FilesystemInfo] metadata returned from an Elide bundle. */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    private fun loadMetadataFromElideBundle(bundle: ArchiveInputStream): FilesystemInfo {
      TODO("not yet implemented")
    }

    /** @return Loaded bundle from the provided input [stream], from the specified [format]. */
    @JvmStatic private fun loadBundle(
      stream: InputStream,
      type: BundleFormat,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem> {
      return when (type) {
        BundleFormat.ELIDE_INTERNAL,
        BundleFormat.TARBALL -> TarArchiveInputStream(stream)
        BundleFormat.TARBALL_COMPRESSED -> TarArchiveInputStream(GZIPInputStream(stream))
      }.use { buf ->
        // step 1: load or generate the file-tree metadata
        if (type == BundleFormat.ELIDE_INTERNAL) {
          loadMetadataFromElideBundle(buf)
          TODO("not yet implemented")
        } else {
          // build a new empty in-memory FS
          val inMemoryFS = Jimfs.newFileSystem(
            "elide-${UUID.random()}",
            fsConfig.build(),
          )

          // decode metadata from tarball while writing entries to new FS
          metadataForTarball(buf, inMemoryFS) to inMemoryFS
        }
      }
    }

    /** @return Loaded bundle from the provided input [stream], guessing the format from the file's [name]. */
    @JvmStatic private fun loadBundle(
      stream: InputStream,
      name: String,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem> {
      // resolve the format from the filename, then pass along
      return loadBundle(stream, when {
        name.endsWith(".tar") -> BundleFormat.TARBALL
        name.endsWith(".tar.gz") -> BundleFormat.TARBALL_COMPRESSED
        name.endsWith(".evfs") -> BundleFormat.ELIDE_INTERNAL
        else -> error(
          "Failed to load bundle from file '$name': unknown format. " +
          "Please provide `.tar`, `.tar.gz`, or `.evfs`."
        )
      }, fsConfig)
    }

    /** @return Bundle pair loaded from the provided [file]. */
    @JvmStatic internal fun loadBundleFromFile(
      file: File?,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem>? {
      if (file == null)
        return null
      if (!file.exists())
        throw IOException("Cannot load bundle from file '${file.path}': Does not exist or not a regular file")
      if (!file.canRead())
        throw AccessDeniedException(file, reason = "Cannot read bundle: access denied.")
      return loadBundle(file.inputStream(), file.name, fsConfig)
    }

    /** @return Bundle pair loaded from the provided [URI]. */
    @JvmStatic internal fun loadBundleFromURI(
      path: URI?,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem>? {
      return when {
        path == null -> null
        path.scheme == "file" -> loadBundleFromFile(path.toPath().toFile(), fsConfig)
        path.scheme == "classpath" -> {
          val filename = path.scheme.replace("classpath:", "")
          val target = EmbeddedGuestVFSImpl::class.java.getResourceAsStream(filename) ?: error(
            "Failed to load bundle from path '$path': Not found"
          )
          loadBundle(target, filename, fsConfig)
        }

        else -> error("Unsupported scheme for loading VFS bundle: '${path.scheme}'")
      }
    }

    /** @return Resolve bundle input data from the provided [builder]. */
    @JvmStatic internal fun resolveBundle(
      builder: Builder,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem>? {
      return when {
        builder.bundle != null -> builder.bundle
        builder.file != null -> loadBundleFromFile(builder.file, fsConfig)
        builder.path != null -> loadBundleFromURI(builder.path, fsConfig)
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
    )

    /** @inheritDoc */
    override fun create(config: EffectiveGuestVFSConfig): EmbeddedGuestVFSImpl = EmbeddedGuestVFSImpl(
      config,
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
      val fsConfig = config.buildFs()
      val (tree, fs) = when (val bundle = resolveBundle(builder, fsConfig)) {
        null -> null to null
        else -> bundle
      }
      val effectiveFS = fs ?: Jimfs.newFileSystem(
        "elide-${UUID.random()}",
        fsConfig.build(),
      )

      return EmbeddedGuestVFSImpl(
        config,
        effectiveFS,
        tree ?: FilesystemInfo.getDefaultInstance(),
      )
    }
  }

  /** @inheritDoc */
  override fun logging(): Logger = logging

  /** @inheritDoc */
  override fun allowsHostFileAccess(): Boolean = false

  /** @inheritDoc */
  override fun allowsHostSocketAccess(): Boolean = false
}

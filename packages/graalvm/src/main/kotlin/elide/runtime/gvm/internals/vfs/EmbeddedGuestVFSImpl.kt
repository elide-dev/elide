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

@file:Suppress("JAVA_MODULE_DOES_NOT_READ_UNNAMED_MODULE")

package elide.runtime.gvm.internals.vfs

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Feature
import com.google.common.jimfs.Jimfs
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
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
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.cfg.GuestIOConfiguration
import elide.runtime.gvm.internals.GuestVFS
import elide.util.UUID
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import kotlin.io.path.exists

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

    /** Compressed tarball (with `gzip`). */
    TARBALL_GZIP,

    /** Compressed tarball (with `xz`). */
    TARBALL_XZ,

    /** Elide's internal bundle format. */
    ELIDE_INTERNAL,
  }

  // Logger.
  private val logging: Logger by lazy {
    Logging.of(EmbeddedGuestVFSImpl::class)
  }

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
   * @param paths Paths to the bundle files, as regular host-filesystem paths, or `classpath:`-prefixed URIs for
   *   resources which should be fetched from the host app class-path.
   * @param files Files to load as file-system bundles.
   */
  @Suppress("unused") internal data class Builder (
    override var readOnly: Boolean = true,
    override var caseSensitive: Boolean = true,
    override var enableSymlinks: Boolean = false,
    override var root: String = ROOT_SYSTEM_DEFAULT,
    override var workingDirectory: String = DEFAULT_CWD,
    override var policy: GuestVFSPolicy = GuestVFSPolicy.DEFAULTS,
    internal var bundle: Pair<FilesystemInfo, FileSystem>? = null,
    internal var paths: List<URI> = emptyList(),
    internal var files: List<File> = emptyList(),
  ) : VFSBuilder<EmbeddedGuestVFSImpl> {
    /** Factory for embedded VFS implementations. */
    companion object Factory : VFSBuilderFactory<EmbeddedGuestVFSImpl, Builder> {
      override fun newBuilder(): Builder = Builder()

      override fun newBuilder(builder: Builder): Builder = builder.copy()
    }

    /**
     * Set the [bundle] to use directly (pre-loaded from some data source).
     *
     * Note that setting this property force-unsets [paths] and [files].
     *
     * @see bundle to set this value as a property.
     * @param bundle [FilesystemInfo] and [FileSystem] pair to use as the bundle.
     * @return This builder.
     */
    fun setBundle(bundle: Pair<FilesystemInfo, FileSystem>): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.bundle = bundle
      this.paths = emptyList()
      this.files = emptyList()
      return this
    }

    /**
     * Set the [paths] to load the bundle file from; can be a regular file-path, or a `classpath:`-prefixed path to load
     * a resource from the host app classpath.
     *
     * Note that setting this property force-unsets [bundle] and [files].
     *
     * @see paths to set this value as a property.
     * @param paths URI to the bundle file to load.
     * @return This builder.
     */
    fun setBundlePaths(paths: List<URI>): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.paths = paths
      this.bundle = null
      this.files = emptyList()
      return this
    }

    /**
     * Set the [files] to load the bundle data from; expected to be a valid and readable regular file, which is a
     * tarball, a compressed tar-ball, or a bundle in Elide's internal format.
     *
     * Note that setting this property force-unsets [bundle] and [paths].
     *
     * @see files to set this value as a property.
     * @param files File to load bundle data from.
     * @return This builder.
     */
    fun setBundleFiles(files: List<File>): VFSBuilder<EmbeddedGuestVFSImpl> {
      this.files = files
      this.bundle = null
      this.paths = emptyList()
      return this
    }

    override fun build(): EmbeddedGuestVFSImpl {
      val config = EffectiveGuestVFSConfig.fromBuilder(this)
      val fsConfig = config.buildFs()
      val (tree, bundle) = when (val bundle = resolveBundles(this, fsConfig)) {
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
      Configuration.forCurrentPlatform()
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
      if (!folderPath.exists()) {
        memoryFS.provider().createDirectory(folderPath)
      }

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
    @JvmStatic private fun metadataForTarball(
      inputs: Iterable<ArchiveInputStream>,
      memoryFS: FileSystem,
    ): FilesystemInfo {
      val fs = FilesystemInfo.newBuilder()
      val root = FileTreeEntry.newBuilder()
      val rootDir = DirectoryRecord.newBuilder()
      var offset = 0L
      rootDir.name = "/"

      val inputset = inputs.iterator()
      var tarball: ArchiveInputStream? = inputset.next()

      while (tarball != null) {
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
            val filedata = ByteArray(entry.size.toInt())
            tarball.read(filedata)

            writeFileToMemoryFS(
              entry,
              memoryFS,
              filedata,
              file,
            )

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

        // seek to next tarball input, if available
        tarball = if (inputset.hasNext()) {
          inputset.next()
        } else {
          null
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

    /** @return Loaded bundle from the provided input [streams], from the specified [format]. */
    @JvmStatic private fun loadBundlesToMemoryFS(
      streams: List<Pair<InputStream, BundleFormat>>,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem> {
      // build a new empty in-memory FS
      val inMemoryFS = Jimfs.newFileSystem(
        "elide-${UUID.random()}",
        fsConfig.build(),
      )

      // read each input tarball and compose the resulting structures
      return streams.stream().map { input ->
        when (input.second) {
          BundleFormat.ELIDE_INTERNAL,
          BundleFormat.TARBALL -> TarArchiveInputStream(input.first) to input.second
          BundleFormat.TARBALL_XZ -> TarArchiveInputStream(XZCompressorInputStream(input.first)) to input.second
          BundleFormat.TARBALL_GZIP -> TarArchiveInputStream(GZIPInputStream(input.first)) to input.second
        }
      }.map { input ->
        if (input.second == BundleFormat.ELIDE_INTERNAL) {
          loadMetadataFromElideBundle(input.first)
          TODO("not yet implemented")
        } else {
          metadataForTarball(listOf(input.first), inMemoryFS)
        }
      }.reduce { left, right ->
        left.toBuilder().mergeFrom(right).build()
      }.orElse(
        FilesystemInfo.getDefaultInstance()
      ) to inMemoryFS  // <-- map the return filesystem to the in-memory FS we built
    }

    /** @return Loaded bundles from the provided input [streams], guessing the format from the file's name. */
    @JvmStatic private fun loadBundles(
      image: Pair<String, InputStream>?,
      streams: List<Pair<String, InputStream>>,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem> {
      // resolve the format from the filename, then pass along
      return loadBundlesToMemoryFS(when (image) {
        null -> streams
        else -> listOf(image) + streams
      }.map { (name, stream) ->
        stream to when {
          name.endsWith(".tar") -> BundleFormat.TARBALL
          name.endsWith(".tar.gz") -> BundleFormat.TARBALL_GZIP
          name.endsWith(".tar.xz") -> BundleFormat.TARBALL_XZ
          name.endsWith(".evfs") -> BundleFormat.ELIDE_INTERNAL
          else -> error(
            "Failed to load bundle from file '$name': unknown format. " +
            "Please provide `.tar`, `.tar.gz`, `.tar.xz`, or `.evfs`."
          )
        }
      }, fsConfig)
    }

    /** @return Bundle pair loaded from the provided [files]. */
    @JvmStatic internal fun loadBundleFiles(
      files: List<File>,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem>? {
      // if we got no paths, we have no bundles
      if (files.isEmpty()) return null

      val sources = files.map { file ->
        if (!file.exists())
          throw IOException("Cannot load bundle from file '${file.path}': Does not exist or not a regular file")
        if (!file.canRead())
          throw AccessDeniedException(file, reason = "Cannot read bundle: access denied.")

        // hand back name + input stream so we can call `loadBundles`
        file.name to file.inputStream()
      }
      return loadBundles(null, sources, fsConfig)
    }

    /** @return Bundle pair loaded from the provided [URI]. */
    @JvmStatic internal fun loadBundleURIs(
      paths: List<URI>,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem>? {
      // if we got no paths, we have no bundles
      if (paths.isEmpty()) return null

      val sources = paths.map { path ->
        when (path.scheme) {
          "file" -> path.toPath().toFile().let { file ->
            file.name to file.inputStream()
          }

          "classpath", "resource", "jar" -> {
            val filename = when (path.scheme) {
              "resource" -> path.toString().removePrefix("resource:")
              "classpath" -> path.toString().removePrefix("classpath:")
              "jar" -> path.toString().split("!").last()
              else -> path.path
            }
            val target = EmbeddedGuestVFSImpl::class.java.getResourceAsStream(filename) ?: error(
              "Failed to load bundle from path '$path': Not found"
            )
            filename to target
          }
          else -> error("Unsupported scheme for loading VFS bundle: '${path.scheme}' (URL: $path)")
        }
      }
      return loadBundles(null, sources, fsConfig)
    }

    /** @return Resolve bundle input data from the provided [builder]. */
    @JvmStatic internal fun resolveBundles(
      builder: Builder,
      fsConfig: Configuration.Builder,
    ): Pair<FilesystemInfo, FileSystem>? {
      return when {
        builder.bundle != null -> builder.bundle
        builder.files.isNotEmpty() -> loadBundleFiles(builder.files, fsConfig)
        builder.paths.isNotEmpty() -> loadBundleURIs(builder.paths, fsConfig)
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

    override fun create(): EmbeddedGuestVFSImpl = EmbeddedGuestVFSImpl(
      EffectiveGuestVFSConfig.DEFAULTS,
    )

    override fun create(config: EffectiveGuestVFSConfig): EmbeddedGuestVFSImpl = EmbeddedGuestVFSImpl(
      config,
    )

    override fun create(builder: VFSBuilder<EmbeddedGuestVFSImpl>): EmbeddedGuestVFSImpl =
      builder.build()

    override fun create(configurator: Builder.() -> Unit): EmbeddedGuestVFSImpl {
      return Builder.newBuilder().apply {
        configurator.invoke(this)
      }.build()
    }
  }

  /** Factory bridge from Micronaut-driven configuration to the Embedded VFS implementation. */
  @Factory internal class EmbeddedVFSConfigurationFactory {
    /**
     * Spawn an embedded VFS implementation driven by Micronaut-style configuration.
     *
     * @param ioConfig Guest I/O configuration to use for creating the VFS.
     * @return Embedded VFS implementation built according to the provided [config].
     */
    @Bean @Singleton internal fun spawn(
      ioConfig: GuestIOConfiguration,
      configurators: List<GuestVFS.VFSConfigurator>,
    ): EmbeddedGuestVFSImpl {
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
        if (config.bundle.isNotEmpty()) {
          paths = config.bundle.plus(configurators.flatMap {
            it.bundles()
          })
        } else if (configurators.isNotEmpty()) {
          paths = configurators.flatMap {
            it.bundles()
          }
        }
      }

      // resolve the filesystem tree and data-bag based on the settings provided to the builder
      val fsConfig = config.buildFs()
      val (tree, fs) = when (val bundle = resolveBundles(builder, fsConfig)) {
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

  override fun logging(): Logger = logging

  override fun allowsHostFileAccess(): Boolean = false

  override fun allowsHostSocketAccess(): Boolean = false
}

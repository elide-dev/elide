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
package elide.runtime.gvm.internals.vfs

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.graalvm.nativeimage.ImageInfo
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.exists
import kotlin.io.path.notExists
import elide.runtime.LogLevel
import elide.runtime.gvm.cfg.GuestIOConfiguration
import elide.runtime.vfs.GuestVFS

/**
 * # VFS: Backed Implementation
 *
 * This class implements a virtual-file-system for guest use, backed by another "[backing]" file-system, to which calls
 * are proxied from the guest. Before proxying each call, I/O security policy is checked and enforced, and logging is
 * performed.
 *
 * The provided [config] is expected to be fully loaded; that is, it should have a loaded filesystem tree and data-bag
 * from which to pull file system information. The [backing] file system used by this class is closed when this class
 * closes.
 *
 * @param VFS Concrete virtual file system type under implementation.
 * @param config Effective guest VFS configuration to apply.
 * @param backing Backing file-system instance which implements the FS to use.
 */
public abstract class AbstractDelegateVFS<VFS> protected constructor (
  config: EffectiveGuestVFSConfig,
  protected val backing: FileSystem,
  private val activeWorkingDirectory: AtomicReference<Path> = AtomicReference(Path.of(config.workingDirectory)),
) : GuestVFS, AbstractBaseVFS<VFS>(config) where VFS: AbstractBaseVFS<VFS> {
  internal companion object {
    /** Translate an [AccessMode] to an [AccessType]. */
    fun AccessMode.toAccessType(): AccessType = when (this) {
      AccessMode.READ -> AccessType.READ
      AccessMode.WRITE -> AccessType.WRITE
      AccessMode.EXECUTE -> error("`EXECUTE` access mode is not supported by Elide VFS")
    }

    /** Path to user's home cache directory. */
    private val userHomeCache by lazy {
      Paths.get(System.getProperty("user.home"), ".cache").toString()
    }

    /** Set of access types that are read-only. */
    private val readOnlyAccess by lazy {
      sortedSetOf(AccessType.READ)
    }

    /** Process-wide file attributes cache. */
    private val attributesCache: Cache<Pair<Path, String>, MutableMap<String, Any>> by lazy {
      CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .maximumSize(100)
        .build()
    }

    /** Process-wide access check cache. */
    private val accessCache: Cache<AccessRequest, AccessResponse> by lazy {
      CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .maximumSize(100)
        .build()
    }

    /** Construct from a Micronaut-driven configuration. */
    @JvmStatic internal fun withConfig(ioConfig: GuestIOConfiguration): EffectiveGuestVFSConfig {
      return EffectiveGuestVFSConfig.withPolicy(
        policy = ioConfig.policy,
        caseSensitive = ioConfig.caseSensitive ?: GuestIOConfiguration.DEFAULT_CASE_SENSITIVE,
        supportsSymbolicLinks = ioConfig.symlinks ?: GuestIOConfiguration.DEFAULT_SYMLINKS,
        root = ioConfig.root ?: GuestIOConfiguration.DEFAULT_ROOT,
        workingDirectory = ioConfig.workingDirectory ?: GuestIOConfiguration.DEFAULT_WORKING_DIRECTORY,
      )
    }
  }

  // Whether to suppress file-not-found exceptions.
  private val suppressNotFound: AtomicBoolean = AtomicBoolean(false)

  private val defaultTempDirectory by lazy {
    // FIXME(@darvld): copy implementation to use the backing FS instead of the default!
    backing.getPath("./.temp").also {
      if(it.notExists()) backing.provider().createDirectory(it)
    }
  }

  // Debug log messages for the current VFS implementation.
  protected fun debugLog(message: () -> String) {
    if (logging.isEnabled(LogLevel.DEBUG)) {
      logging.debug("VFS: ${message()}")
    }
  }

  /**
   * Trigger suppression of file-not-found exceptions for the current VFS instance; this is done by a child class when
   * it recognizes such exceptions and applies other logic.
   *
   * This method should be called early in the VFS setup flow, and not again.
   */
  internal fun suppressNotFoundErr() {
    suppressNotFound.set(true)
  }

  /**
   * Throw a well-formed [GuestIOException] for the provided [types], [path], and [message], which we declined for a
   * guest I/O operation.
   *
   * @param types Access type that was denied.
   * @param path Path that was denied.
   * @param message Extra message, if any.
   * @return Guest I/O exception with the provided inputs, to be thrown.
   */
  protected open fun notAllowed(types: Set<AccessType>, path: Path, message: String? = null): GuestIOException {
    return GuestIOAccessDenied.forPath(path, types, message)
  }

  override fun close(): Unit = backing.close()

  override fun getSeparator(): String = backing.separator

  override fun getPathSeparator(): String = backing.separator

  override fun parsePath(uri: URI): Path {
    return backing.getPath(uri.path.toString())
  }

  override fun parsePath(path: String): Path {
    return backing.getPath(path)
  }

  override fun getPath(vararg segments: String): Path {
    return if (segments.size == 1) {
      backing.getPath(segments[0])
    } else {
      backing.getPath(segments[0], *segments.drop(1).toTypedArray())
    }
  }

  override fun toAbsolutePath(path: Path): Path {
    return path.toAbsolutePath()
  }

  override fun toRealPath(path: Path, vararg linkOptions: LinkOption): Path {
    return path
  }

  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    // special case: if we are accessing a python `<frozen ...>` module, we should always allow it.
    val pathStr = path.toString()
    if (pathStr.startsWith("<frozen ")) {
      debugLog {
        "Allowing access to frozen module"
      }
      return
    }
    // if the access request involves the user's cache dir, allow it.
    if (pathStr.startsWith(userHomeCache)) {
      debugLog {
        "Allowing access to user cache"
      }
      return
    }
    debugLog {
      "Checking access to path: $pathStr, modes: $modes, linkOptions: $linkOptions"
    }
    // @TODO(sgammon): why is it doing this
    val accessTypes: SortedSet<AccessType> = if (modes.isEmpty()) {
      readOnlyAccess
    } else {
      modes.map { it.toAccessType() }.toSortedSet()
    }

    checkPolicy(
      type = accessTypes,
      path = path,
      domain = AccessDomain.GUEST,
    ).let { response ->
      if (response.policy != AccessResult.ALLOW) {
        debugLog {
          "Access check failed: response indicates `DENY`"
        }
        throw notAllowed(accessTypes, path)
      } else {
        debugLog {
          "Access check passed: response indicates `ALLOW`"
        }
      }
    }
  }

  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
    debugLog { "Creating directory at path: '$dir'" }
    enforce(type = AccessType.WRITE, domain = AccessDomain.GUEST, scope = AccessScope.DIRECTORY, path = dir)
    backing.provider().createDirectory(dir, *attrs)
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel {
    val pathStr = path.toString()
    if (pathStr.startsWith("<frozen ")) {
      throw NoSuchFileException(pathStr, null, "Frozen module access")
    }
    debugLog { "Opening byte channel for file at path: '$path'" }
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, scope = AccessScope.FILE, path = path)

    return try {
      backing.provider().newByteChannel(
        path,
        options,
        *attrs
      )
    } catch (err: IOException) {
      when (err) {
        is FileNotFoundException, is NoSuchFileException -> {
          if (suppressNotFound.get()) {
            throw err
          }
        }
      }
      throw err
    }
  }

  override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> {
    debugLog { "Streaming directory entries at path: '$dir'" }
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, scope = AccessScope.DIRECTORY, path = dir)
    return backing.provider().newDirectoryStream(dir, filter)
  }

  override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> {
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, path = path)
    if (ImageInfo.inImageRuntimeCode()) {
      val cached = attributesCache.getIfPresent(path to attributes)
      if (cached != null) {
        return cached
      }
    }
    return backing.provider().readAttributes(path, attributes, *options).toMutableMap().apply {
      if (containsKey("ino")) {
        // fix: convert `ino` to `long`, which gvm filesystems expect
        this["ino"] = when (val ino = this["ino"]) {
          is Int -> ino.toLong()
          else -> ino
        }
      }
    }.also {
      attributesCache.put(path to attributes, it)
    }
  }

  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) {
    debugLog { "Setting attribute '$attribute' for file at path: '$path' (value: '$value', options: '$options')" }
    enforce(type = AccessType.WRITE, domain = AccessDomain.GUEST, path = path)
    return backing.provider().setAttribute(path, attribute, value, *options)
  }

  override fun copy(source: Path, target: Path, vararg options: CopyOption?) {
    debugLog { "Copying from '$source' -> '$target' (options: $options)" }
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, path = source)
    enforce(type = AccessType.WRITE, domain = AccessDomain.GUEST, path = target)
    backing.provider().copy(source, target, *options)
  }

  override fun move(source: Path, target: Path, vararg options: CopyOption?) {
    debugLog { "Moving from '$source' -> '$target' (options: $options)" }
    enforce(type = sortedSetOf(AccessType.READ, AccessType.DELETE), domain = AccessDomain.GUEST, path = source)
    enforce(type = AccessType.WRITE, domain = AccessDomain.GUEST, path = target)
    backing.provider().move(source, target, *options)
  }

  override fun delete(path: Path) {
    debugLog { "Deleting filesystem entry at path: '$path'" }
    enforce(type = AccessType.DELETE, domain = AccessDomain.GUEST, path = path)
    backing.provider().delete(path)
  }

  override fun createLink(link: Path, existing: Path) {
    debugLog { "Creating hard-link from '$link' -> '$existing'" }
    enforce(type = AccessType.WRITE, domain = AccessDomain.GUEST, path = link)
    return backing.provider().createLink(link, existing)
  }

  override fun createSymbolicLink(link: Path, target: Path, vararg attrs: FileAttribute<*>?) {
    debugLog { "Creating soft-link from '$link' -> '$target'" }
    enforce(type = AccessType.WRITE, domain = AccessDomain.GUEST, path = link)
    return backing.provider().createSymbolicLink(link, target)
  }

  override fun readSymbolicLink(link: Path): Path {
    debugLog { "Reading soft-link at '$link'" }
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, path = link)
    return backing.provider().readSymbolicLink(link)
  }

  override fun setCurrentWorkingDirectory(currentWorkingDirectory: Path) {
    debugLog { "Setting CWD to: '$currentWorkingDirectory'" }
    activeWorkingDirectory.set(currentWorkingDirectory)
  }

  override fun getMimeType(path: Path): String? {
    debugLog { "Getting MIME type for file at path: '$path'" }
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, path = path)
    return Files.probeContentType(path)
  }

  override fun getEncoding(path: Path): Charset {
    debugLog { "Fetching encoding for path: '$path'" }
    return StandardCharsets.UTF_8  // TODO(sgammon): make this configurable or resolve from tree
  }

  override fun getTempDirectory(): Path {
    debugLog { "Fetching temp directory path" }
    return defaultTempDirectory
  }

  override fun isSameFile(path1: Path, path2: Path, vararg options: LinkOption): Boolean {
    debugLog { "Checking if '$path1' and '$path2' are the same file" }
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, path = path1)
    enforce(type = AccessType.READ, domain = AccessDomain.GUEST, path = path2)

    // @TODO(sgammon): what to do about `options` here?
    return backing.provider().isSameFile(path1, path2)
  }

  override fun readStream(path: Path, vararg options: OpenOption): InputStream {
    require(path.toString().isNotBlank()) { "Cannot read from blank path" }
    debugLog { "Performing host-side read for path '$path' (options: '$options')" }
    enforce(type = AccessType.READ, domain = AccessDomain.HOST, path = path)
    return backing.provider().newInputStream(path, *options)
  }

  override fun writeStream(path: Path, vararg options: OpenOption): OutputStream {
    require(path.toString().isNotBlank()) { "Cannot write to blank path" }
    debugLog { "Performing host-side write for path '$path' (options: '$options')" }
    enforce(type = AccessType.WRITE, domain = AccessDomain.HOST, path = path)
    return backing.provider().newOutputStream(path, *options)
  }

  override fun checkPolicy(request: AccessRequest): AccessResponse {
    // special case: if we are accessing a python `<frozen ...>` module, we should always allow it.
    if (request.path.toString().startsWith("<frozen ")) {
      return AccessResponse.allow("Frozen module access")
    }
    // if we're in read-only mode, and the `request` represents an operation that writes (or deletes), we can reject it
    // outright because we know it to be un-supported.
    return if (config.readOnly && request.isWrite) {
      debugLog {
        "Write denied because filesystem is in read-only mode"
      }
      AccessResponse.deny("Filesystem is in read-only mode")
    } else {
      if (request.path.notExists())
        throw NoSuchFileException(request.path.toString())

      debugLog {
        "Delegating policy check to attached policy"
      }
      if (ImageInfo.inImageBuildtimeCode()) {
        // don't use caching in build-time code; it drags in Guava's cache implementation
        config.policy.evaluateForPath(request)
      } else when (val cached = accessCache.getIfPresent(request)) {
        // otherwise, defer to the attached policy.
        null -> config.policy.evaluateForPath(request).also { accessCache.put(request, it) }
        else -> cached
      }
    }
  }
}

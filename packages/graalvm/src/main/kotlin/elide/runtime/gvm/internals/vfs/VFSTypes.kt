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

import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.core.api.Symbolic
import java.util.SortedSet

// -- Basic: Files, Directories, Trees -- //

private const val CONST_IDENTITY = 0

// Compression constants.
private const val COMPRESSION_GZIP = 1
private const val COMPRESSION_BROTLI = 2
private const val COMPRESSION_SNAPPY = 3
private const val COMPRESSION_DEFLATE = 4

// Entry constants.
private const val FILE_ENTRY = 9
private const val DIRECTORY_ENTRY = 10

// Hash constants.
private const val HASH_MD5 = 1
private const val HASH_SHA1 = 2
private const val HASH_SHA2 = 3
private const val HASH_SHA256 = 4
private const val HASH_SHA384 = 5
private const val HASH_SHA512 = 6
private const val HASH_SHA3_224 = 7
private const val HASH_SHA3_256 = 8
private const val HASH_SHA3_512 = 9

/**
 * Compression mode.
 *
 * Describes the compression mode applied to a given VFS entry; compression is optional and only applied in certain
 * circumstances. Describes the algorithm used to compress the data.
 */
@Serializable public enum class CompressionMode (override val symbol: Int): Symbolic<Int> {
  IDENTITY(CONST_IDENTITY),
  GZIP(COMPRESSION_GZIP),
  BROTLI(COMPRESSION_BROTLI),
  SNAPPY(COMPRESSION_SNAPPY),
  DEFLATE(COMPRESSION_DEFLATE);

  /** Protocol number for this compression type. */
  public val number: Int get() = symbol

  public companion object: Symbolic.SealedResolver<Int, CompressionMode> {
    override fun resolve(symbol: Int): CompressionMode = when (symbol) {
      CONST_IDENTITY -> IDENTITY
      COMPRESSION_GZIP -> GZIP
      COMPRESSION_BROTLI -> BROTLI
      COMPRESSION_SNAPPY -> SNAPPY
      COMPRESSION_DEFLATE -> DEFLATE
      else -> throw unresolved(symbol)
    }
  }
}

/**
 * File timestamp API.
 *
 * Describes a generic file timestamp.
 */
public sealed interface FileTimestampAPI {
  /** Absolute seconds since epoch. */
  public val seconds: Long

  /** Adjustment offset by nanos. */
  public val nanos: Int
}

/**
 * File timestamp.
 *
 * Describes a generic file timestamp.
 */
@Serializable public sealed interface FileTimestamp: FileTimestampAPI {
  /** Mutable builder for file timestamps. */
  public sealed interface Builder : FileTimestampAPI {
    override var seconds: Long
    override var nanos: Int
    public fun build(): FileTimestamp
  }

  /** Defines a [FileTimestamp] builder backed by JVM data. */
  @JvmInline public value class TimestampPair internal constructor (
    private val pair: Pair<Long, Int>
  ): FileTimestamp {
    override val seconds: Long get() = pair.first
    override val nanos: Int get() = pair.second
  }

  /** Defines a [FileTimestamp] builder backed by JVM data. */
  public data class TimestampPairBuilder internal constructor (
    override var seconds: Long = 0,
    override var nanos: Int = 0,
  ): Builder {
    override fun build(): FileTimestamp = TimestampPair(seconds to nanos)
  }

  public companion object {
    @JvmStatic public fun newBuilder(): Builder = TimestampPairBuilder()
  }
}

/**
 * File system record API.
 *
 * API adhered to by both files and directories.
 */
public sealed interface FileSystemRecordAPI {
  /**
   * Name of this file or directory.
   */
  public val name: String
}

/**
 * File record API.
 *
 * Describes the API adhered to by [FileRecord] objects, and, in mutable form, by [FileRecord.Builder] objects.
 */
public sealed interface FileRecordAPI : FileSystemRecordAPI {
  /**
   * Size of the file, in bytes.
   */
  public val size: Long

  /**
   * Offset distance to this file.
   */
  public val offset: Long

  /**
   * Offset distance to this file.
   */
  public val modified: FileTimestamp?
}

/**
 * File record.
 */
@Serializable public sealed interface FileRecord : FileRecordAPI {
  /**
   * File record builder.
   */
  public sealed interface Builder: FileRecordAPI {
    override var name: String
    override var size: Long
    override var offset: Long
    override var modified: FileTimestamp?
    public fun build(): FileRecord
  }

  /** Implements a [FileRecord] backed by regular JVM data. */
  @JvmRecord public data class FileRecordData internal constructor(
    override val name: String,
    override val size: Long = 0,
    override val offset: Long = 0,
    override val modified: FileTimestamp? = null,
  ): FileRecord

  /** Implements a default [FileRecord] builder. */
  public data class DefaultFileRecordBuilder internal constructor(
    override var name: String = "",
    override var size: Long = 0,
    override var offset: Long = 0,
    override var modified: FileTimestamp? = null,
  ): Builder {
    override fun build(): FileRecord = FileRecordData(
      name = name,
      size = size,
      offset = offset,
      modified = modified,
    )
  }

  /** Factory methods for [FileRecord]. */
  public companion object {
    /** @return Empty [FileRecord] builder. */
    @JvmStatic public fun newBuilder(): Builder = DefaultFileRecordBuilder()
  }
}

/**
 * Type alias: File record builder.
 *
 * Maps to the underlying structural type used for `File` object metadata builders within the runtime VFS structure.
 */
internal typealias FileRecordBuilder = FileRecord.Builder

/**
 * Hash algorithm.
 *
 * Maps to the underlying type used for specifying the hash algorithm for files.
 */
@Serializable public enum class HashAlgorithm (override val symbol: Int): Symbolic<Int> {
  IDENTITY(CONST_IDENTITY),
  MD5(HASH_MD5),
  SHA1(HASH_SHA1),
  SHA2(HASH_SHA2),
  SHA256(HASH_SHA256),
  SHA384(HASH_SHA384),
  SHA512(HASH_SHA512),
  SHA3_224(HASH_SHA3_224),
  SHA3_256(HASH_SHA3_256),
  SHA3_512(HASH_SHA3_512);

  /** Protocol number for this hash algorithm. */
  public val number: Int get() = symbol

  public companion object: Symbolic.SealedResolver<Int, HashAlgorithm> {
    override fun resolve(symbol: Int): HashAlgorithm = when (symbol) {
      CONST_IDENTITY -> IDENTITY
      HASH_MD5 -> MD5
      HASH_SHA1 -> SHA1
      HASH_SHA2 -> SHA2
      HASH_SHA256 -> SHA256
      HASH_SHA384 -> SHA384
      HASH_SHA512 -> SHA512
      HASH_SHA3_224 -> SHA3_224
      HASH_SHA3_256 -> SHA3_256
      HASH_SHA3_512 -> SHA3_512
      else -> throw unresolved(symbol)
    }
  }
}

/**
 * Tree entry API.
 */
public sealed interface TreeEntryAPI {
  /**
   * Case for which property is set.
   */
  public val type: EntryCase?

  /**
   * Directory record.
   */
  public val directory: DirectoryRecord?

  /**
   * File record.
   */
  public val file: FileRecord?
}

/**
 * Tree entry.
 *
 * Maps to a generic tree entry type representing a directory or file.
 */
@Serializable public sealed interface TreeEntry : TreeEntryAPI {
  override val type: EntryCase
  override val directory: DirectoryRecord
  override val file: FileRecord

  /**
   * Whether a directory is set.
   */
  public fun hasDirectory(): Boolean = type == EntryCase.DIRECTORY

  /**
   * Whether a file is set.
   */
  public fun hasFile(): Boolean = type == EntryCase.FILE

  /**
   * Tree entry builder.
   */
  public sealed interface Builder : TreeEntryAPI {
    override var directory: DirectoryRecord?
    override var file: FileRecord?
    override val type: EntryCase?
    public fun build(): TreeEntry
  }

  /** Describes an empty directory tree. */
  public data object None : TreeEntry {
    override val type: EntryCase get() = EntryCase.DIRECTORY
    override val file: FileRecord get() = error("Not a file")
    override val directory: DirectoryRecord get() = DirectoryRecord.newBuilder().apply {
      name = "/"
    }.build()
  }

  /** Sealed [TreeEntry] holding a [DirectoryRecord]. */
  @JvmInline public value class Dir internal constructor(private val dir: DirectoryRecord) : TreeEntry {
    override val directory: DirectoryRecord get() = dir
    override val file: FileRecord get() = error("Cannot access `file` on `Dir`")
    override val type: EntryCase get() = EntryCase.DIRECTORY
  }

  /** Sealed [TreeEntry] holding a [FileRecord]. */
  @JvmInline public value class File internal constructor(private val backing: FileRecord) : TreeEntry {
    override val directory: DirectoryRecord get() = error("Cannot access `file` on `File`")
    override val file: FileRecord get() = backing
    override val type: EntryCase get() = EntryCase.FILE
  }

  /** Implements a default [TreeEntry] builder. */
  public data class DefaultTreeEntryBuilder internal constructor(
    override var directory: DirectoryRecord? = null,
    override var file: FileRecord? = null,
  ) : Builder {
    override val type: EntryCase? get() = when {
      directory != null -> EntryCase.DIRECTORY
      file != null -> EntryCase.FILE
      else -> null
    }

    override fun build(): TreeEntry = when (type) {
      EntryCase.DIRECTORY -> Dir(requireNotNull(directory))
      EntryCase.FILE -> File(requireNotNull(file))
      else -> None
    }
  }

  public companion object {
    @JvmStatic public fun newBuilder(): Builder = DefaultTreeEntryBuilder()
  }
}

/**
 * Entry case.
 *
 * Maps to the enum which describes a tree entry's case (directory or file).
 */
@Serializable public enum class EntryCase (override val symbol: Int) : Symbolic<Int> {
  FILE(FILE_ENTRY),
  DIRECTORY(DIRECTORY_ENTRY);

  /** @return The protocol number for this entry case. */
  public val number: Int get() = symbol

  public companion object: Symbolic.SealedResolver<Int, EntryCase> {
    override fun resolve(symbol: Int): EntryCase = when (symbol) {
      FILE_ENTRY -> FILE
      DIRECTORY_ENTRY -> DIRECTORY
      else -> throw unresolved(symbol)
    }
  }
}

/**
 * Directory record API.
 */
public sealed interface DirectoryRecordAPI : FileSystemRecordAPI {
  /**
   * List of children under this directory.
   */
  public val childrenList: List<TreeEntry>

  /**
   * Number of children under this directory.
   */
  public val childrenCount: Int get() = childrenList.size
}

/**
 * Directory record.
 */
@Serializable public sealed interface DirectoryRecord : DirectoryRecordAPI {
  /**
   * Directory record builder.
   */
  public sealed interface Builder : DirectoryRecordAPI {
    override var name: String
    override var childrenList: MutableList<TreeEntry>
    public fun addChildren(children: List<TreeEntry>): Builder
    public fun addChildren(entry: TreeEntry): Builder
    public fun addChildren(builder: TreeEntry.Builder): Builder
    public fun build(): DirectoryRecord
  }

  /** Implements a [DirectoryRecord] backed by regular JVM data. */
  @JvmRecord public data class DirectoryRecordData internal constructor(
    override val name: String,
    override val childrenList: List<TreeEntry> = emptyList(),
  ): DirectoryRecord

  /** Implements a default [DirectoryRecord] builder. */
  public data class DefaultDirectoryRecordBuilder internal constructor(
    override var name: String = "",
    override var childrenList: MutableList<TreeEntry> = mutableListOf(),
  ) : Builder {
    override fun addChildren(entry: TreeEntry): Builder = apply {
      childrenList.add(entry)
    }

    override fun addChildren(builder: TreeEntry.Builder): Builder = apply {
      childrenList.add(builder.build())
    }

    override fun addChildren(children: List<TreeEntry>): Builder = apply {
      childrenList.addAll(children)
    }

    override fun build(): DirectoryRecord = DirectoryRecordData(
      name = name,
      childrenList = childrenList,
    )
  }

  public companion object {
    @JvmStatic public fun newBuilder(): Builder = DefaultDirectoryRecordBuilder()
  }
}

/**
 * Type alias: Directory record builder.
 *
 * Maps to the underlying structural type used for `Directory` object metadata builders within the runtime VFS
 * structure.
 */
internal typealias DirectoryRecordBuilder = DirectoryRecord.Builder

/**
 * Filesystem metadata API.
 */
public interface FilesystemMetadataAPI

/**
 * Filesystem metadata.
 */
@Serializable public sealed interface FilesystemMetadata {
  /**
   * Filesystem metadata builder.
   */
  public sealed interface Builder : FilesystemMetadataAPI

  /** Implements a [FilesystemMetadata] backed by regular JVM data. */
  public class DefaultFilesystemMetadataBuilder: Builder

  /** Implements a [FilesystemMetadata] backed by regular JVM data. */
  public class FilesystemMetadataData: FilesystemMetadata

  public companion object {
    @JvmStatic public fun newBuilder(): Builder = DefaultFilesystemMetadataBuilder()
  }
}

/**
 * Filesystem info API.
 */
public interface FilesystemInfoAPI {
  /**
   * Root directory.
   */
  public val root: TreeEntry?

  /**
   * Filesystem metadata.
   */
  public val metadata: FilesystemMetadata?

  public fun hasRoot(): Boolean = true
}

/**
 * Filesystem info.
 */
@Serializable public sealed interface FilesystemInfo : FilesystemInfoAPI {
  override val root: TreeEntry
  override val metadata: FilesystemMetadata?

  /**
   * Filesystem info builder.
   */
  public sealed interface Builder : FilesystemInfoAPI {
    override var root: TreeEntry?
    override var metadata: FilesystemMetadata?
    public fun build(): FilesystemInfo
  }

  /** Implements a [FilesystemInfo] backed by regular JVM data. */
  public data class DefaultFilesystemInfoBuilder internal constructor(
    override var root: TreeEntry? = null,
    override var metadata: FilesystemMetadata? = null,
  ): Builder {
    override fun build(): FilesystemInfo = FilesystemInfoData(
      (root ?: TreeEntry.None) to metadata,
    )
  }

  /** Implements a [FilesystemInfo] backed by regular JVM data. */
  @JvmInline public value class FilesystemInfoData internal constructor(
    private val pair: Pair<TreeEntry, FilesystemMetadata?>,
  ): FilesystemInfo {
    override val root: TreeEntry get() = pair.first
    override val metadata: FilesystemMetadata? get() = pair.second
  }

  public companion object {
    // Default empty FS instance.
    private val defaultInstance by lazy { DefaultFilesystemInfoBuilder().build() }
    @JvmStatic public fun newBuilder(): Builder = DefaultFilesystemInfoBuilder()
    @JvmStatic public fun default(): FilesystemInfo = defaultInstance
  }
}

// -- Policy: Access Types, Domains, Scopes -- //

/** Enumerates types of file access supported via the VFS layer. */
@Serializable public enum class AccessType {
  /** Specifies a read operation. */
  READ,

  /** Specifies a write operation (non-delete). */
  WRITE,

  /** Specifies a write operation which deletes data. */
  DELETE,
}

/** Enumerates access domains, which indicate where a VFS request originates from. */
@Serializable public enum class AccessDomain {
  /** Specifies an operation originating from host application code. */
  HOST,

  /** Specifies an operation originating from guest application code. */
  GUEST,
}

/** Enumerates access scopes, which indicate the type of file system object an operation is being performed on. */
@Serializable public enum class AccessScope {
  /** Unspecified, or unknown, access scope. */
  UNSPECIFIED,

  /** The operation relates to a specific file. */
  FILE,

  /** The operation relates to a specific directory. */
  DIRECTORY,
}

/** Specifies an access request for a resource managed by the virtual file system layer. */
@Serializable @JvmRecord public data class AccessRequest(
  /** Access types needed for this operation. */
  val type: SortedSet<AccessType>,

  /** Access domain from which this operation originates. */
  val domain: AccessDomain,

  /** Access scope, indicating the resource type relating to this request, if known. */
  val scope: AccessScope,

  /** Specifies the path to which this request relates. */
  val path: Path,
) : Comparable<AccessRequest> {
  /** Whether this operation constitutes a read, with no writes. */
  val isRead: Boolean get() = type.contains(AccessType.READ) && !isWrite

  /** Whether this operation constitutes a write operation (or delete). */
  val isWrite: Boolean get() = type.contains(AccessType.WRITE) || type.contains(AccessType.DELETE)

  override fun compareTo(other: AccessRequest): Int {
    return path.compareTo(other.path)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AccessRequest

    if (type != other.type) return false
    if (domain != other.domain) return false
    if (scope != other.scope) return false
    if (path != other.path) return false

    return true
  }

  override fun hashCode(): Int {
    var result = type.hashCode()
    result = 31 * result + domain.hashCode()
    result = 31 * result + scope.hashCode()
    result = 31 * result + path.hashCode()
    return result
  }
}

/** Enumerates result types for a virtual file system access request. */
public enum class AccessResult {
  /** The access request is allowed. */
  ALLOW,

  /** The access request is denied. */
  DENY,
}

/** Describes a full response to a virtual file-system [AccessRequest]. */
@Serializable @JvmRecord public data class AccessResponse private constructor (
  /** Specifies the ultimate [AccessResult] for a given VFS [AccessRequest]. */
  public val policy: AccessResult,

  /** Specifies the reason the [policy] was given as the result, if known. */
  public val reason: String? = null,

  /** Specifies the error which produced the [policy] and/or [reason], if known and applicable. */
  @Transient public val err: Throwable? = null,
) {
  internal companion object {
    private val ALLOW_DEFAULT: AccessResponse = AccessResponse(AccessResult.ALLOW)
    private val DENY_DEFAULT: AccessResponse = AccessResponse(AccessResult.DENY)

    /** Create an [AccessResult.ALLOW]-typed [AccessResponse], optionally with the provided [reason]. */
    @JvmStatic fun allow(reason: String? = null): AccessResponse = if (reason.isNullOrBlank()) {
      ALLOW_DEFAULT
    } else {
      AccessResponse(AccessResult.ALLOW, reason)
    }

    /** Create an [AccessResult.DENY]-typed [AccessResponse], optionally with the provided [reason]. */
    @JvmStatic fun deny(reason: String? = null): AccessResponse = if (reason.isNullOrBlank()) {
      DENY_DEFAULT
    } else {
      AccessResponse(AccessResult.DENY, reason)
    }

    /** Create an [AccessResult.DENY]-typed [AccessResponse], with optional extras, indicating an unsupported call. */
    @JvmStatic fun unsupported(err: Throwable? = null, reason: String? = null): AccessResponse = AccessResponse(
      policy = AccessResult.DENY,
      reason = reason,
      err = err,
    )
  }
}

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
@file:Suppress("DEPRECATION")

package elide.runtime.gvm.internals.vfs

import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import elide.core.api.Symbolic

// Whether to use the legacy protobuf types for VFS.
private const val USE_PROTOBUF = true

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

  /** Access the old (deprecated) enum. */
  @Deprecated("Use the native `CompressionMode` enum instead")
  public val protoEnum: tools.elide.data.CompressionMode get() = when (this) {
    IDENTITY -> tools.elide.data.CompressionMode.IDENTITY
    GZIP -> tools.elide.data.CompressionMode.GZIP
    BROTLI -> tools.elide.data.CompressionMode.BROTLI
    SNAPPY -> tools.elide.data.CompressionMode.SNAPPY
    DEFLATE -> tools.elide.data.CompressionMode.DEFLATE
  }

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
public interface FileTimestampAPI {
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
  /** @return Legacy proto for this timestamp. */
  @Deprecated("Stop using protobuf for vfs")
  public fun toProto(): com.google.protobuf.Timestamp

  /** Mutable builder for file timestamps. */
  public sealed interface Builder : FileTimestampAPI {
    override var seconds: Long
    override var nanos: Int
    public fun build(): FileTimestamp
  }

  /** Defines a [FileTimestamp] backed by a protocol buffer. */
  @JvmInline public value class ProtoTimestamp(private val ts: com.google.protobuf.Timestamp): FileTimestamp {
    override val seconds: Long get() = ts.seconds
    override val nanos: Int get() = ts.nanos

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): com.google.protobuf.Timestamp = ts
  }

  /** Defines a [FileTimestamp] builder backed by a protocol buffer. */
  @JvmInline public value class ProtoTimestampBuilder(
    private val builder: com.google.protobuf.Timestamp.Builder
  ): Builder {
    override var seconds: Long
      get() = builder.seconds
      set(value) { builder.seconds = value }

    override var nanos: Int
      get() = builder.nanos
      set(value) { builder.nanos = value }

    override fun build(): FileTimestamp = ProtoTimestamp(builder.build())
  }

  /** Defines a [FileTimestamp] builder backed by JVM data. */
  @JvmInline public value class NativeTimestamp(private val pair: Pair<Long, Int>): FileTimestamp {
    override val seconds: Long get() = pair.first
    override val nanos: Int get() = pair.second

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): com.google.protobuf.Timestamp = com.google.protobuf.Timestamp.newBuilder()
      .setSeconds(seconds)
      .setNanos(nanos)
      .build()
  }

  /** Defines a [FileTimestamp] builder backed by JVM data. */
  public data class NativeTimestampBuilder(
    override var seconds: Long = 0,
    override var nanos: Int = 0,
  ): Builder {
    override fun build(): FileTimestamp = NativeTimestamp(seconds to nanos)
  }

  public companion object {
    /** @return Timestamp wrapping the provided [ts]. */
    @Deprecated("Stop using protobuf for vfs")
    @JvmStatic public fun wrapping(ts: com.google.protobuf.Timestamp): FileTimestamp =
      ProtoTimestamp(ts)

    @JvmStatic public fun native(): Builder = NativeTimestampBuilder()
    @JvmStatic public fun proto(builder: com.google.protobuf.Timestamp.Builder? = null): Builder =
      ProtoTimestampBuilder(builder ?: com.google.protobuf.Timestamp.newBuilder())
    @JvmStatic public fun newBuilder(): Builder = if (USE_PROTOBUF) proto() else native()
  }
}

/**
 * File system record API.
 *
 * API adhered to by both files and directories.
 */
public interface FileSystemRecordAPI {
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
public interface FileRecordAPI : FileSystemRecordAPI {
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
   * Convert this file record to a builder.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toBuilder(): Builder

  /**
   * Convert this file into a V1 proto.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toProto(): tools.elide.vfs.File

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
  @JvmRecord public data class NativeFileRecord(
    override val name: String,
    override val size: Long = 0,
    override val offset: Long = 0,
    override val modified: FileTimestamp? = null,
  ): FileRecord {
    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.File = tools.elide.vfs.File.newBuilder()
      .setName(name)
      .setSize(size)
      .setOffset(offset)
      .build()

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = DefaultFileRecordBuilder(
      name = name,
      size = size,
      offset = offset,
      modified = modified,
    )
  }

  /** Implements a default [FileRecord] builder. */
  public data class DefaultFileRecordBuilder(
    override var name: String = "",
    override var size: Long = 0,
    override var offset: Long = 0,
    override var modified: FileTimestamp? = null,
  ): Builder {
    override fun build(): FileRecord = NativeFileRecord(
      name = name,
      size = size,
      offset = offset,
      modified = modified,
    )
  }

  /** Implements a [FileRecord] backed by a protocol buffer. */
  @JvmInline public value class ProtoFileRecord(private val file: tools.elide.vfs.File): FileRecord {
    override val name: String get() = file.name
    override val size: Long get() = file.size
    override val offset: Long get() = file.offset
    override val modified: FileTimestamp? get() = FileTimestamp.wrapping(file.modified)

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = proto(file.toBuilder())

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.File = file
  }

  /** Implements a default [FileRecord] builder. */
  @JvmInline public value class ProtoFileRecordBuilder(private val builder: tools.elide.vfs.File.Builder):
    Builder {

    override var name: String
      get() = builder.name
      set(value) { builder.name = value }

    override var size: Long
      get() = builder.size
      set(value) { builder.size = value }

    override var offset: Long
      get() = builder.offset
      set(value) { builder.offset = value }

    override var modified: FileTimestamp?
      get() = if (builder.hasModified()) FileTimestamp.wrapping(builder.modified) else null
      set(value) {
        if (value == null) {
          builder.clearModified()
        } else {
          builder.setModified(value.toProto())
        }
      }

    override fun build(): FileRecord = ProtoFileRecord(builder.build())
  }

  /** Factory methods for [FileRecord]. */
  public companion object {
    /** @return Empty native [FileRecord] builder. */
    @JvmStatic public fun native(): Builder = DefaultFileRecordBuilder()

    /** @return Empty native [FileRecord] builder which uses protocol buffers. */
    @Deprecated("Stop using protobuf for vfs")
    @JvmStatic public fun proto(builder: tools.elide.vfs.File.Builder? = null): Builder =
      ProtoFileRecordBuilder(builder ?: tools.elide.vfs.File.newBuilder())

    /** @return Empty [FileRecord] builder. */
    @JvmStatic public fun newBuilder(): Builder = if (USE_PROTOBUF) proto() else native()
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

  /** Access the old (deprecated) enum. */
  @Deprecated("Use the native `HashAlgorithm` enum instead")
  public val protoEnum: tools.elide.std.HashAlgorithm get() = when (this) {
    IDENTITY -> tools.elide.std.HashAlgorithm.IDENTITY
    MD5 -> tools.elide.std.HashAlgorithm.MD5
    SHA1 -> tools.elide.std.HashAlgorithm.SHA1
    SHA2 -> tools.elide.std.HashAlgorithm.SHA2
    SHA256 -> tools.elide.std.HashAlgorithm.SHA256
    SHA384 -> tools.elide.std.HashAlgorithm.SHA384
    SHA512 -> tools.elide.std.HashAlgorithm.SHA512
    SHA3_224 -> tools.elide.std.HashAlgorithm.SHA3_224
    SHA3_256 -> tools.elide.std.HashAlgorithm.SHA3_256
    SHA3_512 -> tools.elide.std.HashAlgorithm.SHA3_512
  }

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
public interface TreeEntryAPI {
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
   * Convert this tree entry to a builder.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toBuilder(): Builder

  /**
   * Convert this tree entry to a legacy protocol buffer.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toProto(): tools.elide.vfs.TreeEntry

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

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = DefaultTreeEntryBuilder()

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.TreeEntry = tools.elide.vfs.TreeEntry.newBuilder().let {
      it.directory = DirectoryRecord.newBuilder().apply {
        name = "/"
      }.build().toProto()
      it.build()
    }
  }

  /** Implements a [TreeEntry] backed by regular JVM data. */
  public sealed interface JvmTreeEntry : TreeEntry {
    /** Sealed [JvmTreeEntry] holding a [DirectoryRecord]. */
    @JvmInline public value class Dir(private val dir: DirectoryRecord) : JvmTreeEntry {
      override val directory: DirectoryRecord get() = dir
      override val file: FileRecord get() = error("Cannot access `file` on `Dir`")
      override val type: EntryCase get() = EntryCase.DIRECTORY
    }

    /** Sealed [JvmTreeEntry] holding a [FileRecord]. */
    @JvmInline public value class File(private val backing: FileRecord) : JvmTreeEntry {
      override val directory: DirectoryRecord get() = error("Cannot access `file` on `File`")
      override val file: FileRecord get() = backing
      override val type: EntryCase get() = EntryCase.FILE
    }

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = DefaultTreeEntryBuilder(
      directory = directory,
      file = file,
    )

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.TreeEntry = tools.elide.vfs.TreeEntry.newBuilder().let {
      when (type) {
        EntryCase.FILE -> it.file = file.toProto()
        EntryCase.DIRECTORY -> it.directory = directory.toProto()
      }
      it.build()
    }
  }

  /** Implements a default [TreeEntry] builder. */
  public data class DefaultTreeEntryBuilder(
    override var directory: DirectoryRecord? = null,
    override var file: FileRecord? = null,
  ) : Builder {
    override val type: EntryCase? get() = when {
      directory != null -> EntryCase.DIRECTORY
      file != null -> EntryCase.FILE
      else -> null
    }

    override fun build(): TreeEntry = when (type) {
      EntryCase.DIRECTORY -> JvmTreeEntry.Dir(requireNotNull(directory))
      EntryCase.FILE -> JvmTreeEntry.File(requireNotNull(file))
      else -> error("No directory or file set")
    }
  }

  /** Implements a [TreeEntry] backed by a protocol buffer. */
  @JvmInline public value class ProtoTreeEntry(private val entry: tools.elide.vfs.TreeEntry): TreeEntry {
    override val type: EntryCase get() = EntryCase.resolve(entry.entryCase.number)
    override fun hasDirectory(): Boolean = entry.hasDirectory()
    override fun hasFile(): Boolean = entry.hasFile()

    override val directory: DirectoryRecord get() =
      if (entry.hasDirectory()) DirectoryRecord.ProtoDirectoryRecord(entry.directory) else error("No directory set")
    override val file: FileRecord get() =
      if (entry.hasFile()) FileRecord.ProtoFileRecord(entry.file) else error("No file set")

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = proto(entry.toBuilder())

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.TreeEntry = entry
  }

  /** Implements a [TreeEntry] builder backed by a protocol buffer. */
  @JvmInline public value class ProtoTreeEntryBuilder(private val builder: tools.elide.vfs.TreeEntry.Builder): Builder {
    override var directory: DirectoryRecord?
      get() = if (builder.hasDirectory())
        DirectoryRecord.ProtoDirectoryRecord(builder.directory) else null
      set(value) {
        when (value) {
          null -> builder.clearDirectory()
          else -> builder.directory = value.toProto()
        }
      }

    override var file: FileRecord?
      get() = if (builder.hasFile()) FileRecord.ProtoFileRecord(builder.file) else null

      set(value) {
        when (value) {
          null -> builder.clearFile()
          else -> builder.file = value.toProto()
        }
      }

    override val type: EntryCase get() = EntryCase.resolve(builder.entryCase.number)
    override fun build(): TreeEntry = ProtoTreeEntry(builder.build())
  }

  public companion object {
    @JvmStatic public fun native(): Builder = DefaultTreeEntryBuilder()

    @JvmStatic public fun proto(builder: tools.elide.vfs.TreeEntry.Builder? = null): Builder =
      ProtoTreeEntryBuilder(builder ?: tools.elide.vfs.TreeEntry.newBuilder())

    @JvmStatic public fun wrapping(entry: tools.elide.vfs.TreeEntry): ProtoTreeEntry =
      ProtoTreeEntry(entry)

    @JvmStatic public fun newBuilder(): Builder = if (USE_PROTOBUF) proto() else native()
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

  /** @return The old (deprecated) enum for this entry case. */
  @Deprecated("Use the native `EntryCase` enum instead")
  public val protoEnum: tools.elide.vfs.TreeEntry.EntryCase get() = when (this) {
    FILE -> tools.elide.vfs.TreeEntry.EntryCase.FILE
    DIRECTORY -> tools.elide.vfs.TreeEntry.EntryCase.DIRECTORY
  }

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
public interface DirectoryRecordAPI : FileSystemRecordAPI {
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
   * Convert this directory record to a builder.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toBuilder(): Builder

  /**
   * Convert this directory record to a legacy protocol buffer.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toProto(): tools.elide.vfs.Directory

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
  @JvmRecord public data class NativeDirectoryRecord(
    override val name: String,
    override val childrenList: List<TreeEntry> = emptyList(),
  ): DirectoryRecord {
    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.Directory = tools.elide.vfs.Directory.newBuilder()
      .setName(name)
      .addAllChildren(childrenList.map { it.toProto() })
      .build()

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = DefaultDirectoryRecordBuilder(
      name = name,
      childrenList = childrenList.toMutableList(),
    )
  }

  /** Implements a default [DirectoryRecord] builder. */
  public data class DefaultDirectoryRecordBuilder(
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

    override fun build(): DirectoryRecord = NativeDirectoryRecord(
      name = name,
      childrenList = childrenList,
    )
  }

  /** Implements a [DirectoryRecord] backed by a protocol buffer. */
  @JvmInline public value class ProtoDirectoryRecord(private val dir: tools.elide.vfs.Directory): DirectoryRecord {
    override val name: String get() = dir.name
    override val childrenList: List<TreeEntry> get() = dir.childrenList.map { TreeEntry.wrapping(it) }

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = proto(dir.toBuilder())

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.Directory = dir
  }

  /** Implements a [DirectoryRecord] builder backed by a protocol buffer. */
  @JvmInline public value class ProtoDirectoryRecordBuilder(
    private val builder: tools.elide.vfs.Directory.Builder
  ): Builder {
    override var name: String
      get() = builder.name
      set(value) { builder.name = value }

    override var childrenList: MutableList<TreeEntry>
      get() = builder.childrenList.map { TreeEntry.wrapping(it) }.toMutableList()
      set(value) { builder.addAllChildren(value.map { it.toProto() }) }

    override fun addChildren(children: List<TreeEntry>): Builder = apply {
      builder.addAllChildren(children.map { it.toProto() })
    }

    override fun addChildren(builder: TreeEntry.Builder): Builder = apply {
      this.builder.addChildren(builder.build().toProto())
    }

    override fun addChildren(entry: TreeEntry): Builder = apply {
      this.builder.addChildren(entry.toProto())
    }

    override fun build(): DirectoryRecord = ProtoDirectoryRecord(builder.build())
  }

  public companion object {
    @JvmStatic public fun native(): Builder = DefaultDirectoryRecordBuilder()

    @Deprecated("Stop using protobuf for vfs")
    @JvmStatic public fun proto(builder: tools.elide.vfs.Directory.Builder? = null): Builder =
      ProtoDirectoryRecordBuilder(builder ?: tools.elide.vfs.Directory.newBuilder())

    @JvmStatic public fun newBuilder(): Builder = if (USE_PROTOBUF) proto() else native()
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
public interface FilesystemMetadataAPI {

}

/**
 * Filesystem metadata.
 */
@Serializable public sealed interface FilesystemMetadata {
  /**
   * Convert this filesystem metadata to a builder.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toBuilder(): Builder

  /**
   * Convert this filesystem metadata to a legacy protocol buffer.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toProto(): tools.elide.vfs.Filesystem.Metadata

  /**
   * Filesystem metadata builder.
   */
  public sealed interface Builder : FilesystemMetadataAPI

  /** Implements a [FilesystemMetadata] backed by a protocol buffer. */
  @JvmInline public value class ProtoFilesystemMetadata(
    private val meta: tools.elide.vfs.Filesystem.Metadata
  ) : FilesystemMetadata {
    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = proto(meta.toBuilder())

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.Filesystem.Metadata = meta
  }

  /** Implements a [FilesystemMetadata] builder backed by a protocol buffer. */
  @JvmInline public value class ProtoFilesystemMetadataBuilder(
    private val builder: tools.elide.vfs.Filesystem.Metadata.Builder,
  ) : Builder {
    //
  }

  /** Implements a [FilesystemMetadata] backed by regular JVM data. */
  public class DefaultFilesystemMetadataBuilder: Builder

  /** Implements a [FilesystemMetadata] backed by regular JVM data. */
  public class NativeFilesystemMetadata: FilesystemMetadata {
    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder =
      TODO("Not yet implemented: `NativeFilesystemMetadata.toBuilder`")

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.Filesystem.Metadata =
      TODO("Not yet implemented: `NativeFilesystemMetadata.toProto`")
  }

  public companion object {
    @JvmStatic public fun wrapping(meta: tools.elide.vfs.Filesystem.Metadata): ProtoFilesystemMetadata =
      ProtoFilesystemMetadata(meta)
    @JvmStatic public fun proto(builder: tools.elide.vfs.Filesystem.Metadata.Builder? = null): Builder =
      ProtoFilesystemMetadataBuilder(builder ?: tools.elide.vfs.Filesystem.Metadata.newBuilder())
    @JvmStatic public fun native(): Builder = DefaultFilesystemMetadataBuilder()
    @JvmStatic public fun newBuilder(): Builder = if (USE_PROTOBUF) proto() else native()
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
   * Convert this filesystem info to a builder.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toBuilder(): Builder

  /**
   * Convert this filesystem info to a legacy protocol buffer.
   */
  @Deprecated("Stop using protobuf for vfs")
  public fun toProto(): tools.elide.vfs.Filesystem

  /**
   * Filesystem info builder.
   */
  public sealed interface Builder : FilesystemInfoAPI {
    override var root: TreeEntry?
    override var metadata: FilesystemMetadata?
    public fun build(): FilesystemInfo
  }

  /** Implements a [FilesystemInfo] backed by regular JVM data. */
  public data class DefaultFilesystemInfoBuilder(
    override var root: TreeEntry? = null,
    override var metadata: FilesystemMetadata? = null,
  ): Builder {
    override fun build(): FilesystemInfo = NativeFilesystemInfo.of(
      root = root ?: TreeEntry.None,
      metadata = metadata,
    )
  }

  /** Implements a [FilesystemInfo] backed by regular JVM data. */
  @JvmInline public value class NativeFilesystemInfo private constructor (
    private val pair: Pair<TreeEntry, FilesystemMetadata?>,
  ): FilesystemInfo {
    override val root: TreeEntry get() = pair.first
    override val metadata: FilesystemMetadata? get() = pair.second

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.Filesystem = tools.elide.vfs.Filesystem.newBuilder()
      .setRoot(root.toProto())
      .let { builder ->
        metadata?.let {
          builder.setMetadata(it.toProto())
        }
        builder
      }
      .build()

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = DefaultFilesystemInfoBuilder(root, metadata)

    public companion object {
      @JvmStatic public fun of(root: TreeEntry, metadata: FilesystemMetadata?): FilesystemInfo =
        NativeFilesystemInfo(root to metadata)
    }
  }

  /** Implements a [FilesystemInfo] backed by a protocol buffer. */
  @JvmInline public value class FilesystemInfoProtoBuilder(
    private val builder: tools.elide.vfs.Filesystem.Builder,
  ): Builder {
    override var root: TreeEntry?
      get() = TreeEntry.wrapping(builder.root)
      set(value) {
        when (value) {
          null -> builder.clearRoot()
          else -> builder.root = value.toProto()
        }
      }

    override var metadata: FilesystemMetadata?
      get() = FilesystemMetadata.wrapping(builder.metadata)
      set(value) {
        when (value) {
          null -> builder.clearMetadata()
          else -> builder.metadata = value.toProto()
        }
      }

    override fun build(): FilesystemInfo = FilesystemInfoProto(builder.build())
  }

  /** Implements a [FilesystemInfo] backed by a protocol buffer. */
  @JvmInline public value class FilesystemInfoProto(private val fs: tools.elide.vfs.Filesystem): FilesystemInfo {
    override val root: TreeEntry get() = TreeEntry.wrapping(fs.root)
    override val metadata: FilesystemMetadata? get() = when (fs.hasMetadata()) {
      true -> FilesystemMetadata.wrapping(fs.metadata)
      else -> null
    }

    @Deprecated("Stop using protobuf for vfs")
    override fun toBuilder(): Builder = proto(fs.toBuilder())

    @Deprecated("Stop using protobuf for vfs")
    override fun toProto(): tools.elide.vfs.Filesystem = fs
  }

  public companion object {
    // Default empty FS instance.
    private val defaultInstance by lazy { native().build() }

    @Deprecated("Stop using protobuf for vfs")
    @JvmStatic public fun proto(builder: tools.elide.vfs.Filesystem.Builder? = null): Builder =
      FilesystemInfoProtoBuilder(builder ?: tools.elide.vfs.Filesystem.newBuilder())
    @JvmStatic public fun native(): Builder = DefaultFilesystemInfoBuilder()
    @JvmStatic public fun newBuilder(): Builder = proto()
    @JvmStatic public fun default(): FilesystemInfo = if (USE_PROTOBUF) proto(
      tools.elide.vfs.Filesystem.getDefaultInstance().toBuilder()
    ).build() else defaultInstance
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
  val type: Set<AccessType>,

  /** Access domain from which this operation originates. */
  val domain: AccessDomain,

  /** Access scope, indicating the resource type relating to this request, if known. */
  val scope: AccessScope,

  /** Specifies the path to which this request relates. */
  val path: Path,
) {
  /** Whether this operation constitutes a read, with no writes. */
  val isRead: Boolean get() = type.contains(AccessType.READ) && !isWrite

  /** Whether this operation constitutes a write operation (or delete). */
  val isWrite: Boolean get() = type.contains(AccessType.WRITE) || type.contains(AccessType.DELETE)
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

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

package elide.wasm

import elide.wasm.internal.*
import elide.wasm.internal.OFlag
import elide.wasm.internal.Right
import elide.wasm.internal.pathCreateDirectory
import elide.wasm.internal.pathOpen

/**
 * File size or length of a region within a file.
 */
typealias Filesize = ULong

/**
 * Filesystem object serial number that is unique within its file system.
 */
typealias INode = ULong

/**
 * A descriptor is a reference to a filesystem object, which may be a file,
 * directory, named pipe, special file, or other object on which filesystem
 * calls may be made.
 */
typealias Descriptor = Int

/**
 * Open flags used by `openAt`.
 */
data class OpenFlags(

    /** Create file if it does not exist. */
    val create: Boolean = false,

    /** Fail if not a directory */
    val directory: Boolean = false,

    /** Fail if file already exists. */
    val exclude: Boolean = false,

    /** Truncate file to size 0. */
    val truncate: Boolean = false,
)

/**
 * Flags determining the method of how paths are resolved.
 */
data class PathFlags(

    /** As long as the resolved path corresponds to a symbolic link, it is expanded. */
    val symlinkFollow: Boolean,
)

/**
 * Standard descriptors.
 */
object StandardDescriptor {

    /** Standard input file descriptor. */
    const val STDIN: Descriptor = 0

    /** Standard output file descriptor. */
    const val STDOUT: Descriptor = 1

    /** Standard error file descriptor. */
    const val STDERR: Descriptor = 2

    /** First preopen file descriptor. */
    const val FIRST_PREOPEN: Descriptor = 3
}

/** The type of a filesystem object referenced by a descriptor.
 *
 * Note: This was called `filetype` in earlier versions of WASI.
 */
enum class DescriptorType {
    /**
     * The type of the descriptor or file is unknown or is different from
     * any of the other types specified.
     */
    UNKNOWN,

    /**
     * The descriptor refers to a block device inode.
     */
    BLOCK_DEVICE,

    /**
     * The descriptor refers to a character device inode.
     */
    CHARACTER_DEVICE,

    /**
     * The descriptor refers to a directory inode.
     */
    DIRECTORY,

    /**
     * The descriptor refers to a named pipe.
     */
    FIFO,

    /**
     * The file refers to a symbolic link inode.
     */
    SYMBOLIC_LINK,

    /**
     * The descriptor refers to a regular file inode.
     */
    REGULAR_FILE,
}

/**
 * Descriptor flags.
 *
 * Note: This was called `fdflags` in earlier versions of WASI.
 */
data class DescriptorFlags(
    /** Read mode: Data can be read. */
    val read: Boolean = false,

    /** Write mode: Data can be written to. */
    val write: Boolean = false,

    /**
     * Requests non-blocking operation.
     *
     * When this flag is enabled, functions may return immediately with an
     * `error-code::would-block` error code in situations where they would otherwise
     * block. However, this non-blocking behavior is not required.
     * Implementations are permitted to ignore this flag and block.
     */
     val nonBlocking: Boolean = false,

     /**
      * Request that writes be performed according to synchronized I/O file
      * integrity completion. The data stored in the file and the file's
      * metadata are synchronized.
      *
      * The precise semantics of this operation have not yet been defined for
      * WASI. At this time, it should be interpreted as a request, and not a
      * requirement.
      */
    val fileIntegritySync: Boolean = false,

    /**
     * Request that writes be performed according to synchronized I/O data
     * integrity completion. Only the data stored in the file is
     * synchronized.
     *
     * The precise semantics of this operation have not yet been defined for
     * WASI. At this time, it should be interpreted as a request, and not a
     * requirement.
     */
    val dataIntegritySync: Boolean = false,

    /**
     * Requests that reads be performed at the same level of integrety
     * requested for writes.
     *
     * The precise semantics of this operation have not yet been defined for
     * WASI. At this time, it should be interpreted as a request, and not a
     * requirement.
     */
    val requestedWriteSync: Boolean = false,

    /**
     * Mutating directories mode: Directory contents may be mutated.
     *
     * When this flag is unset on a descriptor, operations using the
     * descriptor which would create, rename, delete, modify the data or
     * metadata of filesystem objects, or obtain another handle which
     * would permit any of those, shall fail with `error-code::read-only` if
     * they would otherwise succeed.
     *
     * This may only be set on directories.
     */
    val mutateDirectory: Boolean = true,
)

/** Permissions mode used by `openAt`, `changeFilePermissionsAt`, and similar. **/
data class Modes(

    /** True if the resource is considered readable by the containing filesystem. */
    val readable: Boolean = false,

    /**  True if the resource is considered writeable by the containing filesystem. */
    val writeable: Boolean = false,

    /** True if the resource is considered executable by the containing filesystem. This does not apply to directories. */
    val executable: Boolean = false,
)

/**
 * A directory entry.
 */
data class DirectoryEntry(

    /**
     * The serial number of the object referred to by this directory entry.
     * May be none if the inode value is not known.
     *
     * When this is none, libc implementations might do an extra `stat-at`
     * call to retrieve the inode number to fill their `d_ino` fields, so
     * implementations which can set this to a non-none value should do so.
     */
    val inode: INode?,

    /**
     * The type of the file referred to by this directory entry.
     */
    val type: DescriptorType,

    /**
     * The name of the object.
     */
    val name: String,
)

/**
 * Error codes returned by functions.
 * Not all of these error codes are returned by the functions provided by this
 * API; some are used in higher-level library layers, and others are provided
 * merely for alignment with POSIX.
 */
enum class ErrorCode {
    /** Permission denied. */
    ACCESS,

    /** Resource unavailable, or operation would block. */
    WOULD_BLOCK,

    /** Connection already in progress. */
    ALREADY,

    /**  Bad descriptor. */
    BAD_DESCRIPTOR,

    /** Device or resource busy. */
    BUSY,

    /** Resource deadlock would occur. */
    DEADLOCK,

    /** Storage quota exceeded. */
    QUOTA,

    /** File exists. */
    EXIST,

    /** File too large. */
    FILE_TOO_LARGE,

    /** Illegal byte sequence. */
    ILLEGAL_BYTE_SEQUENCE,

    /** Operation in progress. */
    IN_PROGRESS,

    /** Interrupted function. */
    INTERRUPTED,

    /** Invalid argument. */
    INVALID,

    /** I/O error. */
    IO,

    /** Is a directory. */
    IS_DIRECTORY,

    /** Too many levels of symbolic links. */
    LOOP,

    /** Too many links. */
    TOO_MANY_LINKS,

    /** Message too large. */
    MESSAGE_SIZE,

    /** Filename too long. */
    NAME_TOO_LONG,

    /** No such device. */
    NO_DEVICE,

    /** No such file or directory. */
    NO_ENTRY,

    /** No locks available. */
    NO_LOCK,

    /** Not enough space. */
    INSUFFICIENT_MEMORY,

    /** No space left on device. */
    INSUFFICIENT_SPACE,

    /** Not a directory or a symbolic link to a directory. */
    NOT_DIRECTORY,

    /** Directory not empty. */
    NOT_EMPTY,

    /** State not recoverable. */
    NOT_RECOVERABLE,

    /** Not supported */
    UNSUPPORTED,

    /** Inappropriate I/O control operation. */
    NO_TTY,

    /** No such device or address. */
    NO_SUCH_DEVICE,

    /** Value too large to be stored in data type. */
    OVERFLOW,

    /** Operation not permitted. */
    NOT_PERMITTED,

    /** Broken pipe. */
    PIPE,

    /** Read-only file system. */
    READ_ONLY,

    /** Invalid seek. */
    INVALID_SEEK,

    /** Text file busy. */
    TEXT_FILE_BUSY,

    /** Cross-device link. */
    CROSS_DEVICE,
}

/**
 * Result of a read operation.
 *
 * @param data The data read.
 * @param eof True if the end of the file has been reached.
 */
data class ReadResult(val data: ByteArray, val eof: Boolean) {
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + eof.hashCode()
        return result
    }
}

interface WasiFileSystem {

    /**
     * Create a directory.
     * @param path The relative path at which to create the directory.
     * @param descriptor Reference to the parent directory where the new one should be created.
     */
    fun createDirectoryAt(path: String, descriptor: Descriptor = StandardDescriptor.FIRST_PREOPEN)

    /**
     * Open a file or directory.
     *
     * The returned descriptor is not guaranteed to be the lowest-numbered
     * descriptor not currently open/ it is randomized to prevent applications
     * from depending on making assumptions about indexes, since this is
     * error-prone in multi-threaded contexts. The returned descriptor is
     * guaranteed to be less than 2**31.
     *
     * If `flags` contains [DescriptorFlags.mutateDirectory], and the base
     * descriptor doesn't have [DescriptorFlags.mutateDirectory] set,
     * `openAt` fails with [ErrorCode.READ_ONLY].
     *
     * If `flags` contains [DescriptorFlags.write], or `openFlags` contains [OpenFlags.truncate]
     * or [OpenFlags.create], and the base descriptor doesn't have
     * [DescriptorFlags.mutateDirectory] set, `openAt` fails with [ErrorCode.READ_ONLY].
     *
     * Note: This is similar to `openat` in POSIX.
     *
     * TODO Support modes
     *
     * @param path The relative path of the object to open.
     * @param descriptor Reference to the parent directory where the new one should be created.
     * @param openFlags The method by which to open the file.
     * @param flags Flags to use for the resulting descriptor.
     * @param pathFlags Flags determining the method of how the path is resolved.
     */
    fun openAt(
      path: String,
      openFlags: OpenFlags = OpenFlags(),
               flags: DescriptorFlags = DescriptorFlags(read = true),
      pathFlags: PathFlags = PathFlags(true),
      descriptor: Descriptor = StandardDescriptor.FIRST_PREOPEN,
    ): Descriptor

    /**
     * Read directory entries from a directory
     */
    fun readDirectory(
      path: String = "",
      descriptor: Descriptor = StandardDescriptor.FIRST_PREOPEN,
    ): List<DirectoryEntry>

    /**
     * Write to a descriptor, without using and updating the descriptor's offset.
     *
     * It is valid to write past the end of a file; the file is extended to the
     * extent of the write, with bytes between the previous end and the start of
     * the write set to zero.
     *
     * Note: This is similar to `pwrite` in POSIX.
     *
     * @param descriptor The descriptor to write to.
     * @param buffer The data to write.
     */
    fun write(descriptor: Descriptor, buffer: ByteArray, offset: Filesize = 0u): Filesize

    /**
     * Read from a descriptor, without using and updating the descriptor's offset.
     *
     * This function returns a list of bytes containing the data that was
     * read, along with a bool which, when true, indicates that the end of the
     * file was reached. The returned list will contain up to `length` bytes; it
     * may return fewer than requested, if the end of the file is reached or
     * if the I/O operation is interrupted.
     *
     * Note: This is similar to `pread` in POSIX.
     */
    fun read(descriptor: Descriptor, length: Filesize, offset: Filesize = 0u): ReadResult
}

object DefaultWasiFilesystem : WasiFileSystem {

    override fun createDirectoryAt(path: String, descriptor: Descriptor) {
        pathCreateDirectory(descriptor, path)
    }

    override fun openAt(path: String, openFlags: OpenFlags, flags: DescriptorFlags, pathFlags: PathFlags, descriptor: Descriptor): Descriptor {
        return pathOpen(
          fd = descriptor, dirflags = pathFlags.toLookupFlags(), path = path,
            oflags = openFlags.toOFlags(), fsRightsBase = flags.toRights(), fsRightsInheriting = 0, fdflags = flags.toFdflags(),
        )
    }

    override fun readDirectory(path: String, descriptor: Descriptor): List<DirectoryEntry> {
        val fd = pathOpen(
          fd = descriptor, dirflags = 0, path = path,
            oflags = OFlag.DIRECTORY,
            fsRightsBase = Right.FD_READDIR,
            fsRightsInheriting = 0,
            fdflags = 0,
        )
        return fdReadDir(fd).map { (entry, name) ->
            DirectoryEntry(entry.inode.toULong(), entry.type.toDescriptorType(), name)
        }
    }

    override fun write(descriptor: Descriptor, buffer: ByteArray, offset: Filesize): Filesize {
        return fdPWrite(descriptor, listOf(buffer), offset).toULong()
    }

    override fun read(descriptor: Descriptor, length: Filesize, offset: Filesize): ReadResult {
        return fdPRead(descriptor, length, offset).let { ReadResult(it.first, it.second < length.toInt()) }
    }

    private fun Filetype.toDescriptorType() = when (this) {
        Filetype.BLOCK_DEVICE -> DescriptorType.BLOCK_DEVICE
        Filetype.CHARACTER_DEVICE -> DescriptorType.CHARACTER_DEVICE
        Filetype.DIRECTORY -> DescriptorType.DIRECTORY
        Filetype.REGULAR_FILE -> DescriptorType.REGULAR_FILE
        Filetype.SYMBOLIC_LINK -> DescriptorType.SYMBOLIC_LINK
        else -> DescriptorType.UNKNOWN
    }

    private fun PathFlags.toLookupFlags(): LookupFlags {
        return if (this.symlinkFollow) LookupFlag.SYMLINK_FOLLOW else 0
    }

    private fun OpenFlags.toOFlags(): OFlags {
        var flags: OFlags = 0
        if (this.create) flags = flags or OFlag.CREAT
        if (this.directory) flags = flags or OFlag.DIRECTORY
        if (this.exclude) flags = flags or OFlag.EXCL
        if (this.truncate) flags = flags or OFlag.TRUNC
        return flags
    }

    private fun DescriptorFlags.toRights(): Rights {
        var rights: Long = 0
        rights = rights or Right.PATH_OPEN
        rights = rights or Right.POLL_FD_READWRITE
        if (this.read) {
            rights = rights or Right.FD_READ
            rights = rights or Right.FD_SEEK
            rights = rights or Right.FD_TELL
            rights = rights or Right.FD_ADVISE
            rights = rights or Right.FD_READDIR
            rights = rights or Right.PATH_READLINK
            rights = rights or Right.PATH_FILESTAT_GET
            rights = rights or Right.FD_FILESTAT_GET
        }
        if (this.write) {
            rights = rights or Right.FD_DATASYNC
            rights = rights or Right.FD_FDSTAT_SET_FLAGS
            rights = rights or Right.FD_SYNC
            rights = rights or Right.FD_WRITE
            rights = rights or Right.FD_ALLOCATE
            rights = rights or Right.PATH_CREATE_DIRECTORY
            rights = rights or Right.PATH_CREATE_FILE
            rights = rights or Right.PATH_LINK_SOURCE
            rights = rights or Right.PATH_LINK_TARGET
            rights = rights or Right.PATH_RENAME_SOURCE
            rights = rights or Right.PATH_RENAME_TARGET
            rights = rights or Right.PATH_FILESTAT_SET_SIZE
            rights = rights or Right.PATH_FILESTAT_SET_TIMES
            rights = rights or Right.FD_FILESTAT_SET_SIZE
            rights = rights or Right.FD_FILESTAT_SET_TIMES
            rights = rights or Right.PATH_SYMLINK
            rights = rights or Right.PATH_REMOVE_DIRECTORY
            rights = rights or Right.PATH_UNLINK_FILE
        }
        return rights
    }

    private fun DescriptorFlags.toFdflags(): Fdflags {
        var flags: Fdflags = 0
        if (this.dataIntegritySync) flags = flags or FdFlag.DSYNC
        if (this.fileIntegritySync) flags = flags or FdFlag.SYNC
        if (this.requestedWriteSync) flags = flags or FdFlag.RSYNC
        if (this.nonBlocking) flags = flags or FdFlag.NONBLOCK
        return flags
    }
}

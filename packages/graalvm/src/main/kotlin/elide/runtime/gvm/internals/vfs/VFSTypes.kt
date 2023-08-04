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

import java.nio.file.Path


// -- Basic: Files, Directories, Trees -- //

/**
 * Type alias: File record.
 *
 * Maps to the underlying structural type used for `File` object metadata within the runtime VFS structure.
 */
internal typealias FileRecord = tools.elide.vfs.File

/**
 * Type alias: Directory record.
 *
 * Maps to the underlying structural type used for `Directory` object metadata within the runtime VFS structure.
 */
internal typealias DirectoryRecord = tools.elide.vfs.Directory

/**
 * Type alias: Tree record.
 *
 * Maps to the underlying structural type used for the generic `Tree` object within the runtime VFS structure.
 */
internal typealias FileTreeEntry = tools.elide.vfs.TreeEntry

/**
 * Type alias: Filesystem.
 *
 * Maps to the top-level file-system type used to express VFS metadata at runtime.
 */
internal typealias FilesystemInfo = tools.elide.vfs.Filesystem


// -- Policy: Access Types, Domains, Scopes -- //

/** Enumerates types of file access supported via the VFS layer. */
public enum class AccessType {
  /** Specifies a read operation. */
  READ,

  /** Specifies a write operation (non-delete). */
  WRITE,

  /** Specifies a write operation which deletes data. */
  DELETE,
}

/** Enumerates access domains, which indicate where a VFS request originates from. */
public enum class AccessDomain {
  /** Specifies an operation originating from host application code. */
  HOST,

  /** Specifies an operation originating from guest application code. */
  GUEST,
}

/** Enumerates access scopes, which indicate the type of file system object an operation is being performed on. */
public enum class AccessScope {
  /** Unspecified, or unknown, access scope. */
  UNSPECIFIED,

  /** The operation relates to a specific file. */
  FILE,

  /** The operation relates to a specific directory. */
  DIRECTORY,
}

/** Specifies an access request for a resource managed by the virtual file system layer. */
public data class AccessRequest(
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
public class AccessResponse private constructor (
  /** Specifies the ultimate [AccessResult] for a given VFS [AccessRequest]. */
  public val policy: AccessResult,

  /** Specifies the reason the [policy] was given as the result, if known. */
  public val reason: String? = null,

  /** Specifies the error which produced the [policy] and/or [reason], if known and applicable. */
  public val err: Throwable? = null,
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

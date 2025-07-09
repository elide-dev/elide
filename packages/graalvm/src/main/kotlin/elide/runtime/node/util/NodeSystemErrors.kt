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
package elide.runtime.node.util

import java.util.LinkedList
import java.util.TreeMap

/**
 * # Node System Errors
 *
 * Describes in static in-memory data structures the known suite of system errors recognized by the Node.js API; such
 * errors come in three parts:
 *
 * - The error "code" or "ID," which is a numeric identifier (a "magnum") for a unique system error case.
 * - The error "name," which is a human-readable name for the system error.
 * - The error "message," which is a human-readable message describing the system error.
 *
 * These structures can be interrogated via Node's `util` module.
 *
 * @see SystemErrorInfo Host-side information about a system error
 */
public object NodeSystemErrors {
  /**
   * ## Node System Errors: Info
   *
   * Holds information about a system error known to the Node.js API.
   *
   * @property id Numeric ID/code for the system error.
   * @property name Name of the system error.
   * @property message Human-readable message describing the system error.
   */
  public interface SystemErrorInfo {
    public val id: Int
    public val name: String
    public val message: String
  }

  // Holds all system errors in a linked list, for iteration.
  private val allErrors: LinkedList<SystemError>

  // Holds system errors mapped to their numeric ID/code.
  private val errorsById: TreeMap<Int, SystemError>

  // Holds system errors mapped to their name.
  private val errorsByName: TreeMap<String, SystemError>

  /**
   * Check whether the known system errors list contains the ID [id].
   *
   * @param id Numeric ID/code for the system error.
   * @return `true` if the system error is known, `false` otherwise.
   */
  public operator fun contains(id: Int): Boolean = id in errorsById

  /**
   * Retrieve the system error info for the given numeric ID/code [id].
   *
   * @param id Numeric ID/code for the system error.
   * @return The [SystemErrorInfo] for the given ID, or `null` if not found.
   */
  public operator fun get(id: Int): SystemErrorInfo? = errorsById[id]

  /**
   * Check whether the known system errors list contains the name [name].
   *
   * @param name Name of the system error.
   * @return `true` if the system error is known, `false` otherwise.
   */
  public operator fun contains(name: String): Boolean = name in errorsByName

  /**
   * Retrieve the system error info for the given name [name].
   *
   * @param name Name of the system error.
   * @return The [SystemErrorInfo] for the given name, or `null` if not found.
   */
  public operator fun get(name: String): SystemErrorInfo? = errorsByName[name]

  /**
   * Export the known system errors as a read-only map from numeric ID to [SystemErrorInfo].
   *
   * @return A read-only map containing all known system errors, keyed by their numeric ID.
   */
  public fun export(): Map<Int, SystemErrorInfo> = errorsById

  /**
   * Export the known system errors as a read-only sequence.
   *
   * @return A read-only sequence containing all known system errors.
   */
  public fun all(): Sequence<SystemErrorInfo> = allErrors.asSequence()

  // Immutable record representing a system error's associated info.
  @JvmRecord internal data class SystemError internal constructor(
    override val id: Int,
    override val name: String,
    override val message: String,
  ) : SystemErrorInfo

  // Error code constants
  public const val E2BIG: Int = -7
  public const val EACCES: Int = -13
  public const val EADDRINUSE: Int = -98
  public const val EADDRNOTAVAIL: Int = -99
  public const val EAFNOSUPPORT: Int = -97
  public const val EAGAIN: Int = -11
  public const val EAI_ADDRFAMILY: Int = -3000
  public const val EAI_AGAIN: Int = -3001
  public const val EAI_BADFLAGS: Int = -3002
  public const val EAI_BADHINTS: Int = -3013
  public const val EAI_CANCELED: Int = -3003
  public const val EAI_FAIL: Int = -3004
  public const val EAI_FAMILY: Int = -3005
  public const val EAI_MEMORY: Int = -3006
  public const val EAI_NODATA: Int = -3007
  public const val EAI_NONAME: Int = -3008
  public const val EAI_OVERFLOW: Int = -3009
  public const val EAI_PROTOCOL: Int = -3014
  public const val EAI_SERVICE: Int = -3010
  public const val EAI_SOCKTYPE: Int = -3011
  public const val EALREADY: Int = -114
  public const val EBADF: Int = -9
  public const val EBUSY: Int = -16
  public const val ECANCELED: Int = -125
  public const val ECHARSET: Int = -4080
  public const val ECONNABORTED: Int = -103
  public const val ECONNREFUSED: Int = -111
  public const val ECONNRESET: Int = -104
  public const val EDESTADDRREQ: Int = -89
  public const val EEXIST: Int = -17
  public const val EFAULT: Int = -14
  public const val EFBIG: Int = -27
  public const val EFTYPE: Int = -4028
  public const val EHOSTDOWN: Int = -112
  public const val EHOSTUNREACH: Int = -113
  public const val EILSEQ: Int = -84
  public const val EINTR: Int = -4
  public const val EINVAL: Int = -22
  public const val EIO: Int = -5
  public const val EISCONN: Int = -106
  public const val EISDIR: Int = -21
  public const val ELOOP: Int = -40
  public const val EMFILE: Int = -24
  public const val EMLINK: Int = -31
  public const val EMSGSIZE: Int = -90
  public const val ENAMETOOLONG: Int = -36
  public const val ENETDOWN: Int = -100
  public const val ENETUNREACH: Int = -101
  public const val ENFILE: Int = -23
  public const val ENOBUFS: Int = -105
  public const val ENODATA: Int = -61
  public const val ENODEV: Int = -19
  public const val ENOENT: Int = -2
  public const val ENOMEM: Int = -12
  public const val ENONET: Int = -64
  public const val ENOPROTOOPT: Int = -92
  public const val ENOSPC: Int = -28
  public const val ENOSYS: Int = -38
  public const val ENOTCONN: Int = -107
  public const val ENOTDIR: Int = -20
  public const val ENOTEMPTY: Int = -39
  public const val ENOTSOCK: Int = -88
  public const val ENOTSUP: Int = -95
  public const val ENOTTY: Int = -25
  public const val ENXIO: Int = -6
  public const val EOF: Int = -4095
  public const val EOVERFLOW: Int = -75
  public const val EPERM: Int = -1
  public const val EPIPE: Int = -32
  public const val EPROTO: Int = -71
  public const val EPROTONOSUPPORT: Int = -93
  public const val EPROTOTYPE: Int = -91
  public const val ERANGE: Int = -34
  public const val EREMOTEIO: Int = -121
  public const val EROFS: Int = -30
  public const val ESHUTDOWN: Int = -108
  public const val ESOCKTNOSUPPORT: Int = -94
  public const val ESPIPE: Int = -29
  public const val ESRCH: Int = -3
  public const val ETIMEDOUT: Int = -110
  public const val ETXTBSY: Int = -26
  public const val EUNATCH: Int = -49
  public const val EXDEV: Int = -18
  public const val UNKNOWN: Int = -4094

  init {
    listOf(
      E2BIG to ("E2BIG" to "argument list too long"),
      EACCES to ("EACCES" to "permission denied"),
      EADDRINUSE to ("EADDRINUSE" to "address already in use"),
      EADDRNOTAVAIL to ("EADDRNOTAVAIL" to "address not available"),
      EAFNOSUPPORT to ("EAFNOSUPPORT" to "address family not supported"),
      EAGAIN to ("EAGAIN" to "resource temporarily unavailable"),
      EAI_ADDRFAMILY to ("EAI_ADDRFAMILY" to "address family not supported"),
      EAI_AGAIN to ("EAI_AGAIN" to "temporary failure"),
      EAI_BADFLAGS to ("EAI_BADFLAGS" to "bad ai_flags value"),
      EAI_BADHINTS to ("EAI_BADHINTS" to "invalid value for hints"),
      EAI_CANCELED to ("EAI_CANCELED" to "request canceled"),
      EAI_FAIL to ("EAI_FAIL" to "permanent failure"),
      EAI_FAMILY to ("EAI_FAMILY" to "ai_family not supported"),
      EAI_MEMORY to ("EAI_MEMORY" to "out of memory"),
      EAI_NODATA to ("EAI_NODATA" to "no address"),
      EAI_NONAME to ("EAI_NONAME" to "unknown node or service"),
      EAI_OVERFLOW to ("EAI_OVERFLOW" to "argument buffer overflow"),
      EAI_PROTOCOL to ("EAI_PROTOCOL" to "resolved protocol is unknown"),
      EAI_SERVICE to ("EAI_SERVICE" to "service not available for socket type"),
      EAI_SOCKTYPE to ("EAI_SOCKTYPE" to "socket type not supported"),
      EALREADY to ("EALREADY" to "connection already in progress"),
      EBADF to ("EBADF" to "bad file descriptor"),
      EBUSY to ("EBUSY" to "resource busy or locked"),
      ECANCELED to ("ECANCELED" to "operation canceled"),
      ECHARSET to ("ECHARSET" to "invalid Unicode character"),
      ECONNABORTED to ("ECONNABORTED" to "software caused connection abort"),
      ECONNREFUSED to ("ECONNREFUSED" to "connection refused"),
      ECONNRESET to ("ECONNRESET" to "connection reset by peer"),
      EDESTADDRREQ to ("EDESTADDRREQ" to "destination address required"),
      EEXIST to ("EEXIST" to "file already exists"),
      EFAULT to ("EFAULT" to "bad address in system call argument"),
      EFBIG to ("EFBIG" to "file too large"),
      EHOSTUNREACH to ("EHOSTUNREACH" to "host is unreachable"),
      EINTR to ("EINTR" to "interrupted system call"),
      EINVAL to ("EINVAL" to "invalid argument"),
      EIO to ("EIO" to "i/o error"),
      EISCONN to ("EISCONN" to "socket is already connected"),
      EISDIR to ("EISDIR" to "illegal operation on a directory"),
      ELOOP to ("ELOOP" to "too many symbolic links encountered"),
      EMFILE to ("EMFILE" to "too many open files"),
      EMSGSIZE to ("EMSGSIZE" to "message too long"),
      ENAMETOOLONG to ("ENAMETOOLONG" to "name too long"),
      ENETDOWN to ("ENETDOWN" to "network is down"),
      ENETUNREACH to ("ENETUNREACH" to "network is unreachable"),
      ENFILE to ("ENFILE" to "file table overflow"),
      ENOBUFS to ("ENOBUFS" to "no buffer space available"),
      ENODEV to ("ENODEV" to "no such device"),
      ENOENT to ("ENOENT" to "no such file or directory"),
      ENOMEM to ("ENOMEM" to "not enough memory"),
      ENONET to ("ENONET" to "machine is not on the network"),
      ENOPROTOOPT to ("ENOPROTOOPT" to "protocol not available"),
      ENOSPC to ("ENOSPC" to "no space left on device"),
      ENOSYS to ("ENOSYS" to "function not implemented"),
      ENOTCONN to ("ENOTCONN" to "socket is not connected"),
      ENOTDIR to ("ENOTDIR" to "not a directory"),
      ENOTEMPTY to ("ENOTEMPTY" to "directory not empty"),
      ENOTSOCK to ("ENOTSOCK" to "socket operation on non-socket"),
      ENOTSUP to ("ENOTSUP" to "operation not supported on socket"),
      EOVERFLOW to ("EOVERFLOW" to "value too large for defined data type"),
      EPERM to ("EPERM" to "operation not permitted"),
      EPIPE to ("EPIPE" to "broken pipe"),
      EPROTO to ("EPROTO" to "protocol error"),
      EPROTONOSUPPORT to ("EPROTONOSUPPORT" to "protocol not supported"),
      EPROTOTYPE to ("EPROTOTYPE" to "protocol wrong type for socket"),
      ERANGE to ("ERANGE" to "result too large"),
      EROFS to ("EROFS" to "read-only file system"),
      ESHUTDOWN to ("ESHUTDOWN" to "cannot send after transport endpoint shutdown"),
      ESPIPE to ("ESPIPE" to "invalid seek"),
      ESRCH to ("ESRCH" to "no such process"),
      ETIMEDOUT to ("ETIMEDOUT" to "connection timed out"),
      ETXTBSY to ("ETXTBSY" to "text file is busy"),
      EXDEV to ("EXDEV" to "cross-device link not permitted"),
      UNKNOWN to ("UNKNOWN" to "unknown error"),
      EOF to ("EOF" to "end of file"),
      ENXIO to ("ENXIO" to "no such device or address"),
      EMLINK to ("EMLINK" to "too many links"),
      EHOSTDOWN to ("EHOSTDOWN" to "host is down"),
      EREMOTEIO to ("EREMOTEIO" to "remote I/O error"),
      ENOTTY to ("ENOTTY" to "inappropriate ioctl for device"),
      EFTYPE to ("EFTYPE" to "inappropriate file type or format"),
      EILSEQ to ("EILSEQ" to "illegal byte sequence"),
      ESOCKTNOSUPPORT to ("ESOCKTNOSUPPORT" to "socket type not supported"),
      ENODATA to ("ENODATA" to "no data available"),
      EUNATCH to ("EUNATCH" to "protocol driver not attached"),
    ).let {
      allErrors = LinkedList<SystemError>()
      errorsById = TreeMap<Int, SystemError>()
      errorsByName = TreeMap<String, SystemError>()

      it.forEach { (code, pair) ->
        val (name, message) = pair
        val err = SystemError(
          id = code,
          name = name,
          message = message,
        )
        allErrors.add(err)
        errorsById[code] = err
        errorsByName[name] = err
      }
    }
  }
}

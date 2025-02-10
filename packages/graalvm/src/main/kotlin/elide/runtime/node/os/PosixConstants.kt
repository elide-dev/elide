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
package elide.runtime.node.os

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.intrinsics.js.node.os.*
import elide.runtime.intrinsics.js.node.os.OperatingSystemConstants.DlopenFlags
import elide.runtime.intrinsics.js.node.os.OperatingSystemConstants.ErrnoConstants
import elide.vm.annotations.Polyglot

/**
 * Constants for POSIX
 *
 * Defines constants which are provided at the `os.constants` module property for POSIX-style operating systems.
 */
@Suppress("MemberVisibilityCanBePrivate")
public data object PosixConstants: PosixSystemConstants {
  /** Flags for `dlopen`. */
  @get:Polyglot override val dlopen: Dlopen = Dlopen

  /** Error values. */
  @get:Polyglot override val errno: Errno = Errno

  /** Signal constants. */
  @get:Polyglot override val signals: Signals = Signals

  /** Priority constants. */
  @get:Polyglot override val priority: Priority = Priority

  /** Flags for `dlopen`. */
  public data object Dlopen : ProxyObject, DlopenFlags {
    /** Open the library lazily. */
    @get:Polyglot public override val RTLD_LAZY: Int = POSIX_RTLD_LAZY

    /** Open the library immediately. */
    @get:Polyglot public override val RTLD_NOW: Int = POSIX_RTLD_NOW

    /** Open the library globally. */
    @get:Polyglot public override val RTLD_GLOBAL: Int = POSIX_RTLD_GLOBAL

    /** Open the library locally. */
    @get:Polyglot public override val RTLD_LOCAL: Int = POSIX_RTLD_LOCAL

    /** Bind all symbols from this library. */
    @get:Polyglot public override val RTLD_DEEPBIND: Int = POSIX_RTLD_DEEPBIND

    override fun getMemberKeys(): Array<String> =
      arrayOf(SYMBOL_RTLD_LAZY, SYMBOL_RTLD_NOW, SYMBOL_RTLD_GLOBAL, SYMBOL_RTLD_LOCAL, SYMBOL_RTLD_DEEPBIND)
    override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
    override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `os.constants`")
    override fun getMember(key: String?): Any? = when (key) {
      SYMBOL_RTLD_LAZY -> RTLD_LAZY
      SYMBOL_RTLD_NOW -> RTLD_NOW
      SYMBOL_RTLD_GLOBAL -> RTLD_GLOBAL
      SYMBOL_RTLD_LOCAL -> RTLD_LOCAL
      SYMBOL_RTLD_DEEPBIND -> RTLD_DEEPBIND
      else -> null
    }
  }

  /** Error values. */
  public data object Errno : ErrnoConstants {
    public const val E2BIG: Int = E2BIG_CONST
    public const val EACCES: Int = EACCES_CONST
    public const val EADDRINUSE: Int = EADDRINUSE_CONST
    public const val EADDRNOTAVAIL: Int = EADDRNOTAVAIL_CONST
    public const val EAFNOSUPPORT: Int = EAFNOSUPPORT_CONST
    public const val EAGAIN: Int = EAGAIN_CONST
    public const val EALREADY: Int = EALREADY_CONST
    public const val EBADF: Int = EBADF_CONST
    public const val EBADMSG: Int = EBADMSG_CONST
    public const val EBUSY: Int = EBUSY_CONST
    public const val ECANCELED: Int = ECANCELED_CONST
    public const val ECHILD: Int = ECHILD_CONST
    public const val ECONNABORTED: Int = ECONNABORTED_CONST
    public const val ECONNREFUSED: Int = ECONNREFUSED_CONST
    public const val ECONNRESET: Int = ECONNRESET_CONST
    public const val EDEADLK: Int = EDEADLK_CONST
    public const val EDESTADDRREQ: Int = EDESTADDRREQ_CONST
    public const val EDOM: Int = EDOM_CONST
    public const val EDQUOT: Int = EDQUOT_CONST
    public const val EEXIST: Int = EEXIST_CONST
    public const val EFAULT: Int = EFAULT_CONST
    public const val EFBIG: Int = EFBIG_CONST
    public const val EHOSTUNREACH: Int = EHOSTUNREACH_CONST
    public const val EIDRM: Int = EIDRM_CONST
    public const val EILSEQ: Int = EILSEQ_CONST
    public const val EINPROGRESS: Int = EINPROGRESS_CONST
    public const val EINTR: Int = EINTR_CONST
    public const val EINVAL: Int = EINVAL_CONST
    public const val EIO: Int = EIO_CONST
    public const val EISCONN: Int = EISCONN_CONST
    public const val EISDIR: Int = EISDIR_CONST
    public const val ELOOP: Int = ELOOP_CONST
    public const val EMFILE: Int = EMFILE_CONST
    public const val EMLINK: Int = EMLINK_CONST
    public const val EMSGSIZE: Int = EMSGSIZE_CONST
    public const val EMULTIHOP: Int = EMULTIHOP_CONST
    public const val ENAMETOOLONG: Int = ENAMETOOLONG_CONST
    public const val ENETDOWN: Int = ENETDOWN_CONST
    public const val ENETRESET: Int = ENETRESET_CONST
    public const val ENETUNREACH: Int = ENETUNREACH_CONST
    public const val ENFILE: Int = ENFILE_CONST
    public const val ENOBUFS: Int = ENOBUFS_CONST
    public const val ENODATA: Int = ENODATA_CONST
    public const val ENODEV: Int = ENODEV_CONST
    public const val ENOENT: Int = ENOENT_CONST
    public const val ENOEXEC: Int = ENOEXEC_CONST
    public const val ENOLCK: Int = ENOLCK_CONST
    public const val ENOLINK: Int = ENOLINK_CONST
    public const val ENOMEM: Int = ENOMEM_CONST
    public const val ENOMSG: Int = ENOMSG_CONST
    public const val ENOPROTOOPT: Int = ENOPROTOOPT_CONST
    public const val ENOSPC: Int = ENOSPC_CONST
    public const val ENOSR: Int = ENOSR_CONST
    public const val ENOSTR: Int = ENOSTR_CONST
    public const val ENOSYS: Int = ENOSYS_CONST
    public const val ENOTCONN: Int = ENOTCONN_CONST
    public const val ENOTDIR: Int = ENOTDIR_CONST
    public const val ENOTEMPTY: Int = ENOTEMPTY_CONST
    public const val ENOTSOCK: Int = ENOTSOCK_CONST
    public const val ENOTSUP: Int = ENOTSUP_CONST
    public const val ENOTTY: Int = ENOTTY_CONST
    public const val ENXIO: Int = ENXIO_CONST
    public const val EOPNOTSUPP: Int = EOPNOTSUPP_CONST
    public const val EOVERFLOW: Int = EOVERFLOW_CONST
    public const val EPERM: Int = EPERM_CONST
    public const val EPIPE: Int = EPIPE_CONST
    public const val EPROTO: Int = EPROTO_CONST
    public const val EPROTONOSUPPORT: Int = EPROTONOSUPPORT_CONST
    public const val EPROTOTYPE: Int = EPROTOTYPE_CONST
    public const val ERANGE: Int = ERANGE_CONST
    public const val EROFS: Int = EROFS_CONST
    public const val ESPIPE: Int = ESPIPE_CONST
    public const val ESRCH: Int = ESRCH_CONST
    public const val ESTALE: Int = ESTALE_CONST
    public const val ETIME: Int = ETIME_CONST
    public const val ETIMEDOUT: Int = ETIMEDOUT_CONST
    public const val ETXTBSY: Int = ETXTBSY_CONST
    public const val EWOULDBLOCK: Int = EWOULDBLOCK_CONST
    public const val EXDEV: Int = EXDEV_CONST

    override fun getMemberKeys(): Array<String> = arrayOf(
      SYMBOL_E2BIG, SYMBOL_EACCES, SYMBOL_EADDRINUSE, SYMBOL_EADDRNOTAVAIL, SYMBOL_EAFNOSUPPORT, SYMBOL_EAGAIN,
      SYMBOL_EALREADY, SYMBOL_EBADF, SYMBOL_EBADMSG, SYMBOL_EBUSY, SYMBOL_ECANCELED, SYMBOL_ECHILD, SYMBOL_ECONNABORTED,
      SYMBOL_ECONNREFUSED, SYMBOL_ECONNRESET, SYMBOL_EDEADLK, SYMBOL_EDESTADDRREQ, SYMBOL_EDOM, SYMBOL_EDQUOT,
      SYMBOL_EEXIST, SYMBOL_EFAULT, SYMBOL_EFBIG, SYMBOL_EHOSTUNREACH, SYMBOL_EIDRM, SYMBOL_EILSEQ, SYMBOL_EINPROGRESS,
      SYMBOL_EINTR, SYMBOL_EINVAL, SYMBOL_EIO, SYMBOL_EISCONN, SYMBOL_EISDIR, SYMBOL_ELOOP, SYMBOL_EMFILE,
      SYMBOL_EMLINK, SYMBOL_EMSGSIZE, SYMBOL_EMULTIHOP, SYMBOL_ENAMETOOLONG, SYMBOL_ENETDOWN, SYMBOL_ENETRESET,
      SYMBOL_ENETUNREACH, SYMBOL_ENFILE, SYMBOL_ENOBUFS, SYMBOL_ENODATA, SYMBOL_ENODEV, SYMBOL_ENOENT, SYMBOL_ENOEXEC,
      SYMBOL_ENOLCK, SYMBOL_ENOLINK, SYMBOL_ENOMEM, SYMBOL_ENOMSG, SYMBOL_ENOPROTOOPT, SYMBOL_ENOSPC, SYMBOL_ENOSR,
      SYMBOL_ENOSTR, SYMBOL_ENOSYS, SYMBOL_ENOTCONN, SYMBOL_ENOTDIR, SYMBOL_ENOTEMPTY, SYMBOL_ENOTSOCK, SYMBOL_ENOTSUP,
      SYMBOL_ENOTTY, SYMBOL_ENXIO, SYMBOL_EOPNOTSUPP, SYMBOL_EOVERFLOW, SYMBOL_EPERM, SYMBOL_EPIPE, SYMBOL_EPROTO,
      SYMBOL_EPROTONOSUPPORT, SYMBOL_EPROTOTYPE, SYMBOL_ERANGE, SYMBOL_EROFS, SYMBOL_ESPIPE, SYMBOL_ESRCH,
      SYMBOL_ESTALE, SYMBOL_ETIME, SYMBOL_ETIMEDOUT, SYMBOL_ETXTBSY, SYMBOL_EWOULDBLOCK, SYMBOL_EXDEV,
    )

    override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
    override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `os.constants`")

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun getMember(key: String?): Any? = when (key) {
      SYMBOL_E2BIG -> E2BIG
      SYMBOL_EACCES -> EACCES
      SYMBOL_EADDRINUSE -> EADDRINUSE
      SYMBOL_EADDRNOTAVAIL -> EADDRNOTAVAIL
      SYMBOL_EAFNOSUPPORT -> EAFNOSUPPORT
      SYMBOL_EAGAIN -> EAGAIN
      SYMBOL_EALREADY -> EALREADY
      SYMBOL_EBADF -> EBADF
      SYMBOL_EBADMSG -> EBADMSG
      SYMBOL_EBUSY -> EBUSY
      SYMBOL_ECANCELED -> ECANCELED
      SYMBOL_ECHILD -> ECHILD
      SYMBOL_ECONNABORTED -> ECONNABORTED
      SYMBOL_ECONNREFUSED -> ECONNREFUSED
      SYMBOL_ECONNRESET -> ECONNRESET
      SYMBOL_EDEADLK -> EDEADLK
      SYMBOL_EDESTADDRREQ -> EDESTADDRREQ
      SYMBOL_EDOM -> EDOM
      SYMBOL_EDQUOT -> EDQUOT
      SYMBOL_EEXIST -> EEXIST
      SYMBOL_EFAULT -> EFAULT
      SYMBOL_EFBIG -> EFBIG
      SYMBOL_EHOSTUNREACH -> EHOSTUNREACH
      SYMBOL_EIDRM -> EIDRM
      SYMBOL_EILSEQ -> EILSEQ
      SYMBOL_EINPROGRESS -> EINPROGRESS
      SYMBOL_EINTR -> EINTR
      SYMBOL_EINVAL -> EINVAL
      SYMBOL_EIO -> EIO
      SYMBOL_EISCONN -> EISCONN
      SYMBOL_EISDIR -> EISDIR
      SYMBOL_ELOOP -> ELOOP
      SYMBOL_EMFILE -> EMFILE
      SYMBOL_EMLINK -> EMLINK
      SYMBOL_EMSGSIZE -> EMSGSIZE
      SYMBOL_EMULTIHOP -> EMULTIHOP
      SYMBOL_ENAMETOOLONG -> ENAMETOOLONG
      SYMBOL_ENETDOWN -> ENETDOWN
      SYMBOL_ENETRESET -> ENETRESET
      SYMBOL_ENETUNREACH -> ENETUNREACH
      SYMBOL_ENFILE -> ENFILE
      SYMBOL_ENOBUFS -> ENOBUFS
      SYMBOL_ENODATA -> ENODATA
      SYMBOL_ENODEV -> ENODEV
      SYMBOL_ENOENT -> ENOENT
      SYMBOL_ENOEXEC -> ENOEXEC
      SYMBOL_ENOLCK -> ENOLCK
      SYMBOL_ENOLINK -> ENOLINK
      SYMBOL_ENOMEM -> ENOMEM
      SYMBOL_ENOMSG -> ENOMSG
      SYMBOL_ENOPROTOOPT -> ENOPROTOOPT
      SYMBOL_ENOSPC -> ENOSPC
      SYMBOL_ENOSR -> ENOSR
      SYMBOL_ENOSTR -> ENOSTR
      SYMBOL_ENOSYS -> ENOSYS
      SYMBOL_ENOTCONN -> ENOTCONN
      SYMBOL_ENOTDIR -> ENOTDIR
      SYMBOL_ENOTEMPTY -> ENOTEMPTY
      SYMBOL_ENOTSOCK -> ENOTSOCK
      SYMBOL_ENOTSUP -> ENOTSUP
      SYMBOL_ENOTTY -> ENOTTY
      SYMBOL_ENXIO -> ENXIO
      SYMBOL_EOPNOTSUPP -> EOPNOTSUPP
      SYMBOL_EOVERFLOW -> EOVERFLOW
      SYMBOL_EPERM -> EPERM
      SYMBOL_EPIPE -> EPIPE
      SYMBOL_EPROTO -> EPROTO
      SYMBOL_EPROTONOSUPPORT -> EPROTONOSUPPORT
      SYMBOL_EPROTOTYPE -> EPROTOTYPE
      SYMBOL_ERANGE -> ERANGE
      SYMBOL_EROFS -> EROFS
      SYMBOL_ESPIPE -> ESPIPE
      SYMBOL_ESRCH -> ESRCH
      SYMBOL_ESTALE -> ESTALE
      SYMBOL_ETIME -> ETIME
      SYMBOL_ETIMEDOUT -> ETIMEDOUT
      SYMBOL_ETXTBSY -> ETXTBSY
      SYMBOL_EWOULDBLOCK -> EWOULDBLOCK
      SYMBOL_EXDEV -> EXDEV
      else -> null
    }
  }

  override fun getMemberKeys(): Array<String> = arrayOf("dlopen", "errno", "signals", "priority")

  override fun getMember(key: String?): Any? = when (key) {
    "dlopen" -> Dlopen
    "errno" -> Errno
    "signals" -> Signals
    "priority" -> Priority
    else -> null
  }

  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `os.constants`")
  override fun removeMember(key: String?): Boolean = false
}

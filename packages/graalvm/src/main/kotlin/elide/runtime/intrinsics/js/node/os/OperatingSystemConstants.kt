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
@file:Suppress("VariableNaming", "PropertyName")

package elide.runtime.intrinsics.js.node.os

import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.API
import elide.runtime.intrinsics.js.node.os.OperatingSystemConstants.*
import elide.vm.annotations.Polyglot

/** Symbol for the `RTLD_LAZY` flag. */
public const val SYMBOL_RTLD_LAZY: String = "RTLD_LAZY"

/** Symbol for the `RTLD_NOW` flag. */
public const val SYMBOL_RTLD_NOW: String = "RTLD_NOW"

/** Symbol for the `RTLD_GLOBAL` flag. */
public const val SYMBOL_RTLD_GLOBAL: String = "RTLD_GLOBAL"

/** Symbol for the `RTLD_LOCAL` flag. */
public const val SYMBOL_RTLD_LOCAL: String = "RTLD_LOCAL"

/** Symbol for the `RTLD_DEEPBIND` flag. */
public const val SYMBOL_RTLD_DEEPBIND: String = "RTLD_DEEPBIND"

/** Open the library lazily. */
public const val POSIX_RTLD_LAZY: Int = 0x00001

/** Open the library immediately. */
public const val POSIX_RTLD_NOW: Int = 0x00002

/** Open the library globally. */
public const val POSIX_RTLD_GLOBAL: Int = 0x00100

/** Open the library locally. */
public const val POSIX_RTLD_LOCAL: Int = 0x00000

/** Bind all symbols from this library. */
public const val POSIX_RTLD_DEEPBIND: Int = 0x00008

/** Symbol for the `E2BIG` code. */
public const val SYMBOL_E2BIG: String = "E2BIG"
public const val E2BIG_CONST: Int = 7

/** Symbol for the `EACCES` code. */
public const val SYMBOL_EACCES: String = "EACCES"
public const val EACCES_CONST: Int = 13

/** Symbol for the `EADDRINUSE` code. */
public const val SYMBOL_EADDRINUSE: String = "EADDRINUSE"
public const val EADDRINUSE_CONST: Int = 98
/** Symbol for the `EADDRNOTAVAIL` code. */
public const val SYMBOL_EADDRNOTAVAIL: String = "EADDRNOTAVAIL"
public const val EADDRNOTAVAIL_CONST: Int = 99

/** Symbol for the `EAFNOSUPPORT` code. */
public const val SYMBOL_EAFNOSUPPORT: String = "EAFNOSUPPORT"
public const val EAFNOSUPPORT_CONST: Int = 97

/** Symbol for the `EAGAIN` code. */
public const val SYMBOL_EAGAIN: String = "EAGAIN"
public const val EAGAIN_CONST: Int = 11

/** Symbol for the `EALREADY` code. */
public const val SYMBOL_EALREADY: String = "EALREADY"
public const val EALREADY_CONST: Int = 114

/** Symbol for the `EBADF` code. */
public const val SYMBOL_EBADF: String = "EBADF"
public const val EBADF_CONST: Int = 9

/** Symbol for the `EBADMSG` code. */
public const val SYMBOL_EBADMSG: String = "EBADMSG"
public const val EBADMSG_CONST: Int = 74

/** Symbol for the `EBUSY` code. */
public const val SYMBOL_EBUSY: String = "EBUSY"
public const val EBUSY_CONST: Int = 16

/** Symbol for the `ECANCELED` code. */
public const val SYMBOL_ECANCELED: String = "ECANCELED"
public const val ECANCELED_CONST: Int = 125

/** Symbol for the `ECHILD` code. */
public const val SYMBOL_ECHILD: String = "ECHILD"
public const val ECHILD_CONST: Int = 10

/** Symbol for the `ECONNABORTED` code. */
public const val SYMBOL_ECONNABORTED: String = "ECONNABORTED"
public const val ECONNABORTED_CONST: Int = 103

/** Symbol for the `ECONNREFUSED` code. */
public const val SYMBOL_ECONNREFUSED: String = "ECONNREFUSED"
public const val ECONNREFUSED_CONST: Int = 111

/** Symbol for the `ECONNRESET` code. */
public const val SYMBOL_ECONNRESET: String = "ECONNRESET"
public const val ECONNRESET_CONST: Int = 104

/** Symbol for the `EDEADLK` code. */
public const val SYMBOL_EDEADLK: String = "EDEADLK"
public const val EDEADLK_CONST: Int = 35

/** Symbol for the `EDESTADDRREQ` code. */
public const val SYMBOL_EDESTADDRREQ: String = "EDESTADDRREQ"
public const val EDESTADDRREQ_CONST: Int = 89

/** Symbol for the `EDOM` code. */
public const val SYMBOL_EDOM: String = "EDOM"
public const val EDOM_CONST: Int = 33

/** Symbol for the `EDQUOT` code. */
public const val SYMBOL_EDQUOT: String = "EDQUOT"
public const val EDQUOT_CONST: Int = 122

/** Symbol for the `EEXIST` code. */
public const val SYMBOL_EEXIST: String = "EEXIST"
public const val EEXIST_CONST: Int = 17

/** Symbol for the `EFAULT` code. */
public const val SYMBOL_EFAULT: String = "EFAULT"
public const val EFAULT_CONST: Int = 14

/** Symbol for the `EFBIG` code. */
public const val SYMBOL_EFBIG: String = "EFBIG"
public const val EFBIG_CONST: Int = 27

/** Symbol for the `EHOSTUNREACH` code. */
public const val SYMBOL_EHOSTUNREACH: String = "EHOSTUNREACH"
public const val EHOSTUNREACH_CONST: Int = 113

/** Symbol for the `EIDRM` code. */
public const val SYMBOL_EIDRM: String = "EIDRM"
public const val EIDRM_CONST: Int = 43

/** Symbol for the `EILSEQ` code. */
public const val SYMBOL_EILSEQ: String = "EILSEQ"
public const val EILSEQ_CONST: Int = 84

/** Symbol for the `EINPROGRESS` code. */
public const val SYMBOL_EINPROGRESS: String = "EINPROGRESS"
public const val EINPROGRESS_CONST: Int = 115

/** Symbol for the `EINTR` code. */
public const val SYMBOL_EINTR: String = "EINTR"
public const val EINTR_CONST: Int = 4

/** Symbol for the `EINVAL` code. */
public const val SYMBOL_EINVAL: String = "EINVAL"
public const val EINVAL_CONST: Int = 22

/** Symbol for the `EIO` code. */
public const val SYMBOL_EIO: String = "EIO"
public const val EIO_CONST: Int = 5

/** Symbol for the `EISCONN` code. */
public const val SYMBOL_EISCONN: String = "EISCONN"
public const val EISCONN_CONST: Int = 106

/** Symbol for the `EISDIR` code. */
public const val SYMBOL_EISDIR: String = "EISDIR"
public const val EISDIR_CONST: Int = 21

/** Symbol for the `ELOOP` code. */
public const val SYMBOL_ELOOP: String = "ELOOP"
public const val ELOOP_CONST: Int = 40

/** Symbol for the `EMFILE` code. */
public const val SYMBOL_EMFILE: String = "EMFILE"
public const val EMFILE_CONST: Int = 24

/** Symbol for the `EMLINK` code. */
public const val SYMBOL_EMLINK: String = "EMLINK"
public const val EMLINK_CONST: Int = 31

/** Symbol for the `EMSGSIZE` code. */
public const val SYMBOL_EMSGSIZE: String = "EMSGSIZE"
public const val EMSGSIZE_CONST: Int = 90

/** Symbol for the `EMULTIHOP` code. */
public const val SYMBOL_EMULTIHOP: String = "EMULTIHOP"
public const val EMULTIHOP_CONST: Int = 72

/** Symbol for the `ENAMETOOLONG` code. */
public const val SYMBOL_ENAMETOOLONG: String = "ENAMETOOLONG"
public const val ENAMETOOLONG_CONST: Int = 36

/** Symbol for the `ENETDOWN` code. */
public const val SYMBOL_ENETDOWN: String = "ENETDOWN"
public const val ENETDOWN_CONST: Int = 100

/** Symbol for the `ENETRESET` code. */
public const val SYMBOL_ENETRESET: String = "ENETRESET"
public const val ENETRESET_CONST: Int = 102

/** Symbol for the `ENETUNREACH` code. */
public const val SYMBOL_ENETUNREACH: String = "ENETUNREACH"
public const val ENETUNREACH_CONST: Int = 101

/** Symbol for the `ENFILE` code. */
public const val SYMBOL_ENFILE: String = "ENFILE"
public const val ENFILE_CONST: Int = 23

/** Symbol for the `ENOBUFS` code. */
public const val SYMBOL_ENOBUFS: String = "ENOBUFS"
public const val ENOBUFS_CONST: Int = 105

/** Symbol for the `ENODATA` code. */
public const val SYMBOL_ENODATA: String = "ENODATA"
public const val ENODATA_CONST: Int = 61

/** Symbol for the `ENODEV` code. */
public const val SYMBOL_ENODEV: String = "ENODEV"
public const val ENODEV_CONST: Int = 19

/** Symbol for the `ENOENT` code. */
public const val SYMBOL_ENOENT: String = "ENOENT"
public const val ENOENT_CONST: Int = 2

/** Symbol for the `ENOEXEC` code. */
public const val SYMBOL_ENOEXEC: String = "ENOEXEC"
public const val ENOEXEC_CONST: Int = 8

/** Symbol for the `ENOLCK` code. */
public const val SYMBOL_ENOLCK: String = "ENOLCK"
public const val ENOLCK_CONST: Int = 37

/** Symbol for the `ENOLINK` code. */
public const val SYMBOL_ENOLINK: String = "ENOLINK"
public const val ENOLINK_CONST: Int = 67

/** Symbol for the `ENOMEM` code. */
public const val SYMBOL_ENOMEM: String = "ENOMEM"
public const val ENOMEM_CONST: Int = 12

/** Symbol for the `ENOMSG` code. */
public const val SYMBOL_ENOMSG: String = "ENOMSG"
public const val ENOMSG_CONST: Int = 42

/** Symbol for the `ENOPROTOOPT` code. */
public const val SYMBOL_ENOPROTOOPT: String = "ENOPROTOOPT"
public const val ENOPROTOOPT_CONST: Int = 92

/** Symbol for the `ENOSPC` code. */
public const val SYMBOL_ENOSPC: String = "ENOSPC"
public const val ENOSPC_CONST: Int = 28

/** Symbol for the `ENOSR` code. */
public const val SYMBOL_ENOSR: String = "ENOSR"
public const val ENOSR_CONST: Int = 63

/** Symbol for the `ENOSTR` code. */
public const val SYMBOL_ENOSTR: String = "ENOSTR"
public const val ENOSTR_CONST: Int = 60

/** Symbol for the `ENOSYS` code. */
public const val SYMBOL_ENOSYS: String = "ENOSYS"
public const val ENOSYS_CONST: Int = 38

/** Symbol for the `ENOTCONN` code. */
public const val SYMBOL_ENOTCONN: String = "ENOTCONN"
public const val ENOTCONN_CONST: Int = 107

/** Symbol for the `ENOTDIR` code. */
public const val SYMBOL_ENOTDIR: String = "ENOTDIR"
public const val ENOTDIR_CONST: Int = 20

/** Symbol for the `ENOTEMPTY` code. */
public const val SYMBOL_ENOTEMPTY: String = "ENOTEMPTY"
public const val ENOTEMPTY_CONST: Int = 39

/** Symbol for the `ENOTSOCK` code. */
public const val SYMBOL_ENOTSOCK: String = "ENOTSOCK"
public const val ENOTSOCK_CONST: Int = 88

/** Symbol for the `ENOTSUP` code. */
public const val SYMBOL_ENOTSUP: String = "ENOTSUP"
public const val ENOTSUP_CONST: Int = 95

/** Symbol for the `ENOTTY` code. */
public const val SYMBOL_ENOTTY: String = "ENOTTY"
public const val ENOTTY_CONST: Int = 25

/** Symbol for the `ENXIO` code. */
public const val SYMBOL_ENXIO: String = "ENXIO"
public const val ENXIO_CONST: Int = 6

/** Symbol for the `EOPNOTSUPP` code. */
public const val SYMBOL_EOPNOTSUPP: String = "EOPNOTSUPP"
public const val EOPNOTSUPP_CONST: Int = 95

/** Symbol for the `EOVERFLOW` code. */
public const val SYMBOL_EOVERFLOW: String = "EOVERFLOW"
public const val EOVERFLOW_CONST: Int = 75

/** Symbol for the `EPERM` code. */
public const val SYMBOL_EPERM: String = "EPERM"
public const val EPERM_CONST: Int = 1

/** Symbol for the `EPIPE` code. */
public const val SYMBOL_EPIPE: String = "EPIPE"
public const val EPIPE_CONST: Int = 32

/** Symbol for the `EPROTO` code. */
public const val SYMBOL_EPROTO: String = "EPROTO"
public const val EPROTO_CONST: Int = 71

/** Symbol for the `EPROTONOSUPPORT` code. */
public const val SYMBOL_EPROTONOSUPPORT: String = "EPROTONOSUPPORT"
public const val EPROTONOSUPPORT_CONST: Int = 93

/** Symbol for the `EPROTOTYPE` code. */
public const val SYMBOL_EPROTOTYPE: String = "EPROTOTYPE"
public const val EPROTOTYPE_CONST: Int = 91

/** Symbol for the `ERANGE` code. */
public const val SYMBOL_ERANGE: String = "ERANGE"
public const val ERANGE_CONST: Int = 34

/** Symbol for the `EROFS` code. */
public const val SYMBOL_EROFS: String = "EROFS"
public const val EROFS_CONST: Int = 30

/** Symbol for the `ESPIPE` code. */
public const val SYMBOL_ESPIPE: String = "ESPIPE"
public const val ESPIPE_CONST: Int = 29

/** Symbol for the `ESRCH` code. */
public const val SYMBOL_ESRCH: String = "ESRCH"
public const val ESRCH_CONST: Int = 3

/** Symbol for the `ESTALE` code. */
public const val SYMBOL_ESTALE: String = "ESTALE"
public const val ESTALE_CONST: Int = 116

/** Symbol for the `ETIME` code. */
public const val SYMBOL_ETIME: String = "ETIME"
public const val ETIME_CONST: Int = 62

/** Symbol for the `ETIMEDOUT` code. */
public const val SYMBOL_ETIMEDOUT: String = "ETIMEDOUT"
public const val ETIMEDOUT_CONST: Int = 110

/** Symbol for the `ETXTBSY` code. */
public const val SYMBOL_ETXTBSY: String = "ETXTBSY"
public const val ETXTBSY_CONST: Int = 26

/** Symbol for the `EWOULDBLOCK` code. */
public const val SYMBOL_EWOULDBLOCK: String = "EWOULDBLOCK"
public const val EWOULDBLOCK_CONST: Int = 11

/** Symbol for the `EXDEV` code. */
public const val SYMBOL_EXDEV: String = "EXDEV"
public const val EXDEV_CONST: Int = 18

/**
 * # Operating System Constants
 */
@API public sealed interface OperatingSystemConstants : ProxyObject {
  /**
   * Flags which can be provided to `dlopen` to control how a dynamic library is loaded.
   */
  public interface DlopenFlags : ProxyObject {
    /** Load the library lazily. */
    @get:Polyglot public val RTLD_LAZY: Int

    /** Load the library immediately. */
    @get:Polyglot public val RTLD_NOW: Int

    /** Load the library globally. */
    @get:Polyglot public val RTLD_GLOBAL: Int

    /** Load the library locally. */
    @get:Polyglot public val RTLD_LOCAL: Int

    /** Bind all symbols from this library. */
    @get:Polyglot public val RTLD_DEEPBIND: Int

    public fun all(): Sequence<Pair<String, Int>> = sequence {
      yield(SYMBOL_RTLD_LAZY to RTLD_LAZY)
      yield(SYMBOL_RTLD_NOW to RTLD_NOW)
      yield(SYMBOL_RTLD_GLOBAL to RTLD_GLOBAL)
      yield(SYMBOL_RTLD_LOCAL to RTLD_LOCAL)
      yield(SYMBOL_RTLD_DEEPBIND to RTLD_DEEPBIND)
    }
  }

  /**
   * Error codes returned by various system calls.
   */
  public interface ErrnoConstants : ProxyObject

  /**
   * Process signal constants.
   */
  public interface SignalsConstants : ProxyObject

  /**
   * Process priority constants.
   */
  public interface PriorityConstants : ProxyObject {
    /** The process is running with a low priority. */
    @get:Polyglot public val PRIORITY_LOW: Int

    /** The process is running with a priority below normal. */
    @get:Polyglot public val PRIORITY_BELOW_NORMAL: Int

    /** The process is running with the default priority. */
    @get:Polyglot public val PRIORITY_NORMAL: Int

    /** The process is running with a priority above normal. */
    @get:Polyglot public val PRIORITY_ABOVE_NORMAL: Int

    /** The process is running with a high priority. */
    @get:Polyglot public val PRIORITY_HIGH: Int

    /** The process is running with the highest possible priority. */
    @get:Polyglot public val PRIORITY_HIGHEST: Int
  }
}

/**
 * ## Operating System Constants: POSIX
 */
@API public interface PosixSystemConstants : OperatingSystemConstants {
  /**
   * Flags which can be provided to `dlopen` to control how a dynamic library is loaded.
   */
  @get:Polyglot public val dlopen: DlopenFlags

  /**
   * Error codes returned by various system calls.
   */
  @get:Polyglot public val errno: ErrnoConstants

  /**
   * Process signal constants.
   */
  @get:Polyglot public val signals: SignalsConstants

  /**
   * Process priority constants.
   */
  @get:Polyglot public val priority: PriorityConstants
}

/**
 * ## Operating System Constants: Windows (Win32)
 */
@API public interface Win32SystemConstants : OperatingSystemConstants

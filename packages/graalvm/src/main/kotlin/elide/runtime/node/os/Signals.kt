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
import elide.runtime.intrinsics.js.node.os.OperatingSystemConstants.SignalsConstants

/** Signal constants. */
public data object Signals : SignalsConstants {
  public const val SIGHUP: Int = 1
  public const val SIGINT: Int = 2
  public const val SIGQUIT: Int = 3
  public const val SIGILL: Int = 4
  public const val SIGTRAP: Int = 5
  public const val SIGABRT: Int = 6
  public const val SIGIOT: Int = 6
  public const val SIGBUS: Int = 7
  public const val SIGFPE: Int = 8
  public const val SIGKILL: Int = 9
  public const val SIGUSR1: Int = 10
  public const val SIGSEGV: Int = 11
  public const val SIGUSR2: Int = 12
  public const val SIGPIPE: Int = 13
  public const val SIGALRM: Int = 14
  public const val SIGTERM: Int = 15
  public const val SIGSTKFLT: Int = 16
  public const val SIGCHLD: Int = 17
  public const val SIGCONT: Int = 18
  public const val SIGSTOP: Int = 19
  public const val SIGTSTP: Int = 20
  public const val SIGTTIN: Int = 21
  public const val SIGTTOU: Int = 22
  public const val SIGURG: Int = 23
  public const val SIGXCPU: Int = 24
  public const val SIGXFSZ: Int = 25
  public const val SIGVTALRM: Int = 26
  public const val SIGPROF: Int = 27
  public const val SIGWINCH: Int = 28
  public const val SIGIO: Int = 29
  public const val SIGPOLL: Int = 29
  public const val SIGPWR: Int = 30
  public const val SIGSYS: Int = 31

  override fun getMemberKeys(): Array<String> = arrayOf(
    "SIGHUP", "SIGINT", "SIGQUIT", "SIGILL", "SIGTRAP", "SIGABRT",
    "SIGIOT", "SIGBUS", "SIGFPE", "SIGKILL", "SIGUSR1", "SIGSEGV",
    "SIGUSR2", "SIGPIPE", "SIGALRM", "SIGTERM", "SIGSTKFLT", "SIGCHLD",
    "SIGCONT", "SIGSTOP", "SIGTSTP", "SIGTTIN", "SIGTTOU", "SIGURG",
    "SIGXCPU", "SIGXFSZ", "SIGVTALRM", "SIGPROF", "SIGWINCH", "SIGIO",
    "SIGPOLL", "SIGPWR", "SIGSYS",
  )

  override fun hasMember(key: String?): Boolean = key != null && key in memberKeys
  override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `os.constants`")

  override fun getMember(key: String?): Any? = when (key) {
    "SIGHUP" -> SIGHUP
    "SIGINT" -> SIGINT
    "SIGQUIT" -> SIGQUIT
    "SIGILL" -> SIGILL
    "SIGTRAP" -> SIGTRAP
    "SIGABRT" -> SIGABRT
    "SIGIOT" -> SIGIOT
    "SIGBUS" -> SIGBUS
    "SIGFPE" -> SIGFPE
    "SIGKILL" -> SIGKILL
    "SIGUSR1" -> SIGUSR1
    "SIGSEGV" -> SIGSEGV
    "SIGUSR2" -> SIGUSR2
    "SIGPIPE" -> SIGPIPE
    "SIGALRM" -> SIGALRM
    "SIGTERM" -> SIGTERM
    "SIGSTKFLT" -> SIGSTKFLT
    "SIGCHLD" -> SIGCHLD
    "SIGCONT" -> SIGCONT
    "SIGSTOP" -> SIGSTOP
    "SIGTSTP" -> SIGTSTP
    "SIGTTIN" -> SIGTTIN
    "SIGTTOU" -> SIGTTOU
    "SIGURG" -> SIGURG
    "SIGXCPU" -> SIGXCPU
    "SIGXFSZ" -> SIGXFSZ
    "SIGVTALRM" -> SIGVTALRM
    "SIGPROF" -> SIGPROF
    "SIGWINCH" -> SIGWINCH
    "SIGIO" -> SIGIO
    "SIGPOLL" -> SIGPOLL
    "SIGPWR" -> SIGPWR
    "SIGSYS" -> SIGSYS
    else -> null
  }
}

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

package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

/**
 * # GraalVM EE: Sandbox Configuration
 *
 * Configures VM sandbox and resource limit functionality, which is only available in GraalVM Enterprise. These settings
 * are inert on Community Edition.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.enterprise.sandbox")
internal interface GuestVMEESandboxConfig {
  companion object {
    /** Default maximum CPU time: `1000ms`. */
    val DEFAULT_CPU_TIME: Duration = Duration.ofMillis(1000)

    /** Default maximum AST depth: `-1` (unlimited). */
    const val DEFAULT_AST_DEPTH: Long = -1

    /** Default maximum heap memory: `1gb`. */
    const val DEFAULT_HEAP_MEMORY: String = "1GB"

    /** Default maximum number of threads. */
    const val DEFAULT_MAX_THREADS: Int = -1
  }

  /**
   * @return From GraalVM: Limits the total maximum CPU time that (can be) spent running the (guest) application. No
   *   limit is set by default. Example value: '100ms'.
   */
  val maxCpuTime: Duration? get() = DEFAULT_CPU_TIME

  /**
   * @return From GraalVM: Limits the AST depth of parsed functions. Default: no limit.
   */
  val maxAstDepth: Long? get() = DEFAULT_AST_DEPTH

  /**
   * @return From GraalVM: Specifies the maximum heap memory that can be retained by the (guest) application during its
   *   run. No limit is set by default and setting the related expert options has no effect. Example value: '100MB'.
   */
  val maxHeapMemory: String? get() = DEFAULT_HEAP_MEMORY

  /**
   * @return From GraalVM: Limits the number of threads that can be entered by a context at the same point in time
   *   (default: no limit).
   */
  val maxThreads: Int? get() = DEFAULT_MAX_THREADS
}

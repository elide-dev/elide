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
package elide.runtime.runner

import java.util.ServiceLoader

/**
 * # Runners
 *
 * Utility class for obtaining instances of [Runner] implementations.
 */
public object Runners {
  /**
   * Returns all available [Runner] instances.
   *
   * @return A list of all available [Runner] instances.
   */
  @JvmStatic public fun all(): List<Runner<*>> = ServiceLoader.load(Runner::class.java).toList()

  /**
   * Returns all available [Runner] instances.
   *
   * @return A list of all available [Runner] instances.
   */
  @JvmStatic public fun jvm(job: JvmRunner.JvmRunnerJob, truffle: Boolean? = null): List<JvmRunner> = all()
    .filterIsInstance<JvmRunner>()
    .filter {
      when (truffle) {
        null -> true // no filter
        true -> it is TruffleRunner
        false -> it !is TruffleRunner
      }
    }.filter {
      it.eligible(job)
    }
}

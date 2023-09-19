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

package elide.tool.engine

import elide.tool.io.WorkdirManager

/**
 * # Native Engine
 *
 * Provides utilities for loading native portions of Elide early in the boot lifecycle.
 */
object NativeEngine {
  /**
   * ## Load Natives
   *
   * Prepare VM-level settings and trigger loading of critical native libraries.
   *
   * @param workdir Working directory manager.
   * @param extraProps Extra VM properties to set.
   */
  @JvmStatic fun load(workdir: WorkdirManager, extraProps: List<Pair<String, String>>) {
    val tmp = workdir.workingRoot().absolutePath
    val natives = workdir.nativesDirectory().absolutePath

    listOf(
      "io.netty.tmpdir" to tmp,
      "io.netty.native.workdir" to natives,
      "io.netty.native.deleteLibAfterLoading" to "false",
    ).plus(extraProps).forEach {
      System.setProperty(it.first, it.second)
    }
  }

  /**
   * ## Boot Entrypoint
   *
   * Early in the static init process for Elide, this method is called to prepare and load native libraries and apply
   * VM-level settings as early as possible.
   *
   * @param workdir Working directory manager.
   * @param properties Provider of extra VM properties to set.
   */
  @JvmStatic fun boot(workdir: WorkdirManager, properties: () -> List<Pair<String, String>>) {
    load(workdir, properties.invoke())
  }
}

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
package elide.tooling.archive

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.nio.file.Path
import elide.exec.Action
import elide.exec.Result
import elide.exec.asExecResult
import elide.tooling.archive.ArchiveBuilder.Companion.tarGzBuilder
import elide.tooling.archive.ArchiveBuilder.TarGzBuilder

/**
 * ## Tar.gz Tasks
 *
 * Provides utilities for working with gzip-compressed tar archives within the context of an Elide project build;
 * these functions are designed to be used within a task's execution scope.
 */
public object TarGzTasks {
  /**
   * Pack a tar.gz archive at the specified [path], using the provided [block] to build the archive contents.
   *
   * @param path Path to the tar.gz archive to create.
   * @param configure Optional configuration for the [TarArchiveOutputStream] used to write the archive.
   * @param block Block to configure the [TarGzBuilder] for the archive.
   * @return Result of the action, indicating success or failure.
   */
  public fun Action.ActionContext.tarGz(
    path: Path,
    configure: TarArchiveOutputStream.() -> Unit = {},
    block: TarGzBuilder.() -> Unit,
  ): Result = tarGzBuilder(path, configure).apply { block() }.let { builder ->
    runCatching { builder.finalizeArchive() }.asExecResult()
  }
}

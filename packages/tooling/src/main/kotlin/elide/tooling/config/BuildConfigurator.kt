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

@file:Suppress("UnstableApiUsage")

package elide.tooling.config

import java.nio.file.Path
import elide.exec.ActionScope
import elide.exec.TaskGraphBuilder
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.registry.ResolverRegistry

public interface BuildConfigurator : ProjectConfigurator {
  public interface BuildConfiguration {
    public val actionScope: ActionScope
    public val taskGraph: TaskGraphBuilder
    public val resolvers: ResolverRegistry
    public val projectRoot: Path
  }

  public sealed interface BuildNotify
  public interface BuildEvent : BuildNotify
  public interface BuildWork : BuildEvent
  public interface BuildWorker : BuildNotify
  public interface BuildTransfer : BuildNotify

  public interface BuildConsoleController {
    public fun onCurrentWork(work: BuildWork)
  }

  public interface BuildEventController {
    public fun onEventBegin(event: BuildEvent)
    public fun onEventEnd(event: BuildEvent)
    public fun onWorkerStart(id: BuildWorker, task: String)
    public fun onTransferStart(transfer: BuildTransfer, event: BuildEvent)
    public fun onProgress(event: BuildEvent)
    public fun onProgress(worker: BuildWorker, event: BuildEvent)
    public fun onProgress(transfer: BuildTransfer, event: BuildEvent)
    public fun onWorkerEnd(id: BuildWorker)
    public fun onTransferEnd(transfer: BuildTransfer)
    public fun onError(event: BuildEvent)
    public fun onWorkerError(worker: BuildWorker, event: BuildEvent)
    public fun onTransferError(transfer: BuildTransfer, event: BuildEvent)
  }

  public interface ProjectDirectories {
    public val projectRoot: Path
    public val devRoot: Path get() = projectRoot.resolve(".dev")
    public val dependencies: Path get() = devRoot.resolve("dependencies")
    public val artifacts: Path get() = devRoot.resolve("artifacts")
  }

  public interface ElideBuildState {
    public val project: ElideConfiguredProject
    public val console: BuildConsoleController
    public val events: BuildEventController
    public val layout: ProjectDirectories
    public val manifest: ElidePackageManifest
    public val resourcesPath: Path
  }

  public suspend fun contribute(state: ElideBuildState, config: BuildConfiguration)
}

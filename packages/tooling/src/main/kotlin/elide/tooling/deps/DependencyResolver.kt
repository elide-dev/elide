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
package elide.tooling.deps

import java.lang.AutoCloseable
import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import elide.tooling.lockfile.ElideLockfile
import elide.tooling.lockfile.LockfileContributor
import elide.tooling.project.ElideProject

/**
 * ## Dependency Resolver
 */
public sealed interface DependencyResolver : AutoCloseable, LockfileContributor {
  public val ecosystem: DependencyEcosystem

  override suspend fun contribute(root: Path, project: ElideProject?): ElideLockfile.Stanza? = null

  public suspend fun seal()

  public suspend fun resolve(scope: CoroutineScope): Sequence<Job>

  public interface MavenResolver : DependencyResolver {
    override val ecosystem: DependencyEcosystem get() = DependencyEcosystem.Maven
  }

  public interface NpmResolver : DependencyResolver {
    override val ecosystem: DependencyEcosystem get() = DependencyEcosystem.NPM
  }

  public interface JsrResolver : DependencyResolver {
    override val ecosystem: DependencyEcosystem get() = DependencyEcosystem.JSR
  }

  public interface PyPiResolver : DependencyResolver {
    override val ecosystem: DependencyEcosystem get() = DependencyEcosystem.PyPI
  }
}

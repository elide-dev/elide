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
package elide.tooling.project

import elide.tooling.lockfile.LockfileLoader
import java.nio.file.Path

/**
 * Loader which transforms a regular Elide project into a configured Elide project.
 */
public interface ElideProjectLoader {
  /**
   * Factory for producing source sets.
   */
  public val sourceSetFactory: SourceSetFactory

  /**
   * Elide's binary resources path.
   */
  public val resourcesPath: Path

  /**
   * Lockfile provider.
   */
  public val lockfileLoader: LockfileLoader
}

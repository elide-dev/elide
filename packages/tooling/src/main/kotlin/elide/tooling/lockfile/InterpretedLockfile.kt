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
package elide.tooling.lockfile

import java.nio.file.Path
import kotlin.time.Duration

/**
 * ## Interpreted Lockfile
 *
 * Describes a lockfile which has been loaded and checked, and is now ready for consumption.
 *
 * @property path Path to the lockfile; present if this lockfile was read, not present if it is being created.
 * @property root Root path of the lockfile; this is where it will be written to and read from.
 * @property format Format of the lockfile.
 * @property lockfile Lockfile object after de-serialization.
 * @property definition Definition (version) of the lockfile.
 * @property duration Length of time it took to load the lockfile (as applicable).
 */
public sealed interface InterpretedLockfile {
  public val path: Path?
  public val root: Path
  public val format: ElideLockfile.Format
  public val lockfile: ElideLockfile
  public val definition: LockfileDefinition<*>
  public val duration: Duration?

  /**
   * Update this lockfile to match the anticipated lockfile; the returned [InterpretedLockfile] merges the resulting
   * data/operational info.
   *
   * @param anticipated Lockfile data to update to.
   */
  public fun updateTo(duration: Duration? = null, anticipated: () -> ElideLockfile): InterpretedLockfile
}

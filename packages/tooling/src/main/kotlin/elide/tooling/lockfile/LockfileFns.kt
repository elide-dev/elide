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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.measureTimedValue

// Eligible names for lock files.
private val lockfileNames = sortedSetOf(
  "elide.lock.bin",
  "elide.lock.json",
)

internal data class InterpretedLockfileImpl(
  override val path: Path?,
  override val root: Path,
  override val format: ElideLockfile.Format,
  override val lockfile: ElideLockfile,
  override val definition: LockfileDefinition<*>,
  override val duration: Duration?,
) : InterpretedLockfile, ElideLockfile by lockfile {
  override fun updateTo(duration: Duration?, anticipated: () -> ElideLockfile): InterpretedLockfile {
    return InterpretedLockfileImpl(
      path = path,
      root = root,
      format = format,
      lockfile = anticipated(),
      definition = definition,
      duration = duration ?: this.duration,
    )
  }
}

/**
 * Load any present lockfile in the given [root] path; if no root path is provided, the current working directory is
 * used.
 *
 * This method will spawn an async job within the receiving [CoroutineScope] to load the lockfile, and will return the
 * lockfile structure (or throw an error) when awaited.
 *
 * @param root Optional path to the root directory to load the lockfile from. If not provided, the current working is
 *   used.
 * @return A [Deferred] handle to the loaded lockfile, which can be awaited to retrieve the lockfile structure.
 */
public fun CoroutineScope.loadLockfile(
  root: Path? = null,
  def: LockfileDefinition<*> = ElideLockfile.latest(),
): Deferred<InterpretedLockfile> = async(IO) {
  val rootPath = root ?: Path.of(System.getProperty("user.dir"))
  val lockfilePath = lockfileNames
    .map { rootPath.resolve(".dev").resolve(it) }
    .firstOrNull { it.exists() }
    ?: throw NoSuchFileException(rootPath.toFile())

  measureTimedValue { Lockfiles.read(lockfilePath, def) }.let { timed ->
    val (format, lockfile) = timed.value

    InterpretedLockfileImpl(
      path = lockfilePath,
      root = rootPath,
      format = format,
      lockfile = lockfile,
      definition = def,
      duration = timed.duration,
    )
  }
}

/**
 * Load any present lockfile in the given [root] path; if no root path is provided, the current working directory is
 * used.
 *
 * If no lockfile can be located, `null` is returned.
 *
 * @param root Optional path to the root directory to load the lockfile from. If not provided, the current working is
 *   used.
 * @return A [Deferred] handle to the loaded lockfile, which can be awaited to retrieve the lockfile structure.
 */
public suspend fun loadLockfileSafe(
  root: Path? = null,
  def: LockfileDefinition<*> = ElideLockfile.latest(),
): InterpretedLockfile? = try {
  coroutineScope {
    loadLockfile(root, def).await()
  }
} catch (_: NoSuchFileException) {
  null
}

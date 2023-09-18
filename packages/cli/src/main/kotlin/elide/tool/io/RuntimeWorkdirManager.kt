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

package elide.tool.io

import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.annotations.Context
import elide.annotations.Eager
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.HostPlatform
import elide.runtime.core.HostPlatform.OperatingSystem.*
import elide.tool.cli.ElideTool

/** Main implementation of the runtime working directory manager. */
@Eager @Context @Singleton internal class RuntimeWorkdirManager : WorkdirManager {
  private companion object {
    private const val nativesDir = "native"
    private const val tempDir = "temp"
    private const val cachesDir = "caches"
    private const val darwinCachesPath = "/Library/caches/elide"
    private const val linuxCachesPath = "~/elide/caches"
    private const val runtimeDirPrefix = "elide-runtime-"
  }

  private val active: AtomicBoolean = AtomicBoolean(true)
  private val initialized: AtomicBoolean = AtomicBoolean(false)
  private val tempDirectory: AtomicReference<File> = AtomicReference(null)

  private fun currentSharedTempPrefix(): String {
    return StringBuilder().apply {
      append(runtimeDirPrefix)
      append(ElideTool.version())
    }.toString()
  }

  private fun initializeTemporaryWorkdir(): File = synchronized(this) {
    if (initialized.get()) return tempDirectory.get()
    initialized.compareAndSet(false, true)
    tempDirectory.compareAndSet(null, Files.createTempDirectory(currentSharedTempPrefix()).toFile())
    tempDirectory.get()
  }

  private fun obtainWorkdir(): File = if (initialized.get()) {
    tempDirectory.get()
  } else initializeTemporaryWorkdir()

  private fun workSubdir(
    name: String,
    temporary: Boolean = true,
  ): File = obtainWorkdir().resolve(name).apply {
    if (temporary) {
      deleteOnExit()
    }
  }

  private inline fun <R> requireActive(op: () -> R): R {
    require(active.get()) {
      "Cannot use runtime temporary directory resources after the manager has closed"
    }
    return op.invoke()
  }

  private val nativesDirectory by lazy {
    workSubdir(nativesDir, temporary = false)
  }

  private val temporaryDirectory by lazy {
    workSubdir(tempDir, temporary = true)
  }

  @OptIn(DelicateElideApi::class)
  private val cachesDirectory by lazy {
    when (HostPlatform.resolve().os) {
      DARWIN -> File(darwinCachesPath)
      LINUX -> File(linuxCachesPath)
      else -> workSubdir(cachesDir, temporary = false)
    }
  }

  override fun temporaryWorkdir(): File = requireActive {
    obtainWorkdir()
  }

  override fun nativesDirectory(): File = requireActive {
    nativesDirectory
  }

  override fun tmpDirectory(): File = requireActive {
    temporaryDirectory
  }

  override fun cacheDirectory(): File = requireActive {
    cachesDirectory
  }

  override fun close() {
    active.compareAndSet(false, true)
  }
}

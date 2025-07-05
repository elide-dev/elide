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
package elide.runtime.runner.jvm

import org.graalvm.nativeimage.ImageInfo
import java.io.File
import java.nio.file.Path
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.io.path.absolutePathString
import elide.runtime.runner.AbstractRunner
import elide.runtime.runner.JvmRunner
import elide.runtime.runner.RunnerExecution
import elide.runtime.runner.RunnerOutcome
import elide.tooling.Arguments

// Name of the Standard JVM runner.
private const val RUNNER_STD_JVM = "stockjvm"

// Implements a JVM runner which uses standard JDK facilities to run programs.
internal class StandardJvmRunner : AbstractRunner<JvmRunner.JvmRunnerJob>(RUNNER_STD_JVM), JvmRunner {
  private val runnerOptions = atomic(JvmRunnerOptions())
  private val resolvedJavaHome = atomic<Path?>(null)

  // Install custom options for the standard JVM runner.
  fun configureStandardJvmRunner(options: JvmRunnerOptions): StandardJvmRunner = apply {
    runnerOptions.value = options
    if (options.javahome != null || options.javahome != resolvedJavaHome.value) {
      resolvedJavaHome.value = null  // refresh
    }
  }

  // Options for the standard JVM runner.
  data class JvmRunnerOptions(
    // Specific Java Home to use; if none is provided, one will be resolved.
    val javahome: Path? = null,

    // Whether to use reflective invocation within this JVM.
    val reflective: Boolean = false,
  )

  // Resolve the Java Home to use for this runner.
  private fun javaHome(): Path? = when (val currentHome = resolvedJavaHome.value) {
    null -> {
      val javaHomeEnv = System.getenv("JAVA_HOME")
      if (javaHomeEnv != null) {
        Path.of(javaHomeEnv).also { resolvedJavaHome.value = it }
      } else {
        null // no Java Home available
      }
    }

    else -> currentHome
  }

  // Operating mode: Invoke within this JVM using reflection.
  @Suppress("TooGenericExceptionCaught")
  private suspend fun invokeReflectively(exec: RunnerExecution<JvmRunner.JvmRunnerJob>): RunnerOutcome {
    val cls = this::class.java.classLoader.loadClass(exec.job.mainClass)
    if (cls == null) {
      return err("failed to load main class (via reflection): ${exec.job.mainClass}")
    }
    // find static method named `main`
    val mainMethod = cls.getDeclaredMethod("main", Array<String>::class.java)
    if (mainMethod == null) {
      return err("failed to find main method in class (via reflection): ${exec.job.mainClass}")
    }
    // invoke main method with specified args
    val progArgs = exec.job.arguments.asArgumentList().toTypedArray()
    return try {
      withContext(coroutineContext) {
        mainMethod.invoke(cls, progArgs)
        success()
      }
    } catch (err: Throwable) {
      err("failed to invoke main method in class (via reflection): ${exec.job.mainClass}", cause = err)
    }
  }

  // Operating mode: Invoke Java via the command line.
  @Suppress("TooGenericExceptionCaught")
  private suspend fun invokeSubproc(exec: RunnerExecution<JvmRunner.JvmRunnerJob>): RunnerOutcome {
    val home = javaHome() ?: return err("failed to resolve Java Home for standard JVM runner")
    val javaBin = home.resolve("bin").resolve("java")

    return withContext(coroutineContext) {
      val progArgs = exec.job.arguments
      val jvmArgs = exec.job.jvmArgs
      val fullCommand = Arguments.empty().toMutable().apply {
        add(javaBin.absolutePathString())
        addAllStrings(jvmArgs.asArgumentList())

        add("-cp")
        add(exec.job.classpath.joinToString(separator = ":") { it.path.absolutePathString() })

        exec.job.modulepath?.let { modulepath ->
          add("--module-path")
          add(modulepath.joinToString(separator = ":") { it.path.absolutePathString() })
        }

        if (exec.job.mainModule.isPresent) {
          add("${exec.job.mainModule.get()}/${exec.job.mainClass}")
        } else {
          add(exec.job.mainClass)
        }
        addAllStrings(progArgs.asArgumentList())
      }.asArgumentList()

      val procBuilder = ProcessBuilder().apply {
        // command to run
        command(fullCommand)

        // inherit IO from the parent process
        inheritIO()

        // working directory should match
        directory(File(System.getProperty("user.dir")))

        // job environment
        exec.job.environment?.let { env ->
          env.asSequence().forEach {
            environment().put(it.key, it.value)
          }
        }
      }

      try {
        // start and wait for process completion
        val process = procBuilder.start()
        val exitCode = process.waitFor()
        if (exitCode == 0) {
          success()
        } else {
          err("subprocess exited with non-zero exit code: $exitCode")
        }
      } catch (err: Throwable) {
        err("failed to invoke subprocess for main class: ${exec.job.mainClass}", cause = err)
      }
    }
  }

  override suspend fun invoke(exec: RunnerExecution<JvmRunner.JvmRunnerJob>): RunnerOutcome = when {
    // reflective invocation within this JVM is only supported in non-image contexts, and when activated.
    !ImageInfo.inImageCode() && runnerOptions.value.reflective -> invokeReflectively(exec)

    // otherwise, invoke via subproc.
    else -> invokeSubproc(exec)
  }
}

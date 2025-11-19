/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
@file:Suppress("ReturnCount")

package elide.tool.cli.cmd.classpath

import io.micronaut.context.BeanContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import java.io.File
import java.nio.file.Paths
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.joinAll
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeTo
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.Elide
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tooling.ClasspathSpec
import elide.tooling.MultiPathUsage
import elide.tooling.MutableClasspath
import elide.tooling.builder.BuildDriver
import elide.tooling.builder.BuildDriver.resolve
import elide.tooling.deps.DependencyResolver
import elide.tooling.jvm.resolver.MavenAetherResolver
import elide.tooling.project.ProjectManager

@CommandLine.Command(
  name = "classpath",
  aliases = ["cp"],
  mixinStandardHelpOptions = true,
  description = [
    "For this or a specified project, resolve and emit a classpath. " +
            "Dependencies are installed for Maven packages, modulo provided flags.",
    "",
    "Running @|bold elide classpath|@ without arguments installs all dependencies visible to the project if needed" +
            "Running @|bold elide install <usage...>|@ installs the transitive closure for that usage type.",
    "",
    "Then, classpath members are emitted according to `format`, defaulting to a value that can be passed as a valid",
    "classpath to @|bold javac|@ or @|bold kotlinc|@.",
    "",
    "Project structure and dependencies are managed via @|bold elide.pkl|@, or via foreign manifests such as " +
            "@|bold pom.xml|@.",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) classpath|@",
    "   or: elide @|bold,fg(cyan) classpath|@ [OPTIONS] [USAGE...]",
    "",
  ],
)
@Introspected
@ReflectiveAccess
internal class ClasspathCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var beanContext: BeanContext
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  private val projectManager: ProjectManager get() = projectManagerProvider.get()

  @CommandLine.Option(
    names = ["--absolute"],
    negatable = true,
    description = ["Emit absolute paths"],
  )
  internal var preferAbsolute: Boolean? = null

  @CommandLine.Option(
    names = ["--install"],
    negatable = true,
    defaultValue = "true",
    description = ["Auto-install if needed; passing `false` with no install errors"],
  )
  internal var doInstall: Boolean? = null

  @CommandLine.Parameters(
    index = "0",
    arity = "0..N",
    paramLabel = "USAGE",
    description = ["Types of classpath usage; supported strings include `compile`, `runtime`, `test`"],
  )
  internal var usages: List<String> = emptyList()

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    Elide.requestNatives(server = false, tooling = true)
    val projectPath = projectOptions().projectPath()
    val cwd = Paths.get(System.getProperty("user.dir"))

    // 0. decide whether we should emit absolute paths.
    val absolute = when {
      // if the user requests absolute paths, listen to them
      preferAbsolute == true -> true

      // if we are passed a project path which is not cwd, default to absolute paths, since the project may be elsewhere
      // on disk, and we don't want classpaths colliding.
      projectPath != cwd -> true

      // otherwise, default to relative paths.
      else -> false
    }

    // 1. resolve current or specified project
    val project = projectManager.resolveProject(
      projectOptions().projectPath(),
      object: ProjectManager.ProjectEvaluatorInputs {
        override val debug: Boolean get() = false
        override val release: Boolean get() = false
        override val params: List<String>? get() = emptyList()
      }
    ) ?: return CommandResult.err(
      message = "No valid Elide project found, nothing to build"
    )

    // 2. configure, but don't fire, a build context
    val config = BuildDriver.configure(beanContext, project) { _, config ->
      config.settings.caching = true
      config.settings.dependencies = true
      config.settings.install = doInstall != false
      config.settings.checks = false
    }

    // 3. force-resolve & calculate requested classpath
    val resolver = config.resolvers[DependencyResolver.MavenResolver::class] as? MavenAetherResolver
    if (resolver == null) return CommandResult.err(
      message = "No Maven dependencies in this project"
    )
    resolver.seal()
    val (_, jobs) = resolve(config, listOf(resolver))
    jobs.joinAll()
    val eligibleUsages = (usages.takeIf { it.isNotEmpty() } ?: listOf("compile")).mapNotNull { usageStr ->
      when (usageStr.trim().lowercase()) {
        "compile" -> MultiPathUsage.Compile
        "runtime" -> MultiPathUsage.Runtime
        "test" -> MultiPathUsage.TestCompile
        "test-runtime" -> MultiPathUsage.TestRuntime
        else -> null.also {
          logging.warn { "Unrecognized usage string: '$usageStr'" }
        }
      }
    }
    val effective = MutableClasspath.empty().apply {
      eligibleUsages.map { usage ->
        resolver.classpathProvider(
          object : ClasspathSpec {
            override val usage: MultiPathUsage = usage
          },
        )?.classpath() ?: return CommandResult.err(
          message = "No compile classpath dependencies, nothing to emit for usage: $usage",
        )
      }.forEach {
        add(it)
      }
    }
    return if (effective.isEmpty()) err(message = "Empty classpath result") else success().also {
      effective.map {
        when (absolute) {
          true -> it.path.absolutePathString()
          false -> it.path.relativeTo(cwd)
        }
      }.also { suite ->
        output {
          append(suite.joinToString(File.pathSeparator))
        }
      }
    }
  }
}

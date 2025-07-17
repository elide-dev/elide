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
package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Option
import java.nio.file.Path

/**
 * # Options: Projects
 *
 * Defines common command line options shared by all CLI sub-commands which interact with, or manage, Elide projects.
 * These flags define which project to work with.
 */
@Introspected @ReflectiveAccess class ProjectOptions : OptionsMixin<ProjectOptions> {
  /** Specifies an explicit path to an Elide project to use. */
  @Option(
    names = ["-p", "--project"],
    description = ["Path to the project to build"],
    paramLabel = "<path>",
  )
  private var projectPath: String? = null

  /** Specifies an explicit path to an Elide project to use. */
  @Option(
    names = ["--lockfile"],
    description = ["Whether to enable use of Elide's lockfile system"],
    negatable = true,
  )
  var useLockfile: Boolean = true

  /** Specifies that a failure should be emitted if the lockfile is not up to date. */
  @Option(
    names = ["--frozen"],
    description = ["Whether the lockfile should be treated as frozen"],
    negatable = true,
  )
  var frozenLockfile: Boolean = false

  fun projectPath(): Path {
    return projectPath?.let { Path.of(it) } ?: Path.of(System.getProperty("user.dir"))
  }

  override fun merge(other: ProjectOptions?): ProjectOptions {
    val options = ProjectOptions()
    options.projectPath = other?.projectPath ?: this.projectPath
    options.useLockfile = other?.useLockfile ?: this.useLockfile
    options.frozenLockfile = other?.frozenLockfile ?: this.frozenLockfile
    return options
  }
}

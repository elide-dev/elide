/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.internal.cpp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.cpp.CppLibrary
import org.gradle.nativeplatform.Linkage.STATIC
import elide.internal.cpp.tasks.GenerateDummyCppSource as GenerateDummy

public abstract class CppHeaderLibraryPlugin : Plugin<Project> {
  public override fun apply(project: Project) {
    project.pluginManager.apply("cpp-library")

    val library: CppLibrary = project.extensions.getByType(CppLibrary::class.java)
    library.linkage.set(listOf(STATIC))

//    val generateTask: TaskProvider<GenerateDummy> = createTask(project.tasks, project)
//    library.source.from(generateTask.flatMap { it: GenerateDummy -> it.outputFile })
  }

  public companion object {
    private fun createTask(tasks: TaskContainer, project: Project): TaskProvider<GenerateDummy> {
      return tasks.register("generateCppHeaderSourceFile", GenerateDummy::class.java, { task: GenerateDummy ->
        val sourceFile: Provider<RegularFile> =
          project.layout.buildDirectory.file("dummy-source.cpp")
        task.outputFile.set(sourceFile)
        task.getSymbolName().set(
          "__" + toSymbol(project.path) + "_" + toSymbol(
            project.name,
          ) + "__",
        )
      })
    }

    private fun toSymbol(s: String): String {
      return s.replace(":", "_").replace("-", "_")
    }
  }
}

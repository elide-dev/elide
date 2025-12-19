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

package elide.internal.cpp.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.IOException
import java.nio.file.Files

@CacheableTask
public abstract class GenerateDummyCppSource : DefaultTask() {
  private val symbolName: Property<String> = project.objects.property(String::class.java).value("dummy")
  @get:OutputFile public val outputFile: RegularFileProperty = project.objects.fileProperty()

  @TaskAction @Throws(IOException::class) public fun doGenerate() {
    val source = ("void " + symbolName.get()) + "() {}"
    Files.write(outputFile.asFile.get().toPath(), source.toByteArray())
  }

  @Input public fun getSymbolName(): Property<String> {
    return symbolName
  }
}

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

package elide.internal.transforms

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import java.io.File

/**
 *
 */
public abstract class JarMinifier : TransformAction<JarMinifier.Parameters> {
  /**
   *
   */
  public interface Parameters : TransformParameters {
    /**
     * Classes to keep, filed by artifact name.
     */
    @get:Input public var keepClassesByArtifact: Map<String, Set<String>>
  }

  /** Input artifact to transform. */
  @get:PathSensitive(NAME_ONLY) @get:InputArtifact public abstract val inputArtifact: Provider<FileSystemLocation>

  private fun minify(artifact: File, keepClasses: Set<String>, jarFile: File) {
    println("Minifying ${artifact.name}")
    // Implementation ...
  }

  override fun transform(outputs: TransformOutputs) {
    val fileName = inputArtifact.get().asFile.name
    for (entry in parameters.keepClassesByArtifact) {
      if (fileName.startsWith(entry.key)) {
        val nameWithoutExtension = fileName.substring(0, fileName.length - 4)
        minify(inputArtifact.get().asFile, entry.value, outputs.file("${nameWithoutExtension}-min.jar"))
        return
      }
    }
    println("Nothing to minify - using ${fileName} unchanged")
    outputs.file(inputArtifact)
  }
}

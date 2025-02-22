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

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.pathString


public abstract class JarPatcher : TransformAction<JarPatcher.Parameters> {
  public interface Parameters : TransformParameters {
    @get:Input
    public var patchedClasses: MutableMap<MinimalExternalModuleDependency, MutableList<String>>

    @get:Input
    public var patchedResources: MutableMap<MinimalExternalModuleDependency, MutableList<String>>

    public fun patchClass(provider: Provider<MinimalExternalModuleDependency>, name: String) {
      val dep = provider.get()
      val target = patchedClasses.computeIfAbsent(dep) { mutableListOf() }
      target.add(name)
    }

    public fun patchResource(provider: Provider<MinimalExternalModuleDependency>, name: String) {
      val dep = provider.get()
      val target = patchedResources.computeIfAbsent(dep) { mutableListOf() }
      target.add(name)
    }
  }

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputArtifact
  public abstract val inputArtifact: Provider<FileSystemLocation>

  override fun transform(outputs: TransformOutputs) {
    val fileName = inputArtifact.get().asFile.name
    val eligibleCls = parameters.patchedClasses.keys
    val eligibleRes = parameters.patchedResources.keys
    val matchCls = eligibleCls.filter { fileName.startsWith(it.name) }
    val matchRes = eligibleRes.filter { fileName.startsWith(it.name) }

    val dropClasses = sortedSetOf<String>()
    val dropResources = sortedSetOf<String>()
    if (matchCls.isNotEmpty()) {
      require(matchCls.size == 1) { "Multiple class transform matches for ${fileName}; rejecting as unsupported" }
      val match = matchCls.first()
      val patchedClasses = parameters.patchedClasses[match] ?: error("No expected transform found for $fileName")
      dropClasses.addAll(patchedClasses)
    }
    if (matchRes.isNotEmpty()) {
      require(matchRes.size == 1) { "Multiple resource transform matches for ${fileName}; rejecting as unsupported" }
      val match = matchRes.first()
      val patchedResources = parameters.patchedResources[match] ?: error("No expected transform found for $fileName")
      dropResources.addAll(patchedResources)
    }
    if (dropClasses.isNotEmpty() || dropResources.isNotEmpty()) {
      minify(inputArtifact.get().asFile, dropClasses, dropResources, outputs.file(inputArtifact))
    } else {
      println("Nothing to minify - using ${fileName} unchanged")
      outputs.file(inputArtifact)
    }
  }

  private fun minify(artifact: File, dropClasses: Set<String>, dropResources: Set<String>, jarFile: File) {
    println("Dropping ${dropClasses.size} patched classes from '${artifact.name}'...")
    if (dropClasses.isEmpty() && dropResources.isEmpty()) {
      println("No classes or resources to drop from '${artifact.name}'")
      return
    }
    // open the jar as a zipfs
    val src: URI = URI.create("jar:file:" + artifact.toURI().getPath())
    val target: URI = URI.create("jar:file:" + jarFile.toURI().getPath())
    val droppedPaths = dropClasses.map {
      it.replace(".", "/") + ".class"
    }.plus(dropResources).map {
      Paths.get(it)
    }

    try {
      FileSystems.newFileSystem(src, emptyMap<String, Any?>()).use { fsSrc ->
        FileSystems.newFileSystem(target, emptyMap<String, Any?>()).use { fsTarget ->
          // walk all files in fsSrc
          val srcRoot = fsSrc.getPath("/")
          val targetRoot = fsTarget.getPath("/")
          Files.walk(srcRoot).forEach { srcFile ->
            if (srcFile !in droppedPaths) {
              val targetFile = targetRoot.resolve(srcFile.pathString)
              if (Files.isDirectory(srcFile)) {
                Files.createDirectories(targetFile)
              } else {
                Files.copy(srcFile, targetFile)
              }
            }
          }
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}

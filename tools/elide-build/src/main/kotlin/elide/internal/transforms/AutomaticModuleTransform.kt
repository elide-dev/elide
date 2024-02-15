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
import org.gradle.api.file.FileType.FILE
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.work.ChangeType.*
import org.gradle.work.InputChanges
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlin.io.path.Path
import elide.internal.conventions.ModularContext
import elide.internal.conventions.ModuleConfiguration
import elide.internal.conventions.jvm.defaultModuleTransforms

/**
 * # Automatic Modules Transform
 *
 * This Gradle transform is capable of reading JPMS configuration for a given project, merging it with global configs,
 * and then transforming targeted JARs accordingly to enable them for JPMS. Roughly, the logic for this is as follows:
 *
 * &nbsp;
 *
 * ## Transforming JARs for JPMS
 *
 * 1) **Read JAR to locate module class.** First, we peek inside the JAR as a zipfile, to locate the module class, which
 *    always resides at `/module-info.class`, if available. This is not expected to exist.
 *
 * 2) **Bail out for well-formed modules.** If a `module-info.class` exists, no transformation takes place, and the JAR
 *    is simply copied to the output target.
 *
 * 3) **Read JAR to locate manifest.** Next, we peek inside the JAR to read the manifest. If a manifest exists and there
 *    is already a `Automatic-Module-Name` entry, we bail out, as this JAR is not a candidate for transformation. Same
 *    as above, the JAR is simply copied to the output target.
 *
 * 4) **Determine injected automatic module name.** If the JAR is a candidate for transformation, we determine a module
 *    name to assign: if the developer assigned a name that is used; otherwise, a name is generated from the dependency
 *    artifact name.
 *
 * 5) **Inject attribute.** The JAR is unpacked, and the manifest is updated to include the new `Automatic-Module-Name`.
 *
 * 6) **JAR copied.** The JAR is re-packed and copied to the output target.
 *
 * ## Applying this transform
 *
 * Using the Elide build conventions plugin:
 *
 * ```kotlin
 * elide {
 *   java {
 *     configureModularity = true
 *   }
 *
 *   deps {
 *     automaticModules = true
 *
 *     jpms {
 *       module(libs.some.dependency) {
 *         modularize = true
 *         moduleName = "some.module.name"
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * The transform is automatically registered and available when `elide.internal.conventions` are applied.
 */
public abstract class AutomaticModuleTransform : TransformAction<AutomaticModuleTransform.Parameters> {
  /**
   * AMM Parameters
   *
   * Describes parameters which are used to configure the AMM (Automatic Module Manifest) transform.
   */
  public interface Parameters : TransformParameters {
    /** Whether to modularize all dependencies. */
    @get:Input public var modularizeAll: Boolean
  }

  /** Access to configured JPMS settings. */
  private val config: ModularContext by lazy {
    ModularContext()
  }

  /** Logger for this transform. */
  private val logger: Logger by lazy {
    Logging.getLogger(AutomaticModuleTransform::class.java)
  }

  /** Artifact to stamp/transform. */
  @get:PathSensitive(RELATIVE) @get:InputArtifact public abstract val inputArtifact: Provider<FileSystemLocation>

  /**
   * Set of changes to process incrementally.
   */
  @get:Inject public abstract val inputChanges: InputChanges

  // Determine if a JAR is eligible for transformation.
  private fun isJarEligible(jar: File): Boolean {
    return (config.resolve(jar)?.modularize ?: false) || parameters.modularizeAll
  }

  // Read a JAR file in-memory.
  private fun inMemoryZipFromJar(jar: File): ZipFile = ZipFile(jar)

  // Scan a JAR file in-memory for a `module-info.class` entry.
  private fun scanJarForModuleInfo(jar: ZipFile): Boolean {
    return jar.stream().filter { it.name.endsWith("module-info.class") }.findFirst().isPresent
  }

  // Scan a JAR file in-memory for a `META-INF/MANIFEST.MF` entry with an `Automatic-Module-Name` attribute.
  private fun scanJarForManifest(jar: ZipFile): Boolean {
    return jar.stream().filter { it.name.endsWith("MANIFEST.MF") }.findFirst().get()?.let {
      jar.getInputStream(it).use { stream ->
        val manifest = java.util.jar.Manifest(stream)
        manifest.mainAttributes.getValue("Automatic-Module-Name") != null
      }
    } ?: false
  }

  // Attempt to strictly parse a JAR name to obtain dependency info.
  private fun extractModuleNameFromJarName(jar: File): String {
    // jar name sample: `protobuf-java-3.21.11.jar`
    val name = jar.nameWithoutExtension  // `protobuf-java-3.21.11`
    val parts = name.split("-")  // `protobuf`, `java`, `3.21.11`
    return parts.dropLast(1).joinToString("_")  // `protobuf_java`
  }

  // Determine the module name to inject for a given eligible JAR file.
  private fun determineModuleName(jar: File): String {
    return when (val explicit = config.resolve(jar)?.moduleName?.ifBlank { null }) {
      null -> extractModuleNameFromJarName(jar).also {
        logger.debug("Generated module name for ${jar.name}: $it (no explicit name provided)")
      }
      else -> explicit.also {
        logger.debug("Using explicit module name for ${jar.name}: $it")
      }
    }
  }

  // Inject an automatic module name into a JAR file.
  private fun injectAutomaticModuleName(jar: File, zipfile: ZipFile, outputLocation: File) {
    val moduleName = determineModuleName(jar)
    logger.debug("Injecting automatic module name into ${jar.name}: $moduleName")

    ZipOutputStream(FileOutputStream(outputLocation)).use {
      zipfile.stream().forEach { entry ->
        if (entry.name.endsWith("MANIFEST.MF")) {
          val manifest = java.util.jar.Manifest(zipfile.getInputStream(entry))
          manifest.mainAttributes.putValue("Automatic-Module-Name", moduleName)
          it.putNextEntry(java.util.zip.ZipEntry(entry.name))
          manifest.write(it)
          it.closeEntry()
        } else {
          it.putNextEntry(java.util.zip.ZipEntry(entry.name))
          zipfile.getInputStream(entry).use { stream ->
            stream.copyTo(it)
          }
          it.closeEntry()
        }
      }
    }
  }

  // Sanitize all module info and place into the target JAR output file.
  private fun forceRemoveModuleInfo(jar: File, zipfile: ZipFile, outputLocation: File) {
    logger.debug("De-frocking module info from ${jar.name}")
    ZipOutputStream(FileOutputStream(outputLocation)).use {
      zipfile.stream().forEach { entry ->
        if (entry.name.endsWith("MANIFEST.MF")) {
          val manifest = java.util.jar.Manifest(zipfile.getInputStream(entry))
          manifest.mainAttributes["Automatic-Module-Name"]?.let {
            manifest.mainAttributes.remove(it)
          }
          it.putNextEntry(java.util.zip.ZipEntry(entry.name))
          manifest.write(it)
          it.closeEntry()
        } else if (entry.name.endsWith("module-info.class")) {
          // don't include it
          return@forEach
        } else {
          it.putNextEntry(java.util.zip.ZipEntry(entry.name))
          zipfile.getInputStream(entry).use { stream ->
            stream.copyTo(it)
          }
          it.closeEntry()
        }
      }
    }
  }

  // Cleanup deleted or needless outputs.
  private fun cleanOutputs(jar: File, output: File) {
    logger.debug("Removing leftover JPMS outputs for ${jar.name}")
    output.delete()
  }

  // Resolve global or project-level JPMS policy for a given JAR, or return `null` if no policy was found.
  private fun policyForJar(jar: File): ModuleConfiguration? {
    // if we are operating in sanitize mode for this JAR, we need to sanitize it of any module info, instead of adding
    // injected module info.
    val projectConfig = config.resolve(jar)
    val globalConfig = defaultModuleTransforms.keys.find {
      jar.nameWithoutExtension.let { filename ->
        filename == it || filename.contains(it)
      }
    }?.let { foundKey ->
      defaultModuleTransforms[foundKey]
    }
    return projectConfig ?: globalConfig
  }

  override fun transform(outputs: TransformOutputs) {
    val file = inputArtifact.get().asFile
    val outputDir = outputs.dir("${file.name}.jpms")
    logger.lifecycle("Transforming '${file.name}' for JPMS (incremental: ${inputChanges.isIncremental})")
    inputChanges.getFileChanges(inputArtifact).forEach { change ->
      val changedFile = change.file
      if (change.fileType != FILE) return@forEach
      val changePath = Path(change.normalizedPath)
      val zipfile = inMemoryZipFromJar(file)

      // determine policy for jar
      val config = policyForJar(file)
      val defrock = config?.forceClasspath ?: false
      val modularize = config?.modularize ?: parameters.modularizeAll

      when {
        // modularize mode: ignore JARs that aren't configured or enabled for JPMS
        modularize && !isJarEligible(file) -> {
          logger.debug("Skipping ${changedFile.name} - not eligible for JPMS")
          outputDir.resolve(changePath.toString()).parentFile.mkdirs()
          changedFile.copyTo(outputDir.resolve(changePath.toString()), overwrite = true)
          return@forEach
        }

        // modularize mode: ignore JARs that already have a module-info.class
        modularize && scanJarForModuleInfo(zipfile) -> {
          logger.debug("Skipping ${changedFile.name} - already a JPMS module")
          outputDir.resolve(changePath.toString()).parentFile.mkdirs()
          changedFile.copyTo(outputDir.resolve(changePath.toString()), overwrite = true)
          return@forEach
        }

        // modularize mode: ignore JARs that already have an automatic module name
        modularize && scanJarForManifest(zipfile) -> {
          logger.debug("Skipping ${changedFile.name} - already has an automatic module name")
          outputDir.resolve(changePath.toString()).parentFile.mkdirs()
          changedFile.copyTo(outputDir.resolve(changePath.toString()), overwrite = true)
          return@forEach
        }
      }

      // add `-jpms` to name without changing extension, which should be `jar`
      val originalName = changePath.fileName.toString().removeSuffix(".jar")
      val newName = "${originalName}-jpms.jar"
      val newTarget = changePath.resolveSibling(newName)
      val outputLocation = outputDir.resolve(newTarget.toString())
      outputLocation.parentFile.mkdirs()

      when (defrock) {
        true -> when (change.changeType ?: return@forEach) {
          REMOVED -> cleanOutputs(changedFile, outputLocation)
          ADDED, MODIFIED -> {
            // defrock mode: easy because we're just deleting things, so if they don't exist, whatever
            logger.debug("Defrocking ${changedFile.name} - removing module info")
            forceRemoveModuleInfo(changedFile, zipfile, outputLocation)
          }
        }
        false -> when (change.changeType ?: return@forEach) {
          REMOVED -> cleanOutputs(changedFile, outputLocation)
          ADDED, MODIFIED -> {
            logger.info("Transforming ${changedFile.name} for JPMS")
            injectAutomaticModuleName(changedFile, zipfile, outputLocation)
          }
        }
      }
    }
  }
}

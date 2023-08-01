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

import com.github.gmazzo.gradle.plugins.BuildConfigExtension
import org.gradle.api.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.*

/** Defines extensions to the Gradle [Project] type. */
public object GradleProject {
  /** Name of injected class with module metadata for Elide, if none is provided. */
  public const val moduleMetadataName = "ElideMetadata"

  /**
   * Translate a [BuildConstant] into a build-config-plugin call.
   *
   * @param name Name of the build constant.
   * @param value Build constant value with type info.
   */
  private fun BuildConfigExtension.elideConstant(name: String, value: BuildConstant) {
    buildConfigField(
      value.type.reference(),
      name,
      value.wrap(),
    )
  }

  /**
   * Define project build-time constants from the provided properties; if property values are not provided, sensible
   * defaults are resolved from the [Project] receiver.
   *
   * @receiver Project which should be configured with constants.
   * @param target Name of the class to generate.
   * @param packageName Name of the package where the constants should be generated.
   * @param libraryVersion Version constant value to define (if undefined, a version will be resolved).
   * @param optimized Whether we are building in release mode.
   * @param native Whether we are building for a native image target.
   * @param extraProperties Extra constants to define.
   */
  @JvmStatic @JvmOverloads fun Project.projectConstants(
    target: String = moduleMetadataName,
    packageName: String? = null,
    libraryVersion: String? = null,
    optimized: Boolean? = null,
    native: Boolean? = null,
    extraProperties: Map<String, BuildConstant>? = null,
  ) {
    // resolve version
    val version = Constant.string(libraryVersion ?: (this.version as? String) ?: error(
      "Failed to resolve version for project '$name'"
    ))
    val qualifier = Constant.string(packageName ?: (this.group as? String) ?: error(
      "Failed to resolve constant qualifier for project '$name'"
    ))
    val release = Constant.bool(optimized ?: false)
    val graalvm = Constant.bool(native ?: false)

    // configure the build config extension accordingly
    extensions.configure(BuildConfigExtension::class) {
      className(target)
      packageName(qualifier.value.value)
      elideConstant("VERSION", version)
      elideConstant("RELEASE", release)
      elideConstant("NATIVE", graalvm)

      useKotlinOutput {
        topLevelConstants = true
      }

      extraProperties?.forEach { name, value ->
        elideConstant(name, value)
      }
    }
  }
}

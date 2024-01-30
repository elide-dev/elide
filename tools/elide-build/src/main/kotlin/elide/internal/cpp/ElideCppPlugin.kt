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
import org.gradle.language.cpp.CppLibrary
import org.gradle.nativeplatform.test.cpp.CppTestSuite
import org.gradle.nativeplatform.test.cpp.plugins.CppUnitTestPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import elide.internal.conventions.ElideBuildExtension
import elide.internal.conventions.ElideConventionPlugin

public interface ElideCppConfiguration {
  public var headersOnly: Boolean
  public var cppStandard: String
  public var cppSourceSet: Boolean
  public var cppSourceSetName: String

  public companion object {
    public const val DEFAULT_CPP_STANDARD: String = "20"

    @JvmStatic public fun defaults(): ElideCppConfiguration = object: ElideCppConfiguration {
      override var headersOnly: Boolean = false
      override var cppStandard: String = DEFAULT_CPP_STANDARD
      override var cppSourceSet: Boolean = false
      override var cppSourceSetName: String = "cpp"
    }
  }
}

public fun ElideBuildExtension.cpp(action: (ElideCppConfiguration.() -> Unit)? = null) {
  project.plugins.getPlugin(ElideConventionPlugin::class.java).apply {
    project.plugins.apply(ElideCppPlugin::class.java)
    project.plugins.getPlugin(ElideCppPlugin::class.java).apply {
      val cfg = ElideCppConfiguration.defaults()
      if (action != null) {
        action(cfg)
      }
      configureCppFeatures(cfg)
    }
  }
}

private fun ElideBuildExtension.configureCppFeatures(cfg: ElideCppConfiguration) {
  if (cfg.headersOnly) {
    project.plugins.apply(CppHeaderLibraryPlugin::class.java)
  }
  project.extensions.getByType(CppLibrary::class.java).apply {
    if (cfg.cppSourceSet) {
      project.layout.projectDirectory.dir("src/${cfg.cppSourceSetName}Main/cpp").let {
        if (it.asFile.exists()) {
          source.from(it)
        }
      }
    }
  }
  project.plugins.apply(CppUnitTestPlugin::class.java)
  project.extensions.getByType(CppTestSuite::class.java).apply {
    if (cfg.cppSourceSet) {
      project.layout.projectDirectory.dir("src/${cfg.cppSourceSetName}Test/cpp").let {
        if (it.asFile.exists()) {
          source.from(it)
        }
      }
    }
  }
}

public abstract class ElideCppPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // Nothing yet.
  }
}

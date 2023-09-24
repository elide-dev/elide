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

package elide.runtime.feature

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.Feature.IsInConfigurationAccess
import elide.annotations.internal.VMFeature
import elide.runtime.gvm.GuestLanguage

/**
 * # Feature: Engine
 *
 * Defines an abstract GraalVM feature which registers intrinsics and implementation types for a given language or
 * execution engine.
 */
@VMFeature internal abstract class EngineFeature (private val guestLanguage: GuestLanguage) : FrameworkFeature {
  /**
   * @return Main engine implementation class, executable script class, and bindings class.
   */
  protected abstract fun engineTypes(): Triple<String, String, String>

  /**
   * @return Packages to register recursively for reflection.
   */
  protected open fun registeredPackages(): List<String> = emptyList()

  /**
   * @return Intrinsics to register.
   */
  protected open fun registeredIntrinsics(): List<String> = emptyList()

  /**
   * @return List of implementation types to register for reflection.
   */
  protected open fun implementationTypes(): List<String> = engineTypes().let {
    listOf(it.first, it.second, it.third)
  }

  /**
   * Register types which must be callable via reflection or the Polyglot API within for this language engine
   * to operate properly.
   *
   * @param access Before-analysis info for a given GraalVM image.
   */
  private fun registerRuntimeTypes(access: BeforeAnalysisAccess) {
    implementationTypes().forEach {
      registerClassForReflection(access, it)
    }
    registeredIntrinsics().forEach {
      registerClassForReflection(access, it)
    }
    registeredPackages().forEach {
      registerPackageForReflection(access, it)
    }
  }

  override fun getRequiredFeatures(): MutableList<Class<out Feature>> {
    return arrayListOf(
      ProtocolBuffers::class.java,
      VirtualFilesystem::class.java,
    )
  }

  override fun isInConfiguration(access: IsInConfigurationAccess): Boolean {
    return  (
      // the engine implementation must be in the classpath
      access.findClassByName(engineTypes().first) != null
    )
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    registerRuntimeTypes(access)
  }

  override fun getDescription(): String = "Enables the Elide ${guestLanguage.label} runtime"
}

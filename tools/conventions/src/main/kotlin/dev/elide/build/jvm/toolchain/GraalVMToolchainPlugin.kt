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

@file:Suppress("UnstableApiUsage")

package dev.elide.build.jvm.toolchain

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.jvm.toolchain.JavaToolchainResolverRegistry
import javax.inject.Inject

/** Plugin for GraalVM toolchain auto-provisioning. */
abstract class GraalVMToolchainPlugin : Plugin<Settings> {
  @Inject protected abstract fun getToolchainResolverRegistry(): JavaToolchainResolverRegistry

  override fun apply(settings: Settings) {
    settings.extensions.findByType(GraalVMToolchainExtension::class.java)
      ?: settings.extensions.create("elide-graalvm-toolchain", GraalVMToolchainExtension::class.java)
    settings.plugins.apply("jvm-toolchain-management")
    val registry = getToolchainResolverRegistry()
    registry.register(GraalVMToolchainResolver::class.java)
  }
}

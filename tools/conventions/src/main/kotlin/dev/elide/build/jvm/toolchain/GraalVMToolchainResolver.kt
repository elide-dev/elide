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

import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaToolchainDownload
import org.gradle.jvm.toolchain.JavaToolchainRequest
import org.gradle.jvm.toolchain.JavaToolchainResolver
import java.net.URI
import java.util.*
import javax.inject.Inject

/** Resolves/auto-provisions GraalVM toolchains. */
abstract class GraalVMToolchainResolver @Inject constructor (
  private val settings: Settings
) : JavaToolchainResolver {
  /** @inheritDoc */
  override fun resolve(request: JavaToolchainRequest): Optional<JavaToolchainDownload> {
    val ext = settings.extensions.findByType(GraalVMToolchainExtension::class.java)
      ?: return Optional.empty()

    return Optional.of(JavaToolchainDownload {
      ext.renderDownloadUrl().toURI()
    })
  }
}

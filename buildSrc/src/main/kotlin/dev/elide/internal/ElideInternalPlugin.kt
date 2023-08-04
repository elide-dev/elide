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

package dev.elide.internal

import dev.elide.internal.kotlin.plugin.ElideInternalPluginsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Internal build plugin which applies Elide-provided Kotlin plugins. */
class ElideInternalPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.create(ElideInternalExtension.EXTENSION_NAME, ElideInternalExtension::class.java)
  }

  /** Extension for internal build configuration. */
  abstract class ElideInternalExtension @Inject constructor (project: Project) {
    companion object {
      // Name of the extension within build scripts.
      const val EXTENSION_NAME = "elideInternal"
    }

    /** Library and tooling version. */
    public val version: AtomicReference<String> = AtomicReference(null)

    /** Kotlin plugin configuration. */
    public val kotlinPlugins: ElideInternalPluginsExtension = project.extensions.create(
      "kotlinPlugins",
      ElideInternalPluginsExtension::class.java,
    )
  }
}

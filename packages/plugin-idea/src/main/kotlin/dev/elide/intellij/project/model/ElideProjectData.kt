/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package dev.elide.intellij.project.model

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.XCollection
import dev.elide.intellij.Constants

/**
 * Serializable project data resolved from an Elide manifest during project sync. Each linked project's data is read
 * by the [dev.elide.intellij.project.ElideProjectDataService] after being resolved, and is later persisted by the
 * IDE through the [dev.elide.intellij.service.ElideProjectIndexService].
 */
data class ElideProjectData(
  /** Resolved entrypoints from the project's manifest. */
  @XCollection val entrypoints: List<ElideEntrypointInfo> = emptyList(),
) {
  companion object {
    /** Key used to store [dev.elide.intellij.project.data.ElideProjectData] in a project node during resolution. */
    val PROJECT_KEY: Key<ElideProjectData> = Key.create(ElideProjectData::class.java, 100)
  }
}

/**
 * Describes a resolved entrypoint for an Elide project. Prefer using the static factory functions to construct new
 * instances, as they automatically set some of the fields to the proper values.
 */
data class ElideEntrypointInfo(
  @Attribute val kind: Kind = Kind.Generic,
  @Attribute val displayName: String = "",
  @Attribute val descriptiveName: String = "",
  @Attribute val value: String = "",
) {
  /** Identifies the type of entry point, according to its source. */
  enum class Kind {
    Script,
    JvmMainClass,
    Generic,
  }

  companion object {
    /** Returns an entrypoint resolved from a manifest script with the given [name]. */
    @JvmStatic fun script(name: String): ElideEntrypointInfo {
      return ElideEntrypointInfo(
        Kind.Script,
        displayName = name,
        descriptiveName = name,
        value = name,
      )
    }

    /** Returns a JVM entrypoint defined by its main class name in the manifest. */
    @JvmStatic fun jvmMain(mainClassName: String): ElideEntrypointInfo {
      val simpleName = mainClassName.substringAfterLast('.')

      return ElideEntrypointInfo(
        Kind.JvmMainClass,
        displayName = simpleName,
        descriptiveName = simpleName,
        value = mainClassName,
      )
    }

    /** Returns a generic entrypoint defined in the manifest by a relative path to a script or source file. */
    @JvmStatic fun generic(entrypoint: String): ElideEntrypointInfo {
      val simpleName = entrypoint.substringAfterLast('/')

      return ElideEntrypointInfo(
        Kind.Generic,
        displayName = simpleName,
        descriptiveName = simpleName,
        value = entrypoint,
      )
    }
  }
}

/** Returns the raw base command line for the Elide CLI that can be used to invoke this entrypoint. */
val ElideEntrypointInfo.fullCommandLine: String
  get() = "${Constants.Commands.RUN} $value"

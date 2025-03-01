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

package org.gradle.kotlin.dsl

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.konan.target.HostManager
import java.nio.file.Files
import java.nio.file.Path
import elide.internal.conventions.ElideBuildExtension
import elide.internal.conventions.ElideConventionPlugin
import elide.toolchain.host.TargetInfo

public fun Project.checkNatives(
  vararg needs: TaskProvider<*>,
  enforce: Boolean = true,
  targetInfo: TargetInfo = TargetInfo.current(project),
) {
  val quickbuild = (
    project.properties["elide.release"] != "true" ||
    project.properties["elide.buildMode"] == "dev"
  )
  val isRelease = !quickbuild && (
    project.properties["elide.release"] == "true" ||
    project.properties["elide.buildMode"] == "release"
  )
  val checkNative by tasks.registering {
    group = "build"
    description = "Check native libraries required for build/test"
    mustRunAfter(
      ":packages:graalvm:natives"
    )
    doFirst {
      val targetRoot = Path.of(
        rootProject.layout.projectDirectory.dir(
          "target/${targetInfo.triple}/${if (isRelease) "release" else "debug"}"
        ).asFile.path
      )

      val sqliteLibRoot = rootProject.layout.projectDirectory.dir("third_party/sqlite/install/lib").asFile.toPath()
      val ext = when {
        HostManager.hostIsMingw -> "dll"
        HostManager.hostIsMac -> "dylib"
        else -> "so"
      }
      val sqliteLib = targetRoot.resolve("libsqlitejdbc.$ext")

      listOf(
        targetRoot,
        sqliteLib,
        sqliteLibRoot,
      ).forEach {
        if (!Files.exists(it)) {
          val isThirdParty = it.toString().contains("third_party")
          val advice = if (isThirdParty) {
            "Please build third-party natives with 'make natives'."
          } else {
            "Please build Elide's native layer with 'make natives' or 'cargo build'."
          }

          error(
            "Required path does not exist: '$it'. $advice"
          )
        }
      }
    }
  }
  if (enforce) {
    needs.forEach { it.configure { dependsOn(checkNative) } }
  }
}

public fun Project.elide(block: ElideBuildExtension.() -> Unit) {
  plugins.getPlugin(ElideConventionPlugin::class.java).applyElideConventions(this, block)
}

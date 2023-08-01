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

@file:Suppress(
  "unused",
  "JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE",
)

package elide.tool.cli.features

import org.graalvm.nativeimage.hosted.Feature
import jdk.internal.module.Modules

/**
 * # Feature: JLine3
 *
 * Registers JLine3 modules for access in a native image.
 */
class JLine3Feature : Feature {
  override fun afterRegistration(access: Feature.AfterRegistrationAccess) {
    if (!JLine3Feature::class.java.module.isNamed) {
      ModuleLayer.boot().findModule("org.graalvm.nativeimage.base").ifPresent { base: Module? ->
        Modules.addExportsToAllUnnamed(
          base,
          "com.oracle.svm.util"
        )
      }
      ModuleLayer.boot().findModule("org.graalvm.nativeimage.builder").ifPresent { builder: Module? ->
        Modules.addExportsToAllUnnamed(builder, "com.oracle.svm.core.jdk")
        Modules.addExportsToAllUnnamed(builder, "com.oracle.svm.core.jni")
      }
    }
  }
}

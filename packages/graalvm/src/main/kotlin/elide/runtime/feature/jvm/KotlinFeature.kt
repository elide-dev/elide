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

package elide.runtime.feature.jvm

import elide.annotations.internal.VMFeature
import elide.runtime.feature.EngineFeature
import elide.runtime.gvm.internals.GraalVMGuest

/** GraalVM feature which enables reflection required for the Elide Kotlin guest compiler. */
@VMFeature internal class KotlinFeature : EngineFeature(GraalVMGuest.JVM) {
  override fun engineTypes(): Triple<String, String, String> = Triple(
    "elide.runtime.gvm.internals.kotlin.KotlinRuntime",
    "elide.runtime.gvm.internals.kotlin.KotlinExecutableScript",
    "elide.runtime.gvm.internals.kotlin.KotlinInvocationBindings",
  )
}

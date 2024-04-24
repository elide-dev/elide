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
package elide.runtime.feature.js.node

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import elide.annotations.internal.VMFeature
import elide.runtime.feature.FrameworkFeature
import elide.runtime.gvm.internals.node.path.NodePaths
import elide.runtime.gvm.internals.node.process.NodeProcess
import elide.runtime.intrinsics.js.node.FilesystemAPI
import elide.runtime.intrinsics.js.node.FilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.PathAPI
import elide.runtime.intrinsics.js.node.ProcessAPI

/** GraalVM feature which enables reflective access to built-in Node modules. */
@VMFeature internal class NodeJsFeature : FrameworkFeature {
  override fun getDescription(): String = "Enables support for Node.js built-in modules"

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    // `process`
    registerClassForReflection(access, ProcessAPI::class.java.name)
    registerClassForReflection(access, NodeProcess.NodeProcessModuleImpl::class.java.name)

    // `path`
    registerClassForReflection(access, PathAPI::class.java.name)
    registerClassForReflection(access, NodePaths.BasePaths::class.java.name)
    registerClassForReflection(access, NodePaths.PosixPaths::class.java.name)
    registerClassForReflection(access, NodePaths.WindowsPaths::class.java.name)

    // `fs`
    registerClassForReflection(access, FilesystemAPI::class.java.name)
    registerClassForReflection(access, FilesystemPromiseAPI::class.java.name)
  }
}

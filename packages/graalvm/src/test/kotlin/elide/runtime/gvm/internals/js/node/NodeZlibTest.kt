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
package elide.runtime.gvm.internals.js.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.zlib.NodeZlibModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `zlib` built-in module. */
@TestCase internal class NodeZlibTest : NodeModuleConformanceTest<NodeZlibModule>() {
  override val moduleName: String get() = "zlib"
  override fun provide(): NodeZlibModule = NodeZlibModule()
  @Inject lateinit var zlib: NodeZlibModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Options")
    yield("BrotliOptions")
    yield("BrotliCompress")
    yield("BrotliDecompress")
    yield("Deflate")
    yield("DeflateRaw")
    yield("Gunzip")
    yield("Gzip")
    yield("Inflate")
    yield("InflateRaw")
    yield("Unzip")
    yield("ZlibBase")
    yield("constants")
    yield("createBrotliCompress")
    yield("createBrotliDecompress")
    yield("createDeflate")
    yield("createDeflateRaw")
    yield("createUnzip")
    yield("brotliCompress")
    yield("brotliCompressSync")
    yield("brotliDecompress")
    yield("brotliDecompressSync")
    yield("deflate")
    yield("deflateSync")
    yield("deflateRaw")
    yield("deflateRawSync")
    yield("gunzip")
    yield("gunzipSync")
    yield("gzip")
    yield("gzipSync")
    yield("inflate")
    yield("inflateSync")
    yield("inflateRaw")
    yield("inflateRawSync")
    yield("unzip")
    yield("unzipSync")
  }

  @Test override fun testInjectable() {
    assertNotNull(zlib)
  }
}

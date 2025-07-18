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
package elide.tooling.img

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ImagesTest {
  @Test fun testCompressPngInMemory() = runTest {
    val input = ImagesTest::class.java.getResourceAsStream("/test.png") ?: error("Missing test image")
    val rawData = input.readBytes()
    val compressed = Images.compress(ImageOptions.PngOptions(debug = true), from = Images.ImageSourceInMemory {
      Images.imageBufferFor(rawData)
    })

    val compressedData = Array(compressed.result.remaining()) {
      compressed.result.get(it)
    }
    assertNotNull(compressed, "compressed png should not be null")
    assertTrue(compressedData.isNotEmpty(), "compressed data should not be empty")
    assertFalse(compressedData.equals(rawData), "compressed data should not equal original")
  }

  @Test fun testCompressJpgInMemory() = runTest {
    val input = ImagesTest::class.java.getResourceAsStream("/test.jpg") ?: error("Missing test image")
    val rawData = input.readBytes()
    val compressed = Images.compress(ImageOptions.JpgOptions(debug = true), from = Images.ImageSourceInMemory {
      Images.imageBufferFor(rawData)
    })

    val compressedData = Array(compressed.result.remaining()) {
      compressed.result.get(it)
    }
    assertNotNull(compressed, "compressed png should not be null")
    assertTrue(compressedData.isNotEmpty(), "compressed data should not be empty")
    assertFalse(compressedData.equals(rawData), "compressed data should not equal original")
  }
}

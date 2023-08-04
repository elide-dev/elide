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

package tools.elide.data

import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import com.google.protobuf.util.JsonFormat
import tools.elide.crypto.HashAlgorithm
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.test.assertNotNull
import elide.proto.test.data.AbstractDataContainerTests
import elide.testing.annotations.Test

/** Tests for data container protocol buffer models. */
class DataProtoTests : AbstractDataContainerTests<DataContainer>() {
  /** @inheritDoc */
  override fun allocateContainer(): DataContainer = DataContainer.newBuilder().build()

  /** @inheritDoc */
  override fun allocateContainer(data: String): DataContainer = allocateContainer(data.toByteArray(
    StandardCharsets.UTF_8,
  ))

  /** @inheritDoc */
  override fun allocateContainer(data: ByteArray): DataContainer = dataContainer {
    raw = data.toByteString()
    integrity.add(dataFingerprint {
      hash = HashAlgorithm.SHA256
      fingerprint = MessageDigest.getInstance("SHA-256").let { digest ->
        digest.digest(data).toByteString()
      }
    })
  }

  @Test override fun testDataContainer() {
    assertNotNull(dataContainer {
      raw = "hello world".toByteStringUtf8()
      integrity.add(dataFingerprint {
        hash = HashAlgorithm.SHA256
        fingerprint = MessageDigest.getInstance("SHA-256").let { digest ->
          digest.digest("hello world".toByteArray(StandardCharsets.UTF_8)).toByteString()
        }
      })
    })
  }

  @Test override fun testDataContainerJson() {
    val container = allocateContainer("hello world")
    val encoded = JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace()
      .print(container)
    assertNotNull(encoded, "should be able to encode a data container proto as JSON")
  }
}

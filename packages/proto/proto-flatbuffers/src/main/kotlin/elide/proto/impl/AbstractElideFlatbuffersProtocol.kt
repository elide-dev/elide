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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl

import java.util.*
import elide.proto.ElideProtocol
import elide.proto.ElideProtocol.*
import elide.proto.api.data.DataModelStrategy
import elide.proto.impl.data.FlatDataContainer
import elide.proto.impl.data.FlatDataFingerprint
import elide.proto.impl.data.FlatEncoding
import elide.proto.impl.data.FlatHashAlgorithm
import elide.proto.impl.wkt.FlatTimestamp

/**
 * TBD.
 */
public abstract class AbstractElideFlatbuffersProtocol : ElideProtocol {
  // Strategy adapter singleton.
  private val strategy: FlatbuffersModelStrategy = FlatbuffersModelStrategy()

  /** @inheritDoc */
  override val reflection: Boolean get() = false

  /** @inheritDoc */
  override val compression: Boolean get() = true

  /** @inheritDoc */
  override fun engine(): ImplementationLibrary = ImplementationLibrary.FLATBUFFERS

  /** @inheritDoc */
  override fun dialects(): EnumSet<Dialect> = EnumSet.of(
    Dialect.JSON,
    Dialect.FLATBUFFERS,
  )

  /** @inheritDoc */
  override fun base(): Class<*>? = null

  /** @inheritDoc */
  override fun strategy() = strategy

  /** Core data model based on Flatbuffers. */
  public class FlatbuffersModelStrategy : ModelAdapterStrategy {
    /** @inheritDoc */
    override fun model() = object : DataModelStrategy<
      FlatHashAlgorithm,
      FlatEncoding,
      FlatDataContainer,
      FlatDataContainer.Builder,
      FlatDataFingerprint,
      FlatDataFingerprint.Builder,
      FlatTimestamp,
      FlatTimestamp.TimestampBuilder,
    > {
      // Return a Flatbuffer-based data fingerprint factory.
      override fun fingerprints() = FlatDataFingerprint.Factory

      // Return a Flatbuffer-based data container factory.
      override fun containers() = FlatDataContainer.Factory

      // Return a Flatbuffer-based timestamp factory.
      override fun timestamps() = FlatTimestamp.Factory
    }
  }
}

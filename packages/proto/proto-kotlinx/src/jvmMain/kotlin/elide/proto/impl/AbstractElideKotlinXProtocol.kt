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
import elide.proto.ElideProtocol.Dialect
import elide.proto.ElideProtocol.ImplementationLibrary
import elide.proto.ElideProtocol.ModelAdapterStrategy
import elide.proto.api.data.DataModelStrategy
import elide.proto.impl.data.KxDataContainer
import elide.proto.impl.data.KxDataFingerprint
import elide.proto.impl.data.KxEncoding
import elide.proto.impl.data.KxHashAlgorithm
import elide.proto.impl.wkt.KxTimestamp

/**
 * TBD.
 */
public abstract class AbstractElideKotlinXProtocol : ElideProtocol {
  // Strategy singleton.
  private val strategy: KotlinXModelStrategy = KotlinXModelStrategy()

  /** @inheritDoc */
  override val reflection: Boolean get() = true

  /** @inheritDoc */
  override val compression: Boolean get() = true

  /** @inheritDoc */
  override fun engine(): ImplementationLibrary = ImplementationLibrary.KOTLINX

  /** @inheritDoc */
  override fun dialects(): EnumSet<Dialect> = EnumSet.of(
    Dialect.JSON,
    Dialect.PROTO,
  )

  /** @inheritDoc */
  override fun base(): Class<*>? = null

  /** @inheritDoc */
  override fun strategy(): ModelAdapterStrategy = strategy

  /** Implements a model adapter strategy based on KotlinX Serialization. */
  public class KotlinXModelStrategy : ModelAdapterStrategy {
    override fun model() = object : DataModelStrategy<
      KxHashAlgorithm,
      KxEncoding,
      KxDataContainer,
      KxDataContainer.Builder,
      KxDataFingerprint,
      KxDataFingerprint.Builder,
      KxTimestamp,
      KxTimestamp.TimestampBuilder,
    > {
      // Return a KotlinX-based data fingerprint factory.
      override fun fingerprints() = KxDataFingerprint.Factory

      // Return a KotlinX-based data container factory.
      override fun containers() = KxDataContainer.Factory

      // Return a KotlinX-based timestamp factory.
      override fun timestamps() = KxTimestamp.Factory
    }
  }
}

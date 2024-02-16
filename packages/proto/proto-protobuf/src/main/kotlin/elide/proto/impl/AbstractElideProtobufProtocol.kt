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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl

import com.google.protobuf.GeneratedMessageV3
import tools.elide.data.Encoding
import tools.elide.std.HashAlgorithm
import java.util.*
import kotlin.reflect.KClass
import elide.proto.ElideProtocol
import elide.proto.ElideProtocol.*
import elide.proto.api.data.DataModelStrategy
import elide.proto.impl.data.ProtoDataContainer
import elide.proto.impl.data.ProtoDataFingerprint
import elide.proto.impl.wkt.ProtoTimestamp

/**
 * TBD.
 */
public abstract class AbstractElideProtobufProtocol : ElideProtocol {
  // Strategy adapter singleton.
  private val strategy: ProtoModelStrategy = ProtoModelStrategy()

  override val reflection: Boolean get() = true

  override val compression: Boolean get() = true

  override fun engine(): ImplementationLibrary = ImplementationLibrary.PROTOBUF

  override fun dialects(): EnumSet<Dialect> = EnumSet.of(
    Dialect.JSON,
    Dialect.PROTO,
  )

  override fun base(): KClass<*>? = GeneratedMessageV3::class

  override fun strategy(): ModelAdapterStrategy = strategy

  /** Protocol Buffers-backed implementation of basic model features. */
  public class ProtoModelStrategy : ModelAdapterStrategy {
    override fun model() = object : DataModelStrategy<
      HashAlgorithm,
      Encoding,
      ProtoDataContainer,
      ProtoDataContainer.Builder,
      ProtoDataFingerprint,
      ProtoDataFingerprint.Builder,
      ProtoTimestamp,
      ProtoTimestamp.TimestampBuilder,
    > {
      // Return a protocol buffer-based data fingerprint factory.
      override fun fingerprints() = ProtoDataFingerprint.Factory

      // Return a protocol buffer-based data container factory.
      override fun containers() = ProtoDataContainer.Factory

      // Return a protocol buffer-based timestamp factory.
      override fun timestamps() = ProtoTimestamp.Factory
    }
  }
}

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl

import com.google.protobuf.GeneratedMessageV3
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.Encoding
import java.util.*
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

  /** @inheritDoc */
  override val reflection: Boolean get() = true

  /** @inheritDoc */
  override val compression: Boolean get() = true

  /** @inheritDoc */
  override fun engine(): ImplementationLibrary = ImplementationLibrary.PROTOBUF

  /** @InheritDoc */
  override fun dialects(): EnumSet<Dialect> = EnumSet.of(
    Dialect.JSON,
    Dialect.PROTO,
  )

  /** @InheritDoc */
  override fun base(): Class<*>? = GeneratedMessageV3::class.java

  /** @InheritDoc */
  override fun strategy(): ModelAdapterStrategy = strategy

  /** Protocol Buffers-backed implementation of basic model features. */
  public class ProtoModelStrategy : ModelAdapterStrategy {
    /** @inheritDoc */
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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.impl

import elide.proto.ElideProtocol
import elide.proto.ElideProtocol.Dialect
import elide.proto.ElideProtocol.ImplementationLibrary
import elide.proto.ElideProtocol.ModelAdapterStrategy
import elide.proto.api.data.DataModelStrategy
import elide.proto.impl.data.FlatDataContainer
import elide.proto.impl.data.FlatDataFingerprint
import elide.proto.impl.data.FlatEncoding
import elide.proto.impl.data.FlatHashAlgorithm
import elide.proto.impl.wkt.FlatTimestamp
import java.util.*

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

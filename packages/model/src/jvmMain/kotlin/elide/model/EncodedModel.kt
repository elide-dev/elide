@file:Suppress("MemberVisibilityCanBePrivate")

package elide.model

import com.google.common.base.Objects
import com.google.protobuf.Descriptors
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import kotlinx.serialization.Serializable
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets
import javax.annotation.Nonnull
import javax.annotation.concurrent.Immutable

/**
 * Container class for an encoded Protocol Buffer model. Holds raw encoded model data, in any of Protobuf's built-in
 * well-defined serialization formats (for instance [EncodingMode.BINARY] or [EncodingMode.JSON]).
 *
 * Raw model data is encoded before being held by this record. In addition to holding the raw data, it also keeps the
 * fully-qualified path to the model that the data came from, and the serialization format the data lives in. After
 * being wrapped in this class, a batch of model data is additionally compliant with [Serializable], `Cloneable`, and
 * [Comparable].
 *
 * @param rawBytes Raw bytes of the enclosed model.
 * @param dataMode Operating mode for the underlying data. Always `BINARY` unless manually constructed.
 * @param type Type of model held by this entity.
 */
@Serializable
@Immutable
public class EncodedModel private constructor(
  public var rawBytes: ByteArray,
  public var dataMode: EncodingMode,
  public var type: String
): java.io.Serializable, Cloneable {
  public companion object {
    public const val serialVersionUID: Long = 1L

    /**
     * Return an encoded representation of the provided message. This method is rather heavy-weight: it fully encodes the
     * provided Protobuf message into the Protocol Buffers binary format.
     *
     *
     * If a descriptor is *not* already on-hand, use the other variant of this method, which accesses the
     * descriptor directly from the message.
     *
     * @see .from
     * @param message Message to encode as binary.
     * @return Java-serializable wrapper including the encoded Protobuf model.
     */
    @JvmStatic
    public fun from(message: Message, descriptor: Descriptors.Descriptor? = null): EncodedModel {
      return EncodedModel(
        message.toByteArray(),
        EncodingMode.BINARY,
        (descriptor ?: message.descriptorForType).fullName
      )
    }

    /**
     * Wrap a blob of opaque data, asserting that it is actually an encoded model record. Using this factory method, any
     * of the Protobuf serialization formats may be used safely with this class.
     *
     * All details must be provided manually to this method variant. It is incumbent on the developer that they line
     * up properly. For safer options, see the other factory methods on this class.
     *
     * @param type Fully-qualified type name, for the encoded instance we are storing.
     * @param data Raw data for the encoded model to be wrapped.
     * @return Encoded model instance.
     */
    @JvmStatic
    public fun wrap(type: String, mode: EncodingMode, data: ByteArray): EncodedModel {
      return EncodedModel(data, mode, type)
    }
  }
  /**
   * Write the attached encoded Protocol Buffer data to the specified object stream, such that this object is fully
   * packed for Java serialization purposes.
   *
   * @param out Output stream to write this encoded model object to.
   * @throws IOException If an IO error of some kind occurs.
   */
  @Throws(IOException::class)
  private fun writeObject(@Nonnull out: ObjectOutputStream) {
    out.writeObject(type)
    out.writeObject(dataMode)
    out.write(rawBytes.size)
    out.write(rawBytes)
  }

  /**
   * Re-inflate an encoded model from a Java serialization context. Read the stream to install local properties, such
   * that the object is re-constituted.
   *
   * @param in Input stream to read object data from.
   * @throws IOException If an IO error of some kind occurs.
   * @throws ClassNotFoundException If the specified Protobuf model cannot be found or resolved.
   */
  @Throws(IOException::class, ClassNotFoundException::class)
  private fun readObject(@Nonnull `in`: ObjectInputStream) {
    type = java.util.Objects.requireNonNull(
      `in`.readObject() as String,
      "Cannot deserialize EncodedModel with empty type."
    )
    dataMode = java.util.Objects.requireNonNull(
      `in`.readObject() as EncodingMode,
      "Cannot deserialize EncodedModel with empty data mode."
    )

    // read length-prefixed raw bytes
    val datasize = `in`.read()
    val data = ByteArray(datasize)
    val read = `in`.read(data)
    assert(datasize == read)
    rawBytes = data
  }

  // -- Cloneable -- //

  /** @inheritDoc */
  override fun clone(): EncodedModel {
    val rawData = ByteArray(rawBytes.size)
    System.arraycopy(rawBytes, 0, rawData, 0, rawBytes.size)
    return EncodedModel(rawData, dataMode, type)
  }


  // -- Equals/Hash -- //

  /** @inheritDoc */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as EncodedModel
    return Objects.equal(type, that.type) && dataMode === that.dataMode &&
      Objects.equal(
        rawBytes.contentHashCode(),
        that.rawBytes.contentHashCode()
      )
  }

  /** @inheritDoc */
  override fun hashCode(): Int {
    return Objects
      .hashCode(type, dataMode, rawBytes.contentHashCode())
  }

  /** @inheritDoc */
  override fun toString(): String {
    return "EncodedModel{" +
      "dataMode='" + dataMode + '\'' +
      ", type=" + type +
      '}'
  }

  // -- Inflate -- //
  /**
   * Re-inflate the encoded model data held by this object, into an instance of `Model`, via the provided `builder`.
   *
   * **Note:** before the model is returned from this method, it will be casted to match the generic type the user
   * is looking for. It is incumbent on the invoking developer to make sure the generic access that occurs won't produce
   * a [ClassCastException]. [type] can be interrogated to resolve types before inflation.
   *
   * @param model Empty model instance from which to resolve a parser.
   * @param Model Generic model type inflated and returned by this method.
   * @return Instance of the model, inflated from the encoded data.
   * @throws InvalidProtocolBufferException If the held data is incorrectly formatted.
   **/
  @Suppress("UNCHECKED_CAST")
  @Throws(InvalidProtocolBufferException::class)
  public fun <Model: Message> inflate(model: Message): Model {
    return if (dataMode === EncodingMode.JSON) {
      val builder = model.newBuilderForType()
      JsonFormat.parser().merge(
        String(rawBytes, StandardCharsets.UTF_8),
        builder
      )
      builder.build() as Model
    } else {
      model.parserForType.parseFrom(rawBytes) as Model
    }
  }
}

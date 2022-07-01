package elide.model

import com.google.protobuf.Message
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import elide.model.EncodedModel.Companion.wrap
import elide.model.err.ModelDeflateException
import elide.model.err.ModelInflateException
import elide.runtime.Logger
import elide.runtime.Logging
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import javax.annotation.Nonnull
import javax.annotation.concurrent.Immutable
import javax.annotation.concurrent.ThreadSafe


/**
 * Defines a [ModelCodec] which uses Protobuf serialization to export and import protos to and from from raw
 * byte-strings. These formats are built into Protobuf and are considered extremely reliable, even across languages.
 *
 * Two formats of Protobuf serialization are supported:
 *  * **Binary:** Most efficient format. Best for production use. Completely illegible to humans.
 *  * **ProtoJSON:** Protocol Buffers-defined JSON translation protocol.
 *
 * @see ModelCodec Generic model codec interface.
 * @param instance Builder from which to spawn models.
 * @param wireMode Protobuf wire format to use.
 * @param registry Type registry to use.
 */
@Immutable
@ThreadSafe
public class ProtoModelCodec<Model: Message> private constructor (
  private val instance: Model,
  private val wireMode: EncodingMode,
  registry: TypeRegistry?
): ModelCodec<Model, EncodedModel, EncodedModel> {
  /** JSON printer utility, initialized when operating with `wireMode=JSON`.  */
  private var jsonPrinter: JsonFormat.Printer? = null

  /** JSON parser utility, initialized when operating with `wireMode=JSON`.  */
  private var jsonParser: JsonFormat.Parser? = null

  /** Serializer object.  */
  @Nonnull
  private val serializer: ModelSerializer<Model, EncodedModel> = ProtoMessageSerializer()

  /** De-serializer object.  */
  @Nonnull
  private val deserializer: ModelDeserializer<EncodedModel, Model> = ProtoMessageDeserializer()

  init {
    logging.trace {
      "Initializing `ProtoModelCodec` with format '${wireMode.name}'."
    }
    if (wireMode === EncodingMode.JSON) {
      val resolvedRegisry = registry
        ?: TypeRegistry.newBuilder()
          .add(instance.descriptorForType)
          .build()
      jsonParser = JsonFormat.parser()
        .usingTypeRegistry(resolvedRegisry)
      jsonPrinter = JsonFormat.printer()
        .usingTypeRegistry(resolvedRegisry)
        .sortingMapKeys()
        .omittingInsignificantWhitespace()
    } else {
      jsonParser = null
      jsonPrinter = null
    }
  }

  /** Serializes model instances into raw bytes, according to Protobuf wire protocol semantics.  */
  private inner class ProtoMessageSerializer: ModelSerializer<Model, EncodedModel> {
    /**
     * Serialize a model instance from the provided object type to the specified output type, throwing exceptions
     * verbosely if we are unable to correctly, verifiably, and properly export the record.
     *
     * @param input Input record object to serialize.
     * @return Serialized record data, of the specified output type.
     * @throws ModelDeflateException If the model fails to export or serialize for any reason.
     */
    @Throws(ModelDeflateException::class, IOException::class)
    override fun deflate(input: Model): EncodedModel {
      logging.debug {
        "Deflating record of type '${input.descriptorForType.fullName}' with format ${wireMode.name}."
      }
      return if (wireMode === EncodingMode.BINARY) {
        wrap(
          input.descriptorForType.fullName,
          wireMode,
          input.toByteArray()
        )
      } else {
        wrap(
          input.descriptorForType.fullName,
          wireMode,
          Objects.requireNonNull(jsonPrinter)!!.print(input).toByteArray(StandardCharsets.UTF_8)
        )
      }
    }
  }

  /** De-serializes model instances from raw bytes, according to Protobuf wire protocol semantics.  */
  private inner class ProtoMessageDeserializer: ModelDeserializer<EncodedModel, Model> {
    /**
     * De-serialize a model instance from the provided input type, throwing exceptions verbosely if we are unable to
     * correctly load the record.
     *
     * @param input Input data or object from which to load the model instance.
     * @return De-serialized and inflated model instance. Always a [Message].
     * @throws ModelInflateException If the model fails to load for any reason.
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(ModelInflateException::class, IOException::class)
    override fun inflate(input: EncodedModel): Model {
      logging.debug {
        "Inflating record of type '${input.type}' with format ${wireMode.name}."
      }
      return if (wireMode === EncodingMode.BINARY) {
        instance.newBuilderForType().mergeFrom(input.rawBytes).build() as Model
      } else {
        val builder = instance.newBuilderForType()
        Objects.requireNonNull(jsonParser)!!.merge(
          input.rawBytes.decodeToString(),
          builder
        )
        builder.build() as Model
      }
    }
  }

  // -- API: Codec -- //

  /** @inheritDoc */
  override fun instance(): Model {
    return instance
  }

  /**
   * Acquire an instance of the [ModelSerializer] attached to this adapter. The instance is not guaranteed to be created
   * fresh for this invocation.
   *
   * @return Serializer instance.
   * @see .deserializer
   * @see .deserialize
   */
  override fun serializer(): ModelSerializer<Model, EncodedModel> {
    return serializer
  }

  /**
   * Acquire an instance of the [ModelDeserializer] attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @return Deserializer instance.
   * @see .serializer
   * @see .serialize
   */
  override fun deserializer(): ModelDeserializer<EncodedModel, Model> {
    return deserializer
  }

  public companion object {
    /** Default wire format mode.  */
    private val DEFAULT_FORMAT = EncodingMode.BINARY

    /** Log pipe to use.  */
    private val logging: Logger = Logging.of(ProtoModelCodec::class.java)

    // -- Factories -- //

    /**
     * Acquire a Protobuf model codec for the provided model instance. The codec will operate in the default
     * [EncodingMode] unless specified otherwise via the other method variants on this object.
     *
     * @param M Model instance type.
     * @param instance Model instance to return a codec for.
     * @param mode Wire format mode to operate in (one of `JSON` or `BINARY`). Defaults to `BINARY`.
     * @return Model codec which serializes and de-serializes to/from Protobuf wire formats.
     */
    @JvmStatic
    public fun <M : Message> forModel(
      instance: M,
      mode: EncodingMode = DEFAULT_FORMAT,
      registry: Optional<TypeRegistry> = Optional.empty(),
    ): ProtoModelCodec<M> {
      return ProtoModelCodec(
        instance,
        mode,
        registry.orElse(null)
      )
    }
  }
}

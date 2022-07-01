package elide.model

import com.google.protobuf.Message
import elide.model.err.ModelDeflateException
import elide.model.err.ModelInflateException
import java.io.IOException

/**
 * Specifies the requisite interface for a data codec implementation. These objects are responsible for performing model
 * serialization and deserialization, within different circumstances. For example, models are used with databases via
 * adapters that serialize each model into a corresponding database object, or series of database calls.
 *
 *
 * Adapters are *bi-directional*, i.e., they must support transitioning both *to* and *from* message
 * representations, based on circumstance. Services are the only case where this is generally not necessary, because
 * gRPC handles serialization automatically.
 *
 * @see ModelSerializer Surface definition for a model serializer.
 * @see ModelDeserializer Surface definition for a model de-serializer.
 * @param Model Model type which this codec is responsible for serializing and de-serializing.
 * @param WriteIntermediate Intermediate record type which this codec converts model instances into.
 **/
public interface ModelCodec<Model: Message, WriteIntermediate, ReadIntermediate> {
  // -- Components -- //

  /**
   * Acquire an instance of the [ModelSerializer] attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @return Serializer instance.
   */
  public fun serializer(): ModelSerializer<Model, WriteIntermediate>

  /**
   * Acquire an instance of the [ModelDeserializer] attached to this adapter. The instance is not guaranteed to be
   * created fresh for this invocation.
   *
   * @return Deserializer instance.
   */
  public fun deserializer(): ModelDeserializer<ReadIntermediate, Model>

  /**
   * Retrieve the default instance stored with this codec. Each [Message] with a paired [ModelCodec] retains
   * a reference to its corresponding default instance.
   *
   * @return Default model instance.
   */
  public fun instance(): Model

  // -- Proxies -- //
  /**
   * Sugar shortcut to serialize a model through the current codec's installed [ModelSerializer].
   *
   * This method just proxies to that object (which can be acquired via [.serializer]). If any error occurs
   * while serializing, [ModelDeflateException] is thrown.
   *
   * @param instance Input model to serialize.
   * @return Serialized output data or object.
   * @throws ModelDeflateException If some error occurs while serializing the model.
   * @throws IOException If some IO error occurs.
   */
  @Throws(ModelDeflateException::class, IOException::class)
  public fun serialize(instance: Model): WriteIntermediate {
    return serializer().deflate(instance)
  }

  /**
   * Sugar shortcut to de-serialize a model through the current codec's installed [ModelDeserializer].
   *
   * This method just proxies to that object (which can be acquired via [.deserializer]). If any error occurs
   * while de-serializing, [ModelInflateException] is thrown.
   *
   * @param input Input data to de-serialize into a model instance.
   * @return Model instance, deserialized from the input data.
   * @throws ModelInflateException If some error occurs while de-serializing the model.
   * @throws IOException If some IO error occurs.
   */
  @Throws(ModelInflateException::class, IOException::class)
  public fun deserialize(input: ReadIntermediate): Model {
    return deserializer().inflate(input)
  }
}

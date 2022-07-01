package elide.model

import com.google.protobuf.Message
import elide.model.err.ModelInflateException
import java.io.IOException

/**
 * Describes the interface for a de-serializer, which is responsible for transitioning (adapting) objects from a given
 * type (or tree of types) to [Message] instances, corresponding with the data record type managed by this object.
 *
 * Deserializers are part of a wider set of objects, including the inverse of this object, which is called a
 * [ModelSerializer]. Generally these objects come in matching pairs, but sometimes they are mixed and matched as
 * needed. Pairs of serializers/de-serializers are grouped by a [ModelCodec].
 *
 * @see ModelSerializer The inverse of this object, which is responsible for adapting {@link Message} instances to some
 *      other type of object or data.
 * @param Input Input data type, which this de-serializer will convert into a message instance.
 * @param Model Message object type, which is the output of this de-serializer.
 */
public interface ModelDeserializer<Input, Model: Message> {
  /**
   * De-serialize a model instance from the provided input type, throwing exceptions verbosely if we are unable to
   * correctly load the record.
   *
   * @param input Input data or object from which to load the model instance.
   * @return De-serialized and inflated model instance. Always a [Message].
   * @throws ModelInflateException If the model fails to load for any reason.
   * @throws IOException If some IO error occurs.
   */
  @Throws(ModelInflateException::class, IOException::class)
  public fun inflate(input: Input): Model

  /** Describes errors that occur during model deserialization or inflation activities.  */
  public class DeserializationError internal constructor(message: String?) : RuntimeException(message)
}

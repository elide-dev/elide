package elide.model

import com.google.protobuf.Message
import elide.model.err.ModelDeflateException
import java.io.IOException

/**
 * Describes the surface interface of an object responsible for *serializing* business data objects (hereinafter,
 * "models").
 *
 * In other words, converting [Message] instances into some generic type [Output].
 *
 * @param Model Data model which a given serializer implementation is responsible for adapting.
 * @param Output Output type which the serializer will provide when invoked with a matching model instance.
 */
public interface ModelSerializer<Model: Message, Output> {
  /**
   * Describes available *write dispositions*, each of which presents a strategy that governs how an individual
   * write operation is handled with regard to underlying storage. Each option is explained in its own documentation.
   */
  public enum class WriteDisposition {
    /** Blind writes. Just applies the write without regard to side effects.  */
    BLIND,

    /** Create-style writes. Guarantees the object does not exist before writing.  */
    CREATE,

    /** Update-style writes. Guarantees the object *does* exist before writing.  */
    UPDATE
  }

  /**
   * Enumerates modes for encoding enums. In NUMERIC mode, enumerated entry ID numbers are used when serializing enum
   * values. In NAME mode, the string name is used. Both are valid for read operations.
   */
  public enum class EnumSerializeMode {
    /** Encode enum values as their numeric ID.  */
    NUMERIC,

    /** Encode enum values as their string name.  */
    NAME
  }

  /**
   * Enumerates modes for encoding timestamps. In TIMESTAMP mode, numeric timestamps with millisecond precision (since
   * the Unix epoch) are provided. In ISO8601 mode, ISO8601-formatted strings are provided.
   */
  public enum class InstantSerializeMode {
    /** Encode temporal instants as millisecond-precision Unix timestamps.  */
    TIMESTAMP,

    /** Encode temporal instants as ISO8601-formatted strings.  */
    ISO8601
  }

  /** Describes errors that occur during model serialization activities.  */
  public class SerializationError internal constructor(message: String?) : RuntimeException(message)

  /**
   * Serialize a model instance from the provided object type to the specified output type, throwing exceptions
   * verbosely if we are unable to correctly export the record.
   *
   * @param input Input record object to serialize.
   * @return Serialized record data, of the specified output type.
   * @throws ModelDeflateException If the model fails to export or serialize for any reason.
   * @throws IOException If an IO error of some kind occurs.
   */
  @Throws(ModelDeflateException::class, IOException::class)
  public fun deflate(input: Model): Output
}

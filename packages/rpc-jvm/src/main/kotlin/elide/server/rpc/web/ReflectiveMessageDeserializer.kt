package elide.server.rpc.web

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import elide.runtime.Logger
import elide.runtime.Logging
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Deserializer which is responsible for (1) resolving a protocol buffer object from a given reflective [Method], then
 * (2) de-serializing a set of raw bytes into a resolved [Message].
 */
internal class ReflectiveMessageDeserializer: MessageDeserializer {
  // Private logger.
  private val logging: Logger = Logging.of(ReflectiveMessageDeserializer::class)

  /** @inheritDoc */
  override fun deserialize(method: Method, rawData: ByteArray): Message {
    if (method.parameterTypes.isEmpty())
      throw IllegalStateException("Failed to locate protocol buffer request type from method '${method.name}'")
    val firstArgType: Class<*> = method.parameterTypes.first()

    // resolve a parser method for this message type
    val parser = try {
      firstArgType.getMethod("parseFrom", ByteArray::class.java)
    } catch (noSuch: NoSuchMethodException) {
      val msg = "Failed to resolve `parseFrom` method from protocol buffer type '${firstArgType.name}'"
      logging.error(
        msg,
        noSuch
      )
      throw IllegalStateException(
        msg,
        noSuch,
      )
    }

    // invoke the parser
    return try {
      when (val message: Any? = parser.invoke(null, rawData)) {
        null -> {
          logging.warn {
            "Invalid data: failed to decode protocol buffer message for request type '${firstArgType.name}'"
          }
          throw IllegalStateException(
            "Failed to decode protocol buffer request of type '${firstArgType.name}'"
          )
        }
        !is Message -> {
          logging.error {
            "Unfamiliar type received for expected protocol buffer message type '${firstArgType.name}': $message " +
                "of type '${message.javaClass.name}'"
          }
          throw IllegalStateException(
            "Failed to resolve request type '${firstArgType.name}': object is of type '${message.javaClass.name}'"
          )
        }
        else -> {
          logging.trace {
            "Decoded protocol buffer message as request of type '${firstArgType.name}': $message"
          }
          message
        }
      }
    } catch (ite: InvocationTargetException) {
      val cause = ite.cause ?: ite
      if (cause is InvalidProtocolBufferException) {
        logging.warn(
          "Failed to decode request message of type '${firstArgType.name}': invalid proto data",
          cause
        )
        // throw this one directly, because it is handled specifically by `GrpcWebController`.
        throw cause
      } else {
        logging.warn(
          "Failed to dispatch parser method",
          ite
        )
        throw IllegalStateException(
          "Method dispatch failed",
          ite
        )
      }
    }
  }
}

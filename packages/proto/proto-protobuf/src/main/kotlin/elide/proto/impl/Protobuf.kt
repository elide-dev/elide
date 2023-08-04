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

package elide.proto.impl

import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import java.util.*
import kotlinx.datetime.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import elide.proto.impl.Protobuf.ProtoBuilderContext
import elide.util.proto.adapters.ProtoConvertible
import elide.util.proto.adapters.ProtoModel
import elide.util.proto.adapters.ProtoSchemaConvertible

/**
 * # Utilities: Protocol Buffers
 *
 * General common utilities for protocol buffer objects in Java and Kotlin (JVM-only). This includes assembly of proto-
 * message objects from universal Elide entities and vice versa, as well as Java type checking and null checking
 * facilities used when building generic protocol messages.
 *
 * ## Adapting between models and messages
 *
 * All inheritors of [ProtoConvertible] gain the suite of methods defined here, but they are also usable on a static
 * basis, outside of entity contexts. Additionally, since `UniversalModel` also extends [ProtoConvertible], any Elide
 * universal model is immediately usable with these tools.
 *
 * To adopt [ProtoConvertible] (and its static cousin, [ProtoSchemaConvertible]), adopt the former on the object, and
 * the latter on the companion. The former is used to convert the object to a protocol message, and the latter is used
 * to create a new object from a protocol message.
 *
 * For example:
 *
 * ```proto
 * syntax = "proto3";
 * option java_package = "my.cool.models";
 * // ...
 *
 * // Some cool model.
 * message MyCoolModel {
 *   // ID of the model.
 *   string id = 1;
 * }
 * ```
 *
 * ```kotlin
 * // imports...
 * import my.cool.models.MyCoolModel as MyCoolModelProto
 *
 * /** Kotlin implementation of `MyCoolModel`. */
 * @Serializable data class MyCoolModel(
 *   @ProtoNumber(1) @SerialName("id") val id: String,
 * ): ProtoConvertible<MyCoolModelProto> {
 *     /** here is the implementation for moving *to* protos */
 *     override fun toMessage(): MyCoolModelProto = buildFrom(::myCoolModel) { model ->
 *         id = ifNotBlank(model.id) { id = it }
 *     }
 * }
 * ```
 *
 * Note how `buildFrom` and `ifNotBlank` are automatically available via Kotlin context receivers. This interface is
 * modeled by [ProtoBuilderContext] and operates in addition to the context established by the builder itself. The
 * current model is made available as a parameter for easy reference.
 *
 * @see ProtoBuilderContext for the context surface available when building protocol buffer messages.
 */
@Suppress("unused") object Protobuf {
  /**
   * ## Proto Builder: Context
   *
   * Describes the API surface area made available as context during assembly of a Protocol Buffer object via this
   * interface. The context may be used as if the methods are present on `this`. Methods are provided which perform
   * presence / null-ness checks, and which do conversion of essential types to Protocol Buffer Well Known Types.
   *
   * Every [ProtoConvertible] inheritor supports this context automatically, via Kotlin extension methods. To engage the
   * context, call [Protobuf.buildFrom] from within [ProtoConvertible], and pass it the Kotlin generated Protocol
   * Buffer builder function.
   *
   * @see Protobuf for static protocol buffer utilities.
   */
  class ProtoBuilderContext private constructor () {
    /**
     * ### `ifPresent`
     *
     * If the provided [value] is not `null`, dispatch `op` to assign it to a given protocol message property or perform
     * some other task with the value.
     *
     * @see ifNotEmpty to check collections.
     * @see ifNotBlank to check strings.
     * @param C Raw type of the value.
     * @param value Value which can be `null`, and which will be checked for null-ness.
     * @param op Inline operation to dispatch if `value` is non-`null`.
     */
    inline fun <C: Any> ifPresent(value: C?, crossinline op: (value: C) -> Unit) {
      if (value != null) {
        op.invoke(value)
      }
    }

    /**
     * ### `ifPresent`
     *
     * If the provided [Optional] [value] is present, dispatch `op` to assign it to a given protocol message property or
     * perform some other task with the value.
     *
     * @see ifNotEmpty to check collections.
     * @see ifNotBlank to check strings.
     * @param C Inner type of the value.
     * @param value Optional value which can be empty, and which will be checked for presence.
     * @param op Inline operation to dispatch if [value] is present.
     */
    inline fun <C: Any> ifPresent(value: Optional<C>, crossinline op: (value: C) -> Unit) {
      if (value.isPresent) {
        op.invoke(value.get())
      }
    }

    /**
     * ### `ifNotEmpty`
     *
     * Check the provided [Collection] [value] of type [C] for emptiness; if the collection is non-empty, dispatch the
     * provided operation ([op]) to assign the value or otherwise use the value.
     *
     * @see ifPresent to for `null` (which all methods here do).
     * @see ifNotBlank to check strings.
     * @param C Type of value within the collection.
     * @param value Collection to check for emptiness.
     * @param op Inline operation to dispatch if [value] is non-empty.
     */
    inline fun <C: Collection<*>> ifNotEmpty(value: C?, crossinline op: (value: C) -> Unit) {
      if (!value.isNullOrEmpty()) {
        op.invoke(value)
      }
    }

    /**
     * ### `ifNotBlank`
     *
     * Check the provided [String] [value] for `null`-ness, emptiness, and blank-ness; if the [value] passes all checks,
     * dispatch the provided operation ([op]) to assign the value or otherwise use the value.
     *
     * @see ifPresent to for `null` (which all methods here do).
     * @see ifNotEmpty to check collection emptiness.
     * @param value String to check for validity.
     * @param op Inline operation to dispatch if [value] is non-blank, non-empty, and non-`null`.
     */
    inline fun ifNotBlank(value: String?, crossinline op: (value: String) -> Unit) {
      if (!value.isNullOrBlank()) {
        op.invoke(value)
      }
    }

    /**
     * ### `toMessage`
     *
     * Build a [Message] from the provided [ProtoConvertible] model [value] [M] (structured by [type]); if no message
     * can be created (because [value] is `null` or because it results in an empty message), then a default message
     * instance is acquired via the [ProtoSchemaConvertible] interface and returned.
     *
     * @param M Message type which results from the operation.
     * @param Model [ProtoConvertible] type which produces [Message] instances of [M].
     * @param type Type of [Model] to convert to a [Message].
     * @param value [Model] instance to convert to a message.
     */
    @Suppress("UNCHECKED_CAST")
    fun <M: Message, Model: ProtoConvertible<M>> toMessage(type: KClass<Model>, value: Model?): M {
      return value?.toMessage() ?: ((type.companionObjectInstance as? ProtoModel<M>)?.defaultMessageInstance() ?: error(
        "Model value is `null` and no default instance was resolvable"
      ))
    }

    /**
     * ### `timestamp`
     *
     * Build a Protocol Buffer WKT [Timestamp] record from the provided [Instant]; if the provided [Instant] is `null`,
     * return the default instance of [Timestamp].
     *
     * @param value [Instant] to convert to a [Timestamp].
     * @return Protocol buffer [Timestamp] instance.
     */
    fun timestamp(value: Instant?): Timestamp {
      return if (value != null) {
        Timestamp.newBuilder()
          .setSeconds(value.epochSeconds)
          .setNanos(value.nanosecondsOfSecond)
          .build()
      } else {
        Timestamp.getDefaultInstance()
      }
    }

    /**
     * ### `timestamp`
     *
     * Build a Protocol Buffer WKT [Timestamp] record from the provided [Instant]; if the provided [Instant] is `null`,
     * return the default instance of [Timestamp].
     *
     * Alternatively, if the value is present, [op] is dispatched with the value to perform an assignment.
     *
     * @param value [Instant] to convert to a [Timestamp].
     * @param op Operation to perform if the value is present.
     * @return Protocol buffer [Timestamp] instance.
     */
    inline fun timestamp(value: Instant?, crossinline op: (Timestamp) -> Unit) {
      if (value != null) {
        op.invoke(
          Timestamp.newBuilder()
            .setSeconds(value.epochSeconds)
            .setNanos(value.nanosecondsOfSecond)
            .build()
        )
      }
    }

    companion object {
      /** Static creator. */
      @JvmStatic fun create(): ProtoBuilderContext = ProtoBuilderContext()
    }
  }

  /**
   * ### `Instant.fromMessage`
   *
   * Convert a Protocol Buffer WKT [Timestamp] ([proto]) to a KotlinX [Instant]; if the provided [proto] is empty or
   * otherwise uninitialized, then `null` is returned.
   *
   * Use example:
   * ```kotlin
   * Instant.fromMessage(timestamp)
   * ```
   *
   * @param proto Protocol buffer [Timestamp] instance to convert to an [Instant].
   * @return [Instant] instance.
   */
  fun Instant.Companion.fromMessage(proto: Timestamp): Instant? {
    return if (proto.isInitialized && proto.seconds > 0) {
      fromEpochSeconds(
        proto.seconds,
        proto.nanos,
      )
    } else {
      null
    }
  }

  /**
   * ## Builder context: `buildFrom`
   *
   * Establish a [ProtoBuilderContext] instance based on the provided inputs (a model [instance] to build from, and a
   * code-generated Kotlin [builder]), and then run the provided operation ([op]) in order to build the instance; once
   * the instance [M] is built, return it.
   *
   * @param In Input type to build the [Message] instance from.
   * @param M Output [Message] type yielded by this builder operation.
   * @param B Builder type code-generated for [Message] type [M].
   * @param instance Model instance we are going to build from.
   * @param builder Code-generated builder function.
   * @param op Operation to execute to build the message.
   * @return Build message of type [M].
   */
  inline fun <In, M: Message, B: Message.Builder> buildFrom(
    instance: In,
    builder: (B.() -> Unit) -> M,
    crossinline op: context(ProtoBuilderContext) B.(In) -> Unit,
  ): M {
    return builder.invoke {
      op.invoke(
        ProtoBuilderContext.create(),
        this,
        instance,
      )
    }
  }

  /**
   * ## Builder context: `buildFrom`
   *
   * Establish a [ProtoBuilderContext] instance based on the provided inputs (a model [instance] to build from, and a
   * code-generated Kotlin [builder]), and then run the provided operation ([op]) in order to build the instance; once
   * the instance [M] is built, return it.
   *
   * @param In Input [ProtoSchemaConvertible] type to build the [Message] instance from.
   * @param M Output [Message] type yielded by this builder operation.
   * @param B Builder type code-generated for [Message] type [M].
   * @param instance Model instance we are going to build from.
   * @param builder Code-generated builder function.
   * @param op Operation to execute to build the message.
   * @return Build message of type [M].
   */
  inline fun <In: ProtoSchemaConvertible, M: Message, B: Message.Builder> buildFrom(
    instance: In,
    builder: (B.() -> Unit) -> M,
    crossinline op: context(ProtoBuilderContext) B.(In) -> Unit,
  ): M {
    return builder.invoke {
      op.invoke(
        ProtoBuilderContext.create(),
        this,
        instance,
      )
    }
  }

  /**
   * ## Builder context: `Model.buildFrom`
   *
   * Within the context of a [Model] type which extends [ProtoConvertible] and produces [Message] instances of type [M],
   * and a matching [builder] [B], establish a [ProtoBuilderContext] instance based on the provided inputs and then
   * dispatch the provided operation ([op]) in order to build the instance; once the instance [M] is built, return it.
   *
   * @param M Output [Message] type yielded by this builder operation.
   * @param Model Model type (implementing [ProtoConvertible]) which produces [Message] instances of type [M].
   * @param B Builder type code-generated for [Message] type [M]. Inferred from [builder].
   * @param builder Code-generated builder function which produces [Message] instances of type [M].
   * @param op Operation to dispatch to build the entity.
   * @return Built message of type [M].
   */
  inline fun <M: Message, Model: ProtoConvertible<M>, B> Model.buildFrom(
    builder: (B.() -> Unit) -> M,
    crossinline op: context(ProtoBuilderContext) B.(Model) -> Unit,
  ): M {
    return builder.invoke {
      op.invoke(
        ProtoBuilderContext.create(),
        this,
        this@buildFrom,
      )
    }
  }
}

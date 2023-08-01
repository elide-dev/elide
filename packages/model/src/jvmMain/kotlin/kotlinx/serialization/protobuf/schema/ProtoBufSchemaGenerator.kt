@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * Experimental generator of ProtoBuf schema that is compatible with [serializable][Serializable] Kotlin classes
 * and data encoded and decoded by [ProtoBuf] format.
 *
 * The schema is generated based on provided [SerialDescriptor] and is compatible with proto2 schema definition.
 * An arbitrary Kotlin class represent much wider object domain than the ProtoBuf specification, thus schema generator
 * has the following list of restrictions:
 *
 *  * Serial name of the class and all its fields should be a valid Proto [identifier](https://developers.google.com/protocol-buffers/docs/reference/proto2-spec)
 *  * Nullable values are allowed only for Kotlin [nullable][SerialDescriptor.isNullable] types, but not [optional][SerialDescriptor.isElementOptional]
 *    in order to properly distinguish "default" and "absent" values.
 *  * The name of the type without the package directive uniquely identifies the proto message type and two or more fields with the same serial name
 *    are considered to have the same type. Schema generator allows to specify a separate package directive for the pack of classes in order
 *    to mitigate this limitation.
 *  * Nested collections, e.g. `List<List<Int>>` are represented using the artificial wrapper message in order to distinguish
 *    repeated fields boundaries.
 *  * Default Kotlin values are not representable in proto schema. A special commentary is generated for properties with default values.
 *  * Empty nullable collections are not supported by the generated schema and will be prohibited in [ProtoBuf] as well
 *    due to their ambiguous nature.
 *
 * Temporary restrictions:
 *  * [Contextual] data is represented as as `bytes` type
 *  * [Polymorphic] data is represented as a artificial `KotlinxSerializationPolymorphic` message.
 *
 * Other types are mapped according to their specification: primitives as primitives, lists as 'repeated' fields and
 * maps as 'repeated' map entries.
 *
 * The name of messages and enums is extracted from [SerialDescriptor.serialName] in [SerialDescriptor] without the package directive,
 * as substring after the last dot character, the `'?'` character is also removed if it is present at the end of the string.
 */
@ExperimentalSerializationApi
public object ProtoBufSchemaGenerator {

    /**
     * Generate text of protocol buffers schema version 2 for the given [rootDescriptor].
     * The resulting schema will contain all types referred by [rootDescriptor].
     *
     * [packageName] define common protobuf package for all messages and enum in the schema, it may contain `'a'`..`'z'`
     * letters in upper and lower case, decimal digits, `'.'` or `'_'` chars, but must be started only by a letter and
     * not finished by a dot.
     *
     * [options] define values for protobuf options. Option value (map value) is an any string, option name (map key)
     * should be the same format as [packageName].
     *
     * The method throws [IllegalArgumentException] if any of the restrictions imposed by [ProtoBufSchemaGenerator] is violated.
     */
    @ExperimentalSerializationApi
    public fun generateSchemaText(
        rootDescriptor: SerialDescriptor,
        packageName: String? = null,
        options: Map<String, String> = emptyMap()
    ): String = generateSchemaText(listOf(rootDescriptor), packageName, options)

    /**
     * Generate text of protocol buffers schema version 2 for the given serializable [descriptors].
     * [packageName] define common protobuf package for all messages and enum in the schema, it may contain `'a'`..`'z'`
     * letters in upper and lower case, decimal digits, `'.'` or `'_'` chars, but started only from a letter and
     * not finished by dot.
     *
     * [options] define values for protobuf options. Option value (map value) is an any string, option name (map key)
     * should be the same format as [packageName].
     *
     * The method throws [IllegalArgumentException] if any of the restrictions imposed by [ProtoBufSchemaGenerator] is violated.
     */
    @ExperimentalSerializationApi
    public fun generateSchemaText(
        descriptors: List<SerialDescriptor>,
        packageName: String? = null,
        options: Map<String, String> = emptyMap()
    ): String = generateSchemaText(descriptors, ProtoBufGeneratorOptions.DEFAULTS.copy(
        packageName = packageName,
        packageOptions = options,
    ))

    /**
     * Generate text of protocol buffers schema for the given serializable [descriptors], and applying any provided
     * [options] (where the dialect version and other parameters may be specified).
     *
     * [descriptors] is the set of serializable descriptors that should be included in the returned schema.
     *
     * [options] define any explicit options to pass to the schema generator, including the dialect version of protocol
     * buffers and any options to apply to protobuf packages.
     *
     * The method throws [IllegalArgumentException] if any of the restrictions imposed by the implementation generator
     * are violated.
     */
    @ExperimentalSerializationApi
    public fun generateSchemaText(
      descriptors: List<SerialDescriptor>,
      options: ProtoBufGeneratorOptions = ProtoBufGeneratorOptions.DEFAULTS,
    ): String {
        return ProtoBufSchemaGeneratorImpl(options).generateSchemaText(
            descriptors,
            options,
        )
    }
}

@file:OptIn(
  InternalSerializationApi::class,
  ExperimentalSerializationApi::class,
)
@file:Suppress(
  "PropertyName",
  "PrivatePropertyName",
  "RedundantVisibilityModifier",
)

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.protobuf.internal.isPackable
import kotlinx.serialization.protobuf.schema.ProtoBufSyntaxVersion.*

/** Implementation of a protocol buffer syntax generator, from Kotlin models; supports proto syntax 2 and 3. */
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
internal open class ProtoBufSchemaGeneratorImpl (
  protected val options: ProtoBufGeneratorOptions,
) : ProtoBufSchemaBuilder {
  internal companion object {
    private const val PROTOBUF_WRAPPER_VALUE = "google.protobuf.Value"

    private val PROTO_WRAPPERS = setOf(
      PROTOBUF_WRAPPER_VALUE,
    )

    private val KOTLINX_TO_PROTO_TYPE_MAP = mapOf(
      "JsonPrimitive" to "google.protobuf.Value",
      "JsonElement" to "google.protobuf.Value",
      "JsonObject" to "google.protobuf.Struct",
      "JsonArray" to "google.protobuf.ListValue",
    )

    private val PROTO_BASE_IMPORT_MAP = mapOf(
      "google.protobuf.Any" to "google/protobuf/any.proto",
      "google.protobuf.Duration" to "google/protobuf/duration.proto",
      "google.protobuf.Timestamp" to "google/protobuf/timestamp.proto",
      "google.protobuf.Struct" to "google/protobuf/struct.proto",
      "google.protobuf.Value" to "google/protobuf/struct.proto",
    )
  }

  private val IDENTIFIER_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*")

  private val preamble = StringBuilder()
  private val content = StringBuilder()
  private val imports = HashSet<String>()

  private fun removeLineBreaks(text: String): String {
    return text.replace('\n', ' ').replace('\r', ' ')
  }

  protected open fun StringBuilder.writeCommonProtobufPreamble(options: ProtoBufGeneratorOptions) {
    appendLine("""syntax = "${options.syntaxVersion.symbol}";""").appendLine()
    options.packageName?.let { append("package ").append(it).appendLine(';') }

    for ((optionName, optionValue) in options.packageOptions.orEmpty()) {
      val safeOptionName = removeLineBreaks(optionName)
      val safeOptionValue = removeLineBreaks(optionValue)
      safeOptionName.checkIsValidFullIdentifier { "Invalid option name '$it'" }
      append("option ").append(safeOptionName).append(" = \"").append(safeOptionValue).appendLine("\";")
    }
  }

  protected open fun StringBuilder.writeImports() {
    imports.sorted().forEach {
      appendLine("import \"$it\";")
    }
  }

  private fun String.checkIsValidFullIdentifier(messageSupplier: (String) -> String) {
    if (split('.').any { !it.matches(IDENTIFIER_REGEX) }) {
      throw IllegalArgumentException(messageSupplier.invoke(this))
    }
  }

  private fun String.checkIsValidIdentifier(messageSupplier: () -> String) {
    if (!matches(IDENTIFIER_REGEX)) {
      throw IllegalArgumentException(messageSupplier.invoke())
    }
  }

  private fun checkDoubles(descriptors: List<SerialDescriptor>) {
    val rootTypesNames = mutableSetOf<String>()
    val duplicates = mutableListOf<String>()

    descriptors.map { it.messageOrEnumName }.forEach {
      if (!rootTypesNames.add(it)) {
        duplicates += it
      }
    }
    if (duplicates.isNotEmpty()) {
      throw IllegalArgumentException("Serial names of the following types are duplicated: $duplicates")
    }
  }

  private val SerialDescriptor.isOpenPolymorphic: Boolean
    get() = kind == PolymorphicKind.OPEN

  private val SerialDescriptor.isSealedPolymorphic: Boolean
    get() = kind == PolymorphicKind.SEALED

  private val SerialDescriptor.isProtobufNamedType: Boolean
    get() = isProtobufMessageOrEnum || isProtobufScalar

  private val SerialDescriptor.isProtobufScalar: Boolean
    get() = (kind is PrimitiveKind)
        || (kind is StructureKind.LIST && getElementDescriptor(0).kind === PrimitiveKind.BYTE)
        || kind == SerialKind.CONTEXTUAL

  private val SerialDescriptor.isProtobufMessageOrEnum: Boolean
    get() = isProtobufMessage || isProtobufEnum

  private val SerialDescriptor.isProtobufMessage: Boolean
    get() = kind == StructureKind.CLASS || kind == StructureKind.OBJECT || kind == PolymorphicKind.SEALED || kind == PolymorphicKind.OPEN

  private val SerialDescriptor.isProtobufCollection: Boolean
    get() = isProtobufRepeated || isProtobufMap

  private val SerialDescriptor.isProtobufRepeated: Boolean
    get() = (kind == StructureKind.LIST && getElementDescriptor(0).kind != PrimitiveKind.BYTE)
        || (kind == StructureKind.MAP && !getElementDescriptor(0).isValidMapKey)

  private val SerialDescriptor.isProtobufMap: Boolean
    get() = kind == StructureKind.MAP && getElementDescriptor(0).isValidMapKey

  private val SerialDescriptor.isProtobufEnum: Boolean
    get() = kind == SerialKind.ENUM

  private val SerialDescriptor.isValidMapKey: Boolean
    get() = kind == PrimitiveKind.INT || kind == PrimitiveKind.LONG || kind == PrimitiveKind.BOOLEAN || kind == PrimitiveKind.STRING

  private val SerialDescriptor.messageOrEnumName: String
    get() = (serialName.substringAfterLast('.', serialName)).removeSuffix("?")

  private fun SerialDescriptor.protobufTypeName(annotations: List<Annotation> = emptyList()): String {
    return if (KOTLINX_TO_PROTO_TYPE_MAP.contains(messageOrEnumName)) {
      // special case: should be a Proto `Value`
      KOTLINX_TO_PROTO_TYPE_MAP[messageOrEnumName]!!
    } else if (isProtobufScalar) {
      scalarTypeName(annotations)
    } else {
      messageOrEnumName
    }
  }

  private val SerialDescriptor.protobufEnumElementName: String
    get() = serialName.substringAfterLast('.', serialName)

  private fun SerialDescriptor.scalarTypeName(annotations: List<Annotation> = emptyList()): String {
    val integerType = annotations.filterIsInstance<ProtoType>().firstOrNull()?.type ?: ProtoIntegerType.DEFAULT

    if (kind == SerialKind.CONTEXTUAL) {
      return "bytes"
    }

    if (kind is StructureKind.LIST && getElementDescriptor(0).kind == PrimitiveKind.BYTE) {
      return "bytes"
    }

    return when (kind as PrimitiveKind) {
      PrimitiveKind.BOOLEAN -> "bool"
      PrimitiveKind.BYTE, PrimitiveKind.CHAR, PrimitiveKind.SHORT, PrimitiveKind.INT ->
        when (integerType) {
          ProtoIntegerType.DEFAULT -> "int32"
          ProtoIntegerType.SIGNED -> "sint32"
          ProtoIntegerType.FIXED -> "fixed32"
        }
      PrimitiveKind.LONG ->
        when (integerType) {
          ProtoIntegerType.DEFAULT -> "int64"
          ProtoIntegerType.SIGNED -> "sint64"
          ProtoIntegerType.FIXED -> "fixed64"
        }
      PrimitiveKind.FLOAT -> "float"
      PrimitiveKind.DOUBLE -> "double"
      PrimitiveKind.STRING -> "string"
    }
  }

  internal data class TypeDefinition(
    val descriptor: SerialDescriptor,
    val isSynthetic: Boolean = false,
    val ability: String? = null,
    val containingMessageName: String? = null,
    val fieldName: String? = null,
    val builtinType: String? = null,
  )

  private val SyntheticPolymorphicType = TypeDefinition(
    buildClassSerialDescriptor("KotlinxSerializationPolymorphic") {
      element("type", PrimitiveSerialDescriptor("typeDescriptor", PrimitiveKind.STRING))
      element("value", buildSerialDescriptor("valueDescriptor", StructureKind.LIST) {
        element("0", Byte.serializer().descriptor)
      })
    },
    true,
    "polymorphic types"
  )

  internal class NotNullSerialDescriptor(private val original: SerialDescriptor) : SerialDescriptor by original {
    override val isNullable = false
  }

  private val SerialDescriptor.notNull get() = NotNullSerialDescriptor(this)

  private fun StringBuilder.generateMessage(messageType: TypeDefinition): List<TypeDefinition> {
    val messageDescriptor = messageType.descriptor
    val messageName: String
    if (messageType.isSynthetic) {
      append("// This message was generated to support ").append(messageType.ability)
        .appendLine(" and does not present in Kotlin.")

      messageName = messageDescriptor.serialName
      if (messageType.containingMessageName != null) {
        append("// Containing message '").append(messageType.containingMessageName).append("', field '")
          .append(messageType.fieldName).appendLine('\'')
      }
    } else {
      messageName = messageDescriptor.messageOrEnumName
      messageName.checkIsValidIdentifier {
        "Invalid name for the message in protobuf schema '$messageName'. " +
            "Serial name of the class '${messageDescriptor.serialName}'"
      }
      val safeSerialName = removeLineBreaks(messageDescriptor.serialName)
      if (safeSerialName != messageName) {
        append("// serial name '").append(safeSerialName).appendLine('\'')
      }
    }

    append("message ").append(messageName).appendLine(" {")

    val usedNumbers: MutableSet<Int> = mutableSetOf()
    val nestedTypes = mutableListOf<TypeDefinition>()
    for (index in 0 until messageDescriptor.elementsCount) {
      val fieldName = messageDescriptor.getElementName(index)
      fieldName.checkIsValidIdentifier {
        "Invalid name of the field '$fieldName' in message '$messageName' for class with serial " +
            "name '${messageDescriptor.serialName}'"
      }

      val fieldDescriptor = messageDescriptor.getElementDescriptor(index)

      val isList = fieldDescriptor.isProtobufRepeated

      nestedTypes += when {
        fieldDescriptor.isProtobufNamedType -> generateNamedType(messageType, index)
        isList -> generateListType(messageType, index)
        fieldDescriptor.isProtobufMap -> generateMapType(messageType, index)
        else -> throw IllegalStateException(
          "Unprocessed message field type with serial name " +
              "'${fieldDescriptor.serialName}' and kind '${fieldDescriptor.kind}'"
        )
      }

      val annotations = messageDescriptor.getElementAnnotations(index)
      val number = annotations.filterIsInstance<ProtoNumber>().singleOrNull()?.number ?: (index + 1)
      if (!usedNumbers.add(number)) {
        throw IllegalArgumentException("Field number $number is repeated in the class with serial name ${messageDescriptor.serialName}")
      }

      append(' ').append(fieldName).append(" = ").append(number)

      val isPackRequested = annotations.filterIsInstance<ProtoPacked>().singleOrNull() != null
      val isDeprecated = annotations.filterIsInstance<ProtoDeprecated>().singleOrNull() != null
      val flags = listOf(
        "packed" to (isPackRequested && isList && fieldDescriptor.getElementDescriptor(0).isPackable),
        "deprecated" to isDeprecated,
      )
      val hasFlags = flags.any { it.second }

      if (hasFlags) {
        append(" [")
        flags.forEach {
          if (it.second) {
            append(it.first).append(", ")
          }
        }
        appendLine("];")
      } else {
        appendLine(";")
      }
    }
    appendLine('}')

    return nestedTypes
  }

  private fun StringBuilder.generateNamedType(messageType: TypeDefinition, index: Int): List<TypeDefinition> {
    val messageDescriptor = messageType.descriptor

    val fieldDescriptor = messageDescriptor.getElementDescriptor(index)
    val nestedTypes: List<TypeDefinition>
    val typeName: String = when {
      messageDescriptor.isSealedPolymorphic && index == 1 -> {
        appendLine("  // decoded as message with one of these types:")
        nestedTypes = fieldDescriptor.elementDescriptors.map { TypeDefinition(it) }.toList()
        nestedTypes.forEachIndexed { _, childType ->
          append("  //   message ").append(childType.descriptor.messageOrEnumName).append(", serial name '")
            .append(removeLineBreaks(childType.descriptor.serialName)).appendLine('\'')
        }
        fieldDescriptor.scalarTypeName()
      }
      fieldDescriptor.isProtobufScalar -> {
        nestedTypes = emptyList()
        fieldDescriptor.scalarTypeName(messageDescriptor.getElementAnnotations(index))
      }
      fieldDescriptor.isOpenPolymorphic -> {
        nestedTypes = listOf(SyntheticPolymorphicType)
        SyntheticPolymorphicType.descriptor.serialName
      }
      else -> {
        // enum or regular message
        nestedTypes = listOf(TypeDefinition(fieldDescriptor))
        fieldDescriptor.messageOrEnumName
      }
    }

    if (options.emitWarningComments && messageDescriptor.isElementOptional(index)) {
      appendLine("  // WARNING: a default value decoded when value is missing")
    }
    val optional = fieldDescriptor.isNullable || messageDescriptor.isElementOptional(index)

    append("  ").append(when (options.syntaxVersion) {
      // `proto2` allows both `optional` and `required` fields
      PROTO2 -> if (optional) "optional " else "required "

      // `proto3` allows marking fields as `optional` but has no support for `required` fields. `optional` fields in
      // proto3 behave differently with regard to default values.
      PROTO3 -> if (optional) "optional " else ""
    }).append(typeName)

    return nestedTypes
  }

  private fun StringBuilder.generateMapType(messageType: TypeDefinition, index: Int): List<TypeDefinition> {
    val messageDescriptor = messageType.descriptor
    val mapDescriptor = messageDescriptor.getElementDescriptor(index)
    val originalMapValueDescriptor = mapDescriptor.getElementDescriptor(1)
    val valueType = if (originalMapValueDescriptor.isProtobufCollection) {
      createNestedCollectionType(messageType, index, originalMapValueDescriptor, "nested collection in map value")
    } else {
      TypeDefinition(originalMapValueDescriptor)
    }
    val valueDescriptor = valueType.descriptor

    if (options.emitWarningComments && originalMapValueDescriptor.isNullable) {
      appendLine("  // WARNING: nullable map values can not be represented in protobuf")
    }
    generateCollectionAbsenceComment(messageDescriptor, mapDescriptor, index)

    val keyTypeName = mapDescriptor.getElementDescriptor(0).scalarTypeName(mapDescriptor.getElementAnnotations(0))
    val valueTypeName = valueDescriptor.protobufTypeName(mapDescriptor.getElementAnnotations(1))
    append("  map<").append(keyTypeName).append(", ").append(valueTypeName).append(">")

    return if (PROTO_WRAPPERS.contains(valueTypeName)) {
      listOf(
        TypeDefinition(
        originalMapValueDescriptor,
        builtinType = valueTypeName
      )
      )  // type is builtin, so does not need to be generated.
    } else if (valueDescriptor.isProtobufMessageOrEnum) {
      listOf(valueType)
    } else {
      emptyList()
    }
  }

  private fun StringBuilder.generateListType(messageType: TypeDefinition, index: Int): List<TypeDefinition> {
    val messageDescriptor = messageType.descriptor
    val collectionDescriptor = messageDescriptor.getElementDescriptor(index)
    val originalElementDescriptor = collectionDescriptor.getElementDescriptor(0)
    val elementType = if (collectionDescriptor.kind == StructureKind.LIST) {
      if (originalElementDescriptor.isProtobufCollection) {
        createNestedCollectionType(messageType, index, originalElementDescriptor, "nested collection in list")
      } else {
        TypeDefinition(originalElementDescriptor)
      }
    } else {
      createLegacyMapType(messageType, index, "legacy map")
    }

    val elementDescriptor = elementType.descriptor

    if (options.emitWarningComments && elementDescriptor.isNullable) {
      appendLine("  // WARNING: nullable elements of collections can not be represented in protobuf")
    }
    generateCollectionAbsenceComment(messageDescriptor, collectionDescriptor, index)

    val typeName = elementDescriptor.protobufTypeName(messageDescriptor.getElementAnnotations(index))
    append("  repeated ").append(typeName)

    return if (elementDescriptor.isProtobufMessageOrEnum) {
      listOf(elementType)
    } else {
      emptyList()
    }
  }

  private fun camelCaseToUndercaseAllCaps(vararg symbols: String): String {
    val sb = StringBuilder()
    var lastChar: Char? = null
    var lastCharLower = false
    val appendChar = { char: Char ->
      lastChar = char
      lastCharLower = char.isLowerCase()
      sb.append(char.uppercaseChar())
    }
    symbols.forEachIndexed { groupIndex, value ->
      value.forEachIndexed { index, char ->
        if (index != 0 && index != value.lastIndex && char.isUpperCase()) {
          if (lastChar != '_' && lastCharLower)
            appendChar('_')
        }
        appendChar(char)
      }
      if (groupIndex != symbols.lastIndex) {
        sb.append("_")
      }
    }
    return sb.toString()
  }

  @Suppress("USELESS_IS_CHECK", "SENSELESS_COMPARISON")
  private fun StringBuilder.generateEnum(enumType: TypeDefinition) {
    val enumDescriptor = enumType.descriptor
    val enumName = enumDescriptor.messageOrEnumName
    enumName.checkIsValidIdentifier {
      "Invalid name for the enum in protobuf schema '$enumName'. Serial name of the enum " +
          "class '${enumDescriptor.serialName}'"
    }
    val safeSerialName = removeLineBreaks(enumDescriptor.serialName)
    if (safeSerialName != enumName) {
      append("// serial name '").append(safeSerialName).appendLine('\'')
    }

    append("enum ").append(enumName).appendLine(" {")

    val usedNumbers: MutableSet<Int> = mutableSetOf()
    val duplicatedNumbers: MutableSet<Int> = mutableSetOf()
    var defaultSearchIndex = -1
    val explicitEnumDefault = enumDescriptor.elementDescriptors.find {
      defaultSearchIndex += 1
      val annotations = enumDescriptor.getElementAnnotations(defaultSearchIndex)
      annotations.filterIsInstance<ProtoUnknown>().singleOrNull() != null
    }
    val enumSettings = enumDescriptor.annotations.filterIsInstance<ProtoEnum>().singleOrNull()

    val (indexOffset, unknownName) = when (options.syntaxVersion) {
      PROTO2 -> 0 to null  // no index offset / unknown-control
      PROTO3 -> {
        val explicitEnumName = explicitEnumDefault?.protobufEnumElementName
        val unknownEnumName: String = explicitEnumName
          ?: enumSettings?.unknownName?.ifBlank { null }
          ?: camelCaseToUndercaseAllCaps(enumName, "UNKNOWN")

        unknownEnumName.checkIsValidIdentifier {
          "The enum unknown-element name '$unknownEnumName' is invalid in the " +
              "protobuf schema. Serial name of the enum class '${enumDescriptor.serialName}'"
        }
        usedNumbers.add(0)
        append("  ").append(unknownEnumName).append(" = ").append(0).appendLine(';')
        1 to unknownEnumName  // offset indexes by 1 to account for the unknown-element
      }
    }

    enumDescriptor.elementDescriptors.forEachIndexed { index, element ->
      val elementName = element.protobufEnumElementName
      if (elementName == unknownName)
        return@forEachIndexed  // skip unknown entry, it is lifted to the first entry

      elementName.checkIsValidIdentifier {
        "The enum element name '$elementName' is invalid in the " +
            "protobuf schema. Serial name of the enum class '${enumDescriptor.serialName}'"
      }

      val annotations = enumDescriptor.getElementAnnotations(index)
      val number = annotations.filterIsInstance<ProtoNumber>().singleOrNull()?.number ?: (index + indexOffset)
      if (!usedNumbers.add(number)) {
        duplicatedNumbers.add(number)
      }
      val deprecated = annotations.filterIsInstance<ProtoDeprecated>().singleOrNull()
      val flags = listOf(
        "deprecated" to (deprecated != null),
      )
      val flagsPresent = flags.any {
        it.second != null && (if (it.second is Boolean) { it.second } else { true })
      }

      append("  ").append(elementName).append(" = ").append(number)

      if (flagsPresent) {
        append(" [")
        flags.forEachIndexed { flagIndex, pair ->
          val (flag, value) = pair
          append("$flag=${value}")
          if (flagIndex != flags.lastIndex) {
            append(", ")
          }
        }
        appendLine("];")
      } else {
        appendLine(';')
      }
    }
    if (duplicatedNumbers.isNotEmpty()) {
      throw IllegalArgumentException(
        "The class with serial name ${enumDescriptor.serialName} has duplicate " +
            "elements with numbers $duplicatedNumbers"
      )
    }

    appendLine('}')
  }

  private fun StringBuilder.generateCollectionAbsenceComment(
    messageDescriptor: SerialDescriptor,
    collectionDescriptor: SerialDescriptor,
    index: Int
  ) {
    if (!options.emitWarningComments)
      return
    if (!collectionDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
      appendLine("  // WARNING: a default value decoded when value is missing")
    } else if (collectionDescriptor.isNullable && !messageDescriptor.isElementOptional(index)) {
      appendLine("  // WARNING: an empty collection decoded when a value is missing")
    } else if (collectionDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
      appendLine("  // WARNING: a default value decoded when value is missing")
    }
  }

  private fun createLegacyMapType(
    messageType: TypeDefinition,
    index: Int,
    description: String,
  ): TypeDefinition {
    val messageDescriptor = messageType.descriptor
    val fieldDescriptor = messageDescriptor.getElementDescriptor(index)
    val fieldName = messageDescriptor.getElementName(index)
    val messageName = messageDescriptor.messageOrEnumName

    val wrapperName = "${messageName}_${fieldName}"
    val wrapperDescriptor = buildClassSerialDescriptor(wrapperName) {
      element("key", fieldDescriptor.getElementDescriptor(0).notNull)
      element("value", fieldDescriptor.getElementDescriptor(1).notNull)
    }

    return TypeDefinition(
      wrapperDescriptor,
      true,
      description,
      messageType.containingMessageName ?: messageName,
      messageType.fieldName ?: fieldName
    )
  }

  private fun createNestedCollectionType(
    messageType: TypeDefinition,
    index: Int,
    elementDescriptor: SerialDescriptor,
    description: String
  ): TypeDefinition {
    val messageDescriptor = messageType.descriptor
    val fieldName = messageDescriptor.getElementName(index)
    val messageName = messageDescriptor.messageOrEnumName

    val wrapperName = "${messageName}_${fieldName}"
    val wrapperDescriptor = buildClassSerialDescriptor(wrapperName) {
      element("value", elementDescriptor.notNull)
    }

    return TypeDefinition(
      wrapperDescriptor,
      true,
      description,
      messageType.containingMessageName ?: messageName,
      messageType.fieldName ?: fieldName
    )
  }

  private fun StringBuilder.generateProtoSchemaText(
    descriptors: List<SerialDescriptor>,
    options: ProtoBufGeneratorOptions,
  ) {
    // phase 1: fill preamble
    preamble.writeCommonProtobufPreamble(options)

    // phase 2: generate messages & enums
    val generatedTypes = mutableSetOf<String>()
    val queue = ArrayDeque<TypeDefinition>()
    descriptors.map { TypeDefinition(it) }.forEach { queue.add(it) }

    while (queue.isNotEmpty()) {
      val type = queue.removeFirst()
      val descriptor = type.descriptor
      val name = descriptor.messageOrEnumName
      if (!generatedTypes.add(name)) {
        continue
      }

      val builtinType = type.builtinType
      if (builtinType != null) {
        // map builtin type to injected import
        imports.add(
          PROTO_BASE_IMPORT_MAP[builtinType] ?: error("Unknown/unmapped builtin type: '$builtinType'")
        )
        continue
      }

      content.appendLine()
      when {
        descriptor.isProtobufMessage -> queue.addAll(content.generateMessage(type))
        descriptor.isProtobufEnum -> content.generateEnum(type)
        else -> throw IllegalStateException(
          "Unrecognized custom type with serial name "
              + "'${descriptor.serialName}' and kind '${descriptor.kind}'"
        )
      }
    }

    // phase 3: assemble final content
    append(preamble)
    writeImports()
    append(content)
  }

  override fun generateSchemaText(descriptors: List<SerialDescriptor>, options: ProtoBufGeneratorOptions): String  {
    val packageName = options.packageName
    packageName?.let { p -> p.checkIsValidFullIdentifier { "Incorrect protobuf package name '$it'" } }
    checkDoubles(descriptors)

    return StringBuilder().let {
      it.generateProtoSchemaText(
        descriptors,
        options,
      )
      it.toString()
    }
  }
}

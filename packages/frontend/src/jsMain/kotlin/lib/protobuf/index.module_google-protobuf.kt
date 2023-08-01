@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
  "DEPRECATION",
  "ClassName",
  "PropertyName"
)

package lib.protobuf

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array

public typealias MessageArray = Array<Any>

public typealias StaticToObject = (includeInstance: Boolean, msg: Message) -> Any

public typealias FieldValueArray = Array<dynamic /* String? | Number? | Boolean? | Uint8Array? | lib.protobuf.FieldValueArray? */>

public external interface `T$0` {
    @nativeGetter
    public operator fun get(key: Number): ExtensionFieldInfo<Message>?
    @nativeSetter
    public operator fun set(key: Number, value: ExtensionFieldInfo<Message>)
}

public external interface `T$1` {
    @nativeGetter
    public operator fun get(key: Number): ExtensionFieldBinaryInfo<Message>?
    @nativeSetter
    public operator fun set(key: Number, value: ExtensionFieldBinaryInfo<Message>)
}

public open external class Message {
  public open fun getJsPbMessageId(): String?
  public open fun <T> serializeBinaryExtensions(proto: Message, writer: BinaryWriter, extensions: `T$1`, getExtensionFn: (fieldInfo: ExtensionFieldInfo<T>) -> T)
  public open fun <T> readBinaryExtension(proto: Message, reader: BinaryReader, extensions: `T$1`, setExtensionFn: (fieldInfo: ExtensionFieldInfo<T>, param_val: T) -> Unit)
  public open fun toArray(): MessageArray
  public override fun toString(): String
  public open fun <T> getExtension(fieldInfo: ExtensionFieldInfo<T>): T
  public open fun <T> setExtension(fieldInfo: ExtensionFieldInfo<T>, value: T)
  public open fun cloneMessage(): Message /* this */
  public open fun clone(): Message /* this */
  public open fun serializeBinary(): Uint8Array
  public open fun toObject(includeInstance: Boolean = definedExternally): Any

  public companion object {
    public fun initialize(msg: Message, data: MessageArray, messageId: String, suggestedPivot: Number, repeatedFields: Array<Number>? = definedExternally, oneofFields: Array<Array<Number>>? = definedExternally)
    public fun initialize(msg: Message, data: MessageArray, messageId: Number, suggestedPivot: Number, repeatedFields: Array<Number>? = definedExternally, oneofFields: Array<Array<Number>>? = definedExternally)
    public fun <T : Message> toObjectList(field: Array<T>, toObjectFn: (includeInstance: Boolean, data: T) -> Any, includeInstance: Boolean = definedExternally): Array<Any>
    public fun toObjectExtension(msg: Message, obj: Any, extensions: `T$0`, getExtensionFn: (fieldInfo: ExtensionFieldInfo<Message>) -> Message, includeInstance: Boolean = definedExternally)
    public fun getField(msg: Message, fieldNumber: Number): dynamic /* String? | Number? | Boolean? | Uint8Array? | lib.protobuf.FieldValueArray? */
    public fun getOptionalFloatingPointField(msg: Message, fieldNumber: Number): Number?
    public fun getRepeatedFloatingPointField(msg: Message, fieldNumber: Number): Array<Number>
    public fun bytesAsB64(bytes: Uint8Array): String
    public fun bytesAsU8(str: String): Uint8Array
    public fun bytesListAsB64(bytesList: Array<Uint8Array>): Array<String>
    public fun bytesListAsU8(strList: Array<String>): Array<Uint8Array>
    public fun <T> getFieldWithDefault(msg: Message, fieldNumber: Number, defaultValue: T): T
    public fun getMapField(msg: Message, fieldNumber: Number, noLazyCreate: Boolean, valueCtor: Any = definedExternally): Map<Any, Any>
    public fun setField(msg: Message, fieldNumber: Number, value: String?)
    public fun setField(msg: Message, fieldNumber: Number, value: Number?)
    public fun setField(msg: Message, fieldNumber: Number, value: Boolean?)
    public fun setField(msg: Message, fieldNumber: Number, value: Uint8Array?)
    public fun setField(msg: Message, fieldNumber: Number, value: FieldValueArray?)
    public fun addToRepeatedField(msg: Message, fieldNumber: Number, value: Any, index: Number = definedExternally)
    public fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: String?)
    public fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Number?)
    public fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Boolean?)
    public fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Uint8Array?)
    public fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: FieldValueArray?)
    public fun computeOneofCase(msg: Message, oneof: Array<Number>): Number
    public fun <T : Message> getWrapperField(msg: Message, ctor: Any, fieldNumber: Number, required: Number = definedExternally): T
    public fun <T : Message> getRepeatedWrapperField(msg: Message, ctor: Any, fieldNumber: Number): Array<T>
    public fun <T : Message> setWrapperField(msg: Message, fieldNumber: Number, value: T = definedExternally)
    public fun <T : Message> setWrapperField(msg: Message, fieldNumber: Number, value: Map<Any, Any> = definedExternally)
    public fun setOneofWrapperField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Any)
    public fun <T : Message> setRepeatedWrapperField(msg: Message, fieldNumber: Number, value: Array<T> = definedExternally)
    public fun <T : Message> addToRepeatedWrapperField(msg: Message, fieldNumber: Number, value: T?, ctor: Any, index: Number = definedExternally): T
    public fun toMap(field: Array<Any>, mapKeyGetterFn: (field: Any) -> String, toObjectFn: StaticToObject = definedExternally, includeInstance: Boolean = definedExternally)
    public fun <T : Message> difference(m1: T, m2: T): T
    public fun equals(m1: Message, m2: Message): Boolean
    public fun compareExtensions(extension1: Any, extension2: Any): Boolean
    public fun compareFields(field1: Any, field2: Any): Boolean
    public fun <T : Message> clone(msg: T): T
    public fun <T : Message> cloneMessage(msg: T): T
    public fun copyInto(fromMessage: Message, toMessage: Message)
    public fun registerMessageType(id: Number, constructor: Any)
    public fun deserializeBinary(bytes: Uint8Array): Message
    public fun deserializeBinaryFromReader(message: Message, reader: BinaryReader): Message
    public fun serializeBinaryToWriter(message: Message, writer: BinaryWriter)
    public fun toObject(includeInstance: Boolean, msg: Message): Any
    public var extensions: `T$0`
    public var extensionsBinary: `T$1`
  }
}

public external interface `T$2` {
    @nativeGetter
    public operator fun get(key: String): Number?
    @nativeSetter
    public operator fun set(key: String, value: Number)
}

public open external class ExtensionFieldInfo<T>(fieldIndex: Number, fieldName: `T$2`, ctor: Any, toObjectFn: StaticToObject, isRepeated: Number) {
  public open var fieldIndex: Number
  public open var fieldName: Number
  public open var ctor: Any
  public open var toObjectFn: StaticToObject
  public open var isRepeated: Number
  public open fun isMessageType(): Boolean
}

public open external class ExtensionFieldBinaryInfo<T>(fieldInfo: ExtensionFieldInfo<T>, binaryReaderFn: BinaryRead, binaryWriterFn: BinaryWrite, opt_binaryMessageSerializeFn: (msg: Message, writer: BinaryWriter) -> Unit, opt_binaryMessageDeserializeFn: (msg: Message, reader: BinaryReader) -> Message, opt_isPacked: Boolean) {
  public open var fieldInfo: ExtensionFieldInfo<T>
  public open var binaryReaderFn: BinaryRead
  public open var binaryWriterFn: BinaryWrite
  public open var opt_binaryMessageSerializeFn: (msg: Message, writer: BinaryWriter) -> Unit
  public open var opt_binaryMessageDeserializeFn: (msg: Message, reader: BinaryReader) -> Message
  public open var opt_isPacked: Boolean
}

public open external class Map<K, V>(arr: Array<Any /* JsTuple<K, V> */>, valueCtor: Any = definedExternally) {
  public open fun toArray(): Array<dynamic /* JsTuple<K, V> */>
  public open fun toObject(includeInstance: Boolean = definedExternally): Array<dynamic /* JsTuple<K, V> */>
  public open fun toObject(): Array<dynamic /* JsTuple<K, V> */>
  public open fun <VO> toObject(includeInstance: Boolean, valueToObject: (includeInstance: Boolean, valueWrapper: V) -> VO): Array<dynamic /* JsTuple<K, VO> */>
  public open fun getLength(): Number
  public open fun clear()
  public open fun del(key: K): Boolean
  public open fun getEntryList(): Array<dynamic /* JsTuple<K, V> */>
  public open fun entries(): Iterator<dynamic /* JsTuple<K, V> */>
  public open fun keys(): Iterator<K>
  public open fun values(): Iterator<V>
  public open fun forEach(callback: (entry: V, key: K) -> Unit, thisArg: Any = definedExternally)
  public open fun set(key: K, value: V): Map<K, V> /* this */
  public open fun get(key: K): V?
  public open fun has(key: K): Boolean
  public open fun serializeBinary(fieldNumber: Number, writer: BinaryWriter, keyWriterFn: (field: Number, key: K) -> Unit, valueWriterFn: (field: Number, value: V, writerCallback: BinaryWriteCallback) -> Unit, writeCallback: BinaryWriteCallback = definedExternally)
  public interface Iterator<T> {
    public fun next(): IteratorResult<T>
  }
  public interface IteratorResult<T> {
    public var done: Boolean
    public var value: T
  }

  public companion object {
    public fun <TK, TV> fromObject(entries: Array<Any /* JsTuple<TK, TV> */>, valueCtor: Any, valueFromObject: Any): Map<TK, TV>
    public fun <K, V> deserializeBinary(map: Map<K, V>, reader: BinaryReader, keyReaderFn: (reader: BinaryReader) -> K, valueReaderFn: (reader: BinaryReader, value: Any, readerCallback: BinaryReadCallback) -> V, readCallback: BinaryReadCallback = definedExternally, defaultKey: K = definedExternally, defaultValue: V = definedExternally)
  }
}

public typealias BinaryReadReader = (msg: Any, binaryReader: BinaryReader) -> Unit

public typealias BinaryRead = (msg: Any, reader: BinaryReadReader) -> Any

public typealias BinaryReadCallback = (value: Any, binaryReader: BinaryReader) -> Unit

public typealias BinaryWriteCallback = (value: Any, binaryWriter: BinaryWriter) -> Unit

public typealias BinaryWrite = (fieldNumber: Number, value: Any, writerCallback: BinaryWriteCallback) -> Unit

public open external class BinaryReader {
  public constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor()
  public constructor(bytes: ArrayBuffer = definedExternally)
  public constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally)
  public constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor(bytes: Uint8Array = definedExternally)
  public constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally)
  public constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor(bytes: Array<Number> = definedExternally)
  public constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally)
  public constructor(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor(bytes: String = definedExternally)
  public constructor(bytes: String = definedExternally, start: Number = definedExternally)
  public open fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
  public open fun alloc(): BinaryReader
  public open fun alloc(bytes: ArrayBuffer = definedExternally): BinaryReader
  public open fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally): BinaryReader
  public open fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
  public open fun alloc(bytes: Uint8Array = definedExternally): BinaryReader
  public open fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally): BinaryReader
  public open fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
  public open fun alloc(bytes: Array<Number> = definedExternally): BinaryReader
  public open fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally): BinaryReader
  public open fun alloc(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
  public open fun alloc(bytes: String = definedExternally): BinaryReader
  public open fun alloc(bytes: String = definedExternally, start: Number = definedExternally): BinaryReader
  public open fun free()
  public open fun getFieldCursor(): Number
  public open fun getCursor(): Number
  public open fun getBuffer(): Uint8Array
  public open fun getFieldNumber(): Number
  public open fun getWireType(): WireType
  public open fun isDelimited(): Boolean
  public open fun isEndGroup(): Boolean
  public open fun getError(): Boolean
  public open fun setBlock(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock()
  public open fun setBlock(bytes: ArrayBuffer = definedExternally)
  public open fun setBlock(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally)
  public open fun setBlock(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(bytes: Uint8Array = definedExternally)
  public open fun setBlock(bytes: Uint8Array = definedExternally, start: Number = definedExternally)
  public open fun setBlock(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(bytes: Array<Number> = definedExternally)
  public open fun setBlock(bytes: Array<Number> = definedExternally, start: Number = definedExternally)
  public open fun setBlock(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(bytes: String = definedExternally)
  public open fun setBlock(bytes: String = definedExternally, start: Number = definedExternally)
  public open fun reset()
  public open fun advance(count: Number)
  public open fun nextField(): Boolean
  public open fun unskipHeader()
  public open fun skipMatchingFields()
  public open fun skipVarintField()
  public open fun skipDelimitedField()
  public open fun skipFixed32Field()
  public open fun skipFixed64Field()
  public open fun skipGroup()
  public open fun skipField()
  public open fun registerReadCallback(callbackName: String, callback: (binaryReader: BinaryReader) -> Any)
  public open fun runReadCallback(callbackName: String): Any
  public open fun readAny(fieldType: FieldType): dynamic /* Boolean | Number | String | Array<dynamic /* Boolean | Number | String */> | Array<Uint8Array> | Uint8Array */
  public open var readMessage: BinaryRead
  public open fun readGroup(field: Number, message: Message, reader: BinaryReadReader)
  public open fun getFieldDecoder(): BinaryDecoder
  public open fun readInt32(): Number
  public open fun readInt32String(): String
  public open fun readInt64(): Number
  public open fun readInt64String(): String
  public open fun readUint32(): Number
  public open fun readUint32String(): String
  public open fun readUint64(): Number
  public open fun readUint64String(): String
  public open fun readSint32(): Number
  public open fun readSint64(): Number
  public open fun readSint64String(): String
  public open fun readFixed32(): Number
  public open fun readFixed64(): Number
  public open fun readFixed64String(): String
  public open fun readSfixed32(): Number
  public open fun readSfixed32String(): String
  public open fun readSfixed64(): Number
  public open fun readSfixed64String(): String
  public open fun readFloat(): Number
  public open fun readDouble(): Number
  public open fun readBool(): Boolean
  public open fun readEnum(): Number
  public open fun readString(): String
  public open fun readBytes(): Uint8Array
  public open fun readVarintHash64(): String
  public open fun readFixedHash64(): String
  public open fun readPackedInt32(): Array<Number>
  public open fun readPackedInt32String(): Array<String>
  public open fun readPackedInt64(): Array<Number>
  public open fun readPackedInt64String(): Array<String>
  public open fun readPackedUint32(): Array<Number>
  public open fun readPackedUint32String(): Array<String>
  public open fun readPackedUint64(): Array<Number>
  public open fun readPackedUint64String(): Array<String>
  public open fun readPackedSint32(): Array<Number>
  public open fun readPackedSint64(): Array<Number>
  public open fun readPackedSint64String(): Array<String>
  public open fun readPackedFixed32(): Array<Number>
  public open fun readPackedFixed64(): Array<Number>
  public open fun readPackedFixed64String(): Array<String>
  public open fun readPackedSfixed32(): Array<Number>
  public open fun readPackedSfixed64(): Array<Number>
  public open fun readPackedSfixed64String(): Array<String>
  public open fun readPackedFloat(): Array<Number>
  public open fun readPackedDouble(): Array<Number>
  public open fun readPackedBool(): Array<Boolean>
  public open fun readPackedEnum(): Array<Number>
  public open fun readPackedVarintHash64(): Array<String>
  public open fun readPackedFixedHash64(): Array<String>

  public companion object {
    public fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    public fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    public fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    public fun alloc(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
  }
}

public open external class BinaryWriter {
  public open fun writeSerializedMessage(bytes: Uint8Array, start: Number, end: Number)
  public open fun maybeWriteSerializedMessage(bytes: Uint8Array = definedExternally, start: Number = definedExternally, end: Number = definedExternally)
  public open fun reset()
  public open fun getResultBuffer(): Uint8Array
  public open fun getResultBase64String(): String
  public open fun beginSubMessage(field: Number)
  public open fun endSubMessage(field: Number)
  public open fun writeAny(fieldType: FieldType, field: Number, value: Boolean)
  public open fun writeAny(fieldType: FieldType, field: Number, value: Number)
  public open fun writeAny(fieldType: FieldType, field: Number, value: String)
  public open fun writeAny(fieldType: FieldType, field: Number, value: Array<Any /* Boolean | Number | String */>)
  public open fun writeAny(fieldType: FieldType, field: Number, value: Array<Uint8Array>)
  public open fun writeAny(fieldType: FieldType, field: Number, value: Uint8Array)
  public open fun writeInt32(field: Number, value: Number = definedExternally)
  public open fun writeInt32String(field: Number, value: String = definedExternally)
  public open fun writeInt64(field: Number, value: Number = definedExternally)
  public open fun writeInt64String(field: Number, value: String = definedExternally)
  public open fun writeUint32(field: Number, value: Number = definedExternally)
  public open fun writeUint32String(field: Number, value: String = definedExternally)
  public open fun writeUint64(field: Number, value: Number = definedExternally)
  public open fun writeUint64String(field: Number, value: String = definedExternally)
  public open fun writeSint32(field: Number, value: Number = definedExternally)
  public open fun writeSint64(field: Number, value: Number = definedExternally)
  public open fun writeSint64String(field: Number, value: String = definedExternally)
  public open fun writeFixed32(field: Number, value: Number = definedExternally)
  public open fun writeFixed64(field: Number, value: Number = definedExternally)
  public open fun writeFixed64String(field: Number, value: String = definedExternally)
  public open fun writeSfixed32(field: Number, value: Number = definedExternally)
  public open fun writeSfixed64(field: Number, value: Number = definedExternally)
  public open fun writeSfixed64String(field: Number, value: String = definedExternally)
  public open fun writeFloat(field: Number, value: Number = definedExternally)
  public open fun writeDouble(field: Number, value: Number = definedExternally)
  public open fun writeBool(field: Number, value: Boolean = definedExternally)
  public open fun writeEnum(field: Number, value: Number = definedExternally)
  public open fun writeString(field: Number, value: String = definedExternally)
  public open fun writeBytes(field: Number, value: ArrayBuffer = definedExternally)
  public open fun writeBytes(field: Number)
  public open fun writeBytes(field: Number, value: Uint8Array = definedExternally)
  public open fun writeBytes(field: Number, value: Array<Number> = definedExternally)
  public open fun writeBytes(field: Number, value: String = definedExternally)
  public open var writeMessage: BinaryWrite
  public open fun writeGroup(field: Number, value: Any, writeCallback: BinaryWriteCallback)
  public open fun writeFixedHash64(field: Number, value: String = definedExternally)
  public open fun writeVarintHash64(field: Number, value: String = definedExternally)
  public open fun writeRepeatedInt32(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedInt32String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedInt64(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedInt64String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedUint32(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedUint32String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedUint64(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedUint64String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedSint32(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedSint64(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedSint64String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedFixed32(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedFixed64(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedFixed64String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedSfixed32(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedSfixed64(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedSfixed64String(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedFloat(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedDouble(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedBool(field: Number, value: Array<Boolean> = definedExternally)
  public open fun writeRepeatedEnum(field: Number, value: Array<Number> = definedExternally)
  public open fun writeRepeatedString(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedBytes(field: Number, value: Array<Any /* ArrayBuffer | Uint8Array | Array<Number> | String */> = definedExternally)
  public open fun writeRepeatedMessage(field: Number, value: Array<Message>, writerCallback: BinaryWriteCallback)
  public open fun writeRepeatedGroup(field: Number, value: Array<Message>, writerCallback: BinaryWriteCallback)
  public open fun writeRepeatedFixedHash64(field: Number, value: Array<String> = definedExternally)
  public open fun writeRepeatedVarintHash64(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedInt32(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedInt32String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedInt64(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedInt64String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedUint32(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedUint32String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedUint64(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedUint64String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedSint32(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedSint64(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedSint64String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedFixed32(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedFixed64(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedFixed64String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedSfixed32(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedSfixed64(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedSfixed64String(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedFloat(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedDouble(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedBool(field: Number, value: Array<Boolean> = definedExternally)
  public open fun writePackedEnum(field: Number, value: Array<Number> = definedExternally)
  public open fun writePackedFixedHash64(field: Number, value: Array<String> = definedExternally)
  public open fun writePackedVarintHash64(field: Number, value: Array<String> = definedExternally)
}

public open external class BinaryEncoder {
  public open fun length(): Number
  public open fun end(): Array<Number>
  public open fun writeSplitVarint64(lowBits: Number, highBits: Number)
  public open fun writeSplitFixed64(lowBits: Number, highBits: Number)
  public open fun writeUnsignedVarint32(value: Number)
  public open fun writeSignedVarint32(value: Number)
  public open fun writeUnsignedVarint64(value: Number)
  public open fun writeSignedVarint64(value: Number)
  public open fun writeZigzagVarint32(value: Number)
  public open fun writeZigzagVarint64(value: Number)
  public open fun writeZigzagVarint64String(value: String)
  public open fun writeUint8(value: Number)
  public open fun writeUint16(value: Number)
  public open fun writeUint32(value: Number)
  public open fun writeUint64(value: Number)
  public open fun writeInt8(value: Number)
  public open fun writeInt16(value: Number)
  public open fun writeInt32(value: Number)
  public open fun writeInt64(value: Number)
  public open fun writeInt64String(value: String)
  public open fun writeFloat(value: Number)
  public open fun writeDouble(value: Number)
  public open fun writeBool(value: Boolean)
  public open fun writeEnum(value: Number)
  public open fun writeBytes(bytes: Uint8Array)
  public open fun writeVarintHash64(hash: String)
  public open fun writeFixedHash64(hash: String)
  public open fun writeString(value: String): Number
}

public open external class BinaryDecoder {
  public constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor()
  public constructor(bytes: ArrayBuffer = definedExternally)
  public constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally)
  public constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor(bytes: Uint8Array = definedExternally)
  public constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally)
  public constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor(bytes: Array<Number> = definedExternally)
  public constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally)
  public constructor(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
  public constructor(bytes: String = definedExternally)
  public constructor(bytes: String = definedExternally, start: Number = definedExternally)
  public open fun free()
  public open fun clone(): BinaryDecoder
  public open fun clear()
  public open fun getBuffer(): Uint8Array
  public open fun setBlock(data: ArrayBuffer, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(data: ArrayBuffer)
  public open fun setBlock(data: ArrayBuffer, start: Number = definedExternally)
  public open fun setBlock(data: Uint8Array, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(data: Uint8Array)
  public open fun setBlock(data: Uint8Array, start: Number = definedExternally)
  public open fun setBlock(data: Array<Number>, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(data: Array<Number>)
  public open fun setBlock(data: Array<Number>, start: Number = definedExternally)
  public open fun setBlock(data: String, start: Number = definedExternally, length: Number = definedExternally)
  public open fun setBlock(data: String)
  public open fun setBlock(data: String, start: Number = definedExternally)
  public open fun getEnd(): Number
  public open fun setEnd(end: Number)
  public open fun reset()
  public open fun getCursor(): Number
  public open fun setCursor(cursor: Number)
  public open fun advance(count: Number)
  public open fun atEnd(): Boolean
  public open fun pastEnd(): Boolean
  public open fun getError(): Boolean
  public open fun skipVarint()
  public open fun unskipVarint(value: Number)
  public open fun readUnsignedVarint32(): Number
  public open fun readSignedVarint32(): Number
  public open fun readUnsignedVarint32String(): Number
  public open fun readSignedVarint32String(): Number
  public open fun readZigzagVarint32(): Number
  public open fun readUnsignedVarint64(): Number
  public open fun readUnsignedVarint64String(): Number
  public open fun readSignedVarint64(): Number
  public open fun readSignedVarint64String(): Number
  public open fun readZigzagVarint64(): Number
  public open fun readZigzagVarint64String(): Number
  public open fun readUint8(): Number
  public open fun readUint16(): Number
  public open fun readUint32(): Number
  public open fun readUint64(): Number
  public open fun readUint64String(): String
  public open fun readInt8(): Number
  public open fun readInt16(): Number
  public open fun readInt32(): Number
  public open fun readInt64(): Number
  public open fun readInt64String(): String
  public open fun readFloat(): Number
  public open fun readDouble(): Number
  public open fun readBool(): Boolean
  public open fun readEnum(): Number
  public open fun readString(length: Number): String
  public open fun readStringWithLength(): String
  public open fun readBytes(length: Number): Uint8Array
  public open fun readVarintHash64(): String
  public open fun readFixedHash64(): String

  public companion object {
    public fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
    public fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
    public fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
    public fun alloc(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
  }
}

public open external class BinaryIterator(decoder: BinaryDecoder = definedExternally, next: () -> Any? = definedExternally, elements: Array<Any /* Number | Boolean | String */> = definedExternally) {
  public open fun free()
  public open fun clear()
  public open fun get(): dynamic /* Boolean? | Number? | String? */
  public open fun atEnd(): Boolean
  public open fun next(): dynamic /* Boolean? | Number? | String? */

  public companion object {
    public fun alloc(decoder: BinaryDecoder = definedExternally, next: () -> Any? = definedExternally, elements: Array<Any /* Number | Boolean | String */> = definedExternally): BinaryIterator
  }
}

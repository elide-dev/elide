@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS", "unused",
    "DEPRECATION",
    "ClassName"
)

package lib.protobuf

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

typealias MessageArray = Array<Any>

typealias StaticToObject = (includeInstance: Boolean, msg: Message) -> Any

typealias FieldValueArray = Array<dynamic /* String? | Number? | Boolean? | Uint8Array? | lib.protobuf.FieldValueArray? */>

external interface `T$0` {
    @nativeGetter
    operator fun get(key: Number): ExtensionFieldInfo<Message>?
    @nativeSetter
    operator fun set(key: Number, value: ExtensionFieldInfo<Message>)
}

external interface `T$1` {
    @nativeGetter
    operator fun get(key: Number): ExtensionFieldBinaryInfo<Message>?
    @nativeSetter
    operator fun set(key: Number, value: ExtensionFieldBinaryInfo<Message>)
}

external open class Message {
    open fun getJsPbMessageId(): String?
    open fun <T> serializeBinaryExtensions(proto: Message, writer: BinaryWriter, extensions: `T$1`, getExtensionFn: (fieldInfo: ExtensionFieldInfo<T>) -> T)
    open fun <T> readBinaryExtension(proto: Message, reader: BinaryReader, extensions: `T$1`, setExtensionFn: (fieldInfo: ExtensionFieldInfo<T>, param_val: T) -> Unit)
    open fun toArray(): MessageArray
    override fun toString(): String
    open fun <T> getExtension(fieldInfo: ExtensionFieldInfo<T>): T
    open fun <T> setExtension(fieldInfo: ExtensionFieldInfo<T>, value: T)
    open fun cloneMessage(): Message /* this */
    open fun clone(): Message /* this */
    open fun serializeBinary(): Uint8Array
    open fun toObject(includeInstance: Boolean = definedExternally): Any

    companion object {
        fun initialize(msg: Message, data: MessageArray, messageId: String, suggestedPivot: Number, repeatedFields: Array<Number>? = definedExternally, oneofFields: Array<Array<Number>>? = definedExternally)
        fun initialize(msg: Message, data: MessageArray, messageId: Number, suggestedPivot: Number, repeatedFields: Array<Number>? = definedExternally, oneofFields: Array<Array<Number>>? = definedExternally)
        fun <T : Message> toObjectList(field: Array<T>, toObjectFn: (includeInstance: Boolean, data: T) -> Any, includeInstance: Boolean = definedExternally): Array<Any>
        fun toObjectExtension(msg: Message, obj: Any, extensions: `T$0`, getExtensionFn: (fieldInfo: ExtensionFieldInfo<Message>) -> Message, includeInstance: Boolean = definedExternally)
        fun getField(msg: Message, fieldNumber: Number): dynamic /* String? | Number? | Boolean? | Uint8Array? | lib.protobuf.FieldValueArray? */
        fun getOptionalFloatingPointField(msg: Message, fieldNumber: Number): Number?
        fun getRepeatedFloatingPointField(msg: Message, fieldNumber: Number): Array<Number>
        fun bytesAsB64(bytes: Uint8Array): String
        fun bytesAsU8(str: String): Uint8Array
        fun bytesListAsB64(bytesList: Array<Uint8Array>): Array<String>
        fun bytesListAsU8(strList: Array<String>): Array<Uint8Array>
        fun <T> getFieldWithDefault(msg: Message, fieldNumber: Number, defaultValue: T): T
        fun getMapField(msg: Message, fieldNumber: Number, noLazyCreate: Boolean, valueCtor: Any = definedExternally): Map<Any, Any>
        fun setField(msg: Message, fieldNumber: Number, value: String?)
        fun setField(msg: Message, fieldNumber: Number, value: Number?)
        fun setField(msg: Message, fieldNumber: Number, value: Boolean?)
        fun setField(msg: Message, fieldNumber: Number, value: Uint8Array?)
        fun setField(msg: Message, fieldNumber: Number, value: FieldValueArray?)
        fun addToRepeatedField(msg: Message, fieldNumber: Number, value: Any, index: Number = definedExternally)
        fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: String?)
        fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Number?)
        fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Boolean?)
        fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Uint8Array?)
        fun setOneofField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: FieldValueArray?)
        fun computeOneofCase(msg: Message, oneof: Array<Number>): Number
        fun <T : Message> getWrapperField(msg: Message, ctor: Any, fieldNumber: Number, required: Number = definedExternally): T
        fun <T : Message> getRepeatedWrapperField(msg: Message, ctor: Any, fieldNumber: Number): Array<T>
        fun <T : Message> setWrapperField(msg: Message, fieldNumber: Number, value: T = definedExternally)
        fun <T : Message> setWrapperField(msg: Message, fieldNumber: Number, value: Map<Any, Any> = definedExternally)
        fun setOneofWrapperField(msg: Message, fieldNumber: Number, oneof: Array<Number>, value: Any)
        fun <T : Message> setRepeatedWrapperField(msg: Message, fieldNumber: Number, value: Array<T> = definedExternally)
        fun <T : Message> addToRepeatedWrapperField(msg: Message, fieldNumber: Number, value: T?, ctor: Any, index: Number = definedExternally): T
        fun toMap(field: Array<Any>, mapKeyGetterFn: (field: Any) -> String, toObjectFn: StaticToObject = definedExternally, includeInstance: Boolean = definedExternally)
        fun <T : Message> difference(m1: T, m2: T): T
        fun equals(m1: Message, m2: Message): Boolean
        fun compareExtensions(extension1: Any, extension2: Any): Boolean
        fun compareFields(field1: Any, field2: Any): Boolean
        fun <T : Message> clone(msg: T): T
        fun <T : Message> cloneMessage(msg: T): T
        fun copyInto(fromMessage: Message, toMessage: Message)
        fun registerMessageType(id: Number, constructor: Any)
        fun deserializeBinary(bytes: Uint8Array): Message
        fun deserializeBinaryFromReader(message: Message, reader: BinaryReader): Message
        fun serializeBinaryToWriter(message: Message, writer: BinaryWriter)
        fun toObject(includeInstance: Boolean, msg: Message): Any
        var extensions: `T$0`
        var extensionsBinary: `T$1`
    }
}

external interface `T$2` {
    @nativeGetter
    operator fun get(key: String): Number?
    @nativeSetter
    operator fun set(key: String, value: Number)
}

external open class ExtensionFieldInfo<T>(fieldIndex: Number, fieldName: `T$2`, ctor: Any, toObjectFn: StaticToObject, isRepeated: Number) {
    open var fieldIndex: Number
    open var fieldName: Number
    open var ctor: Any
    open var toObjectFn: StaticToObject
    open var isRepeated: Number
    open fun isMessageType(): Boolean
}

external open class ExtensionFieldBinaryInfo<T>(fieldInfo: ExtensionFieldInfo<T>, binaryReaderFn: BinaryRead, binaryWriterFn: BinaryWrite, opt_binaryMessageSerializeFn: (msg: Message, writer: BinaryWriter) -> Unit, opt_binaryMessageDeserializeFn: (msg: Message, reader: BinaryReader) -> Message, opt_isPacked: Boolean) {
    open var fieldInfo: ExtensionFieldInfo<T>
    open var binaryReaderFn: BinaryRead
    open var binaryWriterFn: BinaryWrite
    open var opt_binaryMessageSerializeFn: (msg: Message, writer: BinaryWriter) -> Unit
    open var opt_binaryMessageDeserializeFn: (msg: Message, reader: BinaryReader) -> Message
    open var opt_isPacked: Boolean
}

external open class Map<K, V>(arr: Array<Any /* JsTuple<K, V> */>, valueCtor: Any = definedExternally) {
    open fun toArray(): Array<dynamic /* JsTuple<K, V> */>
    open fun toObject(includeInstance: Boolean = definedExternally): Array<dynamic /* JsTuple<K, V> */>
    open fun toObject(): Array<dynamic /* JsTuple<K, V> */>
    open fun <VO> toObject(includeInstance: Boolean, valueToObject: (includeInstance: Boolean, valueWrapper: V) -> VO): Array<dynamic /* JsTuple<K, VO> */>
    open fun getLength(): Number
    open fun clear()
    open fun del(key: K): Boolean
    open fun getEntryList(): Array<dynamic /* JsTuple<K, V> */>
    open fun entries(): Iterator<dynamic /* JsTuple<K, V> */>
    open fun keys(): Iterator<K>
    open fun values(): Iterator<V>
    open fun forEach(callback: (entry: V, key: K) -> Unit, thisArg: Any = definedExternally)
    open fun set(key: K, value: V): Map<K, V> /* this */
    open fun get(key: K): V?
    open fun has(key: K): Boolean
    open fun serializeBinary(fieldNumber: Number, writer: BinaryWriter, keyWriterFn: (field: Number, key: K) -> Unit, valueWriterFn: (field: Number, value: V, writerCallback: BinaryWriteCallback) -> Unit, writeCallback: BinaryWriteCallback = definedExternally)
    interface Iterator<T> {
        fun next(): IteratorResult<T>
    }
    interface IteratorResult<T> {
        var done: Boolean
        var value: T
    }

    companion object {
        fun <TK, TV> fromObject(entries: Array<Any /* JsTuple<TK, TV> */>, valueCtor: Any, valueFromObject: Any): Map<TK, TV>
        fun <K, V> deserializeBinary(map: Map<K, V>, reader: BinaryReader, keyReaderFn: (reader: BinaryReader) -> K, valueReaderFn: (reader: BinaryReader, value: Any, readerCallback: BinaryReadCallback) -> V, readCallback: BinaryReadCallback = definedExternally, defaultKey: K = definedExternally, defaultValue: V = definedExternally)
    }
}

typealias BinaryReadReader = (msg: Any, binaryReader: BinaryReader) -> Unit

typealias BinaryRead = (msg: Any, reader: BinaryReadReader) -> Any

typealias BinaryReadCallback = (value: Any, binaryReader: BinaryReader) -> Unit

typealias BinaryWriteCallback = (value: Any, binaryWriter: BinaryWriter) -> Unit

typealias BinaryWrite = (fieldNumber: Number, value: Any, writerCallback: BinaryWriteCallback) -> Unit

external open class BinaryReader {
    constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor()
    constructor(bytes: ArrayBuffer = definedExternally)
    constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally)
    constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor(bytes: Uint8Array = definedExternally)
    constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally)
    constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor(bytes: Array<Number> = definedExternally)
    constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally)
    constructor(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor(bytes: String = definedExternally)
    constructor(bytes: String = definedExternally, start: Number = definedExternally)
    open fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    open fun alloc(): BinaryReader
    open fun alloc(bytes: ArrayBuffer = definedExternally): BinaryReader
    open fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally): BinaryReader
    open fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    open fun alloc(bytes: Uint8Array = definedExternally): BinaryReader
    open fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally): BinaryReader
    open fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    open fun alloc(bytes: Array<Number> = definedExternally): BinaryReader
    open fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally): BinaryReader
    open fun alloc(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    open fun alloc(bytes: String = definedExternally): BinaryReader
    open fun alloc(bytes: String = definedExternally, start: Number = definedExternally): BinaryReader
    open fun free()
    open fun getFieldCursor(): Number
    open fun getCursor(): Number
    open fun getBuffer(): Uint8Array
    open fun getFieldNumber(): Number
    open fun getWireType(): WireType
    open fun isDelimited(): Boolean
    open fun isEndGroup(): Boolean
    open fun getError(): Boolean
    open fun setBlock(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock()
    open fun setBlock(bytes: ArrayBuffer = definedExternally)
    open fun setBlock(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally)
    open fun setBlock(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(bytes: Uint8Array = definedExternally)
    open fun setBlock(bytes: Uint8Array = definedExternally, start: Number = definedExternally)
    open fun setBlock(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(bytes: Array<Number> = definedExternally)
    open fun setBlock(bytes: Array<Number> = definedExternally, start: Number = definedExternally)
    open fun setBlock(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(bytes: String = definedExternally)
    open fun setBlock(bytes: String = definedExternally, start: Number = definedExternally)
    open fun reset()
    open fun advance(count: Number)
    open fun nextField(): Boolean
    open fun unskipHeader()
    open fun skipMatchingFields()
    open fun skipVarintField()
    open fun skipDelimitedField()
    open fun skipFixed32Field()
    open fun skipFixed64Field()
    open fun skipGroup()
    open fun skipField()
    open fun registerReadCallback(callbackName: String, callback: (binaryReader: BinaryReader) -> Any)
    open fun runReadCallback(callbackName: String): Any
    open fun readAny(fieldType: FieldType): dynamic /* Boolean | Number | String | Array<dynamic /* Boolean | Number | String */> | Array<Uint8Array> | Uint8Array */
    open var readMessage: BinaryRead
    open fun readGroup(field: Number, message: Message, reader: BinaryReadReader)
    open fun getFieldDecoder(): BinaryDecoder
    open fun readInt32(): Number
    open fun readInt32String(): String
    open fun readInt64(): Number
    open fun readInt64String(): String
    open fun readUint32(): Number
    open fun readUint32String(): String
    open fun readUint64(): Number
    open fun readUint64String(): String
    open fun readSint32(): Number
    open fun readSint64(): Number
    open fun readSint64String(): String
    open fun readFixed32(): Number
    open fun readFixed64(): Number
    open fun readFixed64String(): String
    open fun readSfixed32(): Number
    open fun readSfixed32String(): String
    open fun readSfixed64(): Number
    open fun readSfixed64String(): String
    open fun readFloat(): Number
    open fun readDouble(): Number
    open fun readBool(): Boolean
    open fun readEnum(): Number
    open fun readString(): String
    open fun readBytes(): Uint8Array
    open fun readVarintHash64(): String
    open fun readFixedHash64(): String
    open fun readPackedInt32(): Array<Number>
    open fun readPackedInt32String(): Array<String>
    open fun readPackedInt64(): Array<Number>
    open fun readPackedInt64String(): Array<String>
    open fun readPackedUint32(): Array<Number>
    open fun readPackedUint32String(): Array<String>
    open fun readPackedUint64(): Array<Number>
    open fun readPackedUint64String(): Array<String>
    open fun readPackedSint32(): Array<Number>
    open fun readPackedSint64(): Array<Number>
    open fun readPackedSint64String(): Array<String>
    open fun readPackedFixed32(): Array<Number>
    open fun readPackedFixed64(): Array<Number>
    open fun readPackedFixed64String(): Array<String>
    open fun readPackedSfixed32(): Array<Number>
    open fun readPackedSfixed64(): Array<Number>
    open fun readPackedSfixed64String(): Array<String>
    open fun readPackedFloat(): Array<Number>
    open fun readPackedDouble(): Array<Number>
    open fun readPackedBool(): Array<Boolean>
    open fun readPackedEnum(): Array<Number>
    open fun readPackedVarintHash64(): Array<String>
    open fun readPackedFixedHash64(): Array<String>

    companion object {
        fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
        fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
        fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
        fun alloc(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryReader
    }
}

external open class BinaryWriter {
    open fun writeSerializedMessage(bytes: Uint8Array, start: Number, end: Number)
    open fun maybeWriteSerializedMessage(bytes: Uint8Array = definedExternally, start: Number = definedExternally, end: Number = definedExternally)
    open fun reset()
    open fun getResultBuffer(): Uint8Array
    open fun getResultBase64String(): String
    open fun beginSubMessage(field: Number)
    open fun endSubMessage(field: Number)
    open fun writeAny(fieldType: FieldType, field: Number, value: Boolean)
    open fun writeAny(fieldType: FieldType, field: Number, value: Number)
    open fun writeAny(fieldType: FieldType, field: Number, value: String)
    open fun writeAny(fieldType: FieldType, field: Number, value: Array<Any /* Boolean | Number | String */>)
    open fun writeAny(fieldType: FieldType, field: Number, value: Array<Uint8Array>)
    open fun writeAny(fieldType: FieldType, field: Number, value: Uint8Array)
    open fun writeInt32(field: Number, value: Number = definedExternally)
    open fun writeInt32String(field: Number, value: String = definedExternally)
    open fun writeInt64(field: Number, value: Number = definedExternally)
    open fun writeInt64String(field: Number, value: String = definedExternally)
    open fun writeUint32(field: Number, value: Number = definedExternally)
    open fun writeUint32String(field: Number, value: String = definedExternally)
    open fun writeUint64(field: Number, value: Number = definedExternally)
    open fun writeUint64String(field: Number, value: String = definedExternally)
    open fun writeSint32(field: Number, value: Number = definedExternally)
    open fun writeSint64(field: Number, value: Number = definedExternally)
    open fun writeSint64String(field: Number, value: String = definedExternally)
    open fun writeFixed32(field: Number, value: Number = definedExternally)
    open fun writeFixed64(field: Number, value: Number = definedExternally)
    open fun writeFixed64String(field: Number, value: String = definedExternally)
    open fun writeSfixed32(field: Number, value: Number = definedExternally)
    open fun writeSfixed64(field: Number, value: Number = definedExternally)
    open fun writeSfixed64String(field: Number, value: String = definedExternally)
    open fun writeFloat(field: Number, value: Number = definedExternally)
    open fun writeDouble(field: Number, value: Number = definedExternally)
    open fun writeBool(field: Number, value: Boolean = definedExternally)
    open fun writeEnum(field: Number, value: Number = definedExternally)
    open fun writeString(field: Number, value: String = definedExternally)
    open fun writeBytes(field: Number, value: ArrayBuffer = definedExternally)
    open fun writeBytes(field: Number)
    open fun writeBytes(field: Number, value: Uint8Array = definedExternally)
    open fun writeBytes(field: Number, value: Array<Number> = definedExternally)
    open fun writeBytes(field: Number, value: String = definedExternally)
    open var writeMessage: BinaryWrite
    open fun writeGroup(field: Number, value: Any, writeCallback: BinaryWriteCallback)
    open fun writeFixedHash64(field: Number, value: String = definedExternally)
    open fun writeVarintHash64(field: Number, value: String = definedExternally)
    open fun writeRepeatedInt32(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedInt32String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedInt64(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedInt64String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedUint32(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedUint32String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedUint64(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedUint64String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedSint32(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedSint64(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedSint64String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedFixed32(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedFixed64(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedFixed64String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedSfixed32(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedSfixed64(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedSfixed64String(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedFloat(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedDouble(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedBool(field: Number, value: Array<Boolean> = definedExternally)
    open fun writeRepeatedEnum(field: Number, value: Array<Number> = definedExternally)
    open fun writeRepeatedString(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedBytes(field: Number, value: Array<Any /* ArrayBuffer | Uint8Array | Array<Number> | String */> = definedExternally)
    open fun writeRepeatedMessage(field: Number, value: Array<Message>, writerCallback: BinaryWriteCallback)
    open fun writeRepeatedGroup(field: Number, value: Array<Message>, writerCallback: BinaryWriteCallback)
    open fun writeRepeatedFixedHash64(field: Number, value: Array<String> = definedExternally)
    open fun writeRepeatedVarintHash64(field: Number, value: Array<String> = definedExternally)
    open fun writePackedInt32(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedInt32String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedInt64(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedInt64String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedUint32(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedUint32String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedUint64(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedUint64String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedSint32(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedSint64(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedSint64String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedFixed32(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedFixed64(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedFixed64String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedSfixed32(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedSfixed64(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedSfixed64String(field: Number, value: Array<String> = definedExternally)
    open fun writePackedFloat(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedDouble(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedBool(field: Number, value: Array<Boolean> = definedExternally)
    open fun writePackedEnum(field: Number, value: Array<Number> = definedExternally)
    open fun writePackedFixedHash64(field: Number, value: Array<String> = definedExternally)
    open fun writePackedVarintHash64(field: Number, value: Array<String> = definedExternally)
}

external open class BinaryEncoder {
    open fun length(): Number
    open fun end(): Array<Number>
    open fun writeSplitVarint64(lowBits: Number, highBits: Number)
    open fun writeSplitFixed64(lowBits: Number, highBits: Number)
    open fun writeUnsignedVarint32(value: Number)
    open fun writeSignedVarint32(value: Number)
    open fun writeUnsignedVarint64(value: Number)
    open fun writeSignedVarint64(value: Number)
    open fun writeZigzagVarint32(value: Number)
    open fun writeZigzagVarint64(value: Number)
    open fun writeZigzagVarint64String(value: String)
    open fun writeUint8(value: Number)
    open fun writeUint16(value: Number)
    open fun writeUint32(value: Number)
    open fun writeUint64(value: Number)
    open fun writeInt8(value: Number)
    open fun writeInt16(value: Number)
    open fun writeInt32(value: Number)
    open fun writeInt64(value: Number)
    open fun writeInt64String(value: String)
    open fun writeFloat(value: Number)
    open fun writeDouble(value: Number)
    open fun writeBool(value: Boolean)
    open fun writeEnum(value: Number)
    open fun writeBytes(bytes: Uint8Array)
    open fun writeVarintHash64(hash: String)
    open fun writeFixedHash64(hash: String)
    open fun writeString(value: String): Number
}

external open class BinaryDecoder {
    constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor()
    constructor(bytes: ArrayBuffer = definedExternally)
    constructor(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally)
    constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor(bytes: Uint8Array = definedExternally)
    constructor(bytes: Uint8Array = definedExternally, start: Number = definedExternally)
    constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor(bytes: Array<Number> = definedExternally)
    constructor(bytes: Array<Number> = definedExternally, start: Number = definedExternally)
    constructor(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally)
    constructor(bytes: String = definedExternally)
    constructor(bytes: String = definedExternally, start: Number = definedExternally)
    open fun free()
    open fun clone(): BinaryDecoder
    open fun clear()
    open fun getBuffer(): Uint8Array
    open fun setBlock(data: ArrayBuffer, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(data: ArrayBuffer)
    open fun setBlock(data: ArrayBuffer, start: Number = definedExternally)
    open fun setBlock(data: Uint8Array, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(data: Uint8Array)
    open fun setBlock(data: Uint8Array, start: Number = definedExternally)
    open fun setBlock(data: Array<Number>, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(data: Array<Number>)
    open fun setBlock(data: Array<Number>, start: Number = definedExternally)
    open fun setBlock(data: String, start: Number = definedExternally, length: Number = definedExternally)
    open fun setBlock(data: String)
    open fun setBlock(data: String, start: Number = definedExternally)
    open fun getEnd(): Number
    open fun setEnd(end: Number)
    open fun reset()
    open fun getCursor(): Number
    open fun setCursor(cursor: Number)
    open fun advance(count: Number)
    open fun atEnd(): Boolean
    open fun pastEnd(): Boolean
    open fun getError(): Boolean
    open fun skipVarint()
    open fun unskipVarint(value: Number)
    open fun readUnsignedVarint32(): Number
    open fun readSignedVarint32(): Number
    open fun readUnsignedVarint32String(): Number
    open fun readSignedVarint32String(): Number
    open fun readZigzagVarint32(): Number
    open fun readUnsignedVarint64(): Number
    open fun readUnsignedVarint64String(): Number
    open fun readSignedVarint64(): Number
    open fun readSignedVarint64String(): Number
    open fun readZigzagVarint64(): Number
    open fun readZigzagVarint64String(): Number
    open fun readUint8(): Number
    open fun readUint16(): Number
    open fun readUint32(): Number
    open fun readUint64(): Number
    open fun readUint64String(): String
    open fun readInt8(): Number
    open fun readInt16(): Number
    open fun readInt32(): Number
    open fun readInt64(): Number
    open fun readInt64String(): String
    open fun readFloat(): Number
    open fun readDouble(): Number
    open fun readBool(): Boolean
    open fun readEnum(): Number
    open fun readString(length: Number): String
    open fun readStringWithLength(): String
    open fun readBytes(length: Number): Uint8Array
    open fun readVarintHash64(): String
    open fun readFixedHash64(): String

    companion object {
        fun alloc(bytes: ArrayBuffer = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
        fun alloc(bytes: Uint8Array = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
        fun alloc(bytes: Array<Number> = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
        fun alloc(bytes: String = definedExternally, start: Number = definedExternally, length: Number = definedExternally): BinaryDecoder
    }
}

external open class BinaryIterator(decoder: BinaryDecoder = definedExternally, next: () -> Any? = definedExternally, elements: Array<Any /* Number | Boolean | String */> = definedExternally) {
    open fun free()
    open fun clear()
    open fun get(): dynamic /* Boolean? | Number? | String? */
    open fun atEnd(): Boolean
    open fun next(): dynamic /* Boolean? | Number? | String? */

    companion object {
        fun alloc(decoder: BinaryDecoder = definedExternally, next: () -> Any? = definedExternally, elements: Array<Any /* Number | Boolean | String */> = definedExternally): BinaryIterator
    }
}
//[base](../../../index.md)/[elide.model](../index.md)/[WireMessage](index.md)/[toSerializedBytes](to-serialized-bytes.md)

# toSerializedBytes

[common, js, jvm, native]\
[common]\
expect open fun [toSerializedBytes](to-serialized-bytes.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

[js, jvm, native]\
actual open fun [toSerializedBytes](to-serialized-bytes.md)(): [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html)

Serialize this [WireMessage](index.md) instance into a raw [ByteArray](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-byte-array/index.html), which is suitable for sending over the wire; formats expressed via this interface must keep schema in sync on both sides.

Binary serialization depends on platform but is typically implemented via Protocol Buffer messages. For schemaless serialization, use Proto-JSON.

#### Return

Raw bytes of this message, in serialized form.

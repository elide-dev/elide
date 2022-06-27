//[base](../../../index.md)/[lib.protobuf](../index.md)/[Map](index.md)

# Map

[js]\
open external class [Map](index.md)&lt;[K](index.md), [V](index.md)&gt;(arr: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, valueCtor: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) = definedExternally)

## Constructors

| | |
|---|---|
| [Map](-map.md) | [js]<br>fun [Map](-map.md)(arr: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;, valueCtor: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) = definedExternally) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [js]<br>object [Companion](-companion/index.md) |
| [Iterator](-iterator/index.md) | [js]<br>interface [Iterator](-iterator/index.md)&lt;[T](-iterator/index.md)&gt; |
| [IteratorResult](-iterator-result/index.md) | [js]<br>interface [IteratorResult](-iterator-result/index.md)&lt;[T](-iterator-result/index.md)&gt; |

## Functions

| Name | Summary |
|---|---|
| [clear](clear.md) | [js]<br>open fun [clear](clear.md)() |
| [del](del.md) | [js]<br>open fun [del](del.md)(key: [K](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [entries](entries.md) | [js]<br>open fun [entries](entries.md)(): [Map.Iterator](-iterator/index.md)&lt;dynamic&gt; |
| [forEach](for-each.md) | [js]<br>open fun [forEach](for-each.md)(callback: (entry: [V](index.md), key: [K](index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), thisArg: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html) = definedExternally) |
| [get](get.md) | [js]<br>open fun [get](get.md)(key: [K](index.md)): [V](index.md)? |
| [getEntryList](get-entry-list.md) | [js]<br>open fun [getEntryList](get-entry-list.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;dynamic&gt; |
| [getLength](get-length.md) | [js]<br>open fun [getLength](get-length.md)(): [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html) |
| [has](has.md) | [js]<br>open fun [has](has.md)(key: [K](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [keys](keys.md) | [js]<br>open fun [keys](keys.md)(): [Map.Iterator](-iterator/index.md)&lt;[K](index.md)&gt; |
| [serializeBinary](serialize-binary.md) | [js]<br>open fun [serializeBinary](serialize-binary.md)(fieldNumber: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), writer: [BinaryWriter](../-binary-writer/index.md), keyWriterFn: (field: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), key: [K](index.md)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), valueWriterFn: (field: [Number](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-number/index.html), value: [V](index.md), [BinaryWriteCallback](../index.md#1567219273%2FClasslikes%2F-431612152)) -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html), writeCallback: [BinaryWriteCallback](../index.md#1567219273%2FClasslikes%2F-431612152) = definedExternally) |
| [set](set.md) | [js]<br>open fun [set](set.md)(key: [K](index.md), value: [V](index.md)): [Map](index.md)&lt;[K](index.md), [V](index.md)&gt; |
| [toArray](to-array.md) | [js]<br>open fun [toArray](to-array.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;dynamic&gt; |
| [toObject](to-object.md) | [js]<br>open fun [toObject](to-object.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;dynamic&gt;<br>open fun [toObject](to-object.md)(includeInstance: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = definedExternally): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;dynamic&gt;<br>open fun &lt;[VO](to-object.md)&gt; [toObject](to-object.md)(includeInstance: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), valueToObject: (includeInstance: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html), valueWrapper: [V](index.md)) -&gt; [VO](to-object.md)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;dynamic&gt; |
| [values](values.md) | [js]<br>open fun [values](values.md)(): [Map.Iterator](-iterator/index.md)&lt;[V](index.md)&gt; |

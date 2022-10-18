//[base](../../../index.md)/[elide](../index.md)/[AppEnvironment](index.md)

# AppEnvironment

[common]\
enum [AppEnvironment](index.md) : [Enum](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-enum/index.html)&lt;[AppEnvironment](index.md)&gt; 

Enumerates application environments which can be connected to configuration, secrets, and other behavior.

- 
   `SANDBOX`: The application is executing in a development, test, or experimental environment.
- 
   `LIVE`: The application is executing in a production environment.

## Entries

| | |
|---|---|
| [SANDBOX](-s-a-n-d-b-o-x/index.md) | [common]<br>[SANDBOX](-s-a-n-d-b-o-x/index.md) |
| [LIVE](-l-i-v-e/index.md) | [common]<br>[LIVE](-l-i-v-e/index.md) |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [common]<br>fun [valueOf](value-of.md)(value: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [AppEnvironment](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [common]<br>fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[AppEnvironment](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. |

## Properties

| Name | Summary |
|---|---|
| [name](../../elide.util/-encoding/-b-a-s-e64/index.md#-372974862%2FProperties%2F-1416663450) | [common]<br>val [name](../../elide.util/-encoding/-b-a-s-e64/index.md#-372974862%2FProperties%2F-1416663450): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [ordinal](../../elide.util/-encoding/-b-a-s-e64/index.md#-739389684%2FProperties%2F-1416663450) | [common]<br>val [ordinal](../../elide.util/-encoding/-b-a-s-e64/index.md#-739389684%2FProperties%2F-1416663450): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) |

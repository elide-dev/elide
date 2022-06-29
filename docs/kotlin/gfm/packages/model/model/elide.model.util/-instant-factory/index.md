//[model](../../../index.md)/[elide.model.util](../index.md)/[InstantFactory](index.md)

# InstantFactory

[common]\
expect object [InstantFactory](index.md)

Tool which can be used to convert time and date structures used with Elide Models into different representations, including ISO-8601, standard platform dates, and Protocol Buffer standard dates and times.

[js, native]\
actual object [InstantFactory](index.md)

Tool which can be used to convert time and date structures used with Elide Models into different representations, including ISO-8601, standard platform dates, and Protocol Buffer standard dates and times.

[jvm]\
actual object [InstantFactory](index.md)

Utilities to convert between different time objects, particularly a standard Protocol Buffer Timestamp and Java's time objects, such as [Instant](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html) and [Date](https://docs.oracle.com/javase/8/docs/api/java/util/Date.html).

## Functions

| Name | Summary |
|---|---|
| [date](date.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [date](date.md)(subject: Timestamp): [Date](https://docs.oracle.com/javase/8/docs/api/java/util/Date.html)<br>Convert a Protocol Buffers Timestamp record to a Java [Date](https://docs.oracle.com/javase/8/docs/api/java/util/Date.html). |
| [instant](instant.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [instant](instant.md)(subject: Timestamp): [Instant](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html)<br>Convert a Protocol Buffers Timestamp record to a Java [Instant](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html). |

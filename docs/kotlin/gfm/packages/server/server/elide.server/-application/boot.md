//[server](../../../index.md)/[elide.server](../index.md)/[Application](index.md)/[boot](boot.md)

# boot

[jvm]\
open fun [boot](boot.md)(args: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)

Boot an Elide application with the provided [args](boot.md), if any.

Elide parses its own arguments and applies configuration or state based on any encountered values. All Elide flags are prefixed with &quot;--elide.&quot;. Micronaut-relevant arguments are passed on to Micronaut, and user args are additionally made available.

Elide server arguments can be interrogated via [ServerFlag](../../elide.server.util/-server-flag/index.md)s.

## Parameters

jvm

| | |
|---|---|
| args | Arguments passed to the application. |

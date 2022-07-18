//[server](../../../index.md)/[elide.server.util](../index.md)/[ServerFlag](index.md)

# ServerFlag

[jvm]\
object [ServerFlag](index.md)

Static server flags, which may be set via Java system properties or environment variables.

## Functions

| Name | Summary |
|---|---|
| [resolve](resolve.md) | [jvm]<br>fun &lt;[R](resolve.md)&gt; [resolve](resolve.md)(name: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), defaultValue: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), then: ([String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) -&gt; [R](resolve.md)): [R](resolve.md) |
| [setArgs](set-args.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun [setArgs](set-args.md)(args: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-array/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;)<br>Install server flag value state. |

## Properties

| Name | Summary |
|---|---|
| [appEnv](app-env.md) | [jvm]<br>val [appEnv](app-env.md): [AppEnvironment](../../../../../packages/base/base/elide/-app-environment/index.md)<br>Operating environment for the application. |
| [DEFAULT_INSPECT_PORT](-d-e-f-a-u-l-t_-i-n-s-p-e-c-t_-p-o-r-t.md) | [jvm]<br>const val [DEFAULT_INSPECT_PORT](-d-e-f-a-u-l-t_-i-n-s-p-e-c-t_-p-o-r-t.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html) = 4242<br>Default port to listen on for the VM inspector. |
| [inspect](inspect.md) | [jvm]<br>val [inspect](inspect.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Whether to enable VM inspection. |
| [inspectHost](inspect-host.md) | [jvm]<br>val [inspectHost](inspect-host.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Host where inspection should mount. |
| [inspectPath](inspect-path.md) | [jvm]<br>val [inspectPath](inspect-path.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>Path where inspection should mount. |
| [inspectPort](inspect-port.md) | [jvm]<br>val [inspectPort](inspect-port.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)<br>Port where inspection should mount. |
| [inspectSecure](inspect-secure.md) | [jvm]<br>val [inspectSecure](inspect-secure.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Whether to enable VM inspection secure mode (TLS). |

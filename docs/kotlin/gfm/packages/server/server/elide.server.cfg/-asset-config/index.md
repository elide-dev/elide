//[server](../../../index.md)/[elide.server.cfg](../index.md)/[AssetConfig](index.md)

# AssetConfig

[jvm]\
@ConfigurationProperties(value = &quot;elide.server.assets&quot;)

data class [AssetConfig](index.md)(var enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, var prefix: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;/_/assets&quot;, var etags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, var preferWeakEtags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)

Configuration loaded at runtime which governs Elide's built-in asset serving tools.

#### Parameters

jvm

| | |
|---|---|
| enabled | Whether the asset system is enabled. |
| prefix | URI prefix where static assets are served. |
| etags | Whether to generate, and respond to, ETag headers for assets. |
| preferWeakEtags | Whether to prefer weak ETags. Defaults to `false`. |

## Constructors

| | |
|---|---|
| [AssetConfig](-asset-config.md) | [jvm]<br>fun [AssetConfig](-asset-config.md)(enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, prefix: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;/_/assets&quot;, etags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, preferWeakEtags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [jvm]<br>var [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [etags](etags.md) | [jvm]<br>var [etags](etags.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [preferWeakEtags](prefer-weak-etags.md) | [jvm]<br>var [preferWeakEtags](prefer-weak-etags.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [prefix](prefix.md) | [jvm]<br>var [prefix](prefix.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

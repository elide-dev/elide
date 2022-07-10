//[server](../../../index.md)/[elide.server.cfg](../index.md)/[AssetConfig](index.md)

# AssetConfig

[jvm]\
@ConfigurationProperties(value = &quot;elide.server.assets&quot;)

data class [AssetConfig](index.md)(var enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, var prefix: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;/_/assets&quot;)

Configuration loaded at runtime which governs Elide's built-in asset serving tools.

## Parameters

jvm

| | |
|---|---|
| enabled | Whether the asset system is enabled. |
| prefix | URI prefix where static assets are served. |

## Constructors

| | |
|---|---|
| [AssetConfig](-asset-config.md) | [jvm]<br>fun [AssetConfig](-asset-config.md)(enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, prefix: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;/_/assets&quot;) |

## Properties

| Name | Summary |
|---|---|
| [enabled](enabled.md) | [jvm]<br>var [enabled](enabled.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true |
| [prefix](prefix.md) | [jvm]<br>var [prefix](prefix.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

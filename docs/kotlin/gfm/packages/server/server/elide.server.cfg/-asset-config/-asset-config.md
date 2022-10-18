//[server](../../../index.md)/[elide.server.cfg](../index.md)/[AssetConfig](index.md)/[AssetConfig](-asset-config.md)

# AssetConfig

[jvm]\
fun [AssetConfig](-asset-config.md)(enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, prefix: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;/_/assets&quot;, etags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, preferWeakEtags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)

#### Parameters

jvm

| | |
|---|---|
| enabled | Whether the asset system is enabled. |
| prefix | URI prefix where static assets are served. |
| etags | Whether to generate, and respond to, ETag headers for assets. |
| preferWeakEtags | Whether to prefer weak ETags. Defaults to `false`. |

//[server](../../index.md)/[elide.server.cfg](index.md)

# Package elide.server.cfg

## Types

| Name | Summary |
|---|---|
| [AssetConfig](-asset-config/index.md) | [jvm]<br>@ConfigurationProperties(value = &quot;elide.server.assets&quot;)<br>data class [AssetConfig](-asset-config/index.md)(var enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, var prefix: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;/_/assets&quot;, var etags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = true, var preferWeakEtags: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)<br>Configuration loaded at runtime which governs Elide's built-in asset serving tools. |
| [ServerConfig](-server-config/index.md) | [jvm]<br>@ConfigurationProperties(value = &quot;elide.server&quot;)<br>data class [ServerConfig](-server-config/index.md)(var assets: [AssetConfig](-asset-config/index.md) = AssetConfig())<br>Configuration properties loaded at runtime through Micronaut's configuration system, which govern how Elide hosts server-side code. |

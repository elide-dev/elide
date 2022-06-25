//[server](../../index.md)/[elide.server](index.md)/[asset](asset.md)

# asset

[jvm]\
fun [asset](asset.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), contentType: MediaType?): HttpResponse&lt;*&gt;

Serve an application asset file which is embedded in the application JAR, from the path `/assets/[type]/[path]`.

#### Return

HTTP response wrapping the desired asset, or an HTTP response which serves a 404 if the asset could not be     located at the specified path.

## Parameters

jvm

| | |
|---|---|
| path | Path to the file within the provided [type](asset.md) directory. |
| type | Type of asset to serve; accepted values are `css` and `js`. |
| contentType | Resolved MediaType to use when serving this asset. Must not be null. |

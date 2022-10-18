//[server](../../index.md)/[elide.server](index.md)/[staticFile](static-file.md)

# staticFile

[jvm]\
fun [staticFile](static-file.md)(file: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), contentType: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): HttpResponse&lt;*&gt;

Serve a static file which is embedded in the application JAR, at the path `/static/[file]`.

#### Return

HTTP response wrapping the desired asset, or an HTTP response which serves a 404 if the asset could not be     located at the specified path.

#### Parameters

jvm

| | |
|---|---|
| file | Filename to load from the `/static` root directory. |
| contentType | `Content-Type` value to send back for this file. |

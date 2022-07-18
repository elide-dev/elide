//[server](../../index.md)/[kotlinx.html.tagext](index.md)/[body](body.md)

# body

[jvm]\
inline suspend fun [HTML](../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).[body](body.md)(classes: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, crossinline block: suspend [BODY](../../../../packages/server/kotlinx.html/-b-o-d-y/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html))

Open a `<body>` tag with support for suspension calls.

## Parameters

jvm

| | |
|---|---|
| classes | Classes to apply to the body tag in the DOM. |
| block | Callable block to configure and populate the body tag. |

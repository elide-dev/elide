//[graalvm](../../index.md)/[elide.server](index.md)/[injectSSR](inject-s-s-r.md)

# injectSSR

[jvm]\
fun BODY.[injectSSR](inject-s-s-r.md)(domId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = defaultSsrDomId, classes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = emptySet(), attrs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; = emptyList(), path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = nodeSsrDefaultPath)

Evaluate and inject SSR content into a larger HTML page, using a `<main>` tag as the root element in the dom; apply [domId](inject-s-s-r.md), [classes](inject-s-s-r.md), and any additional [attrs](inject-s-s-r.md) to the root element, if specified.

SSR script content will be loaded from the path `node-prod.js` within the embedded asset section of the JAR (located at `/embedded` at the time of this writing).

## Parameters

jvm

| | |
|---|---|
| domId | ID of the root element to express within the DOM. Defaults to `root`. |
| classes | List of classes to apply to the root DOM element. Defaults to an empty class list. |
| attrs | Set of additional attribute pairs to apply in the DOM to the root element. Defaults to an empty set. |
| path | Path within the embedded asset area of the JAR from which to load the SSR script. Defaults to     `node-prod.js`, which is the default value used by the Node/Kotlin toolchain provided by Elide. |

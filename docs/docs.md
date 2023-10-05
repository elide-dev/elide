
## Elide Framework: API Docs

[Elide][0] is a **polyglot runtime and framework** for building really cool web apps which can mix languages. It's a
**runtime** in the sense that you can run software with it, like with `node` or `python`, and it is a **framework**, in
the sense that it can be used on the JVM via [Maven Central][1].

### Where these docs fit in

These docs are the API docs for using Elide _as a framework_. If you are looking for _Getting Started_ docs, or you're
looking for docs describing Elide's use as a runtime, head to our [website][0] or the main [GitHub repo][2].

### Framework architecture

Elide, as a framework, ranges from a Nothing Burger to Everything You Could Want, based on the set of modules you choose
to add to your app.

| Module          | Platforms                     | Description                                               |
|-----------------|-------------------------------|-----------------------------------------------------------|
| [`core`][10]    | JVM, JavaScript, Native, WASM | Broadest platform support; simplest tools.                |
| [`base`][11]    | JVM, JavaScript, Native       | Useful tools which aren't yet available on all platforms. |
| [`test`][12]    | JVM, JavaScript, Native       | Test and assertion utilities and annotations.             |
| [`ssr`][13]     | JVM, JavaScript               | Shared SSR annotations and logic across platforms.        |
| [`server`][14]  | JVM                           | Server operational code, annotations, controllers.        |

**Note:** In this case, _Native_ refers to [Kotlin Native](https://kotlinlang.org/docs/native-overview.html), not the
GraalVM Native Image tool, which Elide also uses (when used as a runtime).

### Unsupported modules

Additional modules exist, but are either internal to the Elide framework or runtime, or aren't yet supported for public
use. You can feel free to try and use them, but they may not work as expected and APIs may shift without warning.

[0]: https://elide.dev
[1]: https://search.maven.org/search?q=g:dev.elide
[2]: https://github.com/elide-dev/elide
[10]: https://docs.elide.dev/apidocs/packages/core/index.html
[11]: https://docs.elide.dev/apidocs/packages/base/index.html
[12]: https://docs.elide.dev/apidocs/packages/test/index.html
[13]: https://docs.elide.dev/apidocs/packages/ssr/index.html
[14]: https://docs.elide.dev/apidocs/packages/server/index.html

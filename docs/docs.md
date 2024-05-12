<center>
<img src="https://static.elide.dev/assets/org-profile/creative/elide-banner-purple.png" alt="Elide" />
<br />
</center>

<p align="center" class="nounderline">
  <a href="https://elide.dev/discord"><img src="https://img.shields.io/discord/1119121740161884252?b1" /></a>
  <a href="https://bestpractices.coreinfrastructure.org/projects/7690"><img src="https://bestpractices.coreinfrastructure.org/projects/7690/badge" /></a>
  <a href="https://github.com/elide-dev/elide"><img src="https://img.shields.io/badge/Contributor%20Covenant-v1.4-ff69b4.svg" alt="Code of Conduct" /></a>
  <img alt="GitHub License" src="https://img.shields.io/github/license/elide-dev/elide">
  <br />
  <a href="https://openjdk.org/projects/jdk/22/"><img src="https://img.shields.io/badge/-Java%2022-blue.svg?logo=oracle" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/-Kotlin%202.0.0-blue.svg?logo=kotlin&logoColor=white" /></a>
  <a href="https://262.ecma-international.org/13.0/"><img src="https://img.shields.io/badge/-ECMA2023-blue.svg?logo=javascript&logoColor=white" /></a>
  <img alt="Python 3.10.x" src="https://img.shields.io/badge/Python%203.10.x-green?style=flat&logo=python&logoColor=white&color=blue">
  <a href="https://www.ruby-lang.org/"><img src="https://img.shields.io/badge/-Ruby%203.2-blue.svg?logo=ruby&logoColor=white" /></a>
</p>

<br />

## API Reference

[Elide][0] is a **polyglot runtime and framework** for building really cool web apps which can mix languages. It's a
**runtime** in the sense that you can run software with it, like with `node` or `python`, and it is a **framework**, in
the sense that it can be used on the JVM via [Maven Central][1].

### Where these docs fit in

These docs are the API docs for using Elide _as a framework_. If you are looking for _Getting Started_ docs, or you're
looking for docs describing Elide's use as a runtime, head to the [main docs](https://docs.elide.dev), the
[website](https://elide.dev) or the [GitHub repo](https://github.com/elide-dev/elide).

### Framework architecture

Elide, as a framework, ranges from a Nothing Burger to Everything You Could Want, based on the set of modules you choose
to add to your app.

| Module          | Platforms                     | Description                                               |
|-----------------|-------------------------------|-----------------------------------------------------------|
| [`core`][10]    | JVM, JavaScript, Native, WASM | Broadest platform support; simplest tools.                |
| [`base`][11]    | JVM, JavaScript, Native       | Useful tools which aren't yet available on all platforms. |
| [`test`][12]    | JVM, JavaScript, Native       | Test and assertion utilities and annotations.             |
| [`ssr`][13]     | JVM, JavaScript               | Shared SSR annotations and logic across platforms.        |
| [`server`][14]  | JVM, Native Image             | Server operational code, annotations, controllers.        |
| [`graalvm`][15] | JVM, Native Image             | GraalVM integration code and JavaScript support.          |

<br />

> In this case, _Native_ refers to [Kotlin Native](https://kotlinlang.org/docs/native-overview.html), not the GraalVM
> Native Image tool, which Elide also uses (when used as a runtime).

[0]: https://elide.dev
[1]: https://search.maven.org/search?q=g:dev.elide
[2]: https://github.com/elide-dev/elide
[10]: https://docs.elide.dev/apidocs/packages/core/index.html
[11]: https://docs.elide.dev/apidocs/packages/base/index.html
[12]: https://docs.elide.dev/apidocs/packages/test/index.html
[13]: https://docs.elide.dev/apidocs/packages/ssr/index.html
[14]: https://docs.elide.dev/apidocs/packages/server/index.html
[15]: https://docs.elide.dev/apidocs/packages/graalvm/index.html

<br />

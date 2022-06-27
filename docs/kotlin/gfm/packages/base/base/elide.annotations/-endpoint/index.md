//[base](../../../index.md)/[elide.annotations](../index.md)/[Endpoint](index.md)

# Endpoint

[jvm]\
@Singleton

@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.CLASS](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-c-l-a-s-s/index.html)])

annotation class [Endpoint](index.md)

Marks a class as an API endpoint, which enables functionality for type conversion between [elide.model.WireMessage](../../elide.model/-wire-message/index.md) types and Micronaut requests / responses.

[Endpoint](index.md) should be used in conjunction with other Micronaut annotations, like `@Controller`. Classes marked as endpoints automatically participate in DI as Singletons.

## Constructors

| | |
|---|---|
| [Endpoint](-endpoint.md) | [jvm]<br>fun [Endpoint](-endpoint.md)() |

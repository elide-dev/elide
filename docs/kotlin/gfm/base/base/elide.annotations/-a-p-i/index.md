//[base](../../../index.md)/[elide.annotations](../index.md)/[API](index.md)

# API

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.TYPE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-t-y-p-e/index.html)])

annotation class [API](index.md)

Marks an application-level class as an API interface, which defines the abstract surface of a single unit of business logic; combined with [Logic](../-logic/index.md), classes annotated with `API` constitute a set of interface and implementation pairs.

API should only be affixed to interfaces or abstract classes. API interface parameters are preserved and other AOT- style configurations are possible based on this annotation.

## Constructors

| | |
|---|---|
| [API](-a-p-i.md) | [common]<br>fun [API](-a-p-i.md)() |

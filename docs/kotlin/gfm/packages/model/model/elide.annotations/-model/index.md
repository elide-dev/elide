//[model](../../../index.md)/[elide.annotations](../index.md)/[Model](index.md)

# Model

[common]\
@[Target](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-target/index.html)(allowedTargets = [[AnnotationTarget.TYPE](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.annotation/-annotation-target/-t-y-p-e/index.html)])

annotation class [Model](index.md)

Marks an application-level class as a data model, which makes it eligible for reflective use (even in native circumstances such as on GraalVM).

Classes marked as models become available for reflection for all constructors, fields, and methods. Models should typically only depend on other models (ideally via encapsulation), and should be immutable. Kotlin data classes are an example of good model semantics.

## Constructors

| | |
|---|---|
| [Model](-model.md) | [common]<br>fun [Model](-model.md)() |

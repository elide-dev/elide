package elide.annotations

/**
 * # Annotation: Factory
 *
 * Marks a class as a factory for an injected type. Factories are responsible for creating instances of injected types.
 * On JVM platforms, this annotation is aliased to a Micronaut `Factory` annotation.
 *
 * See also: https://docs.micronaut.io/snapshot/api/io/micronaut/context/annotation/Factory.html
 */
public expect annotation class Factory

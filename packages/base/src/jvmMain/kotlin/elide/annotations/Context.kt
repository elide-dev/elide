package elide.annotations

/**
 * # Annotation: Context (JVM)
 *
 * Marks a class as "context"-phase for purposes of dependency injection. Classes marked with this annotation are
 * initialized eagerly on application startup.
 *
 * When used on JVM platforms, this annotation is translated into a Micronaut `Context` annotation:
 * https://docs.micronaut.io/snapshot/api/io/micronaut/context/annotation/Context.html
 *
 * @see Eager which behaves similarly.
 */
public actual typealias Context =  io.micronaut.context.annotation.Context

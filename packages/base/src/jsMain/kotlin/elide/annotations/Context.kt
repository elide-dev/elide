package elide.annotations

/**
 * # Annotation: Context (JS)
 *
 * Marks a class as "context"-phase for purposes of dependency injection. Classes marked with this annotation are
 * initialized eagerly on application startup.
 *
 * @see Eager which behaves similarly.
 */
public actual annotation class Context

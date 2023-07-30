package elide.annotations

/**
 * # Annotation: Singleton (JS)
 *
 * Marks a class which participates in injection (DI) as a singleton; singletons are classes with the constraint that
 * only one instance may exist at runtime. In injected contexts, the singleton lifecycle is managed by the DI container.
 */
public actual annotation class Singleton

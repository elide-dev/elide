package elide.annotations

/**
 * # Annotation: Singleton
 *
 * Marks a class which participates in injection (DI) as a singleton; singletons are classes with the constraint that
 * only one instance may exist at runtime. In injected contexts, the singleton lifecycle is managed by the DI container.
 *
 * On JVM platforms, this annotation behaves like a standard Jakarta `Singleton` annotation:
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/singleton
 */
public expect annotation class Singleton

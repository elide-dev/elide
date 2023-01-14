package elide.annotations

/**
 * # Annotation: Inject (Native)
 *
 * This annotation marks a constructor argument, field/property, or parameter as an injected value. Injected values are
 * generally resolved at build-time but may be resolved at run-time. Dependency Injection (DI) is an opt-in pattern
 * which inverts control of object creation. Instead of creating objects directly, they are created by a DI container
 * and provided to each object that requires them.
 */
public actual annotation class Inject

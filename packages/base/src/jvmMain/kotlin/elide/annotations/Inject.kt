package elide.annotations

/**
 * # Annotation: Inject (JVM)
 *
 * This annotation marks a constructor argument, field/property, or parameter as an injected value. Injected values are
 * generally resolved at build-time but may be resolved at run-time. Dependency Injection (DI) is an opt-in pattern
 * which inverts control of object creation. Instead of creating objects directly, they are created by a DI container
 * and provided to each object that requires them.
 *
 * ## Using DI
 *
 * Using dependency injection depends on your target platform. On JVM platforms, elide uses Micronaut as an injection
 * engine and DI container, but the annotations used are standard and may be used with whatever engine the developer
 * chooses.
 *
 * This annotation, on JVM platforms, is aliased to the standard Jakarta inject annotation:
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/inject
 *
 * See other annotations from Jakarta supported by Elide:
 * - [Named]: Naming for injectable value qualifiers
 * - [Qualifier]: Qualification for injectable values
 * - [Singleton]: Singleton lifecycle restriction
 */
public actual typealias Inject = jakarta.inject.Inject

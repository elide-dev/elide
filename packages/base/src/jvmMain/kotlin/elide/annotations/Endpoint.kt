package elide.annotations

import jakarta.inject.Singleton


/**
 * Marks a class as an API endpoint, which enables functionality for type conversion between [elide.model.WireMessage]
 * types and Micronaut requests / responses.
 *
 * [Endpoint] should be used in conjunction with other Micronaut annotations, like `@Controller`. Classes marked as
 * endpoints automatically participate in DI as [Singleton]s.
 */
@Singleton
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Endpoint

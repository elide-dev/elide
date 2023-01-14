package elide.annotations

/**
 * # Annotation: Named
 *
 * This annotation works in cooperation with [Inject] to qualify injected values via simple string names. "Qualified"
 * values are filtered by their qualification criteria at injection time. In this case, we are filtering by a simple
 * name which should correspond with a name of equal value on an injected instance.
 *
 * Qualifiers can be further customized or filtered via other annotations, such as [Qualifier]. On JVM platforms, this
 * annotation is aliased to a standard Jakarta `Named` annotation:
 *
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/named
 *
 * @see Inject to mark a value as injected.
 */
public expect annotation class Named

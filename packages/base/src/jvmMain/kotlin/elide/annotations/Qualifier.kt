package elide.annotations

/**
 * # Annotation: Qualifier (JVM)
 *
 * This annotation works in cooperation with [Inject] to qualify an injectable value by some criteria. Qualifiers can
 * be affixed to any injectable value; the annotation values at the call-site must match the values on an eligible
 * instance for injection.
 *
 * Other qualifier annotations exist, such as [Named]. On JVM platforms, this annotation is aliased to a standard
 * Jakarta `Qualifier` annotation:
 *
 * https://jakarta.ee/specifications/dependency-injection/2.0/apidocs/jakarta/inject/qualifier
 */
public actual typealias Qualifier = jakarta.inject.Qualifier

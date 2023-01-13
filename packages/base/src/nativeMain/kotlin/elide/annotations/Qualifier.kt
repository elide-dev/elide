package elide.annotations

/**
 * # Annotation: Qualifier (Native)
 *
 * This annotation works in cooperation with [Inject] to qualify an injectable value by some criteria. Qualifiers can
 * be affixed to any injectable value; the annotation values at the call-site must match the values on an eligible
 * instance for injection.
 *
 * Other qualifier annotations exist, such as [Named].
 */
public actual annotation class Qualifier

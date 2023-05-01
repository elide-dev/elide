package elide.annotations

/**
 * # Annotation: Named (WASM)
 *
 * This annotation works in cooperation with [Inject] to qualify injected values via simple string names. "Qualified"
 * values are filtered by their qualification criteria at injection time. In this case, we are filtering by a simple
 * name which should correspond with a name of equal value on an injected instance.
 *
 * Qualifiers can be further customized or filtered via other annotations, such as [Qualifier].
 *
 * @see Inject to mark a value as injected.
 */
public actual annotation class Named

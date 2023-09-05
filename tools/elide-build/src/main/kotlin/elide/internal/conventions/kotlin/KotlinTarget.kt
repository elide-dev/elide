package elide.internal.conventions.kotlin

public sealed interface KotlinTarget {
  public data object JsBrowser : KotlinTarget
  public data object JsNode : KotlinTarget
  public data object Native : KotlinTarget
  public data object JVM : KotlinTarget
  public data object WASM : KotlinTarget

  @JvmInline public value class Multiplatform(
    public val targets: Array<KotlinTarget>,
  ) : KotlinTarget

  public operator fun plus(other: KotlinTarget): Multiplatform = when (other) {
    is Multiplatform -> Multiplatform(other.targets + this)
    else -> Multiplatform(arrayOf(this, other))
  }

  public operator fun contains(other: KotlinTarget): Boolean = when (this) {
    is Multiplatform -> other in targets
    else -> other == this
  }

  public companion object {
    public val All: KotlinTarget = Multiplatform(arrayOf(JVM, JsBrowser, JsNode, Native))
  }
}
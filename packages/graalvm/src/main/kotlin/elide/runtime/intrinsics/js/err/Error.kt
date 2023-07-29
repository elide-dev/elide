package elide.runtime.intrinsics.js.err

import elide.vm.annotations.Polyglot

/** TBD. */
public abstract class Error () : AbstractJSException, RuntimeException() {
  /**
   * TBD.
   */
  @get:Polyglot public abstract override val message: String

  /**
   * TBD.
   */
  @get:Polyglot public abstract val name: String

  /**
   * TBD.
   */
  @get:Polyglot public override val cause: Error? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public open val fileName: String? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public open val lineNumber: Int? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public open val columnNumber: Int? get() = null

  /**
   * TBD.
   */
  @get:Polyglot public val stackTrace: Stacktrace get() {
    TODO("not yet implemented")
  }
}

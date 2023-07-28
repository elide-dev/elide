package elide.tool.cli

/** Specifies languages supported for REPL access. */
internal enum class GuestLanguage (
  internal val id: String,
  internal val formalName: String,
  internal val experimental: Boolean = false,
  internal val unimplemented: Boolean = false,
  internal val extensions: List<String> = emptyList(),
  internal val mimeTypes: List<String> = emptyList(),
) : elide.runtime.gvm.GuestLanguage {
  /** Interactive JavaScript VM. */
  JS (
    id = "js",
    formalName = "JavaScript",
    experimental = true,
    extensions = listOf("js", "cjs", "mjs"),
    mimeTypes = listOf("application/javascript", "application/ecmascript"),
  ),

  /** Interactive Python VM. */
  PYTHON (
    id = "py",
    formalName = "Python",
    experimental = true,
    unimplemented = true,
    extensions = listOf("py"),
    mimeTypes = emptyList(),
  ),

  /** Interactive Python VM. */
  RUBY (
    id = "rb",
    formalName = "Ruby",
    experimental = true,
    unimplemented = true,
    extensions = listOf("rb"),
    mimeTypes = emptyList(),
  ),

  /** Interactive nested JVM. */
  JVM (
    id = "jvm",
    formalName = "JVM",
    experimental = true,
    unimplemented = true,
    extensions = listOf("java"),
    mimeTypes = emptyList(),
  ),

  /** Interactive nested JVM with Kotlin support. */
  KOTLIN (
    id = "kt",
    formalName = "Kotlin",
    experimental = true,
    unimplemented = true,
    extensions = listOf("kt", "kts"),
    mimeTypes = emptyList(),
  ),

  /** Interactive nested JVM. */
  WASM (
    id = "wasm",
    formalName = "WASM",
    experimental = true,
    extensions = listOf("wasm"),
    mimeTypes = listOf("application/wasm"),
  );

  companion object {
    /** @return Language based on the provided ID, or `null`. */
    internal fun resolveFromId(id: String): GuestLanguage? = when (id) {
      JS.id -> JS
      PYTHON.id -> PYTHON
      RUBY.id -> RUBY
      JVM.id -> JVM
      KOTLIN.id -> KOTLIN
      WASM.id -> WASM
      else -> null
    }
  }

  /** @inheritDoc */
  override val symbol: String get() = id

  /** @inheritDoc */
  override val label: String get() = formalName
}

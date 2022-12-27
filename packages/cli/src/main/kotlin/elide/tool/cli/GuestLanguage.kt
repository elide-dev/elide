package elide.tool.cli

/** Specifies languages supported for REPL access. */
internal enum class GuestLanguage constructor (
  internal val id: String,
  internal val formalName: String,
  internal val experimental: Boolean = false,
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
  );

  companion object {
    /** @return Language based on the provided ID, or `null`. */
    internal fun resolveFromId(id: String): GuestLanguage? = when (id) {
      JS.id -> JS
      else -> null
    }
  }

  /** @inheritDoc */
  override val symbol: String get() = id

  /** @inheritDoc */
  override val label: String get() = formalName
}

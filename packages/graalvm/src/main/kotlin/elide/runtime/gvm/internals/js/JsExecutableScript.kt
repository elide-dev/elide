package elide.runtime.gvm.internals.js

import java.util.*
import elide.runtime.gvm.ExecutableScript.ScriptSource
import elide.runtime.gvm.ExecutableScript.ScriptType
import elide.runtime.gvm.InvocationMode
import elide.runtime.gvm.internals.AbstractGVMScript
import elide.runtime.gvm.internals.GraalVMGuest.JAVASCRIPT

/** Implementation of an [AbstractGVMScript] for the [JsRuntime]. */
internal class JsExecutableScript private constructor (
  source: Pair<ScriptSource, String>,
  private val scriptType: ScriptType? = null,
) : AbstractGVMScript(JAVASCRIPT, source.first, source.second) {
  internal companion object {
    /** Mime type for regular JS. */
    private const val MIME_TYPE_JS = "application/javascript"

    /** Mime type for TypeScript. */
    private const val MIME_TYPE_TS = "application/typescript"

    /** MIME type for ECMA modules. */
    private const val MIME_TYPE_JS_MODULE = "application/javascript+module"

    /** Extension expected for JS modules. */
    const val JS_MODULE_EXTENSION = ".mjs"

    /** Extension expected for TS files. */
    const val TS_MODULE_EXTENSION = ".ts"

    /** Script type designation for a regular JS script. */
    internal val JS_SCRIPT = ScriptType.fromMime(MIME_TYPE_JS)

    /** Script type designation for a TypeScript script. */
    internal val TS_SCRIPT = ScriptType.fromMime(MIME_TYPE_TS)

    /** Script type designation for a JS module. */
    internal val JS_MODULE = ScriptType.fromMime(MIME_TYPE_JS_MODULE)

    /** @return JavaScript executable script wrapping the provided [ScriptSource]. */
    @JvmStatic internal fun of(source: ScriptSource, spec: String): JsExecutableScript = JsExecutableScript(
      source to spec
    )

    /** @return JavaScript executable script wrapping the provided [ScriptSource] and explicit [type]. */
    @JvmStatic internal fun of(source: ScriptSource, type: ScriptType, spec: String): JsExecutableScript =
      JsExecutableScript(source to spec, type)
  }

  /** @inheritDoc */
  override fun invocation(): EnumSet<InvocationMode> = EnumSet.allOf(
    InvocationMode::class.java
  )

  /** @inheritDoc */
  override fun type(): ScriptType = when {
    scriptType != null -> scriptType
    source().extension?.endsWith(JS_MODULE_EXTENSION) == true -> JS_MODULE
    source().extension?.endsWith(TS_MODULE_EXTENSION) == true -> TS_SCRIPT
    else -> JS_SCRIPT
  }
}

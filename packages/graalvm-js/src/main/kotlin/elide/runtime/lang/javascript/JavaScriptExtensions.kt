package elide.runtime.lang.javascript

import com.oracle.js.parser.ir.Module.ModuleRequest
import elide.runtime.gvm.loader.ModuleInfo

/**
 * Create a [ModuleInfo] from a [ModuleRequest].
 *
 * @param req The [ModuleRequest] to create a [ModuleInfo] from.
 * @return The [ModuleInfo] corresponding to the [ModuleRequest].
 */
internal fun ModuleInfo.Companion.from(req: ModuleRequest): ModuleInfo = requireNotNull(
  allModuleInfos[req.specifier.toString().substringAfter(':')]
) {
  "Module compile-time name '${req.specifier}' not found"
}

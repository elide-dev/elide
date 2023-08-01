package elide.runtime.gvm.js

import io.micronaut.http.HttpRequest
import java.io.InputStream
import java.io.Reader
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.RequestExecutionInputs
import elide.runtime.gvm.internals.js.JsExecutableScript
import elide.runtime.gvm.internals.js.JsMicronautRequestExecutionInputs
import elide.runtime.gvm.js.JavaScript.embedded
import elide.runtime.gvm.js.JavaScript.literal
import elide.ssr.type.RequestState

/**
 * # Guest VM: JavaScript
 *
 * Convenience entrypoints into JavaScript guest types, including JS embedded and literal scripts. After obtaining an
 * [ExecutableScript] instance, guest code can be executed by calling any of the "execute" methods present on a
 * JavaScript Runtime instance. These may be obtained from the relevant VM facade factory.
 *
 * ## Literal scripts
 *
 * Literal code can be wrapped in an [ExecutableScript] via the [literal] family of methods, which accept string types,
 * [InputStream], and [Reader]. Literal code will be given a special file-name for debugging purposes.
 *
 * ## Embedded scripts
 *
 * Scripts embedded within the host application can be referenced via the [embedded] series of methods. "Embedded"
 * scripts are retrieved from the host app classpath via Elide's built-in asset mechanisms. These should usually be used
 * in tandem with Elide's Gradle plugin, or related build infrastructure, which packs embedded scripts into a consistent
 * place in the classpath.
 *
 * ## File scripts
 *
 * An external file can be loaded via the [file] series of methods. Note that these methods always pull from Host I/O,
 * as they are executed outside the guest VM. Once executing, a script can only "see" or access I/O according to the
 * applied guest I/O policy.
 */
@Suppress("unused", "UNUSED_PARAMETER")
public object JavaScript {
  /**
   * ## JavaScript: Literal from string.
   *
   * Wrap the provided JavaScript [code] in an [ExecutableScript] which can be prepared for execution.
   *
   * @param code Code to wrap.
   * @return Executable script for the provided code snippet.
   */
  @JvmStatic public fun literal(code: CharSequence): ExecutableScript {
    return JsExecutableScript.of(
      ExecutableScript.ScriptSource.LITERAL,
      code.toString(),
    )
  }

  /**
   * ## JavaScript: Embedded in classpath.
   *
   * Load JavaScript code from the file at the provided [path] in the host app classpath, and wrap the result in an
   * [ExecutableScript] which can be prepared for execution.
   *
   * @param path Path to load.
   * @return Executable script for the provided embedded script.
   */
  @JvmStatic public fun embedded(path: String): ExecutableScript {
    return JsExecutableScript.of(
      ExecutableScript.ScriptSource.fromEmbedded(path),
      path,
    )
  }

  /** Utilities for creating JavaScript execution inputs. */
  public object Inputs {
    /** Reference to the singleton for empty inputs. */
    @JvmStatic public val EMPTY: ExecutionInputs = ExecutionInputs.EMPTY

    /**
     * TBD.
     */
    @JvmStatic public fun <S : Any> requestState(
      state: RequestState,
      props: S? = null,
    ): RequestExecutionInputs<HttpRequest<Any>> {
      @Suppress("UNCHECKED_CAST")
      return JsMicronautRequestExecutionInputs.of(
        state.request as HttpRequest<Any>,
        props,
      )
    }
  }
}

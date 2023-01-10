package elide.runtime.gvm.internals.js

import elide.runtime.gvm.internals.GVMInvocationBindings
import elide.runtime.gvm.internals.InvocationBindings
import java.util.EnumSet
import org.graalvm.polyglot.Value as GuestValue

/**
 * # JS: Invocation Bindings
 *
 * Implementation of [InvocationBindings] for the [JsRuntime]. Charged with resolving a set of invocation bindings from
 * an input script and evaluated [GuestValue] pair. In essence, this involves crawling what the user hands back to us
 * from guest code, matching that structure to an invocation style, and then wiring together the appropriate bindings
 * to dispatch those exports.
 *
 * &nbsp;
 *
 * ## Supported dispatch styles
 *
 * JavaScript guest code can be invoked in a number of different ways, depending on the shape of the exported bindings
 * and the intended purpose of the guest application. The simplest approach is to export an anonymous function (async or
 * non-async), which will be invoked for each event. This is the "unary" dispatch style. Even if the exported function
 * is `async`, no result is available until the function totally completes.
 *
 * In the unary dispatch style, only one return value may be provided. Exceptions halt all processing. If the exported
 * function is `async`, the resulting value will be unwrapped from a promise (the invocation API host-side already uses
 * a Promise-style structure, regardless of the exported function's async-ness).
 *
 * More complex or specialized dispatch styles are also available. For example, for streaming server SSR, a `fetch`
 * function can be exported which receives a `request` and a `responder`. The `request` may be used to access server
 * request context for facilitating SSR execution; the `responder` may be used to stream a response.
 *
 * **Exhaustive list of dispatch styles supported by the JavaScript runtime:**
 *
 * - **Unary:** Export a function (either `async` or non-async), which is dispatched for each VM invocation event. The
 *   function does not need to be named.
 *
 * - **Server:** Export an object which includes the function `fetch`. The function should accept at least one argument,
 *   which is the `request` invoking the script. With this export layout, the JavaScript bindings assume a server SSR
 *   execution is expected. The `request` object takes the shape of a standard Fetch API request.
 *
 * - **Render:** Export an object which includes the function `render`. This function should accept up to three
 *   arguments: a `request` (like `fetch`), a `responder` which can accept streamed response content, and a `context`
 *   parameter which provides user props ("state") and other inputs. Once streamed content has concluded, the
 *   request/response cycle must be terminated by returning a `Response`. The final `Response` does not need to specify
 *   a body, `Content-Length`, or `Content-Type`; these are all handled by the streaming response `responder`.
 *
 * ### Unary binding example
 * ```javascript
 * export default async function something() {
 *   return "...";
 * }
 * ```
 *
 * ### Server binding example
 * ```javascript
 * export default {
 *   async fetch(request) {
 *     return new Response(200);
 *   };
 * }
 * ```
 *
 * ### Render binding example
 * ```javascript
 * export default {
 *   async render(request, responder) {
 *     // send content via `responder`, return a `Response` to terminate
 *     return new Response(200);
 *   }
 * }
 * ```
 *
 * **Note:** Multiple bindings can be combined on an `export default {...}` if desired. In this operating mode, the
 * provided `render` function, if any, executes in a sidecar SSR circumstance; when run directly, `fetch` is executed
 * instead.
 *
 * &nbsp;
 *
 * ## Safety of bindings
 *
 * [JsInvocationBindings] are tied to the VM context which they originate from. This means that execution of bindings,
 * and indeed even interrogating binding values, must be confined to the same native thread as the VM context. For this
 * reason, it is expected that only the VM implementation will touch internal binding state.
 *
 * @param modes Supported dispatch modes for this invocation bindings instance.
 * @param mapped Resolved values for each binding.
 * @param types Entrypoint types expressed in [mapped].
 */
internal sealed class JsInvocationBindings constructor (
  internal val mapped: Map<EntrypointInfo, JsEntrypoint>,
  private val modes: EnumSet<DispatchStyle>,
  internal val types: EnumSet<JsEntrypointType>,
) : GVMInvocationBindings<JsInvocationBindings, JsExecutableScript>() {
  /**
   * Internal constructor: Raw value.
   *
   * Constructs a JS invocation binding type from a resolved [value], a [path] to access the value from the evaluated
   * top-most script return value (if any), and the [type] of that invocation binding. This constructor is used when the
   * exported script bindings are simple (only one binding type).
   *
   * @param value Guest value resolved for this binding type.
   * @param path Full JavaScript path to the binding, from the top-most script return value (if any).
   * @param type Type of entrypoint represented by this binding.
   * @param name Name of the binding entrypoint, if any.
   */
  constructor (
    modes: EnumSet<DispatchStyle>,
    value: GuestValue,
    path: List<String>,
    type: JsEntrypointType,
    name: String?,
  ) : this(
    modes = modes,
    types = EnumSet.of(type),
    mapped = EntrypointInfo(type, name).let { entrypointInfo ->
    mapOf(
      entrypointInfo to JsEntrypoint(entrypointInfo, path, value)
    )
  })

  /** Enumerates types of resolved JavaScript entrypoints; a [JsInvocationBindings] sub-class exists for each. */
  internal enum class JsEntrypointType {
    /** Indicates a regular JavaScript function. */
    FUNCTION,

    /** Indicates an asynchronous JavaScript function, which returns a `Promise`. */
    ASYNC_FUNCTION,

    /** Indicates a server-capable interface, which exports a `fetch` function (async). */
    SERVER_ASYNC,

    /** Indicates an SSR-capable interface, which exports a `render` function (async). */
    RENDER_ASYNC,

    /** Special type of entrypoint which indicates support for multiple [JsEntrypointType]s. */
    COMPOUND,
  }

  /**
   * ## Entrypoint info.
   *
   * Used as a key in a mapping of JavaScript entrypoints to their resolved [GuestValue] instances.
   *
   * @param type JavaScript entrypoint type specified by this info key.
   * @param name Name of the function, or entrypoint, etc. Defaults to `null` (anonymous).
   */
  internal data class EntrypointInfo(
    internal val type: JsEntrypointType,
    internal val name: String? = null,
  )

  /**
   * ## Entrypoint spec.
   *
   * Describes a JavaScript entrypoint, including the basic (serializable) [EntrypointInfo], and any additional context
   * needed to dispatch the entrypoint.
   *
   * @param info Basic serializable and comparable info about the entrypoint.
   * @param path Full path to the entrypoint.
   * @param value Guest value resolved for, and corresponding to, this entrypoint. Should be executable.
   */
  internal class JsEntrypoint(
    internal val info: EntrypointInfo,
    internal val path: List<String>,
    internal val value: GuestValue,
  )

  /** Resolves JavaScript invocation binding based on an input `script` and guest `value`. */
  internal companion object Factory : Resolver<JsExecutableScript, JsInvocationBindings> {
    // Resolve the target `value` as a JavaScript function binding.
    @JvmStatic private fun resolveFunction(value: GuestValue, pathPrefix: List<String>): JsInvocationBindings {
      // get the meta object for this object, which should be named `AsyncFunction` if the function is async. if not,
      // then we were handed a normal function.
      val isAsync = when (val meta = value.metaObject) {
        null -> false  // have to assume it is not async if the meta-object was false
        else -> meta.metaSimpleName == "AsyncFunction"
      }
      val fnName = if (value.hasMembers() && value.hasMember("name")) {
        value.getMember("name")?.asString()
      } else {
        null
      }
      return JsFunction(
        value = value,
        async = isAsync,
        name = fnName,
        path = if (fnName != null) {
          pathPrefix.plus(listOf(fnName))
        } else {
          pathPrefix
        }
      )
    }

    // Determine if the provided guest function is async.
    private fun functionIsAsync(target: GuestValue): Boolean {
      return when (val meta = target.metaObject) {
        null -> false  // have to assume it is not async if the meta-object was false
        else -> meta.metaSimpleName == "AsyncFunction"
      }
    }

    // Resolve the target `entry`point as an exported module object.
    @JvmStatic private fun resolveObject(entry: GuestValue): JsInvocationBindings = when {
      // are there multiple default exports? if so, we will need to resolve them all into a compound binding.
      entry.memberKeys.size > 1 -> {
        var expected = 0
        val hasServer = entry.hasMember("fetch")
        if (hasServer) expected += 1
        val hasRender = entry.hasMember("render")
        if (hasRender) expected += 1
        if (expected == 0) error("No usable bindings found for `default` export from guest script")
        val bindings = ArrayList<JsInvocationBindings>(expected)

        // resolve server binding
        if (hasServer) {
          val binding = entry.getMember("fetch") ?: error("Failed to resolve detected `fetch` binding")

          bindings.add(JsServer(
            value = binding,
            async = functionIsAsync(binding),
          ))
        }

        // resolve render binding
        if (hasRender) {
          val binding = entry.getMember("render") ?: error("Failed to resolve detected `render` binding")
          bindings.add(JsRender(
            value = binding,
            async = functionIsAsync(binding),
          ))
        }

        JsCompound(
          entry,
          bindings,
        )
      }

      // first up: `render`. if this is present, the script is exporting a binding for an SSR sidecar execution.
      entry.hasMember("render") -> entry.getMember("render").let { renderEntry ->
        // the returned entrypoint must be executable
        if (!renderEntry.canExecute()) error("`render` must be an async function")
        JsRender(
          value = renderEntry,
          async = functionIsAsync(renderEntry),
        )
      }

      // next up: `fetch`. if this is present, the script is exporting a binding for a server.
      entry.hasMember("fetch") -> entry.getMember("fetch").let { fetchEntry ->
        // the returned entrypoint must be executable
        if (!fetchEntry.canExecute()) error("`fetch` must be an async function")
        JsServer(
          value = fetchEntry,
          async = functionIsAsync(fetchEntry),
        )
      }

      // if we arrive at this error, there was an `export default {...}`, but it did not provide any named methods
      // which we were able to recognize.
      else -> error("No supported binding found on `export default` provided by guest script")
    }

    /**
     * ## Implementation API: Resolve.
     *
     * Given a prepared [script] and a [GuestValue] resulting from evaluation of the script, resolve the available set
     * of exported [JsInvocationBindings] available for dispatch.
     *
     * @param script The script which was evaluated to produce the provided [value].
     * @param value Guest value addressing the evaluated script.
     * @return Resolved set of bindings (of type [JsInvocationBindings]) which are exported from the script and made
     *   available for host dispatch.
     */
     @JvmStatic override fun resolve(script: JsExecutableScript, value: GuestValue): JsInvocationBindings = when {
      // if the exported value can be executed directly, then we were handed a function.
      value.canExecute() -> resolveFunction(value, emptyList())

      // if the exported value has members, there are typically exports to traverse.
      value.hasMembers() -> {
        // typically, we're only looking for the `default` export from the entrypoint.
        if (!value.hasMember("default")) error("Guest script has no exported `default`")

        // grab the entrypoint and traverse to discover the exported bindings.
        val entry = value.getMember("default")
        when {
          // if the entrypoint itself can be executed, there was an `export default function() {}`... so far so good.
          entry.canExecute() -> resolveFunction(entry, listOf("default"))

          // if the entrypoint has members, it means there was an `export default {}`... so far so good.
          entry.hasMembers() -> resolveObject(entry)

          // if we arrive at this error, the exported value was neither an object, nor a function, so we don't know what
          // to do with it.
          else -> error("Failed to resolve entrypoint from default export in guest script")
        }
      }

      // if we arrive at this error, the guest value we are resolving from is both not module-like (it doesn't have
      // members) and not function like, so we don't know what to do with it.
      else -> error("Guest script evaluated to unrecognized value: not a function or exported object")
    }

    /**
     * ## Implementation API: Supported modes.
     *
     * Interrogates a set of resolved [JsInvocationBindings] for a given [JsExecutableScript] to determine the set of
     * supported dispatch modes for that script.
     *
     * Depending on the inputs for a given VM execution, a VM adapter then selects how to dispatch a given script based
     * on the resulting bindings and dispatch modes.
     *
     * @param script The script which was evaluated to produce the provided [bindings].
     * @param bindings Set of materialized bindings resolved for the provided [script].
     * @return Set of supported dispatch modes for the provided [script] and [bindings].
     */
    @JvmStatic override fun supported(script: JsExecutableScript, bindings: JsInvocationBindings): Set<DispatchStyle> {
      return bindings.supported()
    }
  }

  /**
   * Internal: Indicate supported dispatch styles for a set of bindings.
   *
   * Returns a set of supported dispatch modes for a given [JsInvocationBindings] instance.
   *
   * @return Set of supported dispatch modes.
   */
  internal fun supported(): Set<DispatchStyle> = modes

  /**
   * ## JavaScript Bindings: Function.
   *
   * Describes an entrypoint to a JavaScript guest script which is implemented with an asynchronous JavaScript function.
   * The function is expected to return a value which makes sense for its use case (for instance, a string or an HTTP
   * server response). If the function has a name, it should be provided via `name`; the full path to the function,
   * according to ESM or CommonJS import rules, should be provided via `path`.
   *
   * @param value Guest value implementing this function.
   * @param name Name of the function, if any (mostly used for logging).
   * @param path Full path to the function, according to ESM or CommonJS import rules, as applicable.
   */
  internal class JsFunction (
    value: GuestValue,
    async: Boolean,
    name: String?,
    path: List<String>,
  ) : JsInvocationBindings(
    name = name,
    value = value,
    path = path,
    type = if (async) JsEntrypointType.ASYNC_FUNCTION else JsEntrypointType.FUNCTION,
    modes = EnumSet.of(DispatchStyle.UNARY),
  )

  /**
   * ## JavaScript Bindings: Server.
   *
   * Describes an entrypoint to a JavaScript guest script which is implemented with an exported `fetch` function. The
   * function must be `async`; it is expected to return a `Response`, given a `Request`.
   *
   * The function name, in this case, should always be `fetch`, so it is omitted. The full path to the function,
   * according to spec, is `default.fetch`, so this is also omitted.
   *
   * @param value Guest value implementing this function.
   * @param async Whether the function operates asynchronously (returns a `Promise`).
   */
  internal class JsServer (value: GuestValue, async: Boolean) : JsInvocationBindings(
    value = value,
    name = "fetch",
    path = listOf("default", "fetch"),
    type = if (async) JsEntrypointType.SERVER_ASYNC else error("Exported `fetch` method must be `async`"),
    modes = EnumSet.of(DispatchStyle.SERVER),
  )

  /**
   * ## JavaScript Bindings: Render.
   *
   * Describes an entrypoint to a JavaScript guest script which is implemented with an exported `render` function. The
   * function must be `async`; it is expected to return a `Response`, given a `Request`. The function must accept a
   * second parameter and third parameter; the second is called the `responder`, which can send chunked streaming
   * responses before a response concludes; the third is called the `context`, which provides user props ("state") and
   * other useful inputs.
   *
   * The function name, in this case, should always be `render`, so it is omitted. The full path to the function,
   * according to convention, is `default.render`, so this is also omitted.
   *
   * @param value Guest value implementing this function.
   * @param async Whether the function operates asynchronously (returns a `Promise`).
   */
  internal class JsRender (value: GuestValue, async: Boolean) : JsInvocationBindings(
    value = value,
    name = "render",
    path = listOf("default", "render"),
    type = if (async) JsEntrypointType.RENDER_ASYNC else error("Exported `render` method must be `async`"),
    modes = EnumSet.of(DispatchStyle.RENDER),
  )

  /**
   * ## JavaScript Bindings: Compound.
   *
   * Describes a set of bindings which are composed of other bindings. This class is used to express any compatible
   * combination of [JsInvocationBindings] which are present in a single guest script. "Compatible" in this case means
   * that the presence of [JsFunction] is an error (since that type represents an exported naked function, which fully
   * precludes any other exports).
   *
   * @param base Base object which includes these bindings. Expected to be a `default` export.
   * @param bindings Set of bindings to compose.
   */
  internal class JsCompound (
    base: GuestValue,
    private val bindings: List<JsInvocationBindings>,
  ) : JsInvocationBindings(
    value = base,
    name = null,
    path = listOf("default"),
    type = JsEntrypointType.COMPOUND,
    modes = EnumSet.copyOf(bindings.flatMap { it.supported() }),
  )
}

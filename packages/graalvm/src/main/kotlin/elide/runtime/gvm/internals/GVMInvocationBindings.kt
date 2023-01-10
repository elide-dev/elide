package elide.runtime.gvm.internals

import elide.runtime.gvm.ExecutableScript
import org.graalvm.polyglot.Value as GuestValue

/**
 * # GraalVM: Invocation bindings.
 *
 * Abstract base implementation of [InvocationBindings] based on GraalVM guest values ([GuestValue]). Guest invocation
 * bindings describe methods for invoking guest code from Elide. Invocation bindings are specialized per-language, but
 * generally map to a uniform set of "dispatch modes;" these modes describe, in basic language-agnostic form, how guest
 * code should be called.
 *
 * &nbsp;
 *
 * ## Dispatch modes
 *
 * Dispatch modes supported on GraalVM are enumerated in [DispatchStyle], and, at the time of this writing, include:
 *
 * - [DispatchStyle.UNARY]: Basic dispatch with one set of inputs and one output. In this mode, the guest VM executes
 *   in what amounts to be a "function". No output is available until the guest execution terminates.
 *
 * - [DispatchStyle.SERVER]: Dispatch of HTTP requests to the guest. Each HTTP request results in an invocation of the
 *   guest code. In this mode, the guest VM is entirely "in charge" of the request and response lifecycle. The server
 *   interface is wire-compatible with CloudFlare Workers.
 *
 * - [DispatchStyle.RENDER]: Sidecar dispatch of SSR render code, which is designed to operate in tandem with another
 *   guest or the host application. In this mode, the primary output is effectively a big string (although it may be
 *   streamed in chunks). In this mode, the guest VM is charged with rendering output based on input, but is not
 *   ultimately authoritative over the response or the request lifecycle.
 */
internal abstract class GVMInvocationBindings<Bindings, Script> : InvocationBindings
  where Bindings : GVMInvocationBindings<Bindings, Script>,
        Script: ExecutableScript {
  /** Enumerates dispatch styles supported by this implementation. */
  enum class DispatchStyle {
    /** Regular "unary"-style dispatch, with a single source execution and output. */
    UNARY,

    /** Dispatch for guest-defined servers. */
    SERVER,

    /** Sidecar dispatch for SSR rendering. */
    RENDER,
  }

  /**
   * ## Entrypoint spec.
   *
   * Describes a triple of a [script] (type [Script] of [ExecutableScript]), a set of resolved [bindings] (type
   * [Bindings] of [InvocationBindings]), and a suite of [DispatchStyle] modes supported by the script and bindings.
   *
   * @param script Script implementation for this entrypoint.
   * @param bindings Bindings resolved from the attached [script].
   * @param dispatch Available dispatch modes for the attached [script] and [bindings].
   */
  internal class Entrypoint<Bindings, Script>(
    internal val script: Script,
    internal val bindings: Bindings,
    internal val dispatch: Set<DispatchStyle>,
  )

  /**
   * ## Invocation Bindings: Resolver.
   *
   * Describes the expected layout of the companion class for an [InvocationBindings] implementation; this includes
   * methods to resolve a set of bindings for a given evaluated script.
   */
  internal interface Resolver<Script, Bindings>
    where Bindings : GVMInvocationBindings<Bindings, Script>,
          Script: ExecutableScript {
    /**
     * ## Implementation API: Resolve.
     *
     * Given a prepared [script] and a [GuestValue] resulting from evaluation of the script, resolve the available set
     * of exported [Bindings] available for dispatch.
     *
     * @param script The script which was evaluated to produce the provided [value].
     * @param value Guest value addressing the evaluated script.
     * @return Resolved set of bindings (of implementation type [Bindings]) which are exported from the script and made
     *   available for host dispatch.
     */
    fun resolve(script: Script, value: GuestValue): Bindings

    /**
     * ## Implementation API: Supported modes.
     *
     * Interrogates a set of resolved [Bindings] for a given [Script] to determine the set of supported [DispatchStyle]
     * modes for that script. Available modes are [DispatchStyle.UNARY], [DispatchStyle.SERVER], and
     * [DispatchStyle.RENDER] at the time of this writing.
     *
     * Depending on the inputs for a given VM execution, an [AbstractVMAdapter] then selects how to dispatch a given
     * [Script] based on the resulting [Bindings] and [DispatchStyle] modes.
     *
     * @param script The script which was evaluated to produce the provided [bindings].
     * @param bindings Set of materialized bindings resolved for the provided [script].
     * @return Set of supported [DispatchStyle] modes for the provided [script] and [bindings].
     */
    fun supported(script: Script, bindings: Bindings): Set<DispatchStyle>

    /**
     * ## Bindings: Entrypoint.
     *
     * Given a prepared [script] and evaluated [value] result from that script, resolve an [Entrypoint] configuration,
     * which includes a set of materialized [Bindings] and discovered [DispatchStyle] modes. The return value of this
     * method can be cached for the lifetime of a given [script].
     *
     * This method dispatches [resolve], as needed, and [supported], to determine the return value.
     *
     * @param script Script to resolve an entrypoint for.
     * @param value Guest value from evaluating the script.
     * @return Entrypoint configuration for the provided [script] and [value].
     */
    fun entrypoint(script: Script, value: GuestValue): Entrypoint<Bindings, Script> {
      val bindings = resolve(script, value)
      val modes = supported(script, bindings)
      return Entrypoint(script, bindings, modes)
    }
  }
}

package elide.runtime.gvm.internals

/**
 * # Guest: Invocation Bindings.
 *
 * Sealed base interface for guest "invocation bindings," which describes a method for invoking guest code from Elide.
 * Invocation bindings are specialized per-language, but generally map to a uniform set of "dispatch modes;" these modes
 * describe, in basic language-agnostic form, how guest code should be called.
 *
 * &nbsp;
 *
 * ## Dispatch modes
 *
 * Dispatch modes are implemented for each abstract base type of this sealed interface. At the time of this writing,
 * only one base exists, which is [GVMInvocationBindings] (invocation bindings based on GraalVM guests). For the modes
 * supported for GraalVM guests, see that class.
 *
 * &nbsp;
 *
 * ## Resolving bindings
 *
 * Typically, an [AbstractVMAdapter] is the object in charge of resolving a set of invocation bindings from a given
 * script. Then, inputs for the script are matched against the available bindings to determine how best to invoke guest
 * code. When ready, the bindings are exercised to perform the execution, and a response is mediated (if necessary) by
 * the bindings.
 *
 * @see GVMInvocationBindings for a base implementation of invocation bindings for GraalVM-based guests.
 */
internal sealed interface InvocationBindings

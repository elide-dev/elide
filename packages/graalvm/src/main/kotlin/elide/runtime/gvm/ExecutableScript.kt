package elide.runtime.gvm

/**
 * # Executable Script
 *
 * Defines the interface which executable scripts must comply with in order to be compatible with Elide's Guest VM layer
 * and associated features. Executable scripts maintain an internal [State] describing their current point in the script
 * execution lifecycle.
 *
 * Elide doesn't care what language an executable script is written in, so long as it is described by a valid language
 * descriptor ([GuestLanguage]) and supported by the guest VM. Each step in the script lifecycle is described below. See
 * ancillary classes for more information.
 *
 * ## Script Lifecycle
 *
 * When a script is first created, it is initialized with a source target, language, and any relevant arguments, state,
 * or environment. As applicable, Elide may elect to prepend or append content before evaluating user code (in order to
 * produce a stable global environment). At this stage, the script is considered [State.UNINITIALIZED].
 *
 * Once the script has been assembled (linked), it is ready to be parsed/evaluated. At this stage, a VM is exclusively
 * acquired to parse and evaluate the script, and, at the successful conclusion of this initial execution, it moves into
 * the [State.PARSED] stage.
 *
 * Parsed scripts are no longer executing from source, but still need to be interrogated for a proper entrypoint. After
 * this stage completes, the script enters [State.EVALUATED] and is ready to be invoked. At this stage, the active VM is
 * released back into the pool, and the executable script is ready.
 *
 * At some point later (potentially immediately), the script is executed, which moves it into the [State.EXECUTED] stage
 * terminally. The script remains in this stage for the duration of the server's lifecycle, and is re-executed from the
 * cache for each invocation.
 *
 * ## Sealed Interface
 *
 * This interface is sealed to prevent direct external implementation; sub-classes are provided which describe common
 * script loading strategies. The developer may opt to extend one of these sub-classes, where needed and supported.
 *
 * @see GuestLanguage for the expected specification adhered to by guest language descriptors.
 * @see State for an exhaustive enumeration of states that a script may inhabit during its lifecycle.
 */
public sealed interface ExecutableScript {
  /**
   * ## State Lifecycle
   *
   * Enumerates each state that an executable guest script may inhabit during its lifecycle. The process of these states
   * is described in the main [ExecutableScript] docs.
   *
   * - [State.UNINITIALIZED]: The script has just been created and has not yet initialized at all.
   * - [State.PARSED]: The script has been linked and parsed, but has not been evaluated yet.
   * - [State.EVALUATED]: The script has been evaluated but has not been executed yet.
   * - [State.EXECUTED]: The script has executed before and is fully cached.
   */
  public enum class State {
    /** The script has just been created and has not yet initialized at all. */
    UNINITIALIZED,

    /** The script has been linked and parsed, but has not been evaluated yet. */
    PARSED,

    /** The script has been evaluated but has not been executed yet. */
    EXECUTED,

    /** The script has executed before and is fully cached. */
    EVALUATED,
  }

  /**
   * ## Script Type
   *
   */
  @JvmInline public value class ScriptType (private val spec: String) {

  }

  /**
   * ## Script Source
   *
   */
  @JvmInline public value class ScriptSource (private val target: String) {

  }

  /**
   * Indicate the guest language that this script is written in; it is expected that the returned [GuestLanguage] is
   * supported by the active VM, or no execution can take place.
   *
   * @return Guest language associated with this script.
   */
  public fun language(): GuestLanguage
}

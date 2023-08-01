package elide.runtime.gvm.internals

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.GuestScript
import org.graalvm.polyglot.Context as VMContext

/**
 * Abstract Guest VM script.
 *
 * Implements an [GuestScript] type  for use with GraalVM types, such as [GraalVMGuest] (enumerating a supported
 * language) and related framework types like [ExecutableScript].
 */
internal abstract class AbstractGVMScript protected constructor (
  private val language: GraalVMGuest,
  private val source: ExecutableScript.ScriptSource,
  protected val spec: String,
  map: ExecutableScript.SourceMap? = null,
) : GuestScript {
  internal companion object {
    /** Literal code source file name. */
    private const val LITERAL_SOURCE_NAME: String = "<literal>"
  }

  // Logging support.
  private val logging: Logger = Logging.of(AbstractGVMScript::class)

  /** Atomic internal state for this script. */
  private val currentState: AtomicReference<ExecutableScript.State> = AtomicReference(
    ExecutableScript.State.UNINITIALIZED
  )

  /** Whether the script has been populated (via [sourceContent]). */
  private val initialized: AtomicBoolean = AtomicBoolean(false)

  /** Loaded source content for this script, if available. */
  private val sourceContent: AtomicReference<Source> = AtomicReference(null)

  /** Atomic reference to the source map for this script, if available. */
  private val sourceMap: AtomicReference<ExecutableScript.SourceMap?> = AtomicReference(map)

  /** Cached evaluated script (thread-confined). */
  private val ref = ThreadLocal<Value>()

  /**
   * Protected access to update the current script state; should only be updated carefully, after exiting any active VM
   * contexts related to the operation which is updating the state.
   *
   * @param state Target state which the current script should now inhabit.
   */
  @Synchronized protected fun updateState(state: ExecutableScript.State) {
    currentState.set(state)
  }

  /** @inheritDoc */
  override fun state(): ExecutableScript.State = currentState.get()

  /** @inheritDoc */
  override fun language(): GuestLanguage = language

  /** @inheritDoc */
  override fun source(): ExecutableScript.ScriptSource = source

  /** @inheritDoc */
  override fun map(): ExecutableScript.SourceMap? = sourceMap.get()

  // Evaluate the attached script in the current thread, presumed to be the authoritative VM execution thread.
  private fun evaluateScriptForThread(context: VMContext): Value {
    require(initialized.get()) {
      "Cannot evaluate script before it is initialized"
    }
    val content = sourceContent.get() ?: error(
      "Failed to resolve source content for script '$this'"
    )
    val script = try {
      context.eval(content)
    } catch (exc: Exception) {
      throw exc
    }
    this.ref.set(script)
    return script
  }

  // Build a `Source` object from an input stream.
  private fun sourceFromStream(stream: InputStream, type: ExecutableScript.ScriptType): Source = type.charset().let {
    Source.newBuilder(language.symbol, stream.bufferedReader(it), source.filename)
      .interactive(false)
      .cached(true)  // @TODO(sgammon): needs attention for HMR
      .encoding(it)
      .build()
  }

  /**
   * Load the script source content, if needed.
   *
   * This is a one-time operation throughout the lifecycle of a given [AbstractGVMScript] object. Backing script content
   * is loaded from disk, the application classpath, or from a string literal.
   *
   * If the backing script content has already been loaded, this method is a no-op.
   *
   * @return Self.
   */
  internal open fun load(): AbstractGVMScript {
    if (!initialized.get()) {
      val scriptType = type()
      when {
        // if we're dealing with a literal code block, we can extract it directly from `spec`.
        source.isLiteral -> sourceContent.set(Source.newBuilder(language.symbol, spec, LITERAL_SOURCE_NAME)
          .mimeType(scriptType.asMimeType())
          .encoding(scriptType.charset())
          .cached(true)  // @TODO(sgammon): needs attention for HMR
          .interactive(false)
          .buildLiteral())

        source.isEmbedded -> AbstractGVMScript::class.java.getResourceAsStream("/" + source.path).use { stream ->
          sourceContent.set(sourceFromStream(stream ?: error(
            "Failed to locate embedded script resource: '/${source.path}'"
          ), type()))
        }

        source.isFile -> File(source.path).apply {
          if (!exists()) error("Failed to locate script file '${source.path}'")
          if (!canRead()) error("Failed to load script file '${source.path}': Not readable")
          inputStream().use { stream ->
            sourceContent.set(sourceFromStream(stream, type()))
          }
        }

        else -> error("Unidentified script source: $source")
      }
      initialized.set(true)
      updateState(ExecutableScript.State.PARSED)
    }
    return this
  }

  /**
   * Evaluate the script content, if needed.
   *
   * Processes the script content backing this [AbstractGVMScript], holding on to a cached reference to the evaluated
   * script result (which is visible from the current thread).
   *
   * @param context VM context to use to evaluate the script.
   * @return Resulting value from evaluating the script.
   */
  internal open fun evaluate(context: VMContext): Value {
    return when (state()) {
      // if the script is uninitialized, we can't evaluate it
      ExecutableScript.State.UNINITIALIZED -> error(
        "Cannot evaluate script that has not yet been initialized ($this)"
      )

      // if the script has been parsed but has not yet been evaluated, we can evaluate it to produce a hot
      // (context-bound) reference to the script's code. we need to be careful with this reference, though, because it
      // is confined to the thread that executes the VM, so we should stash it for hot access later.
      ExecutableScript.State.PARSED -> evaluateScriptForThread(context)

      // if the script has been evaluated, we can just return the cached reference to the script's code. if the script
      // reference isn't available within this thread, we messed up the thread confinement.
      ExecutableScript.State.EVALUATED, ExecutableScript.State.EXECUTED -> when (val value = ref.get()) {
        null -> {
          logging.warn(
            "Script reference not available when expected for thread '${Thread.currentThread().name}'. " +
              "This likely indicates a thread confinement failure for the VM executor."
          )
          evaluateScriptForThread(context)
        }
        else -> value
      }
    }
  }
}

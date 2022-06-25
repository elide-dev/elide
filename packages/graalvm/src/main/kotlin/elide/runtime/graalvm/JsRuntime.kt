package elide.runtime.graalvm

import com.google.common.annotations.VisibleForTesting
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Factory
import kotlinx.serialization.json.Json
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean


/** JavaScript embedded runtime logic, for use on the JVM. */
@Context class JsRuntime {
  companion object {
    // Singleton instance.
    private val singleton = JsRuntime()

    /** @return Static acquisition of the singleton JavaScript runtime. */
    @JvmStatic fun acquire(): JsRuntime = singleton

    /** @return SDK VM context pre-built for JavaScript execution. */
    @JvmStatic @Factory private fun initializeContext(): org.graalvm.polyglot.Context {
      return org.graalvm.polyglot.Context
        .newBuilder("js")
        .allowExperimentalOptions(true)
        .option("js.ecmascript-version", "2020")
        .option("js.v8-compat", "true")
        .build()
    }
  }

  /** Shortcuts for creating script descriptors. */
  @Suppress("unused") object Script {
    /** @return Embedded script container for the provided [path] (and [charset], defaulting to `UTF-8`). */
    @JvmStatic fun embedded(path: String, charset: Charset = StandardCharsets.UTF_8): EmbeddedScript = EmbeddedScript(
      path = path,
      charset = charset,
    )

    /** @return Literal script container for the provided [script]. */
    @JvmStatic fun literal(script: String, id: String): ExecutableScript = LiteralScript(id, script)
  }

  /** Script runtime manager. */
  class ScriptRuntime {
    companion object {
      private const val embeddedRoot = "embedded"
      private const val manifest = "/$embeddedRoot/runtime/runtime-js.json"
      private val initialized: AtomicBoolean = AtomicBoolean(false)

      // Runtime pre-amble from which to clone and splice executable scripts.
      private var preamble: StringBuilder

      // Runtime finalizer / loader function.
      private var loader: StringBuilder

      init {
        val (p, l) = initialize()
        preamble = p
        loader = l
      }

      // Load a JS artifact for runtime use from the JAR.
      @JvmStatic private fun loadArtifact(path: String): String {
        return (
          JsRuntime::class.java.getResourceAsStream("/$embeddedRoot/runtime/$path") ?:
            throw FileNotFoundException("Unable to locate runtime JS resource $path")
        ).bufferedReader(StandardCharsets.UTF_8).use {
          it.readText()
        }
      }

      // Initialize the script runtime by loading artifacts mentioned in the manifest.
      @JvmStatic @Synchronized private fun initialize(): Pair<StringBuilder, StringBuilder> {
        if (!initialized.get()) {
          initialized.compareAndSet(
            false,
            true
          )

          // resolve the stream
          val manifestStream = ScriptRuntime::class.java.getResourceAsStream(
            manifest
          ) ?: throw IllegalStateException(
            "Failed to resolve JS runtime manifest: '$manifest'. Please check that you have a dependency on " +
            "'graalvm-js', which is required to run embedded SSR scripts."
          )

          // grab content
          val manifestContent = manifestStream.bufferedReader().use { it.readText() }

          // deserialize as script runtime config
          val config = Json.decodeFromString(
            JsRuntimeConfig.serializer(),
            manifestContent
          )

          // load each resource
          val runtimePreamble = StringBuilder()
          config.artifacts.map {
            runtimePreamble.appendLine(loadArtifact(it.name))
          }

          // load entrypoint
          val runtimeEntry = StringBuilder()
          if (config.entry.isNotBlank()) {
            runtimeEntry.appendLine(loadArtifact(config.entry))
          }

          return (
            runtimePreamble to runtimeEntry
          )
        } else {
          throw IllegalStateException(
            "Cannot initialize JS runtime twice"
          )
        }
      }
    }

    /** @return */
    internal fun prepare(script: ExecutableScript): ExecutableScript {
      val content = script.load()
      val container = StringBuilder()
      if (script.installShims) {
        container.append(
          preamble
        )
      }
      container.append(
        content
      )
      if (script.installEntry) {
        container.append(
          loader
        )
      }
      script.assignRendered(
        container
      )
      return script
    }
  }

  /** Embedded script descriptor object. */
  sealed class ExecutableScript(
    internal val installShims: Boolean = true,
    internal val installEntry: Boolean = true,
    val invocationBase: String? = null,
    val invocationTarget: String? = null,
  ) {
    private var renderedContent: StringBuilder? = null

    /** @return The path or some module ID for the embedded script. */
    abstract fun getId(): String

    /** @return Script content, loaded synchronously. */
    abstract fun load(): String

    // Assign rendered preamble+script content before execution.
    internal fun assignRendered(builder: StringBuilder) {
      renderedContent = builder
    }

    internal fun render(): Source {
      val content = renderedContent?.toString() ?: throw IllegalStateException(
        "Cannot render script before it has been prepared by the JS runtime"
      )
      return Source.create(
        "js",
        content
      )
    }
  }

  /** Embedded script implementation which pulls from local JAR resources. */
  class EmbeddedScript(
    val path: String,
    private val charset: Charset = StandardCharsets.UTF_8,
    invocationBase: String? = null,
    invocationTarget: String? = null,
  ): ExecutableScript(
    invocationBase = invocationBase,
    invocationTarget = invocationTarget,
  ) {
    /** @inheritDoc */
    override fun getId(): String = path

    /** @inheritDoc */
    override fun load(): String {
      val stream = javaClass.getResourceAsStream(path) ?:
      throw FileNotFoundException("Embedded script not found: '$path'")

      return stream.bufferedReader(charset).use {
        it.readText()
      }
    }
  }

  /** Embedded script implementation which pulls from a string literal. */
  class LiteralScript(
    private val moduleId: String,
    private val script: String
  ): ExecutableScript() {
    /** @inheritDoc */
    override fun getId(): String = moduleId

    /** @inheritDoc */
    override fun load(): String {
      return script
    }
  }

  // Create the singleton script runtime.
  private val runtime: ScriptRuntime = ScriptRuntime()

  @VisibleForTesting
  internal fun <R> evalExecuteScript(
    context: org.graalvm.polyglot.Context,
    script: ExecutableScript,
    returnType: Class<R>,
    vararg arguments: Any?
  ): R? {
    val interpreted = context.eval(
      runtime.prepare(script).render()
    ) ?: throw IllegalStateException(
      "Failed to interpret script: '${script.getId()}'"
    )

    val base = script.invocationBase
    val target = script.invocationTarget
    val baseTarget: Value = if (target != null) {
      var baseSegment: Value = interpreted
      val baseResolved = if (base != null) {
        base.split(".").forEach {
          baseSegment = baseSegment.getMember(
            it
          ) ?: throw IllegalStateException(
            "Failed to resolve base segment: '$it' in '$base' was not found"
          )
        }
        baseSegment
      } else {
        interpreted
      }

      // from the resolved base segment, pluck the executable member
      val found = baseResolved.getMember(
        script.invocationTarget,
      ) ?: throw IllegalStateException(
        "Failed to invoke script member: '${script.getId()}' (fn: '${script.invocationTarget}')"
      )

      if (!found.canExecute()) {
        throw IllegalStateException(
          "Member found, but not executable, at '${base}.${script.invocationTarget}'"
        )
      } else {
        found
      }
    } else {
      // execute the script directly
      interpreted
    }

    // if we are handed back an executable, execute it, providing the input arguments. in both cases, cast the return
    // value to the expected type.
    return if (baseTarget.canExecute()) {
      baseTarget.execute(
        *arguments
      )?.`as`(
        returnType
      )
    } else {
      interpreted.`as`(
        returnType
      )
    }
  }

  /**
   * Asynchronously execute the provided [script] within an embedded JavaScript VM, by way of GraalVM's runtime engine;
   * de-serialize the result [R] and provide it as the return value.
   *
   * @param script Executable script spec to execute within the embedded JS VM.
   * @return Deferred task which evaluates to the return value [R] when execution finishes.
   */
  fun <R> execute(script: ExecutableScript, returnType: Class<R>, vararg arguments: Any?): R? {
    // interpret the script
    val ctx = initializeContext()
    return ctx.use {
      evalExecuteScript(
        it,
        script,
        returnType,
        *arguments
      )
    }
  }
}

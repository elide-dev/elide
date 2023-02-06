package elide.server

import elide.server.annotations.Eager
import elide.server.runtime.jvm.SecurityProviderConfigurator
import elide.util.RuntimeFlag
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.Micronaut
import io.micronaut.runtime.server.event.ServerStartupEvent
import kotlinx.coroutines.runBlocking
import java.util.LinkedList
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Static class interface which equips a Micronaut application with extra initialization features powered by Elide; to
 * use, simply enforce that your entrypoint object complies with this interface.
 */
public interface Application {
  /** Application initialization hooks. */
  public object Initialization {
    // Stages when a callback can be executed.
    internal enum class CallbackStage {
      INIT,
      WARMUP,
    }

    // Callback list.
    private val callbacks: LinkedList<Pair<CallbackStage, suspend () -> Unit>> = LinkedList()

    // Whether `INIT`-stage callbacks have been executed.
    private val initialized: AtomicBoolean = AtomicBoolean(false)

    // Whether `WARM`-stage callbacks have been executed.
    private val warmed: AtomicBoolean = AtomicBoolean(false)

    /**
     * Unconditionally initialize some server component before the application starts.
     *
     * @param callable Callable to initialize the component with.
     */
    public fun initializeWithServer(callable: suspend () -> Unit) {
      check(!initialized.get()) {
        "Cannot add server init callback after server has initialized"
      }
      callbacks.addFirst(CallbackStage.INIT to callable)
    }

    /**
     * Initialize some server component at server warmup, using the provided [callable]; exceptions thrown from the
     * callable are ignored.
     *
     * If warmup is disabled, no callables in this category are executed.
     *
     * @param callable Callable to execute.
     */
    public fun initializeOnWarmup(callable: suspend () -> Unit) {
      check(!warmed.get()) {
        "Cannot add server warmup callback after server has already warmed"
      }
      callbacks.addLast(CallbackStage.WARMUP to callable)
    }

    // Dispatch initialization callback.
    private suspend fun dispatchCallback(pair: Pair<CallbackStage, suspend () -> Unit>) {
      try {
        pair.second.invoke()
      } catch (e: Exception) {
        // Ignore.
      }
    }

    /**
     * Trigger service-loader-based class loading of eager components.
     */
    internal fun resolveHooks() {
      // force-load all classes marked as initializers
      ServiceLoader.load(ServerInitializer::class.java).forEach { svc ->
        svc.initialize()
      }
    }

    /**
     * Trigger callbacks for the provided server [stage].
     */
    internal suspend fun trigger(stage: CallbackStage) {
      var found = false
      var handled = 0

      when (stage) {
        CallbackStage.INIT -> initialized
        CallbackStage.WARMUP -> warmed
      }.compareAndSet(false, true)

      for (callback in callbacks) {
        if (callback.first == stage) {
          if (!found) {
            found = true
          }
          dispatchCallback(callback)
          handled += 1
        } else if (found) {
          // if the stage changes and has been found, we break, since the list is ordered by stage with `INIT` first and
          // `WARMUP` second.
          break
        }
      }
      callbacks.drop(handled)
    }
  }

  /** Application startup listener and callback trigger. */
  @Context @Eager public class AppStartupListener : ApplicationEventListener<ServerStartupEvent> {
    override fun onApplicationEvent(event: ServerStartupEvent): Unit = runBlocking {
      Initialization.trigger(
        Initialization.CallbackStage.INIT
      )

      if (RuntimeFlag.warmup) {
        Initialization.trigger(
          Initialization.CallbackStage.WARMUP
        )
      }
    }
  }

  /**
   * Boot an Elide application with the provided [args], if any.
   *
   * Elide parses its own arguments and applies configuration or state based on any encountered values. All Elide flags
   * are prefixed with "--elide.". Micronaut-relevant arguments are passed on to Micronaut, and user args are
   * additionally made available.
   *
   * Elide server arguments can be interrogated via [RuntimeFlag]s.
   *
   * @param args Arguments passed to the application.
   */
  @Suppress("SpreadOperator", "DEPRECATION")
  public fun boot(args: Array<String>) {
    RuntimeFlag.setArgs(args)
    SecurityProviderConfigurator.initialize()

    Initialization.resolveHooks()
    Initialization.initializeOnWarmup {
      // Warm up the JVM.
      Runtime.getRuntime().apply {
        gc()
      }
    }

    Micronaut
      .build()
      .eagerInitConfiguration(true)
      .eagerInitSingletons(true)
      .eagerInitAnnotated(Eager::class.java, Context::class.java)
      .args(*args)
      .start()
  }
}

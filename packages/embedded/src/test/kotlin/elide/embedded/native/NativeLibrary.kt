package elide.embedded.native

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.SymbolLookup
import java.lang.invoke.MethodHandle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An abstract base class used for mapping a native shared library via the new foreign memory API.
 *
 * Implementations have access to a collection of functions used to interface with the mapped native shared library.
 * Downcall handles to native functions can be created using [functionHandle] and [voidFunctionHandle].
 *
 * The native library will be loaded automatically from the path specified by the [libraryPath] property before call
 * handles are resolved. This initialization is guaranteed to happen only once.
 */
internal abstract class NativeLibrary {
  /**
   * Provides the absolute path to the shared library mapped by this class.
   *
   * The path must point to the library itself rather than its containing directory, and must not be surrounded by
   * double-quotes ("") as these will be added automatically before calling [System.load].
   */
  protected abstract val libraryPath: String

  /** Internal thread-safe init flag. */
  private val initialized = AtomicBoolean()

  /**
   * Loads the required native libraries. This method is thread-safe, and is inert after the first call, so there is
   * no need for manual synchronization or state-keeping.
   */
  private fun initialize() {
    // only run once across all threads
    if (!initialized.compareAndSet(false, true)) return
    System.load(libraryPath)
  }

  /** Lazy [Linker] instance. */
  private val linker: Linker by lazy {
    initialize()

    // default linker using libraries imported by 'loadLibrary'
    Linker.nativeLinker()
  }

  /** Lazy [SymbolLookup] instance. */
  private val lookup: SymbolLookup by lazy {
    initialize()

    // default lookup using libraries imported by 'loadLibrary'
    SymbolLookup.loaderLookup()
  }

  /**
   * Create a [MethodHandle] for a function with the specified [name] and [argument types][arguments]. The function
   * must be present in the elide native shared library.
   */
  protected fun voidFunctionHandle(name: String, vararg arguments: MemoryLayout): MethodHandle {
    return functionHandle(name, FunctionDescriptor.ofVoid(*arguments))
  }

  /**
   * Create a [MethodHandle] for a function with the specified [name], [return type][returns], and
   * [argument types][arguments]. The function must be present in the elide native shared library.
   */
  protected fun functionHandle(name: String, returns: MemoryLayout, vararg arguments: MemoryLayout): MethodHandle {
    return functionHandle(name, FunctionDescriptor.of(returns, *arguments))
  }

  /**
   * Create a [MethodHandle] for a function with the specified [name] and [descriptor]. The function must be present
   * in the elide native shared library.
   */
  protected fun functionHandle(name: String, descriptor: FunctionDescriptor): MethodHandle {
    return lookup.find(name).map { address ->
      linker.downcallHandle(address, descriptor)
    }.orElse(null) ?: error("Failed to load native function '$name'")
  }
}
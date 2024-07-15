@file:Suppress("RedundantVisibilityModifier")

package dev.elide.cli.bridge

/**
 * # Native Bridge
 *
 * Bridges native methods from Elide's `umbrella` library, via JNI access points.
 */
object CliNativeBridge {
  /** Token expected for the tooling API at version 1.  */
  const val VERSION_V1: String = "v1"

  /** Native platform-agnostic library name for Elide's umbrella library. */
  private const val NATIVE_LIB_NAME = "umbrella"

  /** Whether the native layer has initialized yet. */
  private var initialized: Boolean = false

  /** Initialize the native layer. */
  @Synchronized public fun initialize() {
    if (!initialized) {
      initialized = true
      val init = initializeNative()
      assert(init == 0) { "Failed to initialize native layer; got code $init" }
    }
  }

  /** Initialize the native runtime layer; any non-zero return value indicates an error.  */
  private external fun initializeNative(): Int

  /** Return the tooling protocol version.  */
  external fun apiVersion(): String

  /** Return the library version.  */
  external fun libVersion(): String

  /** Return the suite of reported tool names.  */
  external fun supportedTools(): Array<String>

  /** Return the languages which relate to a given tool.  */
  external fun relatesTo(toolName: String): Array<String>

  /** Return the version string for a tool.  */
  external fun toolVersion(toolName: String): String

  /** Run the Ruff entrypoint.  */
  external fun runRuff(args: Array<String>): Int

  /** Run the Orogene entrypoint.  */
  external fun runOrogene(args: Array<String>): Int

  /** Run the Uv entrypoint.  */
  external fun runUv(args: Array<String>): Int

  init {
    System.loadLibrary(NATIVE_LIB_NAME)
    initialize()
  }
}

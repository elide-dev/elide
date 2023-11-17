package elide.runtime.plugins.kotlin.shell

/** A custom [ClassLoader] exposing a [define] method, which can load new classes from compiled bytecode. */
internal class DynamicClassLoader : ClassLoader() {
  /** Define a new class as specified by [ClassLoader.defineClass]. */
  fun define(className: String, bytecode: ByteArray): Class<*> = defineClass(
    /* name = */ className,
    /* b = */ bytecode,
    /* off = */ 0,
    /* len = */ bytecode.size,
  )
}
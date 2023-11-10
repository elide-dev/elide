package elide.runtime.plugins.jvm

/*
so instead the idea is as follows:
1. Create a GraalVM context with JVM support
2. Using context options, adjust the classpath of the context (before building it obviously), so that it includes the classes of the guest application
3. Through the bindings API, obtain a reference to the "main" class (which is a polyglot `Value` to us in the host)
4. Invoke the static "main" method in that guest class (via `Value.invokeMember`, since we're dealing with guest objects)
5. Profit
*/

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext

/**
 * Run the entrypoint of a guest JVM application. This extension attempts to locate a static method with the
 * specified [mainMethodName] in a class defined by a fully-qualified [mainClassName].
 *
 * A class with the specified [mainClassName] must be present in the guest classpath, as configured by the [JvmConfig]
 * during the construction of the engine.
 *
 * The resolved method must include a single parameter in its signature, with type `Array<String>`, otherwise this call
 * will fail.
 *
 * @see Jvm
 */
@DelicateElideApi public fun PolyglotContext.runJvm(
  mainClassName: String,
  mainMethodName: String = "main",
  arguments: Array<String> = arrayOf(),
): Int {
  // attempt to locate the main class
  val mainClass = bindings(Jvm).getMember(mainClassName) ?: error(
    "Class <$mainClassName> not found in guest context, check your guest classpath configuration",
  )

  // run the entrypoint and encapsulate errors
  val exitCode = runCatching { mainClass.invokeMember(mainMethodName, arguments) }.getOrElse { cause ->
    error("Error during guest execution: $cause")
  }

  // treat void/null return values as a 0 exit code
  if (exitCode == null || exitCode.isNull) return 0

  // unwrap explicit exit code
  check(exitCode.isNumber && exitCode.fitsInInt()) { "Expected return value to be Int, found $exitCode" }
  return exitCode.asInt()
}


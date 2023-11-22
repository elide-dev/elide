/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.plugins.java.shell

import jdk.jshell.spi.ExecutionControl
import jdk.jshell.spi.ExecutionControl.*
import jdk.jshell.spi.ExecutionControlProvider
import jdk.jshell.spi.ExecutionEnv
import org.graalvm.polyglot.PolyglotException
import kotlin.properties.ReadOnlyProperty
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.jvm.interop.asBooleanArray
import elide.runtime.plugins.jvm.interop.asStringOrNull
import elide.runtime.plugins.jvm.interop.guestClass

/**
 * A provider for [GuestExecutionControl] instances wrapping a guest [PolyglotContext].
 *
 * Note that while this class implements the [ExecutionControlProvider] interface, it is not suitable for use with
 * service loaders (and indeed it is not registered as a provider in the metadata), because it requires a guest context
 * as a constructor argument.
 *
 * This limitation is necessary however, to avoid implicit creation of contexts for the sole purpose of running the
 * shell. A suitable entrypoint is provided by the [GuestJavaEvaluator] class, which manages its own JShell instance
 * using this provider, and does not require a service loader call.
 *
 * #### Guest classes
 *
 * This class stores a few guest classes using the [guestClass] property delegate, which lazily resolves a class from
 * the context's bindings. Exception classes related to JShell and certain primitives like ByteArray are cached for
 * use in [mapException] and [mapBytecodes], which are used by the [GuestExecutionControl] class.
 *
 * @see GuestExecutionControl
 */
@DelicateElideApi internal class GuestExecutionProvider(context: PolyglotContext) : ExecutionControlProvider {
  // cached guest classes
  private val localExecutionControl by context.jshellClass("execution.LocalExecutionControl")
  private val classBytecodes by context.executionControlClass("ClassBytecodes")
  private val byteArray by context.guestClass("[B")

  // cached guest exception classes
  private val executionControlException by context.spiClass("ExecutionControlException")
  private val classInstallException by context.executionControlClass("ClassInstallException")
  private val notImplementedException by context.executionControlClass("NotImplementedException")
  private val engineTerminationException by context.executionControlClass("EngineTerminationException")
  private val internalException by context.executionControlClass("InternalException")
  private val resolutionException by context.executionControlClass("ResolutionException")
  private val stoppedException by context.executionControlClass("StoppedException")
  private val userException by context.executionControlClass("UserException")

  override fun name(): String {
    return PROVIDER_NAME
  }

  override fun generate(env: ExecutionEnv, parameters: MutableMap<String, String>): ExecutionControl {
    // delegate to a LocalExecutionControl instance instantiated in the guest context
    val delegate = localExecutionControl.newInstance()
    return GuestExecutionControl(delegate = delegate, provider = this)
  }

  /**
   * Map the target exception to a host type if applicable.
   *
   * If the provided [guestException] is an instance of one of the cached JShell exception classes, it will be mapped
   * to a corresponding host type before being returned. A guest exception that does not correspond to JShell is
   * returned directly.
   *
   * @param guestException A (possibly) JShell exception raised inside the guest context.
   * @return A host [Exception] created from the guest value, if it is a JShell exception, otherwise the original.
   */
  internal fun mapException(guestException: PolyglotException): Exception {
    val exception = guestException.guestObject ?: return guestException

    // some helper extensions
    fun PolyglotValue.exceptionMessage() = invokeMember("getMessage").asStringOrNull()
    fun PolyglotValue.exceptionCause() = invokeMember("causeExceptionClass").asStringOrNull()
    infix fun PolyglotValue.isMetaInstanceOf(guestClass: PolyglotValue) = guestClass.isMetaInstance(this)

    if (!(exception isMetaInstanceOf executionControlException)) {
      // not a jshell exception
      return guestException
    }

    return when {
      exception isMetaInstanceOf notImplementedException -> NotImplementedException(exception.exceptionMessage())
      exception isMetaInstanceOf engineTerminationException -> EngineTerminationException(exception.exceptionMessage())
      exception isMetaInstanceOf internalException -> InternalException(exception.exceptionMessage())
      exception isMetaInstanceOf stoppedException -> StoppedException()

      exception isMetaInstanceOf userException -> UserException(
        /* message = */ exception.exceptionMessage(),
        /* causeExceptionClass = */ exception.exceptionCause(),
        /* stackElements = */ emptyArray(),
      )

      exception isMetaInstanceOf classInstallException -> ClassInstallException(
        /* message = */ exception.exceptionMessage(),
        /* installed = */ exception.invokeMember("installed")?.asBooleanArray(),
      )

      exception isMetaInstanceOf resolutionException -> ResolutionException(
        /* id = */ exception.invokeMember("id").asInt(),
        /* stackElements = */ emptyArray(),
      )

      else -> guestException
    }
  }

  /**
   * Map an array of host bytecodes to a guest array, for use in a [GuestExecutionControl].
   *
   * For each [ClassBytecodes] element in the array, a new [ClassBytecodes] instance is created in the guest context,
   * and the byte array containing the bytecode is also mapped accordingly.
   *
   * @param bytecodes The group of [ClassBytecodes] to be transformed into a guest value.
   * @return The guest equivalent of the input [bytecodes] array.
   */
  internal fun mapBytecodes(bytecodes: Array<out ClassBytecodes>): PolyglotValue {
    val output = classBytecodes.getMember("array").newInstance(bytecodes.size)

    repeat(bytecodes.size) { i ->
      // prepare a guest copy of the byte code array
      val hostByteArray = bytecodes[i].bytecodes()
      val guestByteArray = byteArray.newInstance(hostByteArray.size)

      // copy the bytes to the guest array
      repeat(hostByteArray.size) { j -> guestByteArray.setArrayElement(j.toLong(), hostByteArray[j]) }

      // construct the guest bytecode and add it to the output
      output.setArrayElement(i.toLong(), classBytecodes.newInstance(bytecodes[i].name(), guestByteArray))
    }

    return output
  }

  private companion object {
    /**
     * The name read by the service loader when considering this provider.
     *
     * Note that the [GuestExecutionProvider] class is *not* a valid service provider, since it requires a guest
     * context as constructor parameter. Attempting to resolve a provider using this name will always fail. See the
     * class documentation for the intended usage.
     */
    private const val PROVIDER_NAME = "elide"

    /** Package name prefix used by the [jshellClass] extension. */
    private const val JSHELL_PACKAGE = "jdk.jshell"

    /** Package name prefix used by the [spiClass] extension. */
    private const val JSHELL_SPI_PACKAGE = "jdk.jshell.spi"

    /** Returns a property [guestClass] delegate for the given [name], prefixed with the [JSHELL_PACKAGE] name. */
    private fun PolyglotContext.jshellClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
      return guestClass("$JSHELL_PACKAGE.$name")
    }

    /** Returns a property [guestClass] delegate for the given [name], prefixed with the [JSHELL_SPI_PACKAGE] name. */
    private fun PolyglotContext.spiClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
      return guestClass("$JSHELL_SPI_PACKAGE.$name")
    }

    /** Returns a property [guestClass] delegate for the given [name] in the jdk.jshell.spi.ExecutionControl class. */
    private fun PolyglotContext.executionControlClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
      return guestClass("$JSHELL_SPI_PACKAGE.ExecutionControl\$$name")
    }
  }
}

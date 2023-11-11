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

@DelicateElideApi internal class GuestExecutionControlProvider(
  private val context: PolyglotContext
) : ExecutionControlProvider, GuestExceptionMapper, GuestBytecodeMapper {
  private val system by context.guestClass("java.lang.System")
  private val classBytecodes by context.executionControlClass("ClassBytecodes")
  private val byteArray by context.guestClass("[B")
  private val localExecutionControl by context.jshellClass("execution.LocalExecutionControl")

  private val executionControlException by context.spiClass("ExecutionControlException")
  private val runException by context.spiClass("ExecutionControlException")
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
    return GuestExecutionControl(guestDelegate = delegate, exceptionMapper = this, bytecodeMapper = this)
  }

  override fun map(guestException: PolyglotException): Exception {
    val exception = GuestExceptionWrapper.of(guestException.guestObject ?: return guestException)

    if (!(exception isMetaInstanceOf executionControlException)) {
      // not a jshell exception
      return guestException
    }

    return when {
      exception isMetaInstanceOf notImplementedException -> NotImplementedException(exception.message())
      exception isMetaInstanceOf engineTerminationException -> EngineTerminationException(exception.message())
      exception isMetaInstanceOf internalException -> InternalException(exception.message())
      exception isMetaInstanceOf stoppedException -> StoppedException()

      exception isMetaInstanceOf userException -> UserException(
        exception.message(),
        exception.cause(),
        exception.stackTrace(),
      )

      exception isMetaInstanceOf classInstallException -> ClassInstallException(
        exception.message(),
        exception.value.invokeMember("installed")?.asBooleanArray(),
      )

      exception isMetaInstanceOf resolutionException -> ResolutionException(
        exception.value.invokeMember("id").asInt(),
        exception.stackTrace(),
      )

      else -> guestException
    }
  }

  override fun map(bytecodes: Array<out ClassBytecodes>): PolyglotValue {
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
    private const val PROVIDER_NAME = "espresso"

    private const val JSHELL_PACKAGE = "jdk.jshell"

    private const val JSHELL_SPI_PACKAGE = "jdk.jshell.spi"

    @Suppress("nothing_to_inline")
    private inline fun PolyglotContext.jshellClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
      return guestClass("$JSHELL_PACKAGE.$name")
    }

    @Suppress("nothing_to_inline")
    private inline fun PolyglotContext.spiClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
      return guestClass("$JSHELL_SPI_PACKAGE.$name")
    }

    @Suppress("nothing_to_inline")
    private inline fun PolyglotContext.executionControlClass(name: String): ReadOnlyProperty<Any, PolyglotValue> {
      return guestClass("$JSHELL_SPI_PACKAGE.ExecutionControl\$$name")
    }
  }
}
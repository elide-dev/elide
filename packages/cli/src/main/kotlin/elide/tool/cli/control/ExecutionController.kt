package elide.tool.cli.control

import jdk.jshell.spi.ExecutionControl
import jdk.jshell.spi.ExecutionControl.*
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Value
import java.util.*

/**
 *
 */
abstract class ExecutionController protected constructor(delegate: Lazy<Value>) :
  ExecutionControl {
  private val delegate: Lazy<Value>
  private val tClassBytecodes: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$ClassBytecodes") }
  private val tByteArray: Lazy<Value> =
    Lazy.of { loadClass("[B") }
  private val tExecutionControlException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$ExecutionControlException") }
  private val tRunException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$RunException") }
  private val tClassInstallException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$ClassInstallException") }
  private val tNotImplementedException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$NotImplementedException") }
  private val tEngineTerminationException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$EngineTerminationException") }
  private val tInternalException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$InternalException") }
  private val tResolutionException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$ResolutionException") }
  private val tStoppedException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$StoppedException") }
  private val tUserException: Lazy<Value> =
    Lazy.of { loadClass("jdk.jshell.spi.ExecutionControl\$UserException") }

  init {
    this.delegate = Objects.requireNonNull(delegate)
  }

  abstract fun loadClass(className: String?): Value

  @Throws(
    ClassInstallException::class,
    NotImplementedException::class,
    EngineTerminationException::class
  )
  override fun load(classBytecodes: Array<ClassBytecodes>) {
    try {
      requireNotNull(delegate.get()).invokeMember("load", toGuest(classBytecodes))
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  @Throws(
    ClassInstallException::class,
    NotImplementedException::class,
    EngineTerminationException::class
  )
  override fun redefine(classBytecodes: Array<ClassBytecodes>) {
    try {
      requireNotNull(delegate.get()).invokeMember("redefine", toGuest(classBytecodes))
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  @Throws(RunException::class, EngineTerminationException::class, InternalException::class)
  override fun invoke(className: String?, methodName: String?): String? {
    return try {
      asString(requireNotNull(delegate.get()).invokeMember("invoke", className, methodName))
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  @Throws(RunException::class, EngineTerminationException::class, InternalException::class)
  override fun varValue(className: String?, varName: String?): String? {
    return try {
      asString(requireNotNull(delegate.get()).invokeMember("varValue", className, varName))
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  @Throws(EngineTerminationException::class, InternalException::class)
  override fun addToClasspath(s: String?) {
    try {
      requireNotNull(delegate.get()).invokeMember("addToClasspath", s)
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  @Throws(EngineTerminationException::class, InternalException::class)
  override fun stop() {
    try {
      requireNotNull(delegate.get()).invokeMember("stop")
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  @Throws(RunException::class, EngineTerminationException::class, InternalException::class)
  override fun extensionCommand(command: String, arg: Any?): Any {
    throw NotImplementedException("extensionCommand: $command")
  }

  override fun close() {
    try {
      requireNotNull(delegate.get()).invokeMember("close")
    } catch (e: PolyglotException) {
      throw throwAsHost(e)
    }
  }

  private fun throwAsHost(polyglotException: PolyglotException): Throwable {
    throw throwCheckedException(toHost(polyglotException))
  }

  private fun toHost(polyglotException: PolyglotException): Throwable {
    val e = polyglotException.guestObject
    if (e == null || !requireNotNull(tExecutionControlException.get()).isMetaInstance(e)) {
      // Not a jshell exception, propagate.
      return polyglotException
    }
    if (requireNotNull(tUserException.get()).isMetaInstance(e)) {
      return UserException(getMessage(e), getCauseExceptionClass(e), getStackTrace(e))
    }
    if (requireNotNull(tClassInstallException.get()).isMetaInstance(e)) {
      val guestInstalled = e.invokeMember("installed")
      var installed: BooleanArray? = null
      if (!guestInstalled.isNull) {
        assert(guestInstalled.hasArrayElements())
        val installedLength = Math.toIntExact(guestInstalled.arraySize)
        installed = BooleanArray(installedLength)
        for (i in 0 until installedLength) {
          installed[i] = guestInstalled.getArrayElement(i.toLong()).asBoolean()
        }
      }
      return ClassInstallException(getMessage(e), installed)
    }
    if (requireNotNull(tNotImplementedException.get()).isMetaInstance(e)) {
      return NotImplementedException(getMessage(e))
    }
    if (requireNotNull(tEngineTerminationException.get()).isMetaInstance(e)) {
      return EngineTerminationException(getMessage(e))
    }
    if (requireNotNull(tInternalException.get()).isMetaInstance(e)) {
      return InternalException(getMessage(e))
    }
    if (requireNotNull(tResolutionException.get()).isMetaInstance(e)) {
      val id = e.invokeMember("id").asInt()
      return ResolutionException(id, getStackTrace(e))
    }
    return if (requireNotNull(tStoppedException.get()).isMetaInstance(e)) {
      StoppedException()
    } else polyglotException

    // Unknown ExecutionControlException exception, propagate.
  }

  protected fun toGuest(classBytecodes: Array<ClassBytecodes>): Value {
    val values: Value = requireNotNull(
      this.tClassBytecodes.get()
    ).getMember("array").newInstance(classBytecodes.size)
    for (i in classBytecodes.indices) {
      val bytecodes = classBytecodes[i].bytecodes()
      val bytes: Value = requireNotNull(tByteArray.get()).newInstance(bytecodes.size)
      for (j in bytecodes.indices) {
        bytes.setArrayElement(j.toLong(), bytecodes[j])
      }
      val elem: Value = requireNotNull(this.tClassBytecodes.get()).newInstance(classBytecodes[i].name(), bytes)
      values.setArrayElement(i.toLong(), elem)
    }
    return values
  }

  companion object {
    protected fun asString(stringOrNull: Value): String? {
      return if (stringOrNull.isNull) null else stringOrNull.asString()
    }

    protected fun getMessage(guestException: Value): String? {
      assert(guestException.isException)
      return asString(guestException.invokeMember("getMessage"))
    }

    protected fun getCauseExceptionClass(guestException: Value): String? {
      assert(guestException.isException)
      return asString(guestException.invokeMember("causeExceptionClass"))
    }

    protected fun getStackTrace(@Suppress("unused") guestException: Value?): Array<StackTraceElement?> {
      // TODO(peterssen): Create host stack trace.
      return arrayOfNulls(0)
    }

    protected inline fun <reified E : Throwable> throwCheckedException(ex: E): Throwable {
      throw ex
    }
  }
}
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

package elide.tool.control

import org.graalvm.home.Version
import org.graalvm.polyglot.*
import java.io.*
import javax.script.*
import kotlin.collections.MutableMap.MutableEntry
import kotlin.concurrent.Volatile
import elide.runtime.gvm.internals.GraalVMGuest

/**
 * TBD.
 */
abstract class AbstractScriptEngineFactory : ScriptEngineFactory {
  companion object {
    private val polyglotEngine = Engine.newBuilder().build()
  }

  abstract val engine: GraalVMGuest

  private val languageId: String = engine.engine
  private val language = polyglotEngine.languages[languageId]

  override fun getEngineName(): String {
    return language!!.implementationName
  }

  override fun getEngineVersion(): String {
    return Version.getCurrent().toString()
  }

  override fun getExtensions(): MutableList<String> {
    return mutableListOf(languageId)
  }

  override fun getMimeTypes(): MutableList<String>? {
    return language!!.mimeTypes.toMutableList()
  }

  override fun getNames(): MutableList<String>? {
    return mutableListOf(language!!.name, languageId, language.implementationName)
  }

  override fun getLanguageName(): String {
    return language!!.name
  }

  override fun getLanguageVersion(): String {
    return language!!.version
  }

  override fun getParameter(key: String): Any? {
    return when (key) {
      ScriptEngine.ENGINE -> engineName
      ScriptEngine.ENGINE_VERSION -> engineVersion
      ScriptEngine.LANGUAGE -> languageName
      ScriptEngine.LANGUAGE_VERSION -> languageVersion
      ScriptEngine.NAME -> languageId
      else -> null
    }
  }

  override fun getMethodCallSyntax(obj: String, m: String, vararg args: String): String {
    throw UnsupportedOperationException("Unimplemented method 'getMethodCallSyntax'")
  }

  override fun getOutputStatement(toDisplay: String): String {
    throw UnsupportedOperationException("Unimplemented method 'getOutputStatement'")
  }

  override fun getProgram(vararg statements: String): String {
    throw UnsupportedOperationException("Unimplemented method 'getProgram'")
  }

  override fun getScriptEngine(): ScriptEngine {
    return PolyglotEngine(languageId, this)
  }

  private class PolyglotEngine constructor(
    private val languageId: String,
    private val factory: ScriptEngineFactory
  ) : ScriptEngine,
    Compilable, Invocable, AutoCloseable {
    private val defaultContext: PolyglotContext = PolyglotContext(languageId)

    override fun close() {
      defaultContext.getContext()!!.close()
    }

    @Throws(ScriptException::class) override fun compile(script: String): CompiledScript {
      val src = Source.create(languageId, script)
      try {
        defaultContext.getContext()!!.parse(src) // only for the side-effect of validating the source
      } catch (e: PolyglotException) {
        throw ScriptException(e)
      }
      return PolyglotCompiledScript(src, this)
    }

    @Throws(ScriptException::class) override fun compile(script: Reader): CompiledScript {
      val src: Source
      try {
        src = Source.newBuilder(languageId, script, "sourcefromreader").build()
        defaultContext.getContext()!!.parse(src) // only for the side-effect of validating the source
      } catch (e: PolyglotException) {
        throw ScriptException(e)
      } catch (e: IOException) {
        throw ScriptException(e)
      }
      return PolyglotCompiledScript(src, this)
    }

    @Throws(ScriptException::class) override fun eval(script: String, context: ScriptContext): Any {
      return if (context is PolyglotContext) {
        try {
          context.getContext()!!.eval(languageId, script).`as`(
            Any::class.java,
          )
        } catch (e: PolyglotException) {
          throw ScriptException(e)
        }
      } else {
        throw ClassCastException("invalid context")
      }
    }

    @Throws(ScriptException::class) override fun eval(reader: Reader, context: ScriptContext): Any {
      val src: Source = try {
        Source.newBuilder(languageId, reader, "sourcefromreader").build()
      } catch (e: IOException) {
        throw ScriptException(e)
      }
      return if (context is PolyglotContext) {
        try {
          context.getContext()!!.eval(src).`as`(Any::class.java)
        } catch (e: PolyglotException) {
          throw ScriptException(e)
        }
      } else {
        throw ScriptException("invalid context")
      }
    }

    @Throws(ScriptException::class) override fun eval(script: String): Any {
      return eval(script, defaultContext)
    }

    @Throws(ScriptException::class) override fun eval(reader: Reader): Any {
      return eval(reader, defaultContext)
    }

    @Throws(ScriptException::class) override fun eval(script: String, n: Bindings): Any {
      throw UnsupportedOperationException("Bindings for Polyglot language cannot be created explicitly")
    }

    @Throws(ScriptException::class) override fun eval(reader: Reader, n: Bindings): Any {
      throw UnsupportedOperationException("Bindings for Polyglot language cannot be created explicitly")
    }

    override fun put(key: String, value: Any) {
      defaultContext.getBindings(ScriptContext.ENGINE_SCOPE)!![key] = value
    }

    override fun get(key: String): Any {
      return defaultContext.getBindings(ScriptContext.ENGINE_SCOPE)!![key]!!
    }

    override fun getBindings(scope: Int): Bindings? {
      return defaultContext.getBindings(scope)
    }

    override fun setBindings(bindings: Bindings, scope: Int) {
      defaultContext.setBindings(bindings, scope)
    }

    override fun createBindings(): Bindings {
      throw UnsupportedOperationException("Bindings for Polyglot language cannot be created explicitly")
    }

    override fun getContext(): ScriptContext {
      return defaultContext
    }

    override fun setContext(context: ScriptContext) {
      throw UnsupportedOperationException("The context of a Polyglot ScriptEngine cannot be modified.")
    }

    override fun getFactory(): ScriptEngineFactory {
      return factory
    }

    @Throws(
      ScriptException::class,
      NoSuchMethodException::class,
    ) override fun invokeMethod(thiz: Any, name: String, vararg args: Any): Any {
      return try {
        val receiver = defaultContext.getContext()!!.asValue(thiz)
        if (receiver.canInvokeMember(name)) {
          receiver.invokeMember(name, *args).`as`(Any::class.java)
        } else {
          throw NoSuchMethodException(name)
        }
      } catch (e: PolyglotException) {
        throw ScriptException(e)
      }
    }

    @Throws(
      ScriptException::class,
      NoSuchMethodException::class,
    ) override fun invokeFunction(name: String, vararg args: Any): Any {
      throw UnsupportedOperationException()
    }

    override fun <T : Any?> getInterface(clasz: Class<T>?): T {
      throw UnsupportedOperationException()
    }

    override fun <T : Any?> getInterface(thiz: Any?, clasz: Class<T>?): T {
      return defaultContext.getContext()!!.asValue(thiz).`as`(clasz)
    }
  }

  private class PolyglotContext (private val languageId: String) : ScriptContext {
    private var context: Context? = null
    private val `in`: PolyglotReader
    private val out: PolyglotWriter
    private val err: PolyglotWriter
    private var globalBindings: Bindings? = null

    init {
      `in` = PolyglotReader(InputStreamReader(System.`in`))
      out = PolyglotWriter(OutputStreamWriter(System.out))
      err = PolyglotWriter(OutputStreamWriter(System.err))
    }

    fun getContext(): Context? {
      if (context == null) {
        val builder = Context.newBuilder(languageId)
          .`in`(`in`)
          .out(out)
          .err(err)
          .allowAllAccess(true)
        val globalBindings = getBindings(ScriptContext.GLOBAL_SCOPE)
        if (globalBindings != null) {
          for ((key, value) in globalBindings) {
            if (value is String) {
              builder.option(key, value)
            }
          }
        }
        context = builder.build()
      }
      return context
    }

    override fun setBindings(bindings: Bindings, scope: Int) {
      globalBindings = if (scope == ScriptContext.GLOBAL_SCOPE) {
        if (context == null) {
          bindings
        } else {
          throw UnsupportedOperationException(
            "Global bindings for Polyglot language can only be set before the context is initialized.",
          )
        }
      } else {
        throw UnsupportedOperationException("Bindings objects for Polyglot language is final.")
      }
    }

    override fun getBindings(scope: Int): Bindings? {
      return if (scope == ScriptContext.ENGINE_SCOPE) {
        PolyglotBindings(getContext()!!.getBindings(languageId))
      } else if (scope == ScriptContext.GLOBAL_SCOPE) {
        globalBindings
      } else {
        null
      }
    }

    override fun setAttribute(name: String, value: Any, scope: Int) {
      if (scope == ScriptContext.ENGINE_SCOPE) {
        getBindings(scope)!![name] = value
      } else if (scope == ScriptContext.GLOBAL_SCOPE) {
        if (context == null) {
          globalBindings!![name] = value
        } else {
          throw IllegalStateException("Cannot modify global bindings after context creation.")
        }
      }
    }

    override fun getAttribute(name: String, scope: Int): Any? {
      if (scope == ScriptContext.ENGINE_SCOPE) {
        return getBindings(scope)!![name]!!
      } else if (scope == ScriptContext.GLOBAL_SCOPE) {
        return globalBindings!![name]!!
      }
      return null
    }

    override fun removeAttribute(name: String, scope: Int): Any? {
      val prev = getAttribute(name, scope)
      if (prev != null) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
          getBindings(scope)!!.remove(name)
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
          if (context == null) {
            globalBindings!!.remove(name)
          } else {
            throw IllegalStateException("Cannot modify global bindings after context creation.")
          }
        }
      }
      return prev
    }

    override fun getAttribute(name: String): Any? {
      return getAttribute(name, ScriptContext.ENGINE_SCOPE)
    }

    override fun getAttributesScope(name: String): Int {
      if (getAttribute(name, ScriptContext.ENGINE_SCOPE) != null) {
        return ScriptContext.ENGINE_SCOPE
      } else if (getAttribute(name, ScriptContext.GLOBAL_SCOPE) != null) {
        return ScriptContext.GLOBAL_SCOPE
      }
      return -1
    }

    override fun getWriter(): Writer {
      return out.writer
    }

    override fun getErrorWriter(): Writer {
      return err.writer
    }

    override fun setWriter(writer: Writer) {
      out.writer = writer
    }

    override fun setErrorWriter(writer: Writer) {
      err.writer = writer
    }

    override fun getReader(): Reader {
      return `in`.reader
    }

    override fun setReader(reader: Reader) {
      `in`.reader = reader
    }

    override fun getScopes(): MutableList<Int> {
      return mutableListOf(ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE)
    }

    private class PolyglotReader(inputStreamReader: InputStreamReader) : InputStream() {
      @Volatile var reader: Reader

      init {
        reader = inputStreamReader
      }

      @Throws(IOException::class) override fun read(): Int {
        return reader.read()
      }
    }

    private class PolyglotWriter(outputStreamWriter: OutputStreamWriter) : OutputStream() {
      @Volatile var writer: Writer

      init {
        writer = outputStreamWriter
      }

      @Throws(IOException::class) override fun write(b: Int) {
        writer.write(b)
      }
    }
  }

  private class PolyglotCompiledScript(private val source: Source, private val engine: ScriptEngine) :
    CompiledScript() {
    @Throws(ScriptException::class) override fun eval(context: ScriptContext): Any {
      if (context is PolyglotContext) {
        return context.getContext()!!.eval(source).`as`(Any::class.java)
      }
      throw UnsupportedOperationException(
        "Polyglot CompiledScript instances can only be evaluated in Polyglot.",
      )
    }

    override fun getEngine(): ScriptEngine {
      return engine
    }
  }

  private class PolyglotBindings internal constructor(private val languageBindings: Value) : Bindings {
    override val size: Int
      get() = keys.size

    override fun isEmpty(): Boolean {
      return size == 0
    }

    override fun containsValue(value: Any): Boolean {
      for (s in keys) {
        if (get(s) === value) {
          return true
        }
      }
      return false
    }

    override fun clear() {
      for (s in keys) {
        remove(s)
      }
    }

    override val keys: MutableSet<String>
      get() = languageBindings.memberKeys

    override val values: MutableCollection<Any> get() {
      val values: MutableList<Any> = ArrayList()
      for (s in keys) {
        when (val v = get(s)) {
          null -> {}
          else -> values.add(v)
        }
      }
      return values
    }

    override val entries: MutableSet<MutableEntry<String, Any>> get() {
      val values: MutableSet<MutableEntry<String, Any>> = HashSet()
      for (s in keys) {
        values.add(
          object : MutableEntry<String, Any> {
            override val key: String get() = s
            override val value: Any get() = get(s)!!
            override fun setValue(value: Any): Any {
              return put(s, value)!!
            }
          },
        )
      }
      return values
    }

    override fun put(name: String, value: Any): Any? {
      val previous = get(name)
      languageBindings.putMember(name, value)
      return previous
    }

    override fun putAll(toMerge: Map<out String, Any>) {
      for ((key, value) in toMerge) {
        put(key, value)
      }
    }

    override fun containsKey(key: String?): Boolean {
      return if (key is String) {
        languageBindings.hasMember(key)
      } else {
        false
      }
    }

    override fun get(key: String?): Any? {
      if (key is String) {
        val value = languageBindings.getMember(key)
        if (value != null) {
          return value.`as`(Any::class.java)
        }
      }
      return null
    }

    override fun remove(key: String?): Any? {
      val prev = get(key)
      return if (prev != null) {
        languageBindings.removeMember(key as String)
        prev
      } else {
        null
      }
    }
  }
}

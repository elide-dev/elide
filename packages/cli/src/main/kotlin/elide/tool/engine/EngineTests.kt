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

@file:Suppress("RubyInterpreter", "RubyResolve")

package elide.tool.engine

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Requires
import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import elide.tool.annotations.EmbeddedTest
import elide.tool.testing.SelfTest
import elide.tool.testing.TestContext.Companion.assertDoesNotThrow


abstract class LanguageCondition (private val language: String): Condition {
  companion object {
    private val ENGINE = Engine.create()
  }

  override fun matches(context: ConditionContext<*>): Boolean {
    return ENGINE.languages.containsKey(language)
  }
}

class JsEngineCondition: LanguageCondition("js")
class PythonEngineCondition: LanguageCondition("python")
class RubyEngineCondition: LanguageCondition("ruby")
class JvmEngineCondition: LanguageCondition("java")
class LlvmEngineCondition: LanguageCondition("llvm")
class WasmEngineCondition: LanguageCondition("wasm")

/** Basic engine tests. */
@Bean @EmbeddedTest class EngineTest : SelfTest() {
  override suspend fun SelfTestContext.test() = assertDoesNotThrow {
    Engine.create()
  }.let {
    assertNotNull(it, "should be able to create an engine")
    assertTrue(it.languages.isNotEmpty(), "should be able to query engine languages")

    assertTrue(it.languages.containsKey("js")) {
      "javascript language support should always be present"
    }
  }
}

/** JavaScript engine tests. */
@Requires(condition = JsEngineCondition::class)
@Bean @EmbeddedTest class JsEngineTest : SelfTest() {
  override suspend fun SelfTestContext.test() = assertDoesNotThrow {
    Context.newBuilder("js").engine(Engine.create()).build()
  }.let {
    assertNotNull(it, "should be able to create plain js context")
    it.use { ctx ->
      try {
        ctx.enter()
        val result = ctx.eval(Source.create(
          "js",
          // language=javascript
          """
          function hello() {
              return "hi";
          }
          hello();
          """.trimIndent()
        ))

        assertNotNull(result, "should get result from pure js execution")
        assertFalse(result.isNull, "should not get `null` guest value for expected \"hi\"")
        assertEquals("hi", result.asString(), "should be able to decode guest string")
      } finally {
        ctx.leave()
      }
    }
  }
}

/** WASM engine tests. */
@Requires(condition = WasmEngineCondition::class)
@Bean @EmbeddedTest class WasmEngineTest : SelfTest() {
  override suspend fun SelfTestContext.test() = assertDoesNotThrow {
    Context.newBuilder("wasm").engine(Engine.create()).build()
  }.let {
    assertNotNull(it, "should be able to create plain wasm context")
  }
}

/** Python engine tests. */
@Requires(condition = PythonEngineCondition::class)
@Bean @EmbeddedTest class PythonEngineTest : SelfTest() {
  override suspend fun SelfTestContext.test() = assertDoesNotThrow {
    Context.newBuilder("python").engine(Engine.create()).build()
  }.let {
    assertNotNull(it, "should be able to create plain python context")
    it.use { ctx ->
      try {
        ctx.enter()
        val result = ctx.eval(Source.create(
          "python",
          // language=python
          """
          def hello():
            return "hi"
          hello()
          """.trimIndent()
        ))

        assertNotNull(result, "should get result from pure python execution")
        assertFalse(result.isNull, "should not get `null` guest value for expected \"hi\"")
        assertEquals("hi", result.asString(), "should be able to decode guest string")
      } finally {
        ctx.leave()
      }
    }
  }
}

/** Ruby engine tests. */
@Requires(condition = RubyEngineCondition::class)
@Bean @EmbeddedTest class RubyEngineTest : SelfTest() {
  override suspend fun SelfTestContext.test() = assertDoesNotThrow {
    Context.newBuilder("ruby").engine(Engine.create()).build()
  }.let {
    assertNotNull(it, "should be able to create plain ruby context")
    it.use { ctx ->
      try {
        ctx.enter()
        val result = ctx.eval(Source.create(
          "ruby",
          // language=ruby
          """
          def hello
            "hi"
          end
          hello
          """.trimIndent()
        ))

        assertNotNull(result, "should get result from pure ruby execution")
        assertFalse(result.isNull, "should not get `null` guest value for expected \"hi\"")
        assertEquals("hi", result.asString(), "should be able to decode guest string")
      } finally {
        ctx.leave()
      }
    }
  }
}

/** JVM engine tests. */
@Requires(condition = JvmEngineCondition::class)
@Bean @EmbeddedTest class JvmEngineTest : SelfTest() {
  override suspend fun SelfTestContext.test() = assertDoesNotThrow {
    Context.newBuilder("java").engine(Engine.create()).build()
  }.let {
    assertNotNull(it, "should be able to create plain java context")
  }
}

/** Should always provide an OS name. */
@Bean @EmbeddedTest class OsNameTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertNotNull(System.getProperty("os.name"), "`os.name` should not be `null`")
  }
}

/** Should always provide an OS arch. */
@Bean @EmbeddedTest class OsArchTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertNotNull(System.getProperty("os.arch"), "`os.arch` should not be `null`")
  }
}

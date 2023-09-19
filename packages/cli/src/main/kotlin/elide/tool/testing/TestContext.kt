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

package elide.tool.testing

import java.util.concurrent.atomic.AtomicReference
import elide.tool.testing.TestContext.TestStage.DONE
import elide.tool.testing.TestContext.TestStage.EXECUTING

/**
 * TBD.
 */
interface TestContext: AutoCloseable {
  /** Static utilities for test contexts. */
  object Utils {
    /** Enforce that [stage] is present in the [allowed] stages. */
    @JvmStatic fun enforce(stage: TestStage, vararg allowed: TestStage) {
      require(allowed.contains(stage)) {
        "Cannot perform step in stage '${stage.name}'"
      }
    }

    /** Prepare and run an assertion. */
    @JvmStatic suspend fun assertion(
      name: String,
      stage: TestStage,
      op: suspend () -> Pair<Boolean, Pair<String, Throwable?>?>,
    ) {
      enforce(stage, EXECUTING)
      val (pass, err) = op.invoke()
      if (!pass) throw AssertionFailure(
        name,
        err?.first,
        err?.second,
      )
    }
  }

  @Suppress("unused") companion object {
    /**
     * TBD.
     */
    suspend inline fun <reified X> assertThrows(
      failureMessage: String? = null,
      crossinline op: suspend () -> Unit,
    ): X {
      try {
        op.invoke()
        throw AssertionFailure(
          "assertThrows",
          failureMessage ?: "expected to catch type '${X::class.java.name}', but nothing was thrown"
        )
      } catch (err: Throwable) {
        if (err !is X) throw AssertionFailure(
          "assertThrows",
          failureMessage ?: "expected to throw type '${X::class.java.name}', but got ${err::class.java.name}",
          err,
        )
        return err
      }
    }

    /**
     * TBD.
     */
    suspend inline fun <R> assertDoesNotThrow(
      failureMessage: String? = null,
      crossinline op: suspend () -> R,
    ): R {
      return try {
        op.invoke()
      } catch (err: Throwable) {
        throw AssertionFailure(
          "assertDoesNotThrow",
          failureMessage ?: "expected not to throw, but caught ${err::class.java.name}",
          err,
        )
      }
    }
  }

  /**
   * TBD.
   */
  class AssertionFailure(
    private val name: String,
    message: String? = null,
    err: Throwable? = null,
  ) : RuntimeException(message, err) {
    override fun toString(): String = message ?: "failed $name"
  }

  /**
   * TBD.
   */
  enum class TestStage {
    PENDING,
    EXECUTING,
    DONE;
  }

  /**
   * TBD.
   */
  val stage: TestStage

  /**
   * TBD.
   */
  val result: AtomicReference<TestResult>

  /**
   * TBD.
   */
  suspend fun assert(
    condition: Boolean,
    op: () -> String = { "assertion failed" },
  ) = Utils.assertion("assert", stage) {
    if (condition) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assert(
    condition: Boolean,
    failureMessage: String,
  ) = Utils.assertion("assert", stage) {
    if (condition) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertTrue(
    condition: Boolean,
    op: () -> String = { "assertion failed: not `true`" },
  ) = Utils.assertion("assertTrue", stage) {
    if (condition) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertTrue(condition: Boolean, failureMessage: String) = Utils.assertion("assertTrue", stage) {
    if (condition) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertFalse(
    condition: Boolean,
    op: () -> String = { "assertion failed: not `false`" },
  ) = Utils.assertion("assertFalse", stage) {
    if (!condition) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertFalse(condition: Boolean, failureMessage: String) = Utils.assertion("assertFalse", stage) {
    if (!condition) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertNotNull(
    value: Any?,
    op: () -> String = { "required non-null, got `null`" },
  ) = Utils.assertion("assertNotNull", stage) {
    if (value != null) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertNotNull(
    value: Any?,
    failureMessage: String,
  ) = Utils.assertion("assertNotNull", stage) {
    if (value != null) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertNull(
    value: Any?,
    op: () -> String = { "required `null`, got $value" },
  ) = Utils.assertion("assertNull", stage) {
    if (value != null) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertNull(
    value: Any?,
    failureMessage: String,
  ) = Utils.assertion("assertNull", stage) {
    if (value != null) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertEquals(
    expected: Any?,
    value: Any?,
    op: () -> String = { "value '$value' did not equal expected '$expected' when it should" },
  ) = Utils.assertion("assertEquals", stage) {
    if (value == expected) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertEquals(
    expected: Any?,
    value: Any?,
    failureMessage: String,
  ) = Utils.assertion("assertEquals", stage) {
    if (value == expected) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertNotEquals(
    expected: Any?,
    value: Any?,
    op: () -> String = { "value '$value' equals expected ('$expected') when it should not" },
  ) = Utils.assertion("assertNotEquals", stage) {
    if (value != expected) {
      true to null
    } else {
      false to (op.invoke() to null)
    }
  }

  /**
   * TBD.
   */
  suspend fun assertNotEquals(
    expected: Any?,
    value: Any?,
    failureMessage: String,
  ) = Utils.assertion("assertNotEquals", stage) {
    if (value != expected) {
      true to null
    } else {
      false to (failureMessage to null)
    }
  }
}

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

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import elide.tool.testing.TestResult.Result.*

/**
 * TBD.
 */
sealed interface TestResult {
  /**
   * TBD.
   */
  enum class Result {
    /** The test passed. */
    PASS,

    /** The test failed. */
    FAIL,

    /** The test was ignored. */
    SKIP,

    /** The test was dropped (it is ignored, but not counted as skipped). */
    DROP;

    /** Whether this status should count as a passing effective status. */
    val ok: Boolean get() = when (this) {
      SKIP,
      DROP,
      PASS -> true
      FAIL -> false
    }
  }

  /** Result of the test. */
  val result: Result

  /** Effective (final) result of the test. */
  val effectiveResult: Result

  /** Error captured during execution of the test. */
  val err: Throwable?

  /** Timestamp indicating when the test started. */
  val start: Instant

  /** Timestamp indicating when the test ended. */
  val end: Instant

  /** Output from the test, if any. */
  val output: StringBuilder?

  /** Output from the test, if any. */
  val errOutput: StringBuilder?

  /** Test which produced this result. */
  val test: Testable<*>

  /** Generic info about the test. */
  val info: TestInfo

  /** Test which produced this result. */
  val messages: Collection<String>

  /**
   * TBD.
   */
  @JvmRecord data class TestResultInfo(
    override val result: Result,
    override val err: Throwable?,
    override val start: Instant,
    override val end: Instant,
    override val output: StringBuilder?,
    override val errOutput: StringBuilder?,
    override val test: Testable<*>,
    override val info: TestInfo,
    override val messages: Collection<String>,
  ): TestResult {
    companion object {
      @JvmStatic fun fromInfo(
        result: Result,
        test: Testable<out TestContext>,
        start: Instant,
        info: TestInfo,
        end: Instant,
        messages: Collection<String> = emptyList(),
        output: StringBuilder? = null,
        errOutput: StringBuilder? = null,
      ): TestResultInfo = TestResultInfo(
        result = result,
        err = null,
        start = start,
        end = end,
        output = output,
        errOutput = errOutput,
        test = test,
        info = info,
        messages = messages,
      )

      @JvmStatic fun fromError(
        result: Result,
        test: Testable<out TestContext>,
        start: Instant,
        info: TestInfo,
        end: Instant,
        err: Throwable? = null,
        messages: Collection<String> = emptyList(),
        output: StringBuilder? = null,
        errOutput: StringBuilder? = null,
      ): TestResultInfo = TestResultInfo(
        result = result,
        err = err,
        start = start,
        end = end,
        output = output,
        errOutput = errOutput,
        test = test,
        info = info,
        messages = messages,
      )
    }

    override val effectiveResult: Result get() = when (result) {
      SKIP,
      DROP,
      PASS -> PASS
      FAIL -> FAIL
    }
  }

  /**
   * TBD.
   */
  @JvmInline value class Success private constructor (private val record: TestResultInfo): TestResult by record {
    override val result: Result get() = PASS

    internal companion object {
      @JvmStatic internal fun of(info: TestResultInfo): Success = Success(info)
    }
  }

  /**
   * TBD.
   */
  @JvmInline value class Failure private constructor (private val record: TestResultInfo): TestResult by record {
    override val result: Result get() = FAIL

    internal companion object {
      @JvmStatic internal fun of(info: TestResultInfo): Failure = Failure(info)
    }
  }

  /**
   * TBD.
   */
  @JvmInline value class Skipped private constructor (private val record: TestResultInfo): TestResult by record {
    override val result: Result get() = SKIP

    internal companion object {
      @JvmStatic internal fun of(info: TestResultInfo): Skipped = Skipped(info)
    }
  }

  /**
   * TBD.
   */
  @JvmInline value class Dropped private constructor (private val record: TestResultInfo): TestResult by record {
    override val result: Result get() = DROP

    internal companion object {
      @JvmStatic internal fun of(info: TestResultInfo): Dropped = Dropped(info)
    }
  }

  @Suppress("unused") companion object {
    /**
     * TBD.
     */
    @JvmStatic fun of(
      result: Result,
      test: Testable<out TestContext>,
      start: Instant,
      testInfo: TestInfo,
      err: Throwable? = null,
      end: Instant,
      messages: Collection<String> = emptyList(),
      output: StringBuilder? = null,
      errOutput: StringBuilder? = null,
    ): TestResult = when (result) {
      PASS -> Success.of(TestResultInfo.fromInfo(
        result,
        test,
        start,
        testInfo,
        end,
        messages,
        output,
        errOutput,
      ))

      FAIL -> Failure.of(TestResultInfo.fromError(
        result,
        test,
        start,
        testInfo,
        end,
        err,
        messages,
        output,
        errOutput,
      ))

      SKIP -> Skipped.of(TestResultInfo.fromInfo(
        result,
        test,
        start,
        testInfo,
        end,
        messages,
        output,
        errOutput,
      ))

      DROP -> Dropped.of(TestResultInfo.fromInfo(
        result,
        test,
        start,
        testInfo,
        end,
        messages,
        output,
        errOutput,
      ))
    }

    /**
     * TBD.
     */
    @JvmStatic fun success(
      testInfo: TestInfo,
      test: Testable<*>,
      start: Instant,
      end: Instant = Clock.System.now(),
      messages: Collection<String> = emptyList(),
      output: StringBuilder? = null,
      errOutput: StringBuilder? = null,
    ): TestResult = of(
      PASS,
      test,
      start,
      testInfo,
      messages = messages,
      end = end,
      output = output,
      errOutput = errOutput,
    )

    /**
     * TBD.
     */
    @JvmStatic fun failure(
      testInfo: TestInfo,
      test: Testable<*>,
      start: Instant,
      end: Instant = Clock.System.now(),
      err: Throwable? = null,
      messages: Collection<String> = emptyList(),
      output: StringBuilder? = null,
      errOutput: StringBuilder? = null,
    ): TestResult = of(
      FAIL,
      test,
      start,
      testInfo,
      err,
      messages = messages,
      end = end,
      output = output,
      errOutput = errOutput,
    )

    /**
     * TBD.
     */
    @JvmStatic fun skipped(
      testInfo: TestInfo,
      test: Testable<*>,
      start: Instant,
      end: Instant = Clock.System.now(),
      messages: Collection<String> = emptyList(),
      output: StringBuilder? = null,
      errOutput: StringBuilder? = null,
    ): TestResult = of(
      SKIP,
      test,
      start,
      testInfo,
      end = end,
      messages = messages,
      output = output,
      errOutput = errOutput,
    )

    /**
     * TBD.
     */
    @JvmStatic fun dropped(
      testInfo: TestInfo,
      test: Testable<*>,
      start: Instant,
      end: Instant = Clock.System.now(),
    ): TestResult = of(
      DROP,
      test,
      start,
      testInfo,
      end = end,
    )
  }
}

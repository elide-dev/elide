/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.intrinsics.testing

import java.lang.AutoCloseable
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.interop.ReadOnlyProxyObject
import elide.tooling.testing.TestScope
import elide.tooling.testing.TestOutcome

/**
 * # Testing Registrar
 *
 * Accepts guest-side symbols which are registered for the purpose of running tests; such tests are typically registered
 * by built-ins or by functions/annotations dedicated to test registration in a given guest context.
 */
public interface TestingRegistrar {
  /**
   * Make the internals of this testing registrar immutable; this call is emitted right before the registrar is
   * interrogated for registered tests.
   *
   * Calls to register tests after this call fail with an exception.
   *
   * @return Self.
   */
  public fun freeze(): TestingRegistrar

  /**
   * Retrieve all registered tests witnessed by this registrar.
   *
   * @return Sequence of all tests.
   */
  public fun all(): Sequence<RegisteredTest>

  /**
   * Retrieve all registered tests, emitted by scope, witnessed by this registrar.
   *
   * @return Sequence of all tests, flattened, each paired with its scope.
   */
  public fun grouped(): Sequence<Pair<TestScope<*>, RegisteredTest>>

  /**
   * Register a test with this registrar.
   *
   * @param test Test instance to register.
   */
  public fun register(test: RegisteredTest)

  /**
   * Register a scope with this registrar.
   *
   * @param scope Scope instance to register.
   */
  public fun register(scope: RegisteredScope)

  /**
   * Register a test with this registrar, and within the provided [scope].
   *
   * @param test Test instance to register.
   * @param scope Test scope this instance should be a member of.
   */
  public fun register(test: RegisteredTest, scope: RegisteredScope)

  /**
   * Wrap this register in a nested [scope]; any further calls to [register] (with the returned [Scoped] registrar) will
   * automatically register tests against the scope in question.
   *
   * @param scope Scope to register and build.
   * @return Scoped testing registrar.
   */
  public fun withinScope(scope: RegisteredScope): Scoped

  /**
   * Reset the active test scope, if any.
   */
  public fun resetScope()

  /**
   * ## Scoped Testing Registrar
   *
   * Describes a [TestingRegistrar] which is bound to a given [RegisteredScope]; such registrars automatically file any
   * registered tests against the bound scope.
   */
  public interface Scoped : TestingRegistrar, AutoCloseable {
    public val scope: RegisteredScope
    public val parent: TestingRegistrar

    override fun close() {
      parent.resetScope()
    }
  }

  /**
   * ## Registered Scope
   *
   * Describes a "scope" or "group" of [RegisteredTest] instances; all [RegisteredTest] instances have a scope of some
   * kind, whether it is the root scope describing all tests, or a customized scope, provided by the developer, or an
   * implied scope, like the test class case holding a unit test method.
   */
  public sealed interface RegisteredScope:
    ReadOnlyProxyObject,
    TestingAPI.TestGraphNode.Suite,
    TestScope<RegisteredScope>,
    Comparable<RegisteredScope> {

    // Simple member keys (a name).
    override fun getMemberKeys(): Array<String> = arrayOf("name")
    override fun getMember(key: String): PolyglotValue? {
      return when (key) {
        "name" -> PolyglotValue.asValue(simpleName)
        else -> null
      }
    }
  }

  /**
   * ## Registered Test
   *
   * Describes a test which was registered by guest-side detection. Registered tests consist of at least a qualified
   * name, a simple name, and executable entrypoint. Guest languages can customize this type to affix additional typed
   * information to registered tests.
   *
   * Registered tests are comparable by their [qualifiedName], allowing them to be stored and emitted in a deterministic
   * order regardless of load-order. See [TestingRegistrar] for registered test access.
   */
  public sealed interface RegisteredTest:
    TestingAPI.TestGraphNode.Test,
    TestScope<RegisteredTest>,
    Comparable<RegisteredTest> {
    /**
     * ### Simple name.
     *
     * Provides a display name for the test in reports, on the console, and in other circumstances, where a full name is
     * not desirable.
     */
    override val simpleName: String

    /**
     * ### Qualified name.
     *
     * Well-qualified name for the test, which is unique within the scope of a codebase; qualified names derive from
     * language semantics. For example, a well-qualified JVM test name might be composed of the class name and function
     * name of the test.
     */
    override val qualifiedName: String

    /**
     * ### Entrypoint factory.
     *
     * Factory which provides an instance of [TestEntrypoint], which can be used to execute the test and obtain a
     * [TestOutcome].
     */
    public val entryFactory: (PolyglotContext) -> TestEntrypoint
  }

  /**
   * ## Registered Test (Guest)
   *
   * Extends [RegisteredTest] to make available typed access to a [guestValueFactory].
   */
  public sealed interface RegisteredGuestTest : RegisteredTest {
    /**
     * ### Guest value factory.
     *
     * Obtains the [PolyglotValue] defining this test; this is typically the [TestEntrypoint], but unwrapped. Guest
     * values for tests may or may not be available at registration time.
     */
    public val guestValueFactory: (PolyglotContext) -> PolyglotValue
  }

  /** Simple named test scope. */
  @JvmRecord public data class NamedScope internal constructor (
    override val simpleName: String,
    override val qualifiedName: String,
  ): RegisteredScope

  /** Registered test info. */
  @JvmRecord public data class TestInfo internal constructor (
    override val simpleName: String,
    override val qualifiedName: String,
    override val entryFactory: (PolyglotContext) -> TestEntrypoint,
    override val guestValueFactory: (PolyglotContext) -> PolyglotValue,
  ): RegisteredGuestTest

  /** Factories for creating [TestingRegistrar] accoutrement objects. */
  public companion object {
    @JvmStatic public fun qualifiedNameForBlock(block: PolyglotValue, label: String?): String {
      val sourceFile = block.sourceLocation.source.name
      val fullLabel = "$sourceFile > ${label ?: block.metaSimpleName}"
      return fullLabel
    }

    @JvmStatic public fun qualifiedNameToSimpleName(qual: String): String {
      return qual.split(">").last().trim()
    }

    /** @return Scope using the provided [simpleName] and [qualifiedName]. */
    @JvmStatic public fun namedScope(simpleName: String, qualifiedName: String): NamedScope {
      return NamedScope(simpleName = simpleName, qualifiedName = qualifiedName)
    }

    /** @return Scope using the provided [block] and optional [label]. */
    @JvmStatic public fun namedScope(block: PolyglotValue, label: String? = null): NamedScope {
      return qualifiedNameForBlock(block, label).let { qual ->
        NamedScope(simpleName = label ?: qualifiedNameToSimpleName(qual), qualifiedName = qual)
      }
    }

    // Default entrypoint factory.
    @Suppress("TooGenericExceptionCaught")
    private fun defaultEntryFactory(block: PolyglotValue) = TestEntrypoint {
      try {
        require(block.canExecute()) { "Block is not executable: $block" }
        block.executeVoid()
        TestOutcome.Success
      } catch (err: Throwable) {
        TestOutcome.Failure(err)
      }
    }

    /**
     * Wires together a guest-side test fragment, using the provided [block], optional [label], and optional testing
     * [scope].
     *
     * @param block Block to execute as a test.
     * @param label Optional label for the test.
     * @param scope Optional scope for the test.
     * @return Test info for the test; suitable for registration.
     */
    @JvmStatic public fun guest(
      block: PolyglotValue,
      label: String? = null,
      scope: TestScope<*>? = null,
    ): TestInfo = TestInfo(
      simpleName = label ?: qualifiedNameToSimpleName(qualifiedNameForBlock(block, label)),
      qualifiedName = scope?.qualifiedNameFor(block, label) ?: qualifiedNameForBlock(block, label),
      entryFactory = { defaultEntryFactory(block) },
      guestValueFactory = { block },
    )

    /**
     * Creates a forward-declared registered test record, which can be resolved later from a polyglot context.
     *
     * @param label Label for this specific test.
     * @param qualified Qualified name for this test.
     * @param blockFactory Factory which produces this test's entrypoint, given a polyglot context.
     * @return Test info for the test; suitable for registration.
     */
    @JvmStatic public fun deferred(
      label: String,
      qualified: String,
      blockFactory: (ctx: PolyglotContext) -> PolyglotValue,
    ): TestInfo = TestInfo(
      simpleName = label,
      qualifiedName = qualified,
      entryFactory = {
        defaultEntryFactory(blockFactory.invoke(it).also {
          require(it.canExecute()) { "Block is not executable: $it" }
        })
      },
      guestValueFactory = {
        blockFactory.invoke(it).also {
          require(it.canExecute()) { "Block is not executable: $it" }
        }
      },
    )
  }
}

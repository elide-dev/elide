package elide.exec

import kotlinx.coroutines.test.runTest

inline fun testInScope(crossinline block: suspend ActionScope.() -> Unit): Unit = runTest {
  Action.withScope {
    block()
  }
}

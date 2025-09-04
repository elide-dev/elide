package elide.tooling.testing

import elide.annotations.Singleton

@Singleton public class TestDriverRegistry {
  private val drivers = mutableSetOf<TestDriver<*>>()

  public fun <T : TestCase> register(driver: TestDriver<T>) {
    drivers.find { it.type == driver.type }?.let { driver ->
      throw IllegalArgumentException("Test type ${driver.type} is already handled by $driver")
    }

    drivers.add(driver as TestDriver<*>)
  }

  @Suppress("UNCHECKED_CAST")
  public fun collect(): Set<TestDriver<TestCase>> = drivers.toSet() as Set<TestDriver<TestCase>>
}

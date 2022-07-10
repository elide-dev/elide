package elide.server

/** General utilities for use in Elide tests. */
public object TestUtil {
  /**
   * Load a text fixture file for a test, as a string, from a classpath resource.
   *
   * @param name Name of the resource to load
   * @return File contents.
   */
  public fun loadFixture(name: String): String = TestUtil::class.java.getResourceAsStream(
    name
  ).bufferedReader().use {
    it.readText()
  }

  /**
   * Load a binary fixture from a file for a test, as a string, from a classpath resource.
   *
   * @param name Name of the resource to load
   * @return File contents.
   */
  public fun loadBinary(name: String): ByteArray = TestUtil::class.java.getResourceAsStream(
    name
  ).buffered().use {
    it.readAllBytes()
  }
}

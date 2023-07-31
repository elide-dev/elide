@file:Suppress("RedundantVisibilityModifier")

package elide.proto.test.data

/** Abstract tests for data containers. */
public abstract class AbstractDataContainerTests<Container> {
  /**
   * @return New empty data container.
   */
  public abstract fun allocateContainer(): Container

  /**
   * @return New data container with the specified [data] string.
   */
  public abstract fun allocateContainer(data: String): Container

  /**
   * @return New data container with the specified [data] bytes.
   */
  public abstract fun allocateContainer(data: ByteArray): Container

  /** Test creating a simple data container. */
  public abstract fun testDataContainer()

  /** Test encoding a data container as JSON. */
  public abstract fun testDataContainerJson()
}

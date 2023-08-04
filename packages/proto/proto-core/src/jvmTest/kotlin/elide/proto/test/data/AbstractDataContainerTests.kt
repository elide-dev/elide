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

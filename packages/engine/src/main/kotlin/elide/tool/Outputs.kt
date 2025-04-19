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

package elide.tool

import kotlinx.serialization.Serializable

/**
 * # Program Outputs
 */
@Serializable
public sealed interface Outputs {
  /**
   * ## Outputs: None.
   */
  public interface None : Outputs

  /**
   * ## Outputs: In-memory.
   */
  public sealed interface Memory : Outputs

  /**
   * ## Outputs: On-disk.
   */
  public sealed interface Disk : Outputs {
    /**
     * On-disk outputs: Single file.
     */
    public interface File : Disk

    /**
     * On-disk outputs: Directory.
     */
    public interface Directory : Disk
  }
}

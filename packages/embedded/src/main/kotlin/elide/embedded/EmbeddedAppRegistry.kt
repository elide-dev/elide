/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded

/**
 * A registry for guest applications.
 */
public interface EmbeddedAppRegistry {
  /**
   * Register a new guest app using the specified [id] and [config], returning an [EmbeddedApp] reference that can be
   * used to track the state of the application.
   *
   * If a guest application with the same [id] already exists, an exception will be thrown.
   */
  public fun register(id: EmbeddedAppId, config: EmbeddedAppConfiguration): EmbeddedApp

  /**
   * Remove a guest app with the specified [id] from the registry and [cancel][EmbeddedApp.cancel] it. The application
   * will be unusable after this operation completes.
   */
  public fun remove(id: EmbeddedAppId): Boolean

  /**
   * Returns a guest [EmbeddedApp] with the specified [id] in this registry, or `null` if no application has been
   * registered with that key.
   */
  public fun resolve(id: EmbeddedAppId): EmbeddedApp?

  /**
   * Close the registry, removing and cancelling every registered app. Attempting to [register] new applications after
   * cancellation will throw an exception.
   */
  public fun cancel(): Boolean
}

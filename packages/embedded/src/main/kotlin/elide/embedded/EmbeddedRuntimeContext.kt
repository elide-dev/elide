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

/** A specialized DI container used by the embedded runtime to resolve core services. */
public interface EmbeddedRuntimeContext {
  /** Runtime configuration used by this context. */
  public val configuration: EmbeddedConfiguration

  /** The [EmbeddedAppRegistry] used by the runtime. */
  public val appRegistry: EmbeddedAppRegistry

  /** The call dispatcher used by the runtime. */
  public val dispatcher: EmbeddedCallDispatcher

  /** The embedded call codec used by the runtime. */
  public val codec: EmbeddedCallCodec
}

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
package elide.runtime.gvm.internals.intrinsics.js

import elide.runtime.gvm.GuestLanguage
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.intrinsics.GuestIntrinsic

/** Abstract base class for all intrinsic implementations. */
public abstract class AbstractJsIntrinsic : GuestIntrinsic {
  override fun language(): GuestLanguage = GraalVMGuest.JAVASCRIPT
  override fun symbolicName(): String = "native code"
  @Deprecated("Use symbolicName instead", ReplaceWith("symbolicName"))
  override fun displayName(): String = "native code"

  override fun toString(): String = "[${symbolicName()}]"
}

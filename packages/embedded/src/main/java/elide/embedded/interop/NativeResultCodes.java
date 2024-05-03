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

package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;


/**
 * A collection of C constants representing well-known error codes for runtime operations. During a native build,
 * calls to the methods provided by this class will be replaced with their matching constant value.
 */
@CContext(ElideNativeDirectives.class)
final class NativeResultCodes {
  private NativeResultCodes() {
    // sealed constructor for static class
  }

  /**
   * Constant value for a successful result code.
   */
  @CConstant("ELIDE_OK")
  static native int ok();

  /**
   * Constant value for an unknown error code, returned when an exception is thrown during a runtime operation.
   */
  @CConstant("ELIDE_ERR_UNKNOWN")
  static native int unknownError();

  /**
   * Error code used when the runtime is not yet initialized, but initialization is required for the operation.
   */
  @CConstant("ELIDE_ERR_UNINITIALIZED")
  static native int uninitialized();

  /**
   * Error code used when the runtime's initialization function is called more than once.
   */
  @CConstant("ELIDE_ERR_ALREADY_INITIALIZED")
  static native int alreadyInitialized();
}

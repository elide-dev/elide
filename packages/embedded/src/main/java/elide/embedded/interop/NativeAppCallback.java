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

import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;


/**
 * Represents a native C function which accepts a single {@code int} argument. This callback type is used for async
 * operations (e.g. app startup and shutdown), allowing C code to receive a notification when the operation completes.
 * <p>
 * Use the {@link #call(int)} method to invoke the native function.
 */
@CTypedef(name = "elide_app_callback_t")
interface NativeAppCallback extends CFunctionPointer {
  /**
   * Invoke the native function held by this pointer using an integer result code.
   */
  @InvokeCFunctionPointer
  void call(int result);
}

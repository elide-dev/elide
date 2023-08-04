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

package elide.runtime.gvm.internals.intrinsics

import org.graalvm.nativeimage.Isolate
import org.graalvm.nativeimage.IsolateThread
import org.graalvm.nativeimage.c.function.CEntryPoint

/**
 * TBD.
 */
internal object NativeRuntime {
  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.CREATE_ISOLATE, name = "create_isolate")
  @JvmStatic external fun createIsolate(): IsolateThread?

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.TEAR_DOWN_ISOLATE, name = "tear_down_isolate")
  @JvmStatic external fun tearDownIsolate(thread: IsolateThread)

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.ATTACH_THREAD, name = "attach_thread")
  @JvmStatic external fun attachThread(isolate: Isolate): IsolateThread?

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.DETACH_THREAD, name = "detach_thread")
  @JvmStatic external fun detachThread(thread: IsolateThread): Int

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.GET_CURRENT_THREAD, name = "get_current_thread")
  @JvmStatic external fun currentThread(isolate: Isolate): IsolateThread?

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.GET_ISOLATE, name = "get_isolate")
  @JvmStatic external fun isolateForThread(thread: IsolateThread): Isolate?
}

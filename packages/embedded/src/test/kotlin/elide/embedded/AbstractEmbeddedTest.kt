/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

import org.graalvm.nativeimage.ImageInfo
//import org.graalvm.nativeimage.Isolate
//import org.graalvm.nativeimage.IsolateThread
//import org.graalvm.nativeimage.Isolates
//import org.graalvm.nativeimage.Isolates.CreateIsolateParameters
import org.mockito.Mockito.mock
import kotlinx.coroutines.test.runTest

/** Shared test utilities for embedded runtime testing. */
abstract class AbstractEmbeddedTest {
  interface EmbeddedTestContext {
    //
  }

  companion object {
    const val EMBEDDED_API_VERSION: String = "v1alpha1"


    inline fun embeddedTest(crossinline op: suspend EmbeddedTestContext.() -> Unit): Unit = runTest {
//      val (mockThread, mockIsolate) = mock(IsolateThread::class.java) to mock(Isolate::class.java)
      (object: EmbeddedTestContext {}).apply {
        op()
      }
    }
  }
}

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
package elide.runtime.localai

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import elide.runtime.localai.NativeLocalAi.Model
import elide.runtime.localai.NativeLocalAi.Parameters
import elide.testing.annotations.Test

class NativeLocalAiTest {
  private companion object {
    private val llama2_7b = Model.huggingface(
      repo = "TheBloke/Llama-2-7B-Chat-GGUF",
      name = "llama-2-7b-chat.Q4_K_M.gguf",
    )
  }

  @Test fun testEnsureAvailable() {
    assertDoesNotThrow { NativeLocalAi.ensureAvailable() }
    assertDoesNotThrow { NativeLocalAi.ensureAvailable() }
    assertDoesNotThrow { NativeLocalAi.ensureAvailable() }
  }

  @Test fun testBasicPrompt() {
    val results = assertDoesNotThrow {
      NativeLocalAi.inferSync(
        Parameters.defaults(),
        model = llama2_7b,
        prompt = "Complete the sentence and don't add more: The quick brown fox jumped over",
      )
    }
    assertNotNull(results)
  }
}

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
package elide.runtime.intrinsics.ai

import org.junit.jupiter.api.assertNotNull
import kotlin.test.Ignore
import kotlin.test.assertEquals
import elide.annotations.Inject
import elide.runtime.node.ElideJsModuleTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

// Tests for Elide's built-in AI modules.
@TestCase internal class ElideAIModuleTest : ElideJsModuleTest<ElideLLMModule>() {
  @Inject lateinit var module: ElideLLMModule
  override fun provide(): ElideLLMModule = module
  override val pureModuleName: String get() = "llm"

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("local")
    yield("remote")
    yield("params")
    yield("huggingface")
    yield("version")
    yield("infer")
    yield("inferSync")
  }

  @Test override fun testInjectable() {
    assertNotNull(module) { "Main LLM module should be injectable" }
  }

  @Test fun testLlmApiVersion() = dual {
    val expected = "v1"
    val version = provide().provide().version()
    assertEquals(expected, version)
  }.guest {
    // language=JavaScript
    """
      const { version } = require("elide:llm");
      test(version()).isNotNull();
      test(version()).isEqualTo('v1');
    """
  }

  @Test fun testModelParameterSpec() = executeGuest {
    // language=JavaScript
    """
      const { params } = require("elide:llm");
      const modelParams = params();
      test(modelParams).isNotNull();
    """
  }.doesNotFail()

  @Test fun testHuggingFaceModelSpec() = executeGuest {
    // language=JavaScript
    """
      const { huggingface } = require("elide:llm");
      const spec = huggingface({
        "repo": "TheBloke/Llama-2-7B-Chat-GGUF",
        "name": "llama-2-7b-chat.Q4_K_M.gguf"
      })
      test(spec).isNotNull();
    """
  }.doesNotFail()

  @Test fun testCanImportLocalFromTop() = executeGuest {
    // language=JavaScript
    """
      const { local } = require("elide:llm");
      test(local).isNotNull();
    """
  }.doesNotFail()

  @Test fun testCanImportRemoteFromTop() = executeGuest {
    // language=JavaScript
    """
      const { remote } = require("elide:llm");
      test(remote).isNotNull();
    """
  }.doesNotFail()

  @Test fun testCanImportLocalDirectly() = executeGuest {
    // language=JavaScript
    """
      const local = require("elide:llm/local");
      test(local).isNotNull();
    """
  }.doesNotFail()

  @Test fun testCanImportRemoteDirectly() = executeGuest {
    // language=JavaScript
    """
      const remote = require("elide:llm/remote");
      test(remote).isNotNull();
    """
  }.doesNotFail()

  @Test fun testLlmApiVersionLocalFromTop() = dual {
    val expected = "v1"
    val version = provide().provide().version()
    assertEquals(expected, version)
  }.guest {
    // language=JavaScript
    """
      const { local } = require("elide:llm");
      test(local.version()).isNotNull();
      test(local.version()).isEqualTo('v1');
    """
  }

  @Test fun testLlmApiVersionLocalDirect() = dual {
    val expected = "v1"
    val version = provide().provide().version()
    assertEquals(expected, version)
  }.guest {
    // language=JavaScript
    """
      const { version } = require("elide:llm/local");
      test(version()).isNotNull();
      test(version()).isEqualTo('v1');
    """
  }

  @Test fun testLlmApiVersionRemoteFromTop() = dual {
    val expected = "v1"
    val version = provide().provide().version()
    assertEquals(expected, version)
  }.guest {
    // language=JavaScript
    """
      const { remote } = require("elide:llm");
      test(remote.version()).isNotNull();
      test(remote.version()).isEqualTo('v1');
    """
  }

  @Test fun testLlmApiVersionRemoteDirect() = dual {
    val expected = "v1"
    val version = provide().provide().version()
    assertEquals(expected, version)
  }.guest {
    // language=JavaScript
    """
      const { version } = require("elide:llm/remote");
      test(version()).isNotNull();
      test(version()).isEqualTo('v1');
    """
  }

  @Test @Ignore fun testHuggingFaceInferSync() = executeGuest {
    // language=JavaScript
    """
      const { huggingface, params } = require("elide:llm");
      const { inferSync } = require("elide:llm/local");

      const llama2_7b = huggingface({
        "repo": "TheBloke/Llama-2-7B-Chat-GGUF",
        "name": "llama-2-7b-chat.Q4_K_M.gguf"
      })
      test(llama2_7b).isNotNull();
      const result = inferSync(
        params(),
        llama2_7b,
        "Complete the sentence: The quick brown fox jumped over",
      )
      test(result).isNotNull();
      test(typeof result === 'string').isEqualTo(true);
    """
  }.doesNotFail()

  @Test @Ignore fun testHuggingFaceInfer() = executeGuest {
    // language=JavaScript
    """
      const { huggingface, params } = require("elide:llm");
      const { infer } = require("elide:llm/local");

      const llama2_7b = huggingface({
        "repo": "TheBloke/Llama-2-7B-Chat-GGUF",
        "name": "llama-2-7b-chat.Q4_K_M.gguf"
      })
      test(llama2_7b).isNotNull();
      async function doTest() {
        const result = await infer(
            params(),
            llama2_7b,
            "Complete the sentence: The quick brown fox jumped over",
        )
        test(result).isNotNull();
        test(typeof result === 'string').isEqualTo(true);
        return result;
      }
      doTest().then((result) => {
        test(result).isNotNull();
      }, (err) => {
        console.error("Failed to infer:", err);
      });
    """
  }.doesNotFail()
}

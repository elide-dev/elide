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

package elide.ssr

import org.graalvm.polyglot.Context
import kotlin.test.*

/** Tests for response streaming across polyglot boundaries. */
@Ignore class StreamResponseTest {
  @Test @Ignore
  fun testSSRBasic() {
    // language=javascript
    val testCode = """
      var embedded = (() => {
          this.renderContent = function() {
              return "hello";
          }
      });
      function embeddedExecute() {
        return embedded.renderContent;
      }
      embeddedExecute();
    """.trimIndent()

    Context.newBuilder("js")
      .allowExperimentalOptions(true)
      .build()
      .use {
        val result = it.eval("js", testCode)
        assertNotNull(result)
        assertFalse(result.isString)
        assertTrue(result.canExecute())
        val execution = result.execute()
        assertNotNull(execution)
        assertTrue(execution.isString)
        assertEquals("hello", execution.asString())
      }
  }

  @Test @Ignore
  fun testSSRPluckMethod() {
    // language=javascript
    val testCode = """
      var embedded = (() => {
          this.renderContent = function() {
              return "hello";
          };
          this.streamContent = function(cbk) {
              cbk(this.renderContent());
          };
      });
      function embeddedExecute() {
        const renderContent = embedded.renderContent;
        const streamContent = embedded.streamContent;

        return {
          renderContent,
          streamContent
        };
      }
      embeddedExecute();
    """.trimIndent()

    Context.newBuilder("js")
      .allowExperimentalOptions(true)
      .build()
      .use {
        val result = it.eval("js", testCode)
        assertNotNull(result)
        assertFalse(result.isString)
        assertFalse(result.canExecute())

        val renderContent = result.getMember("renderContent")
        assertNotNull(renderContent)
        assertTrue(renderContent.canExecute())

        val streamContent = result.getMember("streamContent")
        assertNotNull(streamContent)
        assertTrue(streamContent.canExecute())

        val buf: ArrayList<String> = ArrayList()
        val handler: (String) -> Unit = { out ->
          buf.add(out)
        }
        streamContent.executeVoid(handler)
        assertEquals(1, buf.size)
        assertEquals("hello", buf[0])
      }
  }

  @Test @Ignore
  fun testSSRInterface() {
    // language=javascript
    val testCode = """
      var embedded = (() => {
          this.renderContent = function() {
              return "hello";
          };
          this.streamContent = function(cbk) {
              cbk({
                status: 200,
                headers: {},
                content: this.renderContent(),
                hasContent: true
              });
          };
      });
      function embeddedExecute() {
        const renderContent = embedded.renderContent;
        const streamContent = embedded.streamContent;

        return {
          renderContent,
          streamContent
        };
      }
      embeddedExecute();
    """.trimIndent()

    Context.newBuilder("js")
      .allowExperimentalOptions(true)
      .build()
      .use {
        val result = it.eval("js", testCode)
        assertNotNull(result)
        assertFalse(result.isString)
        assertFalse(result.canExecute())

        val renderContent = result.getMember("renderContent")
        assertNotNull(renderContent)
        assertTrue(renderContent.canExecute())

        val streamContent = result.getMember("streamContent")
        assertNotNull(streamContent)
        assertTrue(streamContent.canExecute())

        val buf: ArrayList<ServerResponse> = ArrayList()
        val handler: (ServerResponse) -> Unit = { out ->
          buf.add(out)
        }
        streamContent.executeVoid(handler)
        assertEquals(1, buf.size)
        assertEquals("hello", buf[0].content)
      }
  }

  @Test @Ignore
  fun testSSRStream() {
    // language=javascript
    val testCode = """
      var embedded = (() => {
          this.renderContent = function() {
              return "hello";
          };
          this.streamContent = function(cbk) {
              cbk({
                content: this.renderContent(),
                hasContent: true,
                fin: false
              });
              cbk({
                status: 200,
                headers: {},
                content: this.renderContent(),
                hasContent: true,
                fin: true
              });
          };
      });
      function embeddedExecute() {
        const renderContent = embedded.renderContent;
        const streamContent = embedded.streamContent;

        return {
          renderContent,
          streamContent
        };
      }
      embeddedExecute();
    """.trimIndent()

    Context.newBuilder("js")
      .allowExperimentalOptions(true)
      .build()
      .use {
        val result = it.eval("js", testCode)
        assertNotNull(result)
        assertFalse(result.isString)
        assertFalse(result.canExecute())

        val renderContent = result.getMember("renderContent")
        assertNotNull(renderContent)
        assertTrue(renderContent.canExecute())

        val streamContent = result.getMember("streamContent")
        assertNotNull(streamContent)
        assertTrue(streamContent.canExecute())

        val buf: ArrayList<ServerResponse> = ArrayList()
        val handler: (ServerResponse) -> Unit = { out ->
          buf.add(out)
        }
        streamContent.executeVoid(handler)
        assertEquals(2, buf.size)
        assertEquals("hello", buf[0].content)
        assertEquals("hello", buf[1].content)
      }
  }
}

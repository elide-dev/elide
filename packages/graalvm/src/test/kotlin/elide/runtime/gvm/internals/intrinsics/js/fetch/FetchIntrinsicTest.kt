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
package elide.runtime.gvm.internals.intrinsics.js.fetch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the Fetch API intrinsics provided by Elide. */
@TestCase internal class FetchIntrinsicTest : AbstractJsIntrinsicTest<FetchIntrinsic>() {
  // Injected fetch intrinsic under test.
  @Inject internal lateinit var fetch: FetchIntrinsic

  override fun provide(): FetchIntrinsic = fetch

  @Test override fun testInjectable() {
    assertNotNull(fetch, "should be able to inject fetch intrinsic instance")
  }

  /**
   * Regression test for #1817: Response Content-Type header should be preserved when user specifies it.
   * Previously, mapResponseBody() unconditionally overwrote Content-Type based on body type.
   */
  @Test fun `response should preserve user-specified Content-Type header`() = executeGuest {
    """
    const html = '<html><body>Hello</body></html>';
    const response = new Response(html, {
      status: 200,
      headers: { 'Content-Type': 'text/html; charset=utf-8' }
    });
    const contentType = response.headers.get('Content-Type');
    test(contentType);
    """
  }.thenAssert {
    assertEquals("text/html; charset=utf-8", it.value, "Content-Type header should be preserved")
  }

  /**
   * Regression test for #1817: Response should use default Content-Type when user doesn't specify one.
   */
  @Test fun `response should use default Content-Type when not specified`() = executeGuest {
    """
    const response = new Response('plain text body');
    const contentType = response.headers.get('Content-Type');
    test(contentType);
    """
  }.thenAssert {
    assertEquals("text/plain", it.value, "Content-Type should default to text/plain for string body")
  }

  /**
   * Regression test for #1817: Custom headers other than Content-Type should work.
   */
  @Test fun `response should preserve custom headers`() = executeGuest {
    """
    const response = new Response('body', {
      headers: { 'X-Custom-Header': 'custom-value' }
    });
    const customHeader = response.headers.get('X-Custom-Header');
    test(customHeader);
    """
  }.thenAssert {
    assertEquals("custom-value", it.value, "Custom headers should be preserved")
  }
}

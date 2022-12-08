package elide.runtime.ssr

import kotlin.test.Test
import kotlin.test.assertEquals

class CssChunkTest {
  @Test fun testBasic() {
    val chunk = CssChunk(
      ids = arrayOf("test1", "test2"),
      key = "abc",
      css = "body { color: red; }",
    )
    val chunk2 = CssChunk(
      ids = arrayOf("test1", "test2"),
      key = "abc",
      css = "body { color: red; }",
    )
    val chunk3 = chunk2.copy()
    assertEquals(chunk, chunk2)
    assertEquals(chunk, chunk3)
  }
}

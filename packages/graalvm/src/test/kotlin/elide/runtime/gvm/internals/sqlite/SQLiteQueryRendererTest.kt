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
package elide.runtime.gvm.internals.sqlite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SQLiteQueryRendererTest {
  private val nonDynamic = SqliteQueryRenderer.resolve("SELECT * FROM test;")

  @Test fun testNonDynamicInert() {
    assertNotNull(nonDynamic)
    assert(nonDynamic.equals("SELECT * FROM test;"))
  }

  @Test fun testDynamicPositional() {
    SqliteQueryRenderer.render("SELECT * FROM test WHERE id = ?;", arrayOf(1)).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 1;", it)
    }
    SqliteQueryRenderer.render("SELECT * FROM test WHERE id = ?;", arrayOf("hi")).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 'hi';", it)
    }
    SqliteQueryRenderer.render("SELECT * FROM test WHERE id = ? AND cool = ?;", arrayOf("hi", 5)).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 'hi' AND cool = 5;", it)
    }
  }

  @Test fun testDynamicNamed() {
    SqliteQueryRenderer.render("SELECT * FROM test WHERE id = ${'$'}id;", emptyArray(), mapOf("id" to 1)).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 1;", it)
    }
    SqliteQueryRenderer.render(
      "SELECT * FROM test WHERE id = ${'$'}id AND cool = ${'$'}cool;",
      emptyArray(),
      mapOf(
        "id" to "hi",
        "cool" to 5,
      ),
    ).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 'hi' AND cool = 5;", it)
    }
  }

  @Test fun testDynamicNamedAlternateSymbol() {
    SqliteQueryRenderer.render("SELECT * FROM test WHERE id = :id;", emptyArray(), mapOf("id" to 1)).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 1;", it)
    }
    SqliteQueryRenderer.render(
      "SELECT * FROM test WHERE id = :id AND cool = :cool;",
      emptyArray(),
      mapOf(
        "id" to "hi",
        "cool" to 5,
      ),
    ).let {
      assertNotNull(it)
      assertEquals("SELECT * FROM test WHERE id = 'hi' AND cool = 5;", it)
    }
  }
}

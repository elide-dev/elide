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
package elide.tooling.project.codecs

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import java.io.ByteArrayInputStream

class ElidePklUnionDefaultTest {
  @Test fun `new braces default in union listing yields MavenPackageSpec`() {
    val pkl = """
amends "./packages/cli/src/main/pkl/Project.pkl"

dependencies {
  maven {
    packages {
      new {
        group = "com.google.guava"
        name = "guava"
        version = "33.4.8-jre"
      }
    }
  }
}
""".trimIndent()

    val codec = ElidePackageManifestCodec()
    val manifest = codec.parse(ByteArrayInputStream(pkl.toByteArray()))

    val pkgs = manifest.dependencies.maven.packages
    assertEquals(1, pkgs.size)

    val p = pkgs.first()
    assertEquals("com.google.guava", p.group)
    assertEquals("guava", p.name)
    assertEquals("33.4.8-jre", p.version)
  }
}


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
package elide.tooling

import kotlin.test.*
import elide.tooling.containers.ContainerCoordinate
import elide.tooling.containers.ContainerHashType

class ContainerCoordinateTest {
  // produced by `echo "hello" | sha256sum`
  val sampleSha256 = "5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03"

  @Test fun `parse simple image without tag`() {
    assertNotNull(ContainerCoordinate.parse("simple")).let {
      assertNull(it.registry)
      assertEquals("simple", it.name)
      assertNull(it.tag)
      assertNull(it.hash)
    }
  }

  @Test fun `parse simple image with tag`() {
    assertNotNull(ContainerCoordinate.parse("simple:latest")).let {
      assertNull(it.registry)
      assertEquals("simple", it.name)
      assertNotNull(it.tag)
      assertEquals("latest", it.tag?.asString())
      assertNull(it.hash)
    }
  }

  @Test fun `parse simple image with hash`() {
    assertNotNull(ContainerCoordinate.parse("simple@sha256:$sampleSha256")).let {
      assertNull(it.registry)
      assertEquals("simple", it.name)
      assertNull(it.tag)
      assertNotNull(it.hash)
      assertEquals(sampleSha256, it.hash?.hashValue)
    }
  }

  @Test fun `parse simple image with tag and hash`() {
    assertNotNull(ContainerCoordinate.parse("simple:latest@sha256:$sampleSha256")).let {
      assertNull(it.registry)
      assertEquals("simple", it.name)
      assertNotNull(it.tag)
      assertEquals("latest", it.tag?.asString())
      assertNotNull(it.hash)
      assertEquals(sampleSha256, it.hash?.hashValue)
    }
  }

  @Test fun `parse registry image without tag`() {
    assertNotNull(ContainerCoordinate.parse("ghcr.io/elide-dev/elide")).let {
      assertNotNull(it.registry)
      assertEquals("ghcr.io/elide-dev", it.registry?.asString())
      assertEquals("elide", it.name)
      assertNull(it.tag)
      assertNull(it.hash)
    }
  }

  @Test fun `parse registry image with tag`() {
    assertNotNull(ContainerCoordinate.parse("ghcr.io/elide-dev/elide:latest")).let {
      assertNotNull(it.registry)
      assertEquals( "ghcr.io/elide-dev", it.registry?.asString())
      assertEquals("elide", it.name)
      assertEquals("latest", it.tag?.asString())
      assertNull(it.hash)
    }
  }

  @Test fun `parse registry image with hash`() {
    assertNotNull(ContainerCoordinate.parse("ghcr.io/elide-dev/elide@sha256:$sampleSha256")).let {
      assertNotNull(it.registry)
      assertEquals( "ghcr.io/elide-dev", it.registry?.asString())
      assertEquals("elide", it.name)
      assertNull(it.tag)
      assertEquals(sampleSha256, it.hash?.hashValue)
      assertEquals(ContainerHashType.SHA256, it.hash?.hashType)
      assertEquals("sha256:$sampleSha256", it.hash?.asString())
    }
  }

  @Test fun `parse registry image with hash and tag`() {
    assertNotNull(ContainerCoordinate.parse("ghcr.io/elide-dev/elide:latest@sha256:$sampleSha256")).let {
      assertNotNull(it.registry)
      assertEquals( "ghcr.io/elide-dev", it.registry?.asString())
      assertEquals("elide", it.name)
      assertEquals("latest", it.tag?.asString())
      assertEquals(sampleSha256, it.hash?.hashValue)
      assertEquals(ContainerHashType.SHA256, it.hash?.hashType)
      assertEquals("sha256:$sampleSha256", it.hash?.asString())
    }
  }
}

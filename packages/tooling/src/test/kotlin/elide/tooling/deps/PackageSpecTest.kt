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

package elide.tooling.deps

import kotlin.test.*

class PackageSpecTest {
  @Test fun testPackageSpecTryParseNpm() {
    val spec = PackageSpec.parse("@scope/example@1.0.0")
    assertNotNull(spec)
    val other = PackageSpec.tryParse("@scope/example@1.0.0")
    assertNotNull(other)
    assertEquals(spec, other)
    assertIs<PackageSpec.NpmPackageSpec>(spec)
    assertIs<PackageSpec.NpmPackageSpec>(other)
    assertEquals("@scope", spec.scope)
    assertEquals("example", spec.name)
    assertEquals("1.0.0", spec.version.formatted)
    assertEquals("@scope", other.scope)
    assertEquals("example", other.name)
    assertEquals("1.0.0", other.version.formatted)
  }

  @Test fun testPackageSpecTryParseNpmPrefixed() {
    val spec = PackageSpec.parse("npm:@scope/example@1.0.0")
    assertNotNull(spec)
    val other = PackageSpec.tryParse("npm:@scope/example@1.0.0")
    assertNotNull(other)
    assertEquals(spec, other)
    assertIs<PackageSpec.NpmPackageSpec>(spec)
    assertIs<PackageSpec.NpmPackageSpec>(other)
    assertEquals("@scope", spec.scope)
    assertEquals("example", spec.name)
    assertEquals("1.0.0", spec.version.formatted)
    assertEquals("@scope", other.scope)
    assertEquals("example", other.name)
    assertEquals("1.0.0", other.version.formatted)
  }

  @Test fun testPackageSpecTryParseMaven() {
    val spec = PackageSpec.parse("com.google.guava:guava@1.0.0")
    assertNotNull(spec)
    val other = PackageSpec.tryParse("com.google.guava:guava@1.0.0")
    assertNotNull(other)
    assertEquals(spec, other)
    assertIs<PackageSpec.MavenPackageSpec>(spec)
    assertIs<PackageSpec.MavenPackageSpec>(other)
    assertEquals("com.google.guava", spec.groupId)
    assertEquals("guava", spec.module)
    assertEquals("1.0.0", spec.version.formatted)
  }

  @Test fun testPackageSpecTryParseMavenPrefixed() {
    val spec = PackageSpec.parse("maven:com.google.guava:guava@1.0.0")
    assertNotNull(spec)
    val other = PackageSpec.tryParse("maven:com.google.guava:guava@1.0.0")
    assertNotNull(other)
    assertEquals(spec, other)
    assertIs<PackageSpec.MavenPackageSpec>(spec)
    assertIs<PackageSpec.MavenPackageSpec>(other)
    assertEquals("com.google.guava", spec.groupId)
    assertEquals("guava", spec.module)
    assertEquals("1.0.0", spec.version.formatted)
  }

  @Test fun testPackageSpecTryParsePip() {
    val spec = PackageSpec.parse("example==1.0.0", ecosystem = DependencyEcosystem.PyPI)
    assertNotNull(spec)
    val other = PackageSpec.tryParse("example==1.0.0", ecosystem = DependencyEcosystem.PyPI)
    assertNotNull(other)
    assertEquals(spec, other)
    assertIs<PackageSpec.PipPackageSpec>(spec)
    assertIs<PackageSpec.PipPackageSpec>(other)
    assertEquals("example", spec.module)
    assertEquals("==1.0.0", spec.version.formatted)
    assertEquals("example", other.module)
    assertEquals("==1.0.0", other.version.formatted)
  }

  @Test fun testPackageSpecTryParsePipPrefixed() {
    val spec = PackageSpec.parse("pip:example==1.0.0")
    assertNotNull(spec)
    val other = PackageSpec.tryParse("pip:example==1.0.0")
    assertNotNull(other)
    assertEquals(spec, other)
    assertIs<PackageSpec.PipPackageSpec>(spec)
    assertIs<PackageSpec.PipPackageSpec>(other)
    assertEquals("example", spec.module)
    assertEquals("==1.0.0", spec.version.formatted)
    assertEquals("example", other.module)
    assertEquals("==1.0.0", other.version.formatted)
  }
}

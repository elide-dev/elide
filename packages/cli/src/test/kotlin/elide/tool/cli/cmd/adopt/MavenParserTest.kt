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

package elide.tool.cli.cmd.adopt

import kotlin.test.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import elide.tooling.project.adopt.maven.MavenParser

/** Tests for POM parser functionality. */
class MavenParserTest {
  private fun createTempPom(content: String): Path {
    val tempFile = Files.createTempFile("test-pom", ".xml")
    tempFile.writeText(content)
    return tempFile
  }

  @Test
  fun testParseBasicPom() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    assertEquals("com.example", descriptor.groupId)
    assertEquals("test-project", descriptor.artifactId)
    assertEquals("1.0.0", descriptor.version)
  }

  @Test
  fun testParseBuildPlugins() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>
        <build>
          <plugins>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-compiler-plugin</artifactId>
              <version>3.11.0</version>
            </plugin>
            <plugin>
              <artifactId>maven-surefire-plugin</artifactId>
              <version>3.2.2</version>
            </plugin>
          </plugins>
        </build>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    assertEquals(2, descriptor.plugins.size)

    // First plugin with explicit groupId
    val compilerPlugin = descriptor.plugins[0]
    assertEquals("org.apache.maven.plugins", compilerPlugin.groupId)
    assertEquals("maven-compiler-plugin", compilerPlugin.artifactId)
    assertEquals("3.11.0", compilerPlugin.version)

    // Second plugin with implicit default groupId
    val surefirePlugin = descriptor.plugins[1]
    assertEquals("org.apache.maven.plugins", surefirePlugin.groupId)
    assertEquals("maven-surefire-plugin", surefirePlugin.artifactId)
    assertEquals("3.2.2", surefirePlugin.version)
  }

  @Test
  fun testPropertyDefaultValues() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>

        <properties>
          <guava.version>32.1.3-jre</guava.version>
        </properties>

        <dependencies>
          <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${'$'}{guava.version}</version>
          </dependency>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${'$'}{undefined.version:2.0.9}</version>
          </dependency>
          <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>${'$'}{junit.version:4.13.2}</version>
            <scope>test</scope>
          </dependency>
        </dependencies>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    assertEquals(3, descriptor.dependencies.size)

    // Property with value - should use actual value
    val guavaDep = descriptor.dependencies.find { it.artifactId == "guava" }
    assertNotNull(guavaDep)
    assertEquals("32.1.3-jre", guavaDep.version)

    // Undefined property with default - should use default
    val slf4jDep = descriptor.dependencies.find { it.artifactId == "slf4j-api" }
    assertNotNull(slf4jDep)
    assertEquals("2.0.9", slf4jDep.version)

    // Direct use of default in dependency - should use default
    val junitDep = descriptor.dependencies.find { it.artifactId == "junit" }
    assertNotNull(junitDep)
    assertEquals("4.13.2", junitDep.version)
  }

  @Test
  fun testPropertyDefaultOverriddenByDefinedValue() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>

        <properties>
          <guava.version>32.1.3-jre</guava.version>
        </properties>

        <dependencies>
          <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>${'$'}{guava.version:999.0.0}</version>
          </dependency>
        </dependencies>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    val guavaDep = descriptor.dependencies.find { it.artifactId == "guava" }
    assertNotNull(guavaDep)
    // Should use guava.version (32.1.3-jre) not the default (999.0.0)
    assertEquals("32.1.3-jre", guavaDep.version)
  }

  @Test
  fun testParseRepositories() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>

        <repositories>
          <repository>
            <id>jitpack.io</id>
            <name>JitPack Repository</name>
            <url>https://jitpack.io</url>
          </repository>
          <repository>
            <id>central</id>
            <url>https://repo.maven.apache.org/maven2</url>
          </repository>
        </repositories>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    assertEquals(2, descriptor.repositories.size)

    val jitpack = descriptor.repositories.find { it.id == "jitpack.io" }
    assertNotNull(jitpack)
    assertEquals("JitPack Repository", jitpack.name)
    assertEquals("https://jitpack.io", jitpack.url)

    val central = descriptor.repositories.find { it.id == "central" }
    assertNotNull(central)
    assertEquals("https://repo.maven.apache.org/maven2", central.url)
  }

  @Test
  fun testParseMultiModule() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>parent-project</artifactId>
        <version>1.0.0</version>
        <packaging>pom</packaging>

        <modules>
          <module>module-a</module>
          <module>module-b</module>
          <module>module-c</module>
        </modules>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    assertEquals("pom", descriptor.packaging)
    assertEquals(3, descriptor.modules.size)
    assertTrue(descriptor.modules.contains("module-a"))
    assertTrue(descriptor.modules.contains("module-b"))
    assertTrue(descriptor.modules.contains("module-c"))
  }

  @Test
  fun testParseProfiles() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>

        <profiles>
          <profile>
            <id>production</id>
            <dependencies>
              <dependency>
                <groupId>com.example</groupId>
                <artifactId>prod-lib</artifactId>
                <version>1.0.0</version>
              </dependency>
            </dependencies>
          </profile>
          <profile>
            <id>development</id>
            <dependencies>
              <dependency>
                <groupId>com.example</groupId>
                <artifactId>dev-lib</artifactId>
                <version>1.0.0</version>
              </dependency>
            </dependencies>
          </profile>
        </profiles>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)

    assertEquals(2, descriptor.profiles.size)

    val prodProfile = descriptor.profiles.find { it.id == "production" }
    assertNotNull(prodProfile)
    assertEquals(1, prodProfile.dependencies.size)
    assertEquals("prod-lib", prodProfile.dependencies[0].artifactId)

    val devProfile = descriptor.profiles.find { it.id == "development" }
    assertNotNull(devProfile)
    assertEquals(1, devProfile.dependencies.size)
    assertEquals("dev-lib", devProfile.dependencies[0].artifactId)
  }

  @Test
  fun testActivateProfile() {
    val pom = createTempPom("""
      <?xml version="1.0"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>test-project</artifactId>
        <version>1.0.0</version>

        <dependencies>
          <dependency>
            <groupId>com.example</groupId>
            <artifactId>base-lib</artifactId>
            <version>1.0.0</version>
          </dependency>
        </dependencies>

        <profiles>
          <profile>
            <id>extra-deps</id>
            <dependencies>
              <dependency>
                <groupId>com.example</groupId>
                <artifactId>extra-lib</artifactId>
                <version>2.0.0</version>
              </dependency>
            </dependencies>
          </profile>
        </profiles>
      </project>
    """.trimIndent())

    val descriptor = MavenParser.parse(pom)
    assertEquals(1, descriptor.dependencies.size)

    val activatedDescriptor = MavenParser.activateProfiles(descriptor, listOf("extra-deps"))
    assertEquals(2, activatedDescriptor.dependencies.size)

    val baseDep = activatedDescriptor.dependencies.find { it.artifactId == "base-lib" }
    assertNotNull(baseDep)

    val extraDep = activatedDescriptor.dependencies.find { it.artifactId == "extra-lib" }
    assertNotNull(extraDep)
  }
}

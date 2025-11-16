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

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists
import kotlin.io.path.inputStream

/**
 * Represents a parent POM reference.
 */
data class ParentPom(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val relativePath: String? = null
)

/**
 * Represents a dependency in Maven POM.
 */
data class Dependency(
  val groupId: String,
  val artifactId: String,
  val version: String?,
  val scope: String = "compile",
  val type: String = "jar",
  val classifier: String? = null,
  val optional: Boolean = false
) {
  val coordinate: String
    get() = buildString {
      append("$groupId:$artifactId")
      if (version != null) append(":$version")
    }
}

/**
 * Represents a Maven POM file.
 */
data class PomDescriptor(
  val groupId: String,
  val artifactId: String,
  val version: String,
  val name: String?,
  val description: String?,
  val packaging: String = "jar",
  val dependencies: List<Dependency> = emptyList(),
  val dependencyManagement: Map<String, String> = emptyMap(),
  val modules: List<String> = emptyList(),
  val properties: Map<String, String> = emptyMap(),
  val parent: ParentPom? = null,
  val path: Path
)

/**
 * Parser for Maven POM files.
 */
object PomParser {
  private const val MAVEN_NS = "http://maven.apache.org/POM/4.0.0"

  /**
   * Parse a Maven POM file from the given path.
   */
  fun parse(pomPath: Path): PomDescriptor {
    require(pomPath.exists()) { "POM file not found: $pomPath" }

    val doc = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
    }.newDocumentBuilder().parse(pomPath.inputStream())

    return parsePom(doc, pomPath)
  }

  private fun parsePom(doc: Document, pomPath: Path): PomDescriptor {
    val root = doc.documentElement

    // Parse parent POM reference
    val parent = parseParent(root)

    // Resolve parent POM if present
    val parentDescriptor = parent?.let { resolveParentPom(it, pomPath) }

    // Extract basic project info with parent inheritance
    val groupId = findText(root, "groupId") ?: parentDescriptor?.groupId ?: error("Missing groupId in $pomPath")
    val artifactId = findText(root, "artifactId") ?: error("Missing artifactId in $pomPath")
    val version = findText(root, "version") ?: parentDescriptor?.version ?: error("Missing version in $pomPath")
    val name = findText(root, "name") ?: parentDescriptor?.name
    val description = findText(root, "description") ?: parentDescriptor?.description
    val packaging = findText(root, "packaging") ?: "jar"

    // Parse properties and merge with parent
    val localProperties = parseProperties(root)
    val mergedProperties = (parentDescriptor?.properties ?: emptyMap()) + localProperties + mapOf(
      "groupId" to groupId,
      "artifactId" to artifactId,
      "version" to version
    )

    // Parse dependency management and merge with parent
    val localDepMgmt = parseDependencyManagement(root)
    val mergedDepMgmt = (parentDescriptor?.dependencyManagement ?: emptyMap()) + localDepMgmt

    // Parse dependencies with interpolation
    val dependencies = parseDependencies(root, mergedDepMgmt)
      .map { dep -> dep.copy(version = interpolateProperties(dep.version, mergedProperties)) }

    // Parse modules (for multi-module projects)
    val modules = parseModules(root)

    return PomDescriptor(
      groupId = groupId,
      artifactId = artifactId,
      version = version,
      name = name,
      description = description,
      packaging = packaging,
      dependencies = dependencies,
      dependencyManagement = mergedDepMgmt,
      modules = modules,
      properties = mergedProperties,
      parent = parent,
      path = pomPath
    )
  }

  private fun parseDependencyManagement(root: Element): Map<String, String> {
    val depMgmt = mutableMapOf<String, String>()

    val depMgmtElement = findElement(root, "dependencyManagement") ?: return depMgmt
    val depsElement = findElement(depMgmtElement, "dependencies") ?: return depMgmt

    val depList = depsElement.getElementsByTagName("dependency")
    for (i in 0 until depList.length) {
      val dep = depList.item(i) as? Element ?: continue
      val groupId = findText(dep, "groupId") ?: continue
      val artifactId = findText(dep, "artifactId") ?: continue
      val version = findText(dep, "version") ?: continue

      val key = "$groupId:$artifactId"
      depMgmt[key] = version
    }

    return depMgmt
  }

  private fun parseDependencies(root: Element, depMgmt: Map<String, String>): List<Dependency> {
    val dependencies = mutableListOf<Dependency>()

    // Find direct dependencies element under project root
    val depsElement = root.childNodes.let { nodes ->
      (0 until nodes.length).asSequence()
        .mapNotNull { nodes.item(it) as? Element }
        .firstOrNull { it.localName == "dependencies" || it.tagName == "dependencies" }
    } ?: return dependencies

    val depList = depsElement.getElementsByTagName("dependency")
    for (i in 0 until depList.length) {
      val dep = depList.item(i) as? Element ?: continue

      val groupId = findText(dep, "groupId") ?: continue
      val artifactId = findText(dep, "artifactId") ?: continue
      val scope = findText(dep, "scope") ?: "compile"
      val type = findText(dep, "type") ?: "jar"
      val classifier = findText(dep, "classifier")
      val optional = findText(dep, "optional")?.toBoolean() ?: false

      // Get version: explicit or from dependency management
      val explicitVersion = findText(dep, "version")
      val version = explicitVersion ?: depMgmt["$groupId:$artifactId"]

      if (version != null) {
        dependencies.add(
          Dependency(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            scope = scope,
            type = type,
            classifier = classifier,
            optional = optional
          )
        )
      }
    }

    return dependencies
  }

  private fun parseModules(root: Element): List<String> {
    val modulesElement = findElement(root, "modules") ?: return emptyList()
    val moduleList = modulesElement.getElementsByTagName("module")

    return (0 until moduleList.length).mapNotNull { i ->
      (moduleList.item(i) as? Element)?.textContent?.trim()
    }
  }

  private fun parseProperties(root: Element): Map<String, String> {
    val propsElement = findElement(root, "properties") ?: return emptyMap()
    val properties = mutableMapOf<String, String>()

    val children = propsElement.childNodes
    for (i in 0 until children.length) {
      val child = children.item(i) as? Element ?: continue
      val key = child.localName ?: child.tagName
      val value = child.textContent?.trim() ?: continue
      properties[key] = value
    }

    return properties
  }

  /**
   * Parse parent POM reference from the root element.
   */
  private fun parseParent(root: Element): ParentPom? {
    val parentElement = findElement(root, "parent") ?: return null

    val groupId = findText(parentElement, "groupId") ?: return null
    val artifactId = findText(parentElement, "artifactId") ?: return null
    val version = findText(parentElement, "version") ?: return null
    val relativePath = findText(parentElement, "relativePath")

    return ParentPom(
      groupId = groupId,
      artifactId = artifactId,
      version = version,
      relativePath = relativePath
    )
  }

  /**
   * Resolve parent POM from filesystem.
   * Follows Maven's parent POM resolution strategy:
   * 1. Use relativePath if specified
   * 2. Default to ../pom.xml
   */
  private fun resolveParentPom(parent: ParentPom, childPomPath: Path): PomDescriptor? {
    val relativePath = parent.relativePath ?: "../pom.xml"
    val parentPomPath = childPomPath.parent.resolve(relativePath).normalize()

    // If relativePath points to a directory, look for pom.xml inside
    val actualPath = when {
      parentPomPath.exists() && Files.isDirectory(parentPomPath) -> parentPomPath.resolve("pom.xml")
      else -> parentPomPath
    }

    if (!actualPath.exists()) {
      return null
    }

    // Parse parent POM recursively
    return try {
      parse(actualPath)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Interpolate Maven properties in a string.
   * Supports ${property.name} syntax and built-in Maven properties.
   */
  private fun interpolateProperties(value: String?, properties: Map<String, String>): String? {
    if (value == null) return null

    var result = value
    val propertyPattern = Regex("\\$\\{([^}]+)}")

    propertyPattern.findAll(value).forEach { match ->
      val propertyName = match.groupValues[1]
      val propertyValue = when (propertyName) {
        "project.version", "pom.version" -> properties["version"]
        "project.groupId", "pom.groupId" -> properties["groupId"]
        "project.artifactId", "pom.artifactId" -> properties["artifactId"]
        else -> properties[propertyName]
      }

      if (propertyValue != null) {
        result = result!!.replace(match.value, propertyValue)
      }
    }

    return result
  }

  /**
   * Find the text content of a child element by tag name.
   * Handles both namespaced and non-namespaced elements.
   */
  private fun findText(parent: Element, tagName: String): String? {
    return findElement(parent, tagName)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
  }

  /**
   * Find a child element by tag name.
   * Handles both namespaced and non-namespaced elements.
   */
  private fun findElement(parent: Element, tagName: String): Element? {
    // Try namespace-aware first
    parent.getElementsByTagNameNS(MAVEN_NS, tagName).let { nodes ->
      if (nodes.length > 0) return nodes.item(0) as? Element
    }

    // Fallback to non-namespaced
    parent.getElementsByTagName(tagName).let { nodes ->
      if (nodes.length > 0) return nodes.item(0) as? Element
    }

    // Try direct children with local name matching
    val children = parent.childNodes
    for (i in 0 until children.length) {
      val child = children.item(i) as? Element ?: continue
      if (child.localName == tagName || child.tagName == tagName) {
        return child
      }
    }

    return null
  }
}

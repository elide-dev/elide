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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

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
 * Represents a Maven repository.
 */
data class Repository(
  val id: String,
  val url: String,
  val name: String? = null
)

/**
 * Represents a Maven profile.
 */
data class Profile(
  val id: String,
  val properties: Map<String, String> = emptyMap(),
  val dependencies: List<Dependency> = emptyList(),
  val dependencyManagement: Map<String, String> = emptyMap(),
  val repositories: List<Repository> = emptyList()
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
)

/**
 * Get Maven coordinate string for a dependency.
 */
fun Dependency.coordinate(): String = buildString {
  append("$groupId:$artifactId")
  if (version != null) append(":$version")
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
  val repositories: List<Repository> = emptyList(),
  val profiles: List<Profile> = emptyList(),
  val path: Path
)

/**
 * Parser for Maven POM files.
 */
object PomParser {
  private const val MAVEN_NS = "http://maven.apache.org/POM/4.0.0"
  private const val MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2"

  // Cache directory for downloaded POMs
  private val CACHE_DIR = Path.of(System.getProperty("user.home"), ".elide", "maven-cache")

  // HTTP client for downloading POMs (reused across requests)
  private val httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

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
    // Pass properties for BOM version interpolation
    val localDepMgmt = parseDependencyManagement(root, pomPath, mergedProperties)
    val mergedDepMgmt = (parentDescriptor?.dependencyManagement ?: emptyMap()) + localDepMgmt

    // Parse dependencies with interpolation
    val dependencies = parseDependencies(root, mergedDepMgmt)
      .map { dep -> dep.copy(version = interpolateProperties(dep.version, mergedProperties)) }

    // Parse modules (for multi-module projects)
    val modules = parseModules(root)

    // Parse repositories
    val repositories = parseRepositories(root)

    // Parse profiles
    val profiles = parseProfiles(root, pomPath)

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
      repositories = repositories,
      profiles = profiles,
      path = pomPath
    )
  }

  /**
   * Parse repositories section from POM.
   */
  private fun parseRepositories(root: Element): List<Repository> {
    val repositoriesElement = findElement(root, "repositories") ?: return emptyList()
    val repositoryList = repositoriesElement.getElementsByTagName("repository")

    return (0 until repositoryList.length).mapNotNull { i ->
      val repo = repositoryList.item(i) as? Element ?: return@mapNotNull null
      val id = findText(repo, "id") ?: return@mapNotNull null
      val url = findText(repo, "url") ?: return@mapNotNull null
      val name = findText(repo, "name")

      Repository(id = id, url = url, name = name)
    }
  }

  /**
   * Parse profiles from POM.
   */
  private fun parseProfiles(root: Element, pomPath: Path? = null): List<Profile> {
    val profilesElement = findElement(root, "profiles") ?: return emptyList()
    val profileList = profilesElement.getElementsByTagName("profile")

    return (0 until profileList.length).mapNotNull { i ->
      val profileElement = profileList.item(i) as? Element ?: return@mapNotNull null
      val profileId = findText(profileElement, "id") ?: return@mapNotNull null

      // Parse profile properties
      val profileProps = parseProperties(profileElement)

      // Parse profile dependencies
      val profileDeps = parseDependencies(profileElement, profileProps)

      // Parse profile dependencyManagement
      val profileDepMgmt = parseDependencyManagement(profileElement, pomPath, profileProps)

      // Parse profile repositories
      val profileRepos = parseRepositories(profileElement)

      Profile(
        id = profileId,
        properties = profileProps,
        dependencies = profileDeps,
        dependencyManagement = profileDepMgmt,
        repositories = profileRepos
      )
    }
  }

  private fun parseDependencyManagement(root: Element, pomPath: Path? = null, properties: Map<String, String> = emptyMap()): Map<String, String> {
    val depMgmt = mutableMapOf<String, String>()

    val depMgmtElement = findElement(root, "dependencyManagement") ?: return depMgmt
    val depsElement = findElement(depMgmtElement, "dependencies") ?: return depMgmt

    val depList = depsElement.getElementsByTagName("dependency")
    for (i in 0 until depList.length) {
      val dep = depList.item(i) as? Element ?: continue
      val groupId = findText(dep, "groupId") ?: continue
      val artifactId = findText(dep, "artifactId") ?: continue
      val rawVersion = findText(dep, "version") ?: continue
      val scope = findText(dep, "scope")
      val type = findText(dep, "type")

      // Interpolate properties in version
      val version = interpolateProperties(rawVersion, properties) ?: rawVersion

      // Check if this is a BOM import
      if (scope == "import" && type == "pom") {
        // Import BOM's dependencyManagement
        val bomDepMgmt = importBom(groupId, artifactId, version, pomPath)
        // BOMs are imported first, then overridden by local definitions
        bomDepMgmt.forEach { (key, value) ->
          if (!depMgmt.containsKey(key)) {
            depMgmt[key] = value
          }
        }
      } else {
        // Regular dependency management entry
        val key = "$groupId:$artifactId"
        depMgmt[key] = version
      }
    }

    return depMgmt
  }

  /**
   * Import a BOM (Bill of Materials) POM and extract its dependencyManagement.
   * Resolution strategy:
   * 1. Check local Maven repository (~/.m2/repository)
   * 2. Check Elide cache (~/.elide/maven-cache)
   * 3. Download from Maven Central and cache
   */
  private fun importBom(groupId: String, artifactId: String, version: String, pomPath: Path?): Map<String, String> {
    val pomFileName = "$artifactId-$version.pom"
    val groupPath = groupId.replace('.', '/')

    // Try 1: Local Maven repository
    val localRepo = Path.of(System.getProperty("user.home"), ".m2", "repository")
    val localBomPath = localRepo
      .resolve(groupPath)
      .resolve(artifactId)
      .resolve(version)
      .resolve(pomFileName)

    if (localBomPath.exists()) {
      return parseBomFile(localBomPath)
    }

    // Try 2: Elide cache
    val cachedBomPath = CACHE_DIR
      .resolve(groupPath)
      .resolve(artifactId)
      .resolve(version)
      .resolve(pomFileName)

    if (cachedBomPath.exists()) {
      return parseBomFile(cachedBomPath)
    }

    // Try 3: Download from Maven Central
    val downloadedPath = downloadPomFromMavenCentral(groupId, artifactId, version)
    return if (downloadedPath != null) {
      parseBomFile(downloadedPath)
    } else {
      emptyMap()
    }
  }

  /**
   * Parse a BOM file and extract its dependencyManagement.
   */
  private fun parseBomFile(bomPath: Path): Map<String, String> {
    return try {
      val bomDoc = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
      }.newDocumentBuilder().parse(bomPath.inputStream())

      parseDependencyManagement(bomDoc.documentElement, bomPath)
    } catch (e: Exception) {
      // BOM parsing failed
      emptyMap()
    }
  }

  /**
   * Download a POM from Maven Central and cache it locally.
   * Returns the path to the cached file, or null if download failed.
   * Used for both BOMs and parent POMs.
   */
  private fun downloadPomFromMavenCentral(
    groupId: String,
    artifactId: String,
    version: String
  ): Path? {
    val groupPath = groupId.replace('.', '/')
    val pomFileName = "$artifactId-$version.pom"

    // Construct Maven Central URL
    val url = "$MAVEN_CENTRAL_URL/$groupPath/$artifactId/$version/$pomFileName"

    // Create HTTP request
    val request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(30))
      .GET()
      .build()

    return try {
      // Execute request
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

      if (response.statusCode() != 200) {
        // POM not found on Maven Central
        return null
      }

      // Cache the downloaded POM
      val cachedPath = CACHE_DIR
        .resolve(groupPath)
        .resolve(artifactId)
        .resolve(version)
        .resolve(pomFileName)

      // Create parent directories
      cachedPath.parent.createDirectories()

      // Write to cache
      cachedPath.writeText(response.body())

      cachedPath
    } catch (e: Exception) {
      // Network error or other failure
      null
    }
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
   * Resolve parent POM with multi-tier resolution strategy:
   * 1. Local filesystem via relativePath (default ../pom.xml)
   * 2. Local Maven repository (~/.m2/repository)
   * 3. Elide cache (~/.elide/maven-cache)
   * 4. Download from Maven Central
   */
  private fun resolveParentPom(parent: ParentPom, childPomPath: Path): PomDescriptor? {
    // Try 1: Local filesystem via relativePath
    val relativePath = parent.relativePath ?: "../pom.xml"
    val parentPomPath = childPomPath.parent.resolve(relativePath).normalize()

    // If relativePath points to a directory, look for pom.xml inside
    val actualPath = when {
      parentPomPath.exists() && Files.isDirectory(parentPomPath) -> parentPomPath.resolve("pom.xml")
      else -> parentPomPath
    }

    if (actualPath.exists()) {
      return try {
        parse(actualPath)
      } catch (e: Exception) {
        null
      }
    }

    // Try 2: Local Maven repository
    val groupPath = parent.groupId.replace('.', '/')
    val pomFileName = "${parent.artifactId}-${parent.version}.pom"

    val localRepo = Path.of(System.getProperty("user.home"), ".m2", "repository")
    val localParentPath = localRepo
      .resolve(groupPath)
      .resolve(parent.artifactId)
      .resolve(parent.version)
      .resolve(pomFileName)

    if (localParentPath.exists()) {
      return try {
        parse(localParentPath)
      } catch (e: Exception) {
        null
      }
    }

    // Try 3: Elide cache
    val cachedParentPath = CACHE_DIR
      .resolve(groupPath)
      .resolve(parent.artifactId)
      .resolve(parent.version)
      .resolve(pomFileName)

    if (cachedParentPath.exists()) {
      return try {
        parse(cachedParentPath)
      } catch (e: Exception) {
        null
      }
    }

    // Try 4: Download from Maven Central
    val downloadedPath = downloadPomFromMavenCentral(parent.groupId, parent.artifactId, parent.version)
    return if (downloadedPath != null) {
      try {
        parse(downloadedPath)
      } catch (e: Exception) {
        null
      }
    } else {
      null
    }
  }

  /**
   * Interpolate Maven properties in a string.
   * Supports:
   * - ${property.name} - Custom properties
   * - ${project.version} - Built-in Maven properties
   * - ${env.VAR_NAME} - Environment variables
   * - ${os.name}, ${java.version} - System properties
   */
  private fun interpolateProperties(value: String?, properties: Map<String, String>): String? {
    if (value == null) return null

    var result = value
    val propertyPattern = Regex("\\$\\{([^}]+)}")

    propertyPattern.findAll(value).forEach { match ->
      val propertyName = match.groupValues[1]

      val propertyValue = when {
        // Environment variables: ${env.VAR_NAME}
        propertyName.startsWith("env.") -> {
          val envVarName = propertyName.substringAfter("env.")
          System.getenv(envVarName)
        }

        // System properties: ${os.name}, ${java.version}, etc.
        propertyName in listOf("os.name", "os.version", "os.arch", "java.version", "java.home",
                               "user.name", "user.home", "user.dir", "file.separator", "path.separator") -> {
          System.getProperty(propertyName)
        }

        // Built-in Maven properties
        propertyName == "project.version" || propertyName == "pom.version" -> properties["version"]
        propertyName == "project.groupId" || propertyName == "pom.groupId" -> properties["groupId"]
        propertyName == "project.artifactId" || propertyName == "pom.artifactId" -> properties["artifactId"]
        propertyName == "project.baseDir" || propertyName == "basedir" -> properties["baseDir"]

        // Custom properties from POM
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

  /**
   * Activate profiles and merge their properties/dependencies into the PomDescriptor.
   *
   * @param pom The POM descriptor to activate profiles for
   * @param profileIds List of profile IDs to activate
   * @return A new PomDescriptor with activated profile data merged in
   */
  fun activateProfiles(pom: PomDescriptor, profileIds: List<String>): PomDescriptor {
    if (profileIds.isEmpty()) return pom

    // Find activated profiles
    val activatedProfiles = pom.profiles.filter { it.id in profileIds }

    if (activatedProfiles.isEmpty()) return pom

    // Merge properties (profiles override base properties)
    val mergedProperties = pom.properties.toMutableMap()
    activatedProfiles.forEach { profile ->
      mergedProperties.putAll(profile.properties)
    }

    // Merge dependencies (add profile dependencies to base)
    val mergedDependencies = (pom.dependencies + activatedProfiles.flatMap { it.dependencies }).distinctBy {
      "${it.groupId}:${it.artifactId}"
    }

    // Merge dependencyManagement (profiles override base versions)
    val mergedDepMgmt = pom.dependencyManagement.toMutableMap()
    activatedProfiles.forEach { profile ->
      mergedDepMgmt.putAll(profile.dependencyManagement)
    }

    // Merge repositories (add profile repositories to base)
    val mergedRepositories = (pom.repositories + activatedProfiles.flatMap { it.repositories }).distinctBy {
      it.id
    }

    return pom.copy(
      properties = mergedProperties,
      dependencies = mergedDependencies,
      dependencyManagement = mergedDepMgmt,
      repositories = mergedRepositories
    )
  }
}

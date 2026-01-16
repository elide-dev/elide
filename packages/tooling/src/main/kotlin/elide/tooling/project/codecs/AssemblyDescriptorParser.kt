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

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * Parser for Maven assembly descriptor XML files.
 *
 * Assembly descriptors define how to create distribution archives (tar.gz, zip)
 * by specifying formats, base directory, and file sets to include.
 *
 * Example descriptor with file sets and include/exclude patterns for source files.
 */
public object AssemblyDescriptorParser {

  /**
   * Parse an assembly descriptor XML file into an Assembly artifact configuration.
   *
   * @param descriptorPath Path to the assembly descriptor XML file
   * @return Parsed Assembly configuration
   */
  public fun parse(descriptorPath: Path): ElidePackageManifest.Assembly {
    val doc = DocumentBuilderFactory.newInstance()
      .newDocumentBuilder()
      .parse(descriptorPath.toFile())

    val root = doc.documentElement

    // Parse <id>
    val id = root.getChildText("id") ?: "dist"

    // Parse <formats><format>...</format></formats>
    val formats = root.getChildElement("formats")?.let { formatsNode ->
      formatsNode.childElements("format").mapNotNull { it.textContent?.trim() }
    } ?: listOf("tar.gz", "zip")

    // Parse <baseDirectory>
    val baseDirectory = root.getChildText("baseDirectory")

    // Parse <fileSets><fileSet>...</fileSet></fileSets>
    val fileSets = root.getChildElement("fileSets")?.let { fileSetsNode ->
      fileSetsNode.childElements("fileSet").map { parseFileSet(it) }
    } ?: emptyList()

    return ElidePackageManifest.Assembly(
      id = id,
      formats = formats,
      baseDirectory = baseDirectory,
      fileSets = fileSets,
      descriptorPath = descriptorPath.toString(),
    )
  }

  /**
   * Parse a fileSet element into an AssemblyFileSet.
   */
  private fun parseFileSet(fileSetElement: Element): ElidePackageManifest.AssemblyFileSet {
    val directory = fileSetElement.getChildText("directory")
    val outputDirectory = fileSetElement.getChildText("outputDirectory")

    val includes = fileSetElement.getChildElement("includes")?.let { includesNode ->
      includesNode.childElements("include").mapNotNull { it.textContent?.trim() }
    } ?: emptyList()

    val excludes = fileSetElement.getChildElement("excludes")?.let { excludesNode ->
      excludesNode.childElements("exclude").mapNotNull { it.textContent?.trim() }
    } ?: emptyList()

    return ElidePackageManifest.AssemblyFileSet(
      directory = directory,
      outputDirectory = outputDirectory,
      includes = includes,
      excludes = excludes,
    )
  }

  /** Get the text content of a child element with the given name, or null if not found. */
  private fun Element.getChildText(name: String): String? {
    return getChildElement(name)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
  }

  /** Get the first child element with the given name, or null if not found. */
  private fun Element.getChildElement(name: String): Element? {
    val children = childNodes
    for (i in 0 until children.length) {
      val child = children.item(i)
      if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == name) {
        return child as Element
      }
    }
    return null
  }

  /** Get all child elements with the given name. */
  private fun Element.childElements(name: String): List<Element> {
    val result = mutableListOf<Element>()
    val children = childNodes
    for (i in 0 until children.length) {
      val child = children.item(i)
      if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == name) {
        result.add(child as Element)
      }
    }
    return result
  }
}

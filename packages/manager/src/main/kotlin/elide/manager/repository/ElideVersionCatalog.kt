@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package elide.manager.repository

import kotlinx.serialization.Serializable
import elide.manager.ElideVersionDto
import elide.manager.HostPlatformSerializer
import elide.runtime.core.HostPlatform

/**
 * A version catalog. Every [StandardElideRepository] has exactly one.
 *
 * @property versions Elide versions mapped to [ElideSystemCatalogs][ElideSystemCatalog].
 * @author Lauri Heino <datafox>
 */
@Serializable public data class ElideVersionCatalog(val versions: Map<String, ElideSystemCatalog>)

/**
 * A catalog of available operating systems and architectures for a given version of Elide. Every key in [platforms] is
 * an [HostPlatform] and every value is a path/url.
 *
 * The path must either be an absolute path or a path relative to the path containing the [ElideVersionCatalog] file.
 *
 * In a [LocalElideRepository] absolute paths start with `/` on Linux and Mac and with `C:\` (or any other drive letter)
 * on Windows. In a [RemoteElideRepository] absolute paths start with `https://` or `http://`.
 *
 * The path must not contain a file extension, as multiple files with the same name and different extension are expected
 * (archival formats, signatures).
 *
 * @property platforms [HostPlatforms][HostPlatform] mapped to a path without extension.
 */
@Serializable
public data class ElideSystemCatalog(
  val platforms: Map<@Serializable(with = HostPlatformSerializer::class) HostPlatform, String>
)

/**
 * Creates an [ElideVersionCatalog] from a map of [ElideVersionDtos][ElideVersionDto] and paths. Paths must follow
 * requirements described in [ElideSystemCatalog].
 */
public fun Map<ElideVersionDto, String>.toVersionCatalog(): ElideVersionCatalog {
  val map = mutableMapOf<String, MutableMap<HostPlatform, String>>()
  forEach { (version, path) -> map.getOrPut(version.version) { mutableMapOf() }.put(version.platform, path) }
  return ElideVersionCatalog(map.mapValues { (_, systems) -> ElideSystemCatalog(systems.toMap()) })
}

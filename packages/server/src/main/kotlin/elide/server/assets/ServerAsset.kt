package elide.server.assets

import io.micronaut.http.MediaType
import tools.elide.assets.AssetBundle

/**
 * Describes a server-side asset which is embedded in an application bundle through Elide's asset tools and protocol
 * buffer for asset bundle metadata.
 *
 * @param mediaType Type of media assigned to this asset descriptor.
 * @param dynamicEligible Whether this type of media is eligible for dynamic transformation.
 */
public sealed class ServerAsset private constructor (
  private val mediaType: MediaType,
  private val dynamicEligible: Boolean,
) {
  /**
   * Describes a JavaScript asset which is embedded in a given Elide application, and described by Elide's protocol
   * buffer structures; when read from the application bundle and interpreted, this class is used to hold script info.
   */
  public class Script(
    private val descriptor: AssetBundle.ScriptBundle
  ): ServerAsset(
    mediaType = MediaType("application/javascript", "js"),
    dynamicEligible = true,
  )

  /**
   * Describes a stylesheet asset which is embedded in a given Elide application, and described by Elide's protocol
   * buffer structures; when read from the application bundle and interpreted, this class is used to hold document info.
   */
  public class Stylesheet(
    private val descriptor: AssetBundle.StyleBundle
  ): ServerAsset(
    mediaType = MediaType("text/css", "css"),
    dynamicEligible = true,
  )

  /**
   * Describes a generic text asset of some kind, for example, `humans.txt` or `robots.txt`; when read from the app
   * bundle and interpreted, this class is used to hold file info.
   */
  public class Text(
    private val descriptor: AssetBundle.GenericBundle
  ): ServerAsset(
    mediaType = MediaType("text/plain", "txt"),
    dynamicEligible = false,
  )
}

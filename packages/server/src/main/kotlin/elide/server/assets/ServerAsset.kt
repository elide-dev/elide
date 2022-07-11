package elide.server.assets

import elide.server.AssetModuleId
import tools.elide.assets.AssetBundle
import java.util.SortedSet

/**
 * Describes a server-side asset which is embedded in an application bundle through Elide's asset tools and protocol
 * buffer for asset bundle metadata.
 *
 * @param module ID assigned by the developer to this asset module.
 * @param assetType Type of asset being referenced by this object.
 * @param index Index of this asset within the content bundle, if applicable.
 */
public sealed class ServerAsset private constructor(
  internal val module: AssetModuleId,
  internal val assetType: AssetType,
  internal val index: SortedSet<Int>?,
) {
  /**
   * Describes a JavaScript asset which is embedded in a given Elide application, and described by Elide's protocol
   * buffer structures; when read from the application bundle and interpreted, this class is used to hold script info.
   *
   * @param descriptor Script-type settings bundle describing this asset.
   * @param index Index of the content payload, within the live asset bundle, corresponding to this script.
   */
  public class Script(
    internal val descriptor: AssetBundle.ScriptBundle,
    index: SortedSet<Int>?,
  ) : ServerAsset(
    module = descriptor.module,
    assetType = AssetType.SCRIPT,
    index = index,
  )

  /**
   * Describes a stylesheet asset which is embedded in a given Elide application, and described by Elide's protocol
   * buffer structures; when read from the application bundle and interpreted, this class is used to hold document info.
   *
   * @param descriptor Stylesheet-type settings bundle describing this asset.
   * @param index Index of the content payload, within the live asset bundle, corresponding to this stylesheet.
   */
  public class Stylesheet(
    internal val descriptor: AssetBundle.StyleBundle,
    index: SortedSet<Int>?,
  ) : ServerAsset(
    module = descriptor.module,
    assetType = AssetType.STYLESHEET,
    index = index,
  )

  /**
   * Describes a generic text asset of some kind, for example, `humans.txt` or `robots.txt`; when read from the app
   * bundle and interpreted, this class is used to hold file info.
   *
   * @param descriptor Text-type settings bundle describing this asset.
   * @param index Index of the content payload, within the live asset bundle, corresponding to this text asset.
   */
  public class Text(
    internal val descriptor: AssetBundle.GenericBundle,
    index: SortedSet<Int>?,
  ) : ServerAsset(
    module = descriptor.module,
    assetType = AssetType.TEXT,
    index = index,
  )
}

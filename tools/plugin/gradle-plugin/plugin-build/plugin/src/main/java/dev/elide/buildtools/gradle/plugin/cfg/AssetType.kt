package dev.elide.buildtools.gradle.plugin.cfg

/** Enumerates types of assets which can be embedded for server use. */
@Suppress("unused", "MemberVisibilityCanBePrivate", "RedundantVisibilityModifier")
public enum class AssetType private constructor(
    val extension: String,
    val contentType: String
) {
    /** The asset is a script. */
    SCRIPT("js", "application/javascript"),

    /** The asset is a stylesheet. */
    STYLESHEET("css", "text/css"),

    /** The asset is generic text. */
    TEXT("txt", "text/plain");

    /** Default include path to use for this asset type. */
    public val defaultInclude: String get() = "**/*.$extension"
}

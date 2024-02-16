package dev.elide.buildtools.gradle.plugin.cfg

import com.google.protobuf.ByteString
import tools.elide.assets.AssetBundle
import tools.elide.assets.AssetBundleKt.assetContent
import tools.elide.data.CompressionMode
import tools.elide.data.compressedData
import tools.elide.data.dataContainer
import tools.elide.data.dataFingerprint
import tools.elide.std.HashAlgorithm
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/** Intermediate object which carries asset content and fingerprint data. */
internal data class AssetContent(
    /** Calculated info for this asset. */
    internal val assetInfo: AssetInfo,

    /** Set of interpreted file records for this asset. */
    internal val assets: ConcurrentLinkedQueue<AssetFile>,
) : java.io.Serializable {
    /** Describes an interpreted asset file, which carries full content/digest data. */
    internal data class AssetFile(
        internal val filename: String,
        internal val base: String,
        internal val size: Long,
        internal val digestAlgorithm: HashAlgorithm,
        internal val digest: ByteArray,
        internal val content: ByteArray,
        @Transient internal val file: File,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AssetFile

            if (filename != other.filename) return false
            if (base != other.base) return false
            if (size != other.size) return false
            if (!digest.contentEquals(other.digest)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = filename.hashCode()
            result = 31 * result + base.hashCode()
            return result
        }

        /** @return Protocol buffer representation of this record. */
        internal fun toProto(moduleId: String, token: String): AssetBundle.AssetContent {
            return assetContent {
                this.filename = this@AssetFile.filename
                this.module = moduleId
                this.token = token
                this.variant.add(compressedData {
                    this.compression = CompressionMode.IDENTITY
                    this.size = this@AssetFile.size
                    this.data = dataContainer {
                        this.raw = ByteString.copyFrom(this@AssetFile.content)
                        this.integrity.add(dataFingerprint {
                            this.hash = digestAlgorithm
                            this.fingerprint = ByteString.copyFrom(this@AssetFile.digest)
                        })
                    }
                })
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AssetContent

        if (assetInfo != other.assetInfo) return false

        return true
    }

    override fun hashCode(): Int {
        return assetInfo.hashCode()
    }
}

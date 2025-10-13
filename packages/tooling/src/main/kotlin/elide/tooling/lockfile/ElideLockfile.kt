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
@file:OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)

package elide.tooling.lockfile

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import elide.core.crypto.HashAlgorithm
import elide.core.encoding.Encoding
import elide.tooling.project.ProjectEcosystem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.io.Serializable as JSerializable

// Default digest algorithm.
private const val DEFAULT_DIGEST_ALGORITHM = "SHA-1"

// Hash algorithm to use for raw byte-string fingerprints.
private const val BYTES_HASH_ALGORITHM = DEFAULT_DIGEST_ALGORITHM

// Hash algorithm to use for file digest fingerprints.
private const val FILE_DIGEST_ALG = DEFAULT_DIGEST_ALGORITHM

/**
 * # Elide Lockfile
 *
 * Describes, in conceptual terms, an Elide Project lockfile; lockfile implementations are versioned, but all versions
 * comply with, and ultimately participate in hierarchy with, this top-level interface.
 *
 * @property version Structural version of this lockfile.
 * @property fingerprint Top-level fingerprint.
 * @property stanzas Stanzas constituent to this lockfile.
 */
@Serializable public sealed interface ElideLockfile {
  /**
   * ## Lockfile Element
   *
   * Describes an element which is constituent to an Elide project lockfile; some elements are additionally
   * [Fingerprinted], and some are not.
   *
   * @property identifier Unique identifier for this element.
   */
  @Serializable public sealed interface LockfileElement<T>: Comparable<T> where T: LockfileElement<T> {
    public val identifier: String

    override fun compareTo(other: T): Int {
      return identifier.compareTo(other.identifier)
    }
  }

  /**
   * Base exception for all lockfile errors.
   */
  public class LockfileException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

  /**
   * ## Fingerprinted
   *
   * Element which provides a [Fingerprint], and which factors into the top-level lockfile [Fingerprint].
   *
   * @property fingerprint Unique fingerprint for this element.
   */
  public interface Fingerprinted {
    public val fingerprint: Fingerprint
  }

  /**
   * ## Lockfile Format
   *
   * Describes the supported formats for a lockfile; supported formats are additionally confined by version.
   */
  public enum class Format {
    /** Use JSON format. */
    JSON,

    /** Use fast binary serialization. */
    BINARY,

    /** Auto choice of format (default). */
    AUTO;

    public val filename: String get() = when (this) {
      JSON -> "elide.lock.json"
      BINARY -> "elide.lock.bin"
      AUTO -> "elide.lock"
    }

    /** @return Extension using the current format for the provided file [name]. */
    public fun extensionFor(name: String): String {
      return when (this) {
        JSON -> "$name.json"
        BINARY -> "$name.bin"
        AUTO -> name
      }
    }
  }

  /**
   * ## Lockfile Version
   *
   * Describes how a lockfile version is represented; each version extends this interface and exports routines which
   * account for format differences.
   */
  @Serializable public sealed interface Version {
    /** Ordinal version number; each new version increments. */
    public val ordinal: UInt

    /** Label to show for this version. */
    public val label: String

    /** Default format for the lockfile. */
    public val defaultFormat: Format get() = Format.JSON

    /** Default filename for the lockfile. */
    public val defaultFilename: String get() = defaultFormat.extensionFor("elide.lock")

    /** Default path where the lockfile should be loaded from, and written to. */
    public val defaultPath: Path get() = Path.of(".dev").resolve(defaultFilename)

    /** Whether this format is still supported. */
    public val supported: Boolean get() = true

    /** Whether this format is the current version. */
    public val current: Boolean get() = true
  }

  /**
   * ## Fingerprint
   *
   * Describes fingerprint data which uniquely identifies the content of subject [InputMaterial], or of the contents of
   * a lockfile stanza or lockfile itself (as applicable). Fingerprint structures are typically computed with hashing of
   * the underlying data.
   */
  @Serializable public sealed interface Fingerprint : Comparable<Fingerprint> {
    /**
     * Compute a stable byte-string representation of this fingerprint.
     *
     * @return Byte-string representation of this fingerprint.
     */
    public fun asBytes(): ByteString

    override fun compareTo(other: Fingerprint): Int {
      return asBytes().compareTo(other.asBytes())
    }

    /**
     * ### No Content
     *
     * Fingerprint singleton which represents no content; this is used when fingerprinting absent data.
     */
    @Serializable public data object NoContent : Fingerprint {
      override fun asBytes(): ByteString = ByteString()
    }

    /**
     * ### Raw Fingerprint
     *
     * Holds raw fingerprint data; used only when reading a lockfile, when context isn't important.
     */
    @Serializable public class RawFingerprint private constructor (
      private val data: ByteArray,
    ): Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [data]. */
        @JvmStatic public fun of(data: ByteArray): Fingerprint = RawFingerprint(data)
      }

      override fun asBytes(): ByteString {
        return ByteString(data)
      }
    }

    /**
     * ### Numeric Fingerprint
     *
     * Wraps a [Long] as a numeric fingerprint value; expected to be unique within the context of its use (for example,
     * within a lockfile stanza).
     */
    @Serializable @JvmInline @SerialName("numeric") public value class Numeric private constructor (
      public val value: Long,
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [value]. */
        @JvmStatic public fun of(value: Long): Fingerprint = Numeric(value)
      }

      override fun asBytes(): ByteString {
        return ByteString(value.toByte())
      }
    }

    /**
     * ### File State
     *
     * Understands a file asset's fingerprint by its last-modified time and size.
     */
    @Serializable @JvmInline @SerialName("filestate") public value class FileState private constructor (
      public val pair: Pair<Long, Long>,
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [lastmod] and [size]. */
        @JvmStatic public fun of(lastmod: Long, size: Long): Fingerprint = FileState(lastmod to size)

        /** @return Fingerprint for the provided [file]. */
        @JvmStatic public fun of(file: File): Fingerprint = of(file.lastModified(), file.length())
      }

      @Suppress("MagicNumber")
      override fun asBytes(): ByteString {
        val lastmod = pair.first
        val size = pair.second

        // encode the lastmod and size as a 16-byte array, with the first 8 bytes being lastmod and the next 8 bytes
        // being size.
        val bytes = ByteArray(16) {
          when (it) {
            0 -> (lastmod shr 56).toByte()
            1 -> (lastmod shr 48).toByte()
            2 -> (lastmod shr 40).toByte()
            3 -> (lastmod shr 32).toByte()
            4 -> (lastmod shr 24).toByte()
            5 -> (lastmod shr 16).toByte()
            6 -> (lastmod shr 8).toByte()
            7 -> lastmod.toByte()
            in 8..15 -> ((size shr ((it - 8) * 8))).toByte()
            else -> error("Invalid index")
          }
        }
        return ByteString(bytes)
      }
    }

    /**
     * ### File Digest
     *
     * Understands a file asset via a digest and size pair.
     */
    @Serializable @JvmInline @SerialName("filedigest") public value class FileDigest private constructor (
      public val pair: Pair<Long, Int>,
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [size] and [digest]. */
        @JvmStatic public fun of(size: Long, digest: ByteArray): Fingerprint = FileDigest(
          size to Base64.encode(digest).hashCode()
        )

        /** @return Fingerprint for the provided [file]. */
        @JvmStatic public fun of(file: File): Fingerprint {
          return of(file.length(), MessageDigest.getInstance(FILE_DIGEST_ALG).let {
            file.inputStream().buffered().use { stream ->
              it.update(stream.readAllBytes())
              it.digest()
            }
          })
        }
      }

      @Suppress("MagicNumber")
      override fun asBytes(): ByteString {
        return ByteString(ByteArray(4) { idx ->
            when (idx) {
                0 -> (pair.first shr 56).toByte()
                1 -> (pair.first shr 48).toByte()
                2 -> (pair.first shr 40).toByte()
                3 -> (pair.first shr 32).toByte()
                else -> error("Invalid index")
            }
        })
      }
    }

    /**
     * ### Bytes Fingerprint
     *
     * Wraps a [ByteArray] as a raw-data fingerprint value; expected to be unique within the context of its use (for
     * example, within a lockfile stanza).
     */
    @Serializable @JvmRecord @SerialName("bytes") public data class Bytes private constructor (
      @ProtoNumber(1) public val value: ByteArray,
      @kotlinx.serialization.Transient @Transient public val digest: String =
        MessageDigest.getInstance(BYTES_HASH_ALGORITHM).let { digester ->
          digester.update(value)
          Base64.encode(digester.digest())
        },
      @ProtoNumber(2) public val hash: Int = digest.hashCode(),
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [value]. */
        @JvmStatic public fun of(value: ByteArray): Fingerprint = Bytes(value)
      }

      override fun asBytes(): ByteString {
        return ByteString(value)
      }

      override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bytes

        if (!value.contentEquals(other.value)) return false
        if (digest != other.digest) return false

        return true
      }

      override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + digest.hashCode()
        return result
      }
    }

    /**
     * ### Digest Fingerprint
     *
     * Wraps a [ByteArray] as a fingerprint value and carries a hash algorithm specification; expected to be unique
     * within the context of its use (for example, within a lockfile stanza).
     */
    @Serializable @JvmInline @SerialName("digest") public value class Digest private constructor (
      public val pair: Triple<HashAlgorithm, Encoding?, ByteArray>,
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [value]. */
        @JvmStatic public fun of(digest: HashAlgorithm, encoding: Encoding?, value: ByteArray): Fingerprint {
          return Digest(Triple(digest, encoding, value))
        }
      }

      override fun asBytes(): ByteString {
        return ByteString(pair.third)
      }
    }

    /**
     * ### String Fingerprint
     *
     * Wraps a [String] as a fingerprint value; expected to be unique within the context of its use (for example, within
     * a lockfile stanza).
     */
    @Serializable @JvmInline @SerialName("string") public value class StringValue private constructor (
      public val value: String,
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [value]. */
        @JvmStatic public fun of(value: String): Fingerprint = StringValue(value)
      }

      override fun asBytes(): ByteString {
        return ByteString(value.toByteArray(StandardCharsets.UTF_8))
      }
    }

    /**
     * ### Compound Fingerprint
     *
     * Wraps one or more other [Fingerprint] values; aggregates each value via the [asBytes] function, and indicates a
     * stable hash code.
     */
    @Serializable(with = CompoundFingerprintSerializer::class)
    @JvmRecord @SerialName("compound") public data class Compound private constructor (
      @kotlinx.serialization.Transient @Transient public val constituents: Set<Fingerprint> = emptySet(),
      @ProtoNumber(1) public val hash: Int = constituents.map {
        it.asBytes().toByteArray()
      }.let { byteArrays ->
        MessageDigest.getInstance(BYTES_HASH_ALGORITHM).let { digester ->
          byteArrays.forEach { byteGroup ->
            digester.update(byteGroup)
          }
          Base64.encode(digester.digest()).hashCode()
        }
      },
    ) : Fingerprint, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Fingerprint for the provided [other] values. */
        @JvmStatic public fun of(other: Set<Fingerprint>): Fingerprint = Compound(other)

        /** @return Fingerprint from the provided pre-calculated hash, with no constituent visibility. */
        @JvmStatic public fun of(code: Int): Compound = Compound(emptySet(), code)
      }

      override fun asBytes(): ByteString {
        return ByteString(hash.toByte())
      }
    }

    // Serializes compound fingerprints in a simple expression of data, instead of their constituent parts.
    private object CompoundFingerprintSerializer: KSerializer<Compound> {
      override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("compound", PrimitiveKind.INT)

      override fun deserialize(decoder: Decoder): Compound {
        return Compound.of(decoder.decodeInt())
      }

      override fun serialize(encoder: Encoder, value: Compound) {
        encoder.encodeInt(value.hash)
      }
    }

    /** Factories for obtaining [Fingerprint] values. */
    public companion object {
      /** @return Fingerprint for the provided [value]. */
      @JvmStatic public fun of(value: Long): Fingerprint = Numeric.of(value)

      /** @return Fingerprint for the provided [value]. */
      @JvmStatic public fun of(value: ByteArray): Fingerprint = Bytes.of(value)

      /** @return Fingerprint for the provided [value]. */
      @JvmStatic public fun of(digest: HashAlgorithm, value: ByteArray, encoding: Encoding? = null): Fingerprint {
        return Digest.of(digest, encoding, value)
      }

      /** @return Fingerprint for the provided [value]. */
      @JvmStatic public fun of(value: String): Fingerprint = StringValue.of(value)

      /** @return Fingerprint for the provided [file]. */
      @JvmStatic public fun ofFileState(file: File): Fingerprint = FileState.of(file)

      /** @return Fingerprint for the provided [file]. */
      @JvmStatic public fun ofFileDigest(file: File): Fingerprint = FileDigest.of(file)

      /** @return Fingerprint wrapping the provided [fingerprints]. */
      @Suppress("UNCHECKED_CAST")
      @JvmStatic public fun of(fingerprints: Collection<Fingerprint>): Fingerprint = Compound.of(
        when {
          fingerprints is Set<*> -> fingerprints
          else -> fingerprints.toSortedSet()
        } as Set<Fingerprint>
      )
    }
  }

  /**
   * ## Input Material
   *
   * Input material which factors into the considerations for this lockfile or lockfile stanza; input material typically
   * holds its own [Fingerprint] and may contain other structural elements or remarks.
   */
  @Serializable public sealed interface InputMaterial : LockfileElement<InputMaterial>, Fingerprinted {
    /** Unique identifier for this input material. */
    override val identifier: String

    /** Fingerprint for this input material. */
    override val fingerprint: Fingerprint

    /** Optional remarks accompanying this input material. */
    public val remarks: Remarks?

    /**
     * ### File Input
     *
     * Describes a project-relative file as [InputMaterial].
     */
    @Serializable public sealed interface File : InputMaterial {
      /** Relative path for the file. */
      public val relativePath: String
    }

    /**
     * ### Dependency Manifest
     *
     * Specifies a dependency manifest which may itself specify a [PeerLockfile] as well.
     *
     * @property ecosystem Ecosystem for this dependency manifest.
     * @property identifier Unique identifier for this lockfile (name, typically, or relative path).
     * @property fingerprint Fingerprint for this lockfile.
     * @property relativePath Relative path for this lockfile.
     * @property remarks Optional remarks accompanying this lockfile.
     */
    @SerialName("dependencyManifest")
    @JvmRecord @Serializable public data class DependencyManifest private constructor (
      @ProtoNumber(1) public val ecosystem: ProjectEcosystem,
      @ProtoNumber(2) override val identifier: String,
      @ProtoNumber(3) override val fingerprint: Fingerprint,
      @ProtoNumber(4) override val remarks: Remarks = Remarks.None,
    ) : File, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Dependency manifest for the provided [ecosystem], [identifier], and [fingerprint]. */
        @JvmStatic public fun of(
          ecosystem: ProjectEcosystem,
          identifier: String,
          fingerprint: Fingerprint,
          remarks: Remarks = Remarks.None,
        ): DependencyManifest {
          return DependencyManifest(ecosystem, identifier, fingerprint, remarks)
        }
      }

      override val relativePath: String get() = identifier
    }

    /**
     * ### Peer Lockfile
     *
     * Specifies a lockfile which is peer to this lockfile; this includes foreign lockfile formats which are generated
     * by other tools (for example, `package-lock.json` or `package-lock.kdl`).
     *
     * @property generatedBy Tool which generated this lockfile.
     * @property identifier Unique identifier for this lockfile (name, typically, or relative path).
     * @property fingerprint Fingerprint for this lockfile.
     * @property relativePath Relative path for this lockfile.
     * @property remarks Optional remarks accompanying this lockfile.
     */
    @SerialName("peerLockfile")
    @JvmRecord @Serializable public data class PeerLockfile private constructor (
      public val generatedBy: String,
      override val identifier: String,
      override val fingerprint: Fingerprint,
      override val remarks: Remarks? = null,
    ) : File, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L

        /** @return Peer lockfile for the provided [generatedBy], [identifier], and [fingerprint]. */
        @JvmStatic public fun of(
          generatedBy: String,
          identifier: String,
          fingerprint: Fingerprint,
          remarks: Remarks? = null,
        ): PeerLockfile {
          return PeerLockfile(generatedBy, identifier, fingerprint, remarks)
        }
      }

      override val relativePath: String get() = identifier
    }
  }

  /**
   * ## Remarks
   *
   * String remarks or other structural elements which can be contributed to explain or clarify content within a project
   * lockfile.
   *
   * @property message Remarks as a string message.
   */
  @Serializable public sealed interface Remarks {
    /** Remarks as a string message. */
    @ProtoNumber(1) public val message: String

    /** Object form of no-remarks. */
    @SerialName("NoRemarks")
    @Serializable public data object None: Remarks {
      @ProtoNumber(1) override val message: String get() = ""
    }

    /**
     * ### Text Remarks
     *
     * Simple string-based remarks.
     *
     * @property message Remarks as a string message.
     */
    @SerialName("text")
    @JvmRecord @Serializable public data class Text internal constructor (
      @ProtoNumber(1) override val message: String,
    ) : Remarks, JSerializable {
      public companion object {
        // Serial version UID.
        @JvmStatic public val serialVersionUID: Long = 1L
      }
    }

    public companion object {
      @JvmStatic public fun text(message: String): Remarks {
        return Text(message)
      }
    }
  }

  /**
   * ## State
   *
   * Custom state which can be contributed (structurally) by lockfile contributors as part of a [Stanza]; all state
   * types must inherit from one of the state children, and all state types must be serializable, both by KotlinX
   * Serialization and Java Serialization.
   */
  @Serializable public sealed interface State {
    @Serializable @SerialName("NoState") public data object NoState : State
  }

  /**
   * ## Maven Lockfile (State)
   *
   * Maven-specific lockfile state structure; used by Maven build contributor.
   */
  @Serializable @JvmRecord @SerialName("MavenLockfile") public data class MavenLockfile(
    @ProtoNumber(1) public val classpath: List<MavenArtifact>,
    @ProtoNumber(2) public val usage: List<MavenUsage>,
  ) : State, JSerializable {
    public companion object {
      // Serial version UID.
      @JvmStatic public val serialVersionUID: Long = 1L
    }
  }

  /** Maven classpath artifact entry. */
  @Serializable @JvmRecord @SerialName("MavenArtifact") public data class MavenArtifact(
    @ProtoNumber(1) public val id: UInt,
    @ProtoNumber(2) public val coordinate: String,
    @ProtoNumber(3) public val artifact: String,
    @ProtoNumber(4) public val fingerprint: Fingerprint,
  ) : JSerializable, Comparable<MavenArtifact> {
    public companion object {
      // Serial version UID.
      @JvmStatic public val serialVersionUID: Long = 1L
    }

    override fun compareTo(other: MavenArtifact): Int {
      return id.compareTo(other.id)
    }
  }

  /** Maven classpath usage type. */
  @Serializable public enum class MavenUsageType : JSerializable {
    COMPILE,
    RUNTIME,
    PROCESSORS,
    TEST_PROCESSORS,
    TEST,
    TEST_RUNTIME,
    DEV_ONLY,
    MODULES,
  }

  /** Holds Maven usage types/mappings for dependencies; needed for classpath assembly. */
  @Serializable @JvmRecord @SerialName("MavenUsage") public data class MavenUsage(
    @ProtoNumber(1) public val id: UInt,
    @ProtoNumber(2) public val types: Set<MavenUsageType>,
  ) : JSerializable {
    public companion object {
      // Serial version UID.
      @JvmStatic public val serialVersionUID: Long = 1L
    }
  }

  /**
   * ## Stanza
   *
   * Describes a stanza of information contributed to this lockfile; since Elide lockfile structures cover multiple
   * ecosystems, stanzas can be produced and "contributed" depending on which are in use for a given project. Lockfile
   * stanzas specify their own [fingerprint] and have control over their own [inputs].
   *
   * @property contributedBy Tool or module which contributed this stanza.
   * @property inputs Inputs which are relevant to this stanza.
   * @property fingerprint Fingerprint for this stanza.
   * @property remarks Remarks for this stanza; optional.
   * @property state State for this stanza; optional. Must extend [State].
   */
  @Serializable public sealed interface Stanza : LockfileElement<Stanza>, Fingerprinted {
    public val contributedBy: String?
    public val inputs: Set<InputMaterial>
    override val fingerprint: Fingerprint
    public val remarks: Remarks?
    public val state: State?
  }

  /**
   * ## Stanza Data
   *
   * Non-custom implementation of stanza data.
   */
  @Serializable @JvmRecord @SerialName("data") public data class StanzaData (
    override val identifier: String,
    override val fingerprint: Fingerprint,
    override val contributedBy: String = "",
    override val inputs: Set<InputMaterial>,
    override val remarks: Remarks = Remarks.None,
    override val state: State = State.NoState,
  ) : Stanza

  /** Structural version of this lockfile. */
  @ProtoNumber(1) @SerialName("version") public val version: Version

  /** Top-level fingerprint. */
  @ProtoNumber(2) @SerialName("fingerprint") public val fingerprint: Fingerprint

  /** Stanzas constituent to this lockfile. */
  @ProtoNumber(3) @SerialName("stanzas") public val stanzas: Set<Stanza>

  /** Accessors and factories which ultimately produce [ElideLockfile] instances. */
  public companion object {
    /** @return Latest lockfile definition format. */
    @JvmStatic public fun latest(): LockfileDefinition<*> = ElideLockfileV1

    /** @return Sequence of all lockfile versions, in the order they should be tried. */
    @JvmStatic public fun supported(): Sequence<LockfileDefinition<*>> = sequenceOf(
      latest()
    )
  }
}

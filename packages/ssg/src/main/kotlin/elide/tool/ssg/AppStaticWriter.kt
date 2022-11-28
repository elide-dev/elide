package elide.tool.ssg

import kotlinx.coroutines.Deferred
import java.io.Closeable
import kotlin.jvm.Throws

/**
 * # SSG: Static Site Writer
 *
 * This interface defines the expected API surface of an object which can take a completed Static Site Generator compile
 * routine and write the output. Typically, this is handled by the default implementation, which simply writes according
 * to CLI or programmatic invocation parameters.
 *
 * During testing, an alternate implementation can be provided which holds results in memory, or does something else
 * fancy with the compiler results.
 */
public interface AppStaticWriter : Closeable, AutoCloseable {
  /**
   * Container for the full set of written fragment outputs.
   *
   * @param path Output base path on the filesystem.
   * @param fragments Set of fragments to encapsulate.
   */
  public class FragmentOutputs private constructor (
    public val path: String,
    public val fragments: List<StaticFragment>,
  ) {
    public companion object {
      /** Build a set of [FragmentOutputs] from the provided [fragments]. */
      @JvmStatic public fun of(path: String, fragments: List<StaticFragment>): FragmentOutputs {
        return FragmentOutputs(path, fragments)
      }
    }
  }

  /**
   * Container for a single written fragment output.
   *
   * @param fragment Fragment to wrap.
   * @param writeResult Result of the write operation.
   * @param path Relative path to this file, from the output base.
   * @param size Size of the written file, in bytes.
   * @param compressed Compressed size of the written file, in bytes.
   * @param err Throwable caught while writing, if failed.
   */
  public class FragmentWrite private constructor (
    public val fragment: StaticFragment,
    public val writeResult: Boolean,
    public val path: String,
    public val size: Long? = null,
    public val compressed: Long? = null,
    public val err: Throwable? = null,
  ) {
    public companion object {
      @JvmStatic
      public fun success(fragment: StaticFragment, path: String, size: Long, compressed: Long = -1): FragmentWrite =
        FragmentWrite(fragment, true, path, size, compressed)

      @JvmStatic
      public fun failure(fragment: StaticFragment, path: String, err: Throwable): FragmentWrite =
        FragmentWrite(fragment, false, path, null, null, err)
    }

    override fun toString(): String {
      val successLabel = if (writeResult) {
        "Success"
      } else {
        "Failure"
      }
      return "FragmentWrite($successLabel, path = $path, size = $size, compressed = $compressed)"
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as FragmentWrite
      return (
        (fragment == other.fragment) &&
        (writeResult == other.writeResult) &&
        (path == other.path) &&
        (size == other.size) &&
        (compressed == other.compressed)
      )
    }

    override fun hashCode(): Int {
      var result = fragment.hashCode()
      result = 31 * result + writeResult.hashCode()
      result = 31 * result + path.hashCode()
      result = 31 * result + (size?.hashCode() ?: 0)
      result = 31 * result + (compressed?.hashCode() ?: 0)
      return result
    }


  }

  /**
   * Given the original set of compiler [params] passed via CLI or programmatic invocation, and a target output [buffer]
   * to place results in, write all held outputs in the buffer, and then return a set of [FragmentOutputs] as results.
   *
   * This method operates synchronously, with suspension. For async dispatch (that produces a [Deferred] job), see
   * [writeAsync].
   *
   * Because this method immediately awaits results, exceptions are thrown at the callsite; known exceptions are
   * translated to [SSGCompilerError].
   *
   * @see writeAsync for asynchronous dispatch.
   * @param params Compiler parameters, provided via CLI or programmatic invocation.
   * @param buffer Output buffer which should be written.
   * @return Set of written fragment outputs.
   * @throws SSGCompilerError if a fatal compiler error occurs.
   */
  @Throws(SSGCompilerError::class)
  public suspend fun write(params: SiteCompilerParams, buffer: StaticSiteBuffer): FragmentOutputs = writeAsync(
    params,
    buffer,
  ).await()

  /**
   * Given the original set of compiler [params] passed via CLI or programmatic invocation, and a target output [buffer]
   * to place results in, write all held outputs in the buffer, and then return a set of [FragmentOutputs] as results.
   *
   * This method operates asynchronously, with suspension. For virtualized synchronous dispatch (that produces a
   * [FragmentOutputs] result directly), see [write].
   *
   * @see write for synchronous dispatch.
   * @param params Compiler parameters, provided via CLI or programmatic invocation.
   * @param buffer Output buffer which should be written.
   * @return Set of written fragment outputs.
   */
  public suspend fun writeAsync(params: SiteCompilerParams, buffer: StaticSiteBuffer): Deferred<FragmentOutputs>
}

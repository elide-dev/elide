package elide.tool.ssg

/**
 * Describes the result of an SSG compiler invocation.
 *
 * @param success Whether the invocation was successful.
 * @param exitCode Exit code for this compilation.
 */
public sealed class SiteCompileResult(
  public val success: Boolean = false,
  public open val params: SiteCompilerParams,
  public open val exitCode: Int = if (success) { 0 } else { -1 },
) {
  /** Successful compilation response payload. */
  public class Success(
    params: SiteCompilerParams,
    public val appInfo: LoadedAppInfo,
    public val output: String,
    public val buffer: StaticSiteBuffer,
  ) : SiteCompileResult(true, params) {
    override fun toString(): String = "CompileResult(Success)"
  }

  /** Compilation failure response. */
  public class Failure(
    params: SiteCompilerParams,
    public val err: Throwable,
    exitCode: Int,
  ) : SiteCompileResult(false, params, exitCode) {
    override fun toString(): String = "CompileResult(Failure, exitCode = $exitCode)"
  }
}

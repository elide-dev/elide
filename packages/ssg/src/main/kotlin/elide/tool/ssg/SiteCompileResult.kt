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
  public data class Success(
    override val params: SiteCompilerParams,
    public val appInfo: LoadedAppInfo,
    public val output: String,
    public val buffer: StaticSiteBuffer,
  ) : SiteCompileResult(true, params)

  /** Compilation failure response. */
  public data class Failure(
    override val params: SiteCompilerParams,
    public val err: Throwable,
    override val exitCode: Int,
  ) : SiteCompileResult(false, params, exitCode)
}

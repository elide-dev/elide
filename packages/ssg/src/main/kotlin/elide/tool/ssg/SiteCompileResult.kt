/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

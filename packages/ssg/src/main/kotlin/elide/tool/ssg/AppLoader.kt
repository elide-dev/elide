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

import tools.elide.meta.AppManifest
import java.io.Closeable
import java.net.URL
import kotlinx.coroutines.Deferred

/**
 * # SSG: Application Runner
 *
 * This interface defines the API surface of an object charged with running an Elide application within the context of
 * an SSG compiler build. The app must be running while requests are executed against it; then, after request activity
 * has concluded, the app must safely be stopped and cleaned up.
 *
 * In CLI circumstances, the app will need to be run on the command line, either via a JAR or native executable. In
 * testing circumstances, the app can be run inline with requests directed within the test process.
 *
 * There is a special mode, "HTTP mode," which, when active, directs the application runner to load against an external
 * HTTP URL instead of a JAR or native executable. In this mode, the `target` parameter passed on the command line, or
 * in the scope of a programmatic invocation, is expected to be an HTTP or HTTPS URL instead of a file system path.
 *
 * ## Target application expectations
 *
 * The target application is expected to be a well-formed Elide application, which extends Micronaut's same guarantees;
 * i.e. the app must be configurable via environment variables and system properties at runtime, so that the compiler
 * can assign a port and other configuration values to the app.
 *
 * ## HTTP mode
 *
 * When "HTTP mode" is active, the app loader will direct requests to an external URL, instead of loading classes from
 * a JAR. In this mode, the `target` parameter is expected to be a well-formed HTTP or HTTPS URL. Requests are executed
 * against the provided URL with the loaded app manifest.
 */
public interface AppLoader : Closeable, AutoCloseable {
  /**
   * Prepare to execute an application by loading the `jar` or HTTP target specified within the provided [params]; if
   * extra classpath entries are provided by invoking tooling, they can be considered here.
   *
   * This method operates synchronously. For asynchronous dispatch, see [prepAsync].
   *
   * @see prepAsync for asynchronous dispatch.
   * @param params Compiler parameters to consider.
   * @param app Loaded application manifest.
   * @return Loaded application info.
   * @throws SSGCompilerError if an error occurs while loading the application JAR.
   */
  @Throws(SSGCompilerError::class)
  public suspend fun prep(params: SiteCompilerParams, app: AppManifest): LoadedAppInfo = prepAsync(
    params,
    app,
  ).await()

  /**
   * Prepare to execute an application by loading the `target` from the provided [params] (if HTTP mode is inactive);
   * otherwise, parse the provided URL in HTTP mode.
   *
   * If extra classpath entries are provided by invoking tooling, they can be considered here. This method operates
   * asynchronously. For synchronous dispatch, see [prep]. The returned URL
   *
   * @see prep for synchronous dispatch.
   * @param params Compiler parameters to consider.
   * @param app Loaded application manifest.
   * @return Deferred job that completes with [LoadedAppInfo] and a [URL] when the JAR is loaded, or throws an error.
   * @throws SSGCompilerError if an error occurs while loading the application JAR.
   */
  public suspend fun prepAsync(params: SiteCompilerParams, app: AppManifest): Deferred<LoadedAppInfo>

  /**
   * Generate a set of seed requests against known application endpoints, based on the [AppManifest] provided to the
   * [prep] method; the provided [factory] should be used to generate each request.
   *
   * @param factory Factory to use to generate requests.
   * @return Set of seed requests, as an [Int] count paired with a [Sequence].
   */
  public suspend fun generateRequests(factory: RequestFactory): Pair<Int, Sequence<StaticFragmentSpec>>

  /**
   * Execute the provided [StaticFragmentSpec] against the currently-loaded application; prepare the result in a final
   * [StaticFragment] record and return it.
   *
   * If a response cannot be acquired from the user's application for whatever reason, `null` is returned, so that it
   * may hold place for error counts.
   *
   * @param spec Fragment specification to execute.
   * @return Finalized fragment, or `null` if a non-fatal error occurred.
   */
  public suspend fun executeRequest(spec: StaticFragmentSpec): StaticFragment?
}

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
@file:Suppress("unused")

package elide.runtime.feature.js

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeReflection
import elide.annotations.engine.VMFeature
import elide.runtime.feature.FrameworkFeature

@VMFeature internal class FetchFeature: FrameworkFeature {
  override fun getDescription(): String = "Sets up reflective types for Fetch API support"

  override fun getRequiredFeatures(): List<Class<out Feature?>?> = listOf(
    JavaScriptFeature::class.java,
  )

  private fun allFetchReflectiveTypes(): List<Class<*>> = listOf(
    java.lang.AutoCloseable::class.java,
    java.lang.Comparable::class.java,
    java.nio.charset.StandardCharsets::class.java,
    java.util.Base64::class.java,
    java.net.URI::class.java,
    java.net.http.HttpClient::class.java,
    java.net.http.HttpClient.Redirect::class.java,
    java.net.http.HttpRequest::class.java,
    java.net.http.HttpRequest.BodyPublishers::class.java,
    java.net.http.HttpResponse::class.java,
    java.net.http.HttpResponse.BodyHandlers::class.java,
    java.net.ConnectException::class.java,
  )

  override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess?) {
    allFetchReflectiveTypes().forEach {
      RuntimeReflection.register(it)
      RuntimeReflection.registerAllClasses(it)
      RuntimeReflection.registerAllConstructors(it)
      RuntimeReflection.registerAllMethods(it)
      RuntimeReflection.registerAllFields(it)
    }
    super.beforeAnalysis(access)
  }
}

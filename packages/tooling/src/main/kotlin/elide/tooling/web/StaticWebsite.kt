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
package elide.tooling.web

/**
 * ## Static Website
 *
 * Describes a type of [WebProject] which ultimately renders to static files.
 */
public interface StaticWebsite : WebProject {
  // Static sites are not dynamic.
  override fun isDynamic(): Boolean = false

  // Static sites do not start their own servers.
  override fun startsServer(): Boolean = false
}

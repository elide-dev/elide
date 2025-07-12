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
 * ## Static Website (Routed)
 *
 * Describes a type of [WebProject], specifically, a [StaticWebsite], for which routing is performed by the framework,
 * based on project structure on-disk. This is also referred to as a "static site generator" (SSG) project.
 */
public sealed interface RoutedStaticWebsite : StaticWebsite {
}

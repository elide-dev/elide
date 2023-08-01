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

package elide.core.platform

/**
 * # Core: Platform-specific Defaults
 *
 * It is expected that a set of platform defaults are defined for each platform that Elide supports. These defaults can
 * reference the global set of universal defaults ([elide.core.Defaults]), or override with their own values. All
 * defaults must be defined at compile time.
 */
public expect class PlatformDefaults : elide.core.PlatformDefaults

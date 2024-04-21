/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime.plugins.js

import elide.runtime.core.DelicateElideApi

@DelicateElideApi public enum class JavaScriptVersion {
  ES5,
  ES6,
  ES2017,
  ES2018,
  ES2019,
  ES2020,
  ES2021,
  ES2022,
  ES2023,
  STABLE,
  LATEST,
}

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

package elide.runtime

/**
 * # Elide: Runtime
 *
 * This object provides hard-coded constants which Elide uses to load packages and perform other key work at runtime. In
 * the [generatedPackage], classes and constants are held which are passed through by the build tools.
 */
public object Runtime {
  /** Package under which build-time values are provided. */
  public const val generatedPackage: String = "elide.runtime.generated"
}

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

@file:Suppress("RedundantVisibilityModifier")

package elide.tool.bundler

import picocli.CommandLine
import java.io.File

/**
 * TBD.
 */
public class CommonBundleOptions {
  /** Bundle file we are working with, as applicable. */
  @set:CommandLine.Option(
    names = ["-f", "--bundle"],
    paramLabel = "FILE",
    description = ["Specifies the bundle file we should work with"],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var file: File? = null

  /** Explicit flag to consume from `stdin`. */
  @set:CommandLine.Option(
    names = ["--stdin"],
    description = ["Indicates that the bundler should wait for data from standard-in"],
    scope = CommandLine.ScopeType.INHERIT,
  )
  internal var stdin: Boolean = false
}

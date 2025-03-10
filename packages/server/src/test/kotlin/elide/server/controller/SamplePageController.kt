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

package elide.server.controller

import io.micronaut.http.annotation.Get
import kotlinx.html.strong
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import kotlinx.html.title
import elide.server.*
import elide.server.annotations.Page

/** Sample page controller for testing. */
@Page class SamplePageController : PageController() {
  @Get("/") suspend fun indexPage() = html {
    head {
      title { +"Hello, Elide!" }
      stylesheet("/styles/base.css")
      script("/scripts/ui.js", defer = true)
    }
    body {
      strong {
        +"Hello Sample Page!"
      }
      script("/scripts/ui.js", defer = true)
    }
  }

  @Get("/something.txt") suspend fun somethingText() = staticFile(
    "something.txt",
    "text/plain",
  )

  @Get("/something-not-found.txt") suspend fun somethingMissing() = staticFile(
    "something-not-found.txt",
    "text/plain",
  )
}

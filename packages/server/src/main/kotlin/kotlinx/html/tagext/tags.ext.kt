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

package kotlinx.html.tagext

import kotlinx.html.*

/**
 * Open a `<body>` tag with support for suspension calls.
 *
 * @Param classes Classes to apply to the body tag in the DOM.
 * @param block Callable block to configure and populate the body tag.
 */
@HtmlTagMarker
public suspend inline fun HTML.body(
  classes : String? = null,
  crossinline block : suspend BODY.() -> Unit
) : Unit = BODY(
  attributesMapOf("class", classes),
  consumer
).visitSuspend(block)


/**
 * Open a `<head>` tag with support for suspension calls.
 *
 * @param block Callable block to configure and populate the body tag.
 */
@HtmlTagMarker
public suspend inline fun HTML.head(
  crossinline block : suspend HEAD.() -> Unit
) : Unit = HEAD(emptyMap, consumer).visitSuspend(
  block
)

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

package kotlinx.html


// Visitor with suspension support.
public suspend inline fun <T : Tag> T.visitSuspend(crossinline block: suspend T.() -> Unit): Unit = visitTagSuspend {
  block()
}

// Tag visitor with suspension support.
@Suppress("TooGenericExceptionCaught")
public suspend inline fun <T : Tag> T.visitTagSuspend(crossinline block: suspend T.() -> Unit) {
  consumer.onTagStart(this)
  try {
    this.block()
  } catch (err: Throwable) {
    throw err
  } finally {
    consumer.onTagEnd(this)
  }
}

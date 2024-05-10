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

@file:Suppress("WildcardImport")

package elide.server.ssr

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.html.BODY
import kotlinx.html.script
import kotlinx.html.stream.appendHTML
import kotlinx.html.unsafe
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.ExecutableScript
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.js.JavaScript
import elide.server.SuspensionRenderer
import elide.server.controller.ElideController
import elide.server.controller.PageWithProps
import elide.ssr.ServerResponse
import elide.ssr.type.RequestState
import elide.vm.annotations.Polyglot

/** Renderer class which executes JavaScript via SSR and provides the resulting response to Micronaut. */
@Suppress("MemberVisibilityCanBePrivate", "unused", "SpreadOperator")
public class ServerSSRRenderer (
  private val body: BODY,
  private val handler: ElideController,
  private val request: HttpRequest<*>,
  private val script: ExecutableScript,
  private val buffer: StringBuilder = StringBuilder(),
  private var job: AtomicReference<Job?> = AtomicReference(null),
) : SuspensionRenderer<ByteArrayOutputStream> {
  public companion object {
    /** ID in the DOM where SSR data is affixed, if present. */
    public const val ssrId: String = "ssr-data"
  }

  // Logger.
  private val logging: Logger = Logging.of(ServerSSRRenderer::class)

  /** Execute the provided operation with any prepared SSR execution context. */
  internal suspend fun prepareContext(op: suspend (ExecutionInputs, Any?) -> StringBuilder): String {
    return if (handler is PageWithProps<*>) {
      // build context
      val state = RequestState(
        request,
        null,
      )
      val (props, serialized) = handler.finalizeAsync(state).await()
      val buf = op.invoke(
        JavaScript.Inputs.requestState(
          state,
          props,
        ),
        props,
      )
      if (props != null && serialized != null) {
        val subBuffer = StringBuilder()
        subBuffer.appendHTML().script(
          type = "application/json"
        ) {
          attributes["id"] = ssrId
          unsafe {
            +serialized
          }
        }
        buf.append(subBuffer)
      }
      buf.toString()
    } else {
      StringBuilder("<!doctype html>").append(
        op.invoke(JavaScript.Inputs.EMPTY, null)
      ).toString()
    }
  }

  // Handle a chunk which is ready to serve back to the invoking agent.
  @Polyglot private fun chunkReady(chunk: ServerResponse) {
    if (chunk.css.isNotBlank()) {
      buffer.append(chunk.css)
    }
    if (chunk.hasContent) {
      buffer.append(chunk.content)
    }
    if (chunk.fin) {
      job.get()?.cancel()
    }
  }

  /**
   * Render the attached [script] with suspension support, and return the resulting content as a regular [String].
   *
   * @return String render result from [script].
   */
  public suspend fun renderSuspendAsync(): Deferred<String> = coroutineScope {
    return@coroutineScope async {
      prepareContext { _, props ->
        TODO("Stubbed for transition to plugin-based VM")
//        val vm = handler.context().findBean(VMFacadeFactory::class.java).orElseThrow {
//          error("Failed to resolve JavaScript runtime provider")
//        }.acquireVM(
//          GraalVMGuest.JAVASCRIPT  // @TODO(sgammon): don't hard-code this
//        )
//
//        buffer.apply {
//          logging.trace("Starting SSR execution")
//          val op = vm.executeRender(
//            script,
//            request,
//            receiver = ::chunkReady,
//            context = props,
//          )
//          job.set(op)
//          op.join()
//        }
      }
    }
  }

  /**
   * Render the attached [script] and return the resulting content as a [ByteArrayOutputStream], built from the result
   * of [renderSuspendAsync].
   *
   * @return Byte stream of resulting content.
   */
  override suspend fun render(): ByteArrayOutputStream {
    val byteStream = ByteArrayOutputStream()
    byteStream.bufferedWriter(StandardCharsets.UTF_8).use {
      it.write(
        renderSuspendAsync().await()
      )
    }
    return byteStream
  }

  /**
   * Render the attached [script] into a [ByteArrayOutputStream], and wrap it in a Micronaut [MutableHttpResponse]
   * provided at [response].
   *
   * @param response Base mutable response to fill body data for.
   * @return Mutable [response] with body data filled in from the execution result of [script].
   */
  public suspend fun renderResponse(
    response: MutableHttpResponse<ByteArrayOutputStream>,
  ): MutableHttpResponse<ByteArrayOutputStream> {
    return response.body(
      render()
    )
  }
}

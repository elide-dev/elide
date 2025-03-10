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
package elide.runtime.gvm.internals.sqlite

import java.util.concurrent.atomic.AtomicReference

// Token used for dynamic positional rendering.
private const val DYNAMIC_TOKEN_POSITIONAL = '?'

// Token used for dynamic named rendering.
private const val DYNAMIC_TOKEN_NAMED = '$'

// Alternate token used for dynamic named rendering.
private const val DYNAMIC_TOKEN_NAMED_ALT = ':'

// Holds context for a query template; materialized when the query is rendered.
@JvmRecord private data class QueryTemplateContext(
  val identity: Int,
  val positional: Array<out Any?>? = null,
  val named: Map<String, Any?>? = null,
) {
  override fun hashCode(): Int = identity
  override fun equals(other: Any?): Boolean = (other as? QueryTemplateContext)?.identity == this.identity
}

// If the subject string is non-dynamic, return self; otherwise, prepare and render a final query string.
private inline fun String.selfIfNonDynamic(crossinline block: (Int) -> String): String = any {
  it == DYNAMIC_TOKEN_POSITIONAL || it == DYNAMIC_TOKEN_NAMED || it == DYNAMIC_TOKEN_NAMED_ALT
}.let { dynamic ->
  when (dynamic) {
    false -> this
    true -> block(this.hashCode())
  }
}

// Renders a dynamic query with the provided argument context. Designed to reject certain types which are known to
// originate from user-provided data, or which are known to be unsafe.
internal class SqliteQueryRenderer private constructor (
  private val identity: Int,
  private val template: String,
  private val renderContext: AtomicReference<QueryTemplateContext> = AtomicReference(),
) {
  companion object {
    // Create a query renderer.
    private fun create(template: String, identity: Int) =
      SqliteQueryRenderer(identity, template)

    // Create or resolve a query renderer.
    fun resolve(template: String, identity: Int = template.hashCode()) = create(template, identity)

    // Creates a new query renderer with the provided template, and renders the string.
    fun render(template: String, positional: Array<out Any?>? = null, named: Map<String, Any?>? = null): String =
      template.selfIfNonDynamic { identity ->
        SqliteQueryRenderer(identity, template).render(QueryTemplateContext(identity, positional, named))
      }
  }

  // Cached rendered query.
  private val cachedRendered: AtomicReference<String> = AtomicReference(renderContext.get()?.let { render(it) })

  private fun obtainRendered(): String = cachedRendered.get() ?: render(renderContext.get()).also {
    cachedRendered.set(it)
  }

  private fun invalidate() {
    renderContext.set(null)
    cachedRendered.set(null)
  }

  // Render a nice query rendering error, which mentions the query itself, the position where the error happened, and
  // any helpful advice to fix the error.
  @Suppress("UNUSED_PARAMETER")
  private fun queryRenderError(
    subject: String,
    ctx: QueryTemplateContext? = null,
    message: String,
    position: Int = -1,
    soFar: Int = -1,
  ): Throwable {
    TODO("no error rendering yet (message: $message)")
  }

  // Render a single resolved dynamic query value into the receiving string builder.
  private fun StringBuilder.stringifyAppend(skip: Int, value: Any?): Int = skip.also {
    when (value) {
      // special `NULL` token
      null -> append("NULL")

      // string value, quoted with single-quotes
      is String -> append('\'').append(value).append('\'')

      // otherwise, let the regular string-conversion algorithm handle it
      else -> append(value)
    }
  }

  // Render a single positional value, at the provided `position`, from the provided query context; the value must be
  // reducible to a string safely, and is expected to be present, or an error is thrown.
  private fun StringBuilder.renderPositionalValue(position: Int, ctx: Array<out Any?>, offset: Int = 0): Int =
    stringifyAppend(offset, ctx[position])

  // Render a single named value, at the provided `name`, from the provided query context; the value must be reducible
  // to a string safely, and is expected to be present, or an error is thrown.
  private fun StringBuilder.renderNamedValue(variableName: String, ctx: Map<String, Any?>): Int =
    stringifyAppend(variableName.length, ctx[variableName])

  // Render a single dynamic query value, either as a positional parameter or named parameter. This function is called
  // at the beginning position of the string, before the name or positional index has been resolved; the returned pair
  // indicates the positions to skip and the rendered value to splice in.
  private fun StringBuilder.renderValue(
    trigger: Char,
    renderedSoFar: Int,
    subject: String,
    position: Int,
    ctx: QueryTemplateContext?,
  ): Int {
    // we don't know if the context is even available yet, but we need to decode the variable name so we can include it
    // in error messages.
    val isPositional = trigger == DYNAMIC_TOKEN_POSITIONAL
    return when {
      // if the context is missing, the user provided no arguments at all; generate an error.
      ctx == null -> throw queryRenderError(subject, message = "No query arguments provided")

      // if the parameter is positional...
      isPositional -> when {
        // ...but there are no positional parameters, generate an error.
        ctx.positional == null -> throw queryRenderError(
          subject,
          ctx,
          "No positional arguments provided",
          position,
          soFar = renderedSoFar,
        )

        // otherwise, we are ready to perform the replacement.
        else -> renderPositionalValue(renderedSoFar, ctx.positional)
      }

      // if the parameter is named...
      else -> when (val named = ctx.named) {
        // ...but there are no named parameters, generate an error.
        null -> throw queryRenderError(
          subject,
          ctx,
          "No named arguments provided",
          position,
          soFar = renderedSoFar,
        )

        else -> when {
          // ...but the named parameters are empty, generate an error.
          named.isEmpty() -> throw queryRenderError(
            subject,
            ctx,
            "Named arguments are empty",
            position,
            soFar = renderedSoFar,
          )

          // otherwise, begin processing the variable name...
          else -> StringBuilder().apply {
            for (charI in (position + 1)..subject.lastIndex) {
              val char = subject[charI]
              if (char !in 'A'..'Z' && char !in 'a'..'z') break
              append(char)
            }
          }.toString().let { variableName ->
            when {
              variableName.isEmpty() -> throw queryRenderError(
                subject,
                ctx,
                "Empty variable at position $position in '$subject'",
                position,
                soFar = renderedSoFar,
              )

              // when the variable name is missing, generate an error.
              !named.containsKey(variableName) ->  throw queryRenderError(
                subject,
                ctx,
                "No named query argument at '$variableName'",
                position,
                soFar = renderedSoFar,
              )

              // otherwise, we are ready to process the replacement.
              else -> renderNamedValue(variableName, ctx.named)
            }
          }
        }
      }
    }
  }

  // Renders the current query and argument suite, assigning to the cache afterward.
  private fun renderCurrent(ctx: QueryTemplateContext?): String = StringBuilder().apply {
    var skipCount = 0
    var renderings = 0
    for (charI in 0..template.lastIndex) {
      // if we should be skipping characters, do that while consuming within the loop
      if (skipCount > 0) {
        skipCount--
        continue
      }

      // otherwise, step character-wise through the string...
      when (val char = template[charI]) {
        // if we are asked to inject a positional parameter, do so, and then apply the skip amount to omit the rest of
        // the positional variable name, as applicable.
        DYNAMIC_TOKEN_POSITIONAL -> {
          skipCount = renderValue(DYNAMIC_TOKEN_POSITIONAL, renderings, template, charI, ctx)
          renderings++
        }

        // if we are asked to inject a named parameter, do so, and then apply the skip amount to omit the rest of the
        // name, as applicable.
        DYNAMIC_TOKEN_NAMED, DYNAMIC_TOKEN_NAMED_ALT -> {
          skipCount = renderValue(DYNAMIC_TOKEN_NAMED, renderings, template, charI, ctx)
          renderings++
        }

        // otherwise, just append characters.
        else -> append(char)
      }
    }
  }.toString().also {
    renderContext.set(ctx)
    cachedRendered.set(it)
  }

  // Renders the query with the provided context.
  private fun render(context: QueryTemplateContext?): String {
    var checkCache = true
    return when (context) {
      null -> renderContext.get()
      else -> when (val current = renderContext.get()) {
        null -> context
        else -> if (current.equals(context)) current else {
          checkCache = false
          invalidate()
          context
        }
      }
    }.let {
      if (!checkCache) renderCurrent(it) else (cachedRendered.get() ?: renderCurrent(it))
    }
  }

  override fun hashCode(): Int = identity

  override fun equals(other: Any?): Boolean = when (other) {
    is String -> template == other || when (val cached = cachedRendered.get()) {
      null -> render(renderContext.get()) == other
      else -> cached == other
    }

    is SqliteQueryRenderer -> identity == other.identity
    else -> false
  }

  override fun toString(): String = cachedRendered.get() ?: obtainRendered()
}

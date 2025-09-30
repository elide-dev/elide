/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2.flask

import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import org.graalvm.polyglot.Value
import java.net.URLEncoder

public class FlaskRouter {
  private enum class RouteVariableType {
    STRING,
    INT,
    FLOAT,
    PATH,
    UUID,
  }

  internal sealed interface MatcherResult {
    data object NoMatch : MatcherResult
    data object MethodNotAllowed : MatcherResult
    @JvmInline value class MissingVariable(val name: String) : MatcherResult
    @JvmInline value class InvalidVariable(val name: String) : MatcherResult

    data class Match(
      val handler: Value,
      val parameters: Map<String, Any>,
    ) : MatcherResult
  }

  private class Matcher(
    val rule: String,
    val pattern: Regex,
    val handler: Value,
    val variableTypes: Map<String, RouteVariableType>,
    val methods: Array<HttpMethod>,
  ) {
    fun reifyPath(variables: Map<String, Any>): String = buildString {
      val applied = mutableSetOf<String>()

      val baseUrl = rule.replace(untypedVariableRegex) {
        val name = it.groups[1]?.value ?: error("Internal error: failed to resolve variable name")
        val value = variables[name] ?: error("Missing value for path variable $name")

        applied.add(name)
        URLEncoder.encode(value.toString(), Charsets.UTF_8)
      }

      append(baseUrl)
      if (applied.size < variables.size) {
        var queryParams = 0
        for ((name, value) in variables) if (name !in applied) {
          if (queryParams > 0) append("&")
          else append("?")
          
          queryParams++
          append(URLEncoder.encode(name, Charsets.UTF_8))
          append("=")
          append(URLEncoder.encode(value.toString(), Charsets.UTF_8))
        }
      }
    }

    fun match(request: HttpRequest): MatcherResult {
      if (request.method() !in methods) return MatcherResult.NoMatch

      val routeMatch = pattern.matchEntire(request.uri().substringBefore('?'))
        ?: return MatcherResult.NoMatch

      val parameters = buildMap {
        variableTypes.forEach { (name, type) ->
          val value = routeMatch.groups[name]?.value
            ?: return MatcherResult.MissingVariable(name)

          val mappedValue: Any? = when (type) {
            RouteVariableType.INT -> value.toIntOrNull()
            RouteVariableType.FLOAT -> value.toFloatOrNull()
            else -> value
          }

          if (mappedValue == null) return MatcherResult.InvalidVariable(name)
          put(name, mappedValue)
        }
      }

      return MatcherResult.Match(handler, parameters)
    }
  }

  private val stack = mutableMapOf<String, Matcher>()

  internal fun urlFor(endpoint: String, variables: Map<String, Any>): String {
    val matcher = stack[endpoint] ?: error("Endpoint $endpoint is not registered")
    return matcher.reifyPath(variables)
  }

  internal fun match(request: HttpRequest): MatcherResult {
    return stack.values.firstNotNullOfOrNull { matcher ->
      matcher.match(request).takeUnless { it == MatcherResult.NoMatch }
    } ?: MatcherResult.NoMatch
  }

  internal fun register(endpoint: String, pattern: String, methods: Array<HttpMethod>, handler: Value) {
    if (stack.containsKey(endpoint)) error("Endpoint $endpoint is already registered")
    stack[endpoint] = compileMatcher(pattern, methods, handler)
  }

  private companion object {
    private val pathVariableRegex = Regex("<(?:(\\w+):)?([a-zA-Z_]+)>")
    private val untypedVariableRegex = Regex("<(?:\\w+:)?([a-zA-Z_]+)>")

    private fun compileMatcher(rule: String, methods: Array<HttpMethod>, handler: Value): Matcher {
      val pathVariables = mutableMapOf<String, RouteVariableType>()

      val pattern = rule.replace(pathVariableRegex) {
        val type = when (it.groups[1]?.value) {
          "int" -> RouteVariableType.INT
          "float" -> RouteVariableType.FLOAT
          "uuid" -> RouteVariableType.UUID
          "path" -> RouteVariableType.PATH
          "string", null -> RouteVariableType.STRING
          else -> error("Unsupported variable type: ${it.groups[1]?.value}")
        }

        val name = it.groups[2]?.value ?: error("Invalid path variable")
        pathVariables[name] = type

        if (type == RouteVariableType.PATH) "(?<$name>[a-zA-Z0-9_\\-/]+)"
        else "(?<$name>[a-zA-Z0-9_\\-]+)"
      }.let {
        // when the canonical path uses a trailing slash, it should also match requests without it
        if (it.endsWith("/")) "$it?" else it
      }.let {
        Regex(it)
      }

      return Matcher(rule, pattern, handler, pathVariables, methods)
    }
  }
}

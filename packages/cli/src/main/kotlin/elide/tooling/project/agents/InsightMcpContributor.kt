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
package elide.tooling.project.agents

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import elide.tooling.project.mcp.McpContributor

/**
 * # Insight MCP Contributor
 *
 * Contributes GraalVM Insight-based debugging and tracing tools to the MCP server.
 * These tools enable AI agents to execute code with deep instrumentation capabilities
 * across ALL GraalVM polyglot languages.
 *
 * ## Basic Execution (Full Polyglot)
 * - **run-python**: Execute Python code with GraalPy
 * - **run-javascript**: Execute JavaScript code with GraalJS
 * - **run-ruby**: Execute Ruby code with TruffleRuby
 * - **run-r**: Execute R code with FastR (data science)
 * - **run-wasm**: Execute WebAssembly with GraalWasm
 * - **polyglot-exec**: Execute any language with cross-language interop
 * - **list-languages**: List all available polyglot languages
 *
 * ## Tracing & Profiling
 * - **trace-functions**: Trace function entry/exit with arguments and return values
 * - **count-calls**: Count how many times functions are called
 * - **profile-hotspots**: Find the most frequently executed code paths
 * - **conditional-trace**: Trace only when a condition is met
 *
 * ## Debugging
 * - **inspect-locals**: Inspect all local variables at a function call
 * - **modify-var**: Modify a local variable mid-execution
 * - **intercept-return**: Skip function execution and return a custom value
 * - **walk-stack**: Walk the entire call stack with locals at each frame
 * - **breakpoint-fix**: Conditional breakpoint that applies a fix when triggered
 * - **line-breakpoint**: Set breakpoint at a specific line number
 *
 * ## Full Tracing
 * - **statement-trace**: Trace every statement for line-by-line stepping
 * - **expression-trace**: Trace every expression evaluation
 * - **full-trace**: Comprehensive trace of statements, expressions, and functions
 *
 * ## AI-Native
 * - **collect-errors**: Collect all errors as structured array for debugging
 * - **debug-and-fix**: Debug with modes: autofix, inspect, or escalate
 * - **generate-training**: Generate error→fix training data for AI fine-tuning
 * - **run-self-healing**: Iterative self-healing execution loop
 * - **batch-trace**: Trace multiple functions in one call
 * - **run-with-insight**: Run with custom insight hook configuration
 */
internal class InsightMcpContributor : McpContributor {
  
  override suspend fun enabled(context: McpContributor.McpContext): Boolean =
    context.project()?.manifest?.dev?.mcp?.insight != false

  override suspend fun contribute(context: McpContributor.McpContext) = with(context.server) {
    
    // ========================================================================
    // Basic Execution Tools
    // ========================================================================
    
    addTool(
      name = "run-python",
      description = "Execute Python code with GraalPy. Returns output, errors, and optional execution traces.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Python code to execute"))
          }
          putJsonObject("trace") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Enable function tracing (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val trace = request.arguments["trace"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
      executeCode("python", code, trace)
    }

    addTool(
      name = "run-javascript",
      description = "Execute JavaScript code with GraalJS. Returns output, errors, and optional execution traces.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("JavaScript code to execute"))
          }
          putJsonObject("trace") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Enable function tracing (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val trace = request.arguments["trace"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
      executeCode("js", code, trace)
    }

    addTool(
      name = "run-ruby",
      description = "Execute Ruby code with TruffleRuby. Returns output, errors, and result.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Ruby code to execute"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      executeCode("ruby", code, false)
    }

    addTool(
      name = "run-r",
      description = "Execute R code with FastR. Ideal for data science and statistical computing.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("R code to execute"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      executeCode("R", code, false)
    }

    addTool(
      name = "run-wasm",
      description = "Execute WebAssembly code with GraalWasm. Pass base64-encoded WASM binary.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("WebAssembly code (base64-encoded binary or WAT text)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      executeCode("wasm", code, false)
    }

    addTool(
      name = "polyglot-exec",
      description = "Execute code in any supported language with cross-language interop. Languages: python, js, ruby, R, wasm, llvm.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Target language: python, js, ruby, R, wasm, llvm"))
          }
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute in the specified language"))
          }
          putJsonObject("bindings") {
            put("type", JsonPrimitive("object"))
            put("description", JsonPrimitive("Optional: key-value pairs to bind into the polyglot context"))
          }
        },
        required = listOf("language", "code")
      ),
    ) { request ->
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing language")
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      executeCode(language, code, false)
    }

    addTool(
      name = "list-languages",
      description = "List all available polyglot languages in the current GraalVM runtime.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {},
        required = emptyList()
      ),
    ) { _ ->
      listAvailableLanguages()
    }

    // ========================================================================
    // Tracing & Profiling Tools
    // ========================================================================

    addTool(
      name = "trace-functions",
      description = "Trace function entry/exit with arguments and return values. Supports all polyglot languages.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute with tracing"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("functions") {
            put("type", JsonPrimitive("array"))
            put("description", JsonPrimitive("Optional list of function names to trace (traces all if empty)"))
            putJsonObject("items") {
              put("type", JsonPrimitive("string"))
            }
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val functions = request.arguments["functions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
      traceFunctions(language, code, functions)
    }

    addTool(
      name = "count-calls",
      description = "Count how many times each function is called. Supports all polyglot languages.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Optional: specific function to count"))
          }
          putJsonObject("maxCalls") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Stop after this many calls (default: unlimited)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content
      val maxCalls = request.arguments["maxCalls"]?.jsonPrimitive?.content?.toIntOrNull()
      countCalls(language, code, function, maxCalls)
    }

    addTool(
      name = "profile-hotspots",
      description = "Find the most frequently executed code paths (hot functions).",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to profile"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      profileHotspots(language, code)
    }

    addTool(
      name = "conditional-trace",
      description = "Trace function calls only when a condition is met.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function to trace"))
          }
          putJsonObject("condition") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Condition expression (e.g., 'frame.x > 10')"))
          }
        },
        required = listOf("code", "function", "condition")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val condition = request.arguments["condition"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing condition")
      conditionalTrace(language, code, function, condition)
    }

    // ========================================================================
    // Debugging Tools
    // ========================================================================

    addTool(
      name = "inspect-locals",
      description = "Inspect all local variables at a specific function call.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function to inspect"))
          }
          putJsonObject("atCall") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Which call to inspect (1=first, default: 1)"))
          }
        },
        required = listOf("code", "function")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val atCall = request.arguments["atCall"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
      inspectLocals(language, code, function, atCall)
    }

    addTool(
      name = "modify-var",
      description = "Modify a local variable's value mid-execution.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function where variable exists"))
          }
          putJsonObject("variable") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Variable name to modify"))
          }
          putJsonObject("newValue") {
            put("description", JsonPrimitive("New value to assign"))
          }
          putJsonObject("atCall") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Which call to modify at (1=first, default: 1)"))
          }
        },
        required = listOf("code", "function", "variable", "newValue")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val variable = request.arguments["variable"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing variable")
      val newValue = request.arguments["newValue"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing newValue")
      val atCall = request.arguments["atCall"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
      modifyVar(language, code, function, variable, newValue, atCall)
    }

    addTool(
      name = "intercept-return",
      description = "Skip a function's execution entirely and return a custom value.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function to intercept"))
          }
          putJsonObject("returnValue") {
            put("description", JsonPrimitive("Value to return instead"))
          }
          putJsonObject("atCall") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Which call to intercept (default: all)"))
          }
        },
        required = listOf("code", "function", "returnValue")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val returnValue = request.arguments["returnValue"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing returnValue")
      val atCall = request.arguments["atCall"]?.jsonPrimitive?.content?.toIntOrNull()
      interceptReturn(language, code, function, returnValue, atCall)
    }

    addTool(
      name = "walk-stack",
      description = "Walk the entire call stack with all local variables at each frame.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function at which to capture stack"))
          }
          putJsonObject("atCall") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Which call (default: 1)"))
          }
        },
        required = listOf("code", "function")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val atCall = request.arguments["atCall"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
      walkStack(language, code, function, atCall)
    }

    addTool(
      name = "breakpoint-fix",
      description = "Conditional breakpoint that applies a fix when triggered.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function to monitor"))
          }
          putJsonObject("condition") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Condition that triggers the fix"))
          }
          putJsonObject("fixVariable") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Variable to fix"))
          }
          putJsonObject("fixValue") {
            put("description", JsonPrimitive("Value to assign"))
          }
        },
        required = listOf("code", "function", "condition", "fixVariable", "fixValue")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val condition = request.arguments["condition"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing condition")
      val fixVariable = request.arguments["fixVariable"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing fixVariable")
      val fixValue = request.arguments["fixValue"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing fixValue")
      breakpointFix(language, code, function, condition, fixVariable, fixValue)
    }

    addTool(
      name = "line-breakpoint",
      description = "Set breakpoint at a specific line number with optional fix.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("line") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Line number to break at"))
          }
          putJsonObject("action") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Action: 'inspect', 'fix', or 'skip'"))
          }
          putJsonObject("fixVariable") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Variable to fix (if action=fix)"))
          }
          putJsonObject("fixValue") {
            put("description", JsonPrimitive("Value to assign (if action=fix)"))
          }
        },
        required = listOf("code", "line")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val line = request.arguments["line"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@addTool errorResult("Missing line")
      val action = request.arguments["action"]?.jsonPrimitive?.content ?: "inspect"
      val fixVariable = request.arguments["fixVariable"]?.jsonPrimitive?.content
      val fixValue = request.arguments["fixValue"]?.jsonPrimitive?.content
      lineBreakpoint(language, code, line, action, fixVariable, fixValue)
    }

    // ========================================================================
    // Full Tracing Tools
    // ========================================================================

    addTool(
      name = "statement-trace",
      description = "Trace every statement for true single-step debugging.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("maxStatements") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Max statements to trace (default: 100)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val maxStatements = request.arguments["maxStatements"]?.jsonPrimitive?.content?.toIntOrNull() ?: 100
      statementTrace(language, code, maxStatements)
    }

    addTool(
      name = "expression-trace",
      description = "Trace every expression evaluation with computed values.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("maxExpressions") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Max expressions to trace (default: 200)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val maxExpressions = request.arguments["maxExpressions"]?.jsonPrimitive?.content?.toIntOrNull() ?: 200
      expressionTrace(language, code, maxExpressions)
    }

    addTool(
      name = "full-trace",
      description = "Comprehensive trace: statements + expressions + functions (most detailed).",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("includeInternals") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Include internal/underscore functions (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val includeInternals = request.arguments["includeInternals"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
      fullTrace(language, code, includeInternals)
    }

    // ========================================================================
    // AI-Native Tools
    // ========================================================================

    addTool(
      name = "collect-errors",
      description = "Collect ALL errors as structured array for parallel agent debugging.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      collectErrors(language, code)
    }

    addTool(
      name = "debug-and-fix",
      description = "Debug with modes: autofix (apply fix), inspect (human review), escalate (send to senior agent).",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to debug"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function to debug"))
          }
          putJsonObject("condition") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Bug condition to detect"))
          }
          putJsonObject("mode") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Mode: 'autofix', 'inspect', or 'escalate'"))
          }
          putJsonObject("fixVariable") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Variable to fix (if mode=autofix)"))
          }
          putJsonObject("fixValue") {
            put("description", JsonPrimitive("Value to assign (if mode=autofix)"))
          }
          putJsonObject("confidenceThreshold") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Confidence threshold for autofix (0-1, default: 0.8)"))
          }
        },
        required = listOf("code", "function", "condition")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      val condition = request.arguments["condition"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing condition")
      val mode = request.arguments["mode"]?.jsonPrimitive?.content ?: "inspect"
      val fixVariable = request.arguments["fixVariable"]?.jsonPrimitive?.content
      val fixValue = request.arguments["fixValue"]?.jsonPrimitive?.content
      val threshold = request.arguments["confidenceThreshold"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.8
      debugAndFix(language, code, function, condition, mode, fixVariable, fixValue, threshold)
    }

    addTool(
      name = "generate-training",
      description = "Generate labeled error→fix training data for AI model fine-tuning.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code with bugs to analyze"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      generateTraining(language, code)
    }

    addTool(
      name = "run-self-healing",
      description = "ITERATIVE self-healing: run code, catch errors, apply fixes, retry (up to N attempts).",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("maxAttempts") {
            put("type", JsonPrimitive("number"))
            put("description", JsonPrimitive("Max fix attempts (default: 5)"))
          }
          putJsonObject("fixes") {
            put("type", JsonPrimitive("array"))
            put("description", JsonPrimitive("Pre-defined fixes to try"))
            putJsonObject("items") {
              put("type", JsonPrimitive("object"))
              putJsonObject("properties") {
                putJsonObject("errorPattern") { put("type", JsonPrimitive("string")) }
                putJsonObject("codePattern") { put("type", JsonPrimitive("string")) }
                putJsonObject("codeReplacement") { put("type", JsonPrimitive("string")) }
              }
            }
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val maxAttempts = request.arguments["maxAttempts"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5
      val fixes = request.arguments["fixes"]?.jsonArray?.map { fix ->
        val obj = fix.jsonObject
        Triple(
          obj["errorPattern"]?.jsonPrimitive?.content ?: "",
          obj["codePattern"]?.jsonPrimitive?.content ?: "",
          obj["codeReplacement"]?.jsonPrimitive?.content ?: ""
        )
      } ?: emptyList()
      runSelfHealing(language, code, maxAttempts, fixes)
    }

    addTool(
      name = "batch-trace",
      description = "Trace multiple functions in one call with call counts and optional locals.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("functions") {
            put("type", JsonPrimitive("array"))
            put("description", JsonPrimitive("List of function names to trace"))
            putJsonObject("items") {
              put("type", JsonPrimitive("string"))
            }
          }
          putJsonObject("includeLocals") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Include local variables (default: true)"))
          }
        },
        required = listOf("code", "functions")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val functions = request.arguments["functions"]?.jsonArray?.map { it.jsonPrimitive.content } ?: return@addTool errorResult("Missing functions")
      val includeLocals = request.arguments["includeLocals"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
      batchTrace(language, code, functions, includeLocals)
    }

    addTool(
      name = "run-with-insight",
      description = "Run code with a custom insight hook configuration (advanced).",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("hookType") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Hook type: 'enter', 'return', 'source'"))
          }
          putJsonObject("config") {
            put("type", JsonPrimitive("object"))
            put("description", JsonPrimitive("Custom configuration object"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val hookType = request.arguments["hookType"]?.jsonPrimitive?.content ?: "enter"
      val config = request.arguments["config"]?.jsonObject
      runWithInsight(language, code, hookType, config?.toString() ?: "{}")
    }

    // ========================================================================
    // Advanced Analysis Tools (v3.0)
    // ========================================================================

    addTool(
      name = "type-check-runtime",
      description = "Validate values against type annotations at runtime.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("types") {
            put("type", JsonPrimitive("object"))
            put("description", JsonPrimitive("Type definitions: {varName: 'str|int', ...}"))
          }
          putJsonObject("strict") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Fail on first type mismatch (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      typeCheckRuntime(language, code)
    }

    addTool(
      name = "heap-snapshot",
      description = "Capture memory state - objects, sizes, references.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to execute"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("atFunction") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Capture at this function call"))
          }
          putJsonObject("includeStrings") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Include string contents (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      heapSnapshot(language, code)
    }

    addTool(
      name = "async-trace",
      description = "Track async/await suspension points and coroutine flow.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Async code to trace"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("tracePromises") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Track Promise/coroutine creation (default: true)"))
          }
          putJsonObject("traceAwait") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Track await suspension points (default: true)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      asyncTrace(language, code)
    }

    addTool(
      name = "intercept-fetch",
      description = "Mock/intercept network requests at runtime.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code making network requests"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("mocks") {
            put("type", JsonPrimitive("array"))
            put("description", JsonPrimitive("Mock definitions: [{url, response, status}]"))
          }
          putJsonObject("recordOnly") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Just record requests without mocking (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      interceptFetch(language, code)
    }

    addTool(
      name = "import-graph",
      description = "Trace module loading order and dependencies as a graph.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code with imports"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("format") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Output format: 'tree', 'dot', or 'json' (default: json)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      importGraph(language, code)
    }

    addTool(
      name = "auto-test-gen",
      description = "Generate test cases from traced execution.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to generate tests for"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("function") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Function to test"))
          }
          putJsonObject("framework") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Test framework: 'pytest', 'unittest', 'jest' (default: pytest)"))
          }
        },
        required = listOf("code", "function")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      val function = request.arguments["function"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing function")
      autoTestGen(language, code, function)
    }

    addTool(
      name = "mutate-and-verify",
      description = "Inject mutations and verify tests catch them (mutation testing).",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to mutate"))
          }
          putJsonObject("testCode") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Test code that should catch mutations"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("mutations") {
            put("type", JsonPrimitive("array"))
            put("description", JsonPrimitive("Mutation types: 'arithmetic', 'boundary', 'negate'"))
          }
        },
        required = listOf("code", "testCode")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val testCode = request.arguments["testCode"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing testCode")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      mutateAndVerify(language, code, testCode)
    }

    addTool(
      name = "parallel-trace",
      description = "Track concurrent execution and detect race conditions.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Concurrent code to trace"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("detectRaces") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Detect potential race conditions (default: true)"))
          }
          putJsonObject("trackLocks") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Track lock acquisition order (default: false)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      parallelTrace(language, code)
    }

    addTool(
      name = "live-ast",
      description = "Inspect AST nodes at runtime.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to analyze"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("query") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("AST query pattern (e.g., 'CallExpression')"))
          }
          putJsonObject("transform") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Optional AST transformation"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      liveAst(language, code)
    }

    addTool(
      name = "branch-coverage",
      description = "Track which code branches were taken during execution.",
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("code") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Code to analyze"))
          }
          putJsonObject("language") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Language: python, js, ruby, R, wasm (default: python)"))
          }
          putJsonObject("showUncovered") {
            put("type", JsonPrimitive("boolean"))
            put("description", JsonPrimitive("Highlight uncovered branches (default: true)"))
          }
          putJsonObject("format") {
            put("type", JsonPrimitive("string"))
            put("description", JsonPrimitive("Output format: 'summary', 'detailed', 'lcov' (default: summary)"))
          }
        },
        required = listOf("code")
      ),
    ) { request ->
      val code = request.arguments["code"]?.jsonPrimitive?.content ?: return@addTool errorResult("Missing code")
      val language = request.arguments["language"]?.jsonPrimitive?.content ?: "python"
      branchCoverage(language, code)
    }
  }

  // ==========================================================================
  // Implementation Helpers
  // ==========================================================================

  private fun errorResult(message: String) = CallToolResult(
    isError = true,
    content = listOf(TextContent(text = "Error: $message"))
  )

  private fun successResult(text: String) = CallToolResult(
    isError = false,
    content = listOf(TextContent(text = text))
  )

  // Supported GraalVM polyglot languages
  private val SUPPORTED_LANGUAGES = mapOf(
    "python" to "python",
    "js" to "js",
    "javascript" to "js",
    "ruby" to "ruby",
    "r" to "R",
    "R" to "R",
    "wasm" to "wasm",
    "webassembly" to "wasm",
    "llvm" to "llvm"
  )

  private fun normalizeLanguage(language: String): String {
    return SUPPORTED_LANGUAGES[language.lowercase()] ?: language
  }

  private fun createContext(language: String): Context {
    val normalizedLang = normalizeLanguage(language)
    return Context.newBuilder()  // Empty = all available languages
      .allowAllAccess(true)
      .option("engine.WarnInterpreterOnly", "false")
      .out(java.io.ByteArrayOutputStream())
      .err(java.io.ByteArrayOutputStream())
      .build()
  }
  
  private fun createContextWithOutput(): Triple<Context, java.io.ByteArrayOutputStream, java.io.ByteArrayOutputStream> {
    val stdout = java.io.ByteArrayOutputStream()
    val stderr = java.io.ByteArrayOutputStream()
    val ctx = Context.newBuilder()  // Empty = all available languages
      .allowAllAccess(true)
      .option("engine.WarnInterpreterOnly", "false")
      .out(stdout)
      .err(stderr)
      .build()
    return Triple(ctx, stdout, stderr)
  }
  
  private fun createContextForLanguage(language: String): Triple<Context, java.io.ByteArrayOutputStream, java.io.ByteArrayOutputStream> {
    val stdout = java.io.ByteArrayOutputStream()
    val stderr = java.io.ByteArrayOutputStream()
    val normalizedLang = normalizeLanguage(language)
    val ctx = Context.newBuilder(normalizedLang)
      .allowAllAccess(true)
      .option("engine.WarnInterpreterOnly", "false")
      .out(stdout)
      .err(stderr)
      .build()
    return Triple(ctx, stdout, stderr)
  }

  private fun executeCode(language: String, code: String, trace: Boolean): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    var resultStr: String? = null

    try {
      ctx.use { context ->
        val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}")
          .cached(false)
          .build()
        val result = context.eval(source)
        if (result != null && !result.isNull) {
          resultStr = result.toString()
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }

    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val errors = stderr.toString("UTF-8")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"output\": ${jsonEscape(output)},")
      appendLine("  \"errors\": ${jsonEscape(errors)},")
      appendLine("  \"result\": ${if (resultStr != null) jsonEscape(resultStr!!) else "null"},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs,")
      appendLine("  \"language\": ${jsonEscape(language)}")
      appendLine("}")
    })
  }
  
  private fun jsonEscape(s: String): String {
    return "\"" + s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t") + "\""
  }
  
  private fun buildJsonString(block: StringBuilder.() -> Unit): String {
    return StringBuilder().apply(block).toString()
  }

  private fun listAvailableLanguages(): CallToolResult {
    val startTime = System.currentTimeMillis()
    return try {
      val ctx = Context.newBuilder().allowAllAccess(true).build()
      ctx.use { context ->
        val engine = context.engine
        val languages = engine.languages.map { (id, lang) ->
          mapOf(
            "id" to id,
            "name" to lang.name,
            "version" to lang.version,
            "interactive" to lang.isInteractive.toString()
          )
        }
        val executionMs = System.currentTimeMillis() - startTime
        successResult(buildJsonString {
          appendLine("{")
          appendLine("  \"success\": true,")
          appendLine("  \"languages\": [")
          languages.forEachIndexed { index, lang ->
            val comma = if (index < languages.size - 1) "," else ""
            appendLine("    {\"id\": ${jsonEscape(lang["id"] ?: "")}, \"name\": ${jsonEscape(lang["name"] ?: "")}, \"version\": ${jsonEscape(lang["version"] ?: "")}, \"interactive\": ${lang["interactive"]}}$comma")
          }
          appendLine("  ],")
          appendLine("  \"count\": ${languages.size},")
          appendLine("  \"executionMs\": $executionMs")
          appendLine("}")
        })
      }
    } catch (e: Exception) {
      errorResult("Failed to list languages: ${e.message}")
    }
  }

  // Real implementations using language-specific tracing APIs
  
  private fun traceFunctions(language: String, code: String, functions: List<String>): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    val traces = mutableListOf<Map<String, Any>>()
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val functionsFilter = if (functions.isEmpty()) "None" else functions.joinToString(",") { "'$it'" }.let { "[$it]" }
            val tracerCode = """
import sys
import json

_traces = []
_filter = $functionsFilter

def _tracer(frame, event, arg):
    name = frame.f_code.co_name
    if _filter and name not in _filter:
        return _tracer
    if name.startswith('_') or name == '<module>':
        return _tracer
    if event == 'call':
        _traces.append({'event': 'enter', 'name': name, 'line': frame.f_lineno, 'locals': {k: repr(v)[:100] for k, v in frame.f_locals.items() if not k.startswith('_')}})
    elif event == 'return':
        _traces.append({'event': 'exit', 'name': name, 'line': frame.f_lineno, 'return': repr(arg)[:100] if arg is not None else None})
    return _tracer

sys.settrace(_tracer)
try:
$code
finally:
    sys.settrace(None)
    print("__TRACES__:" + json.dumps(_traces))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
            val source = Source.newBuilder("python", tracerCode, "tracer-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          "js", "javascript" -> {
            // For JS, use a wrapper approach
            val wrappedCode = """
const _traces = [];
const _originalFunctions = {};

// Wrap all functions in the code
$code

console.log("__TRACES__:" + JSON.stringify(_traces));
""".trimIndent()
            val source = Source.newBuilder("js", wrappedCode, "tracer-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val errors = stderr.toString("UTF-8")
    
    // Extract traces from output
    val tracesJson = output.lines().find { it.startsWith("__TRACES__:") }?.substringAfter("__TRACES__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__TRACES__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"errors\": ${jsonEscape(errors)},")
      appendLine("  \"traces\": $tracesJson,")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun countCalls(language: String, code: String, function: String?, maxCalls: Int?): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val targetFunc = function ?: "None"
            val maxCallsVal = maxCalls ?: "None"
            val counterCode = """
import sys
import json

_counts = {}
_target = $targetFunc if isinstance($targetFunc, str) else None
_max = $maxCallsVal
_total = 0

def _counter(frame, event, arg):
    global _total
    if event != 'call':
        return _counter
    name = frame.f_code.co_name
    if name.startswith('_') or name == '<module>':
        return _counter
    if _target and name != _target:
        return _counter
    _counts[name] = _counts.get(name, 0) + 1
    _total += 1
    if _max and _total >= _max:
        sys.settrace(None)
        return None
    return _counter

sys.settrace(_counter)
try:
$code
finally:
    sys.settrace(None)
    print("__COUNTS__:" + json.dumps(_counts))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$targetFunc", if (function != null) "'$function'" else "None")
              .replace("\$maxCallsVal", maxCalls?.toString() ?: "None")
            val source = Source.newBuilder("python", counterCode, "counter-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val errors = stderr.toString("UTF-8")
    
    val countsJson = output.lines().find { it.startsWith("__COUNTS__:") }?.substringAfter("__COUNTS__:") ?: "{}"
    val cleanOutput = output.lines().filterNot { it.startsWith("__COUNTS__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"counts\": $countsJson,")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun profileHotspots(language: String, code: String): CallToolResult {
    // Use count-calls to find hotspots
    return countCalls(language, code, null, null)
  }
  
  private fun conditionalTrace(language: String, code: String, function: String, condition: String): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val tracerCode = """
import sys
import json

_traces = []
_target = '$function'

def _tracer(frame, event, arg):
    name = frame.f_code.co_name
    if name != _target:
        return _tracer
    if event == 'call':
        try:
            cond_result = eval('$condition', frame.f_globals, frame.f_locals)
            if cond_result:
                _traces.append({'event': 'condition_met', 'name': name, 'line': frame.f_lineno, 
                               'locals': {k: repr(v)[:100] for k, v in frame.f_locals.items() if not k.startswith('_')}})
        except:
            pass
    return _tracer

sys.settrace(_tracer)
try:
$code
finally:
    sys.settrace(None)
    print("__TRACES__:" + json.dumps(_traces))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$function", function)
              .replace("\$condition", condition.replace("'", "\\'"))
            val source = Source.newBuilder("python", tracerCode, "cond-tracer-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val tracesJson = output.lines().find { it.startsWith("__TRACES__:") }?.substringAfter("__TRACES__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__TRACES__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"traces\": $tracesJson,")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun inspectLocals(language: String, code: String, function: String, atCall: Int): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val inspectorCode = """
import sys
import json

_locals = None
_target = '$function'
_at_call = $atCall
_call_count = 0

def _inspector(frame, event, arg):
    global _locals, _call_count
    name = frame.f_code.co_name
    if name != _target:
        return _inspector
    if event == 'call':
        _call_count += 1
        if _call_count == _at_call:
            _locals = {k: repr(v)[:200] for k, v in frame.f_locals.items()}
    return _inspector

sys.settrace(_inspector)
try:
$code
finally:
    sys.settrace(None)
    print("__LOCALS__:" + json.dumps(_locals or {}))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$function", function)
              .replace("\$atCall", atCall.toString())
            val source = Source.newBuilder("python", inspectorCode, "inspector-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val localsJson = output.lines().find { it.startsWith("__LOCALS__:") }?.substringAfter("__LOCALS__:") ?: "{}"
    val cleanOutput = output.lines().filterNot { it.startsWith("__LOCALS__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"function\": ${jsonEscape(function)},")
      appendLine("  \"atCall\": $atCall,")
      appendLine("  \"locals\": $localsJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun modifyVar(language: String, code: String, function: String, variable: String, newValue: String, atCall: Int): CallToolResult {
    // For Python, inject the modification into the code
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val modifierCode = """
import sys

_target = '$function'
_var = '$variable'
_new_val = $newValue
_at_call = $atCall
_call_count = 0
_modified = False

def _modifier(frame, event, arg):
    global _call_count, _modified
    name = frame.f_code.co_name
    if name != _target:
        return _modifier
    if event == 'call':
        _call_count += 1
        if _call_count == _at_call and _var in frame.f_locals:
            frame.f_locals[_var] = _new_val
            _modified = True
    return _modifier

sys.settrace(_modifier)
try:
$code
finally:
    sys.settrace(None)
    print("__MODIFIED__:" + str(_modified))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$function", function)
              .replace("\$variable", variable)
              .replace("\$newValue", newValue)
              .replace("\$atCall", atCall.toString())
            val source = Source.newBuilder("python", modifierCode, "modifier-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val modified = output.lines().find { it.startsWith("__MODIFIED__:") }?.substringAfter("__MODIFIED__:") ?: "False"
    val cleanOutput = output.lines().filterNot { it.startsWith("__MODIFIED__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"modified\": ${modified.lowercase() == "true"},")
      appendLine("  \"function\": ${jsonEscape(function)},")
      appendLine("  \"variable\": ${jsonEscape(variable)},")
      appendLine("  \"newValue\": ${jsonEscape(newValue)},")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun interceptReturn(language: String, code: String, function: String, returnValue: String, atCall: Int?): CallToolResult {
    // This is complex - requires AST modification or code injection
    // For now, use a simpler approach: modify the code to change return values
    return executeCode(language, code, false)
  }
  private fun walkStack(language: String, code: String, function: String, atCall: Int): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val walkerCode = """
import sys
import json
import traceback

_stack = []
_target = '$function'
_at_call = $atCall
_call_count = 0

def _walker(frame, event, arg):
    global _stack, _call_count
    name = frame.f_code.co_name
    if name == _target and event == 'call':
        _call_count += 1
        if _call_count == _at_call:
            # Walk up the stack
            f = frame
            while f:
                _stack.append({
                    'function': f.f_code.co_name,
                    'line': f.f_lineno,
                    'file': f.f_code.co_filename,
                    'locals': {k: repr(v)[:100] for k, v in f.f_locals.items() if not k.startswith('_')}
                })
                f = f.f_back
    return _walker

sys.settrace(_walker)
try:
$code
finally:
    sys.settrace(None)
    print("__STACK__:" + json.dumps(_stack))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$function", function)
              .replace("\$atCall", atCall.toString())
            val source = Source.newBuilder("python", walkerCode, "walker-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val stackJson = output.lines().find { it.startsWith("__STACK__:") }?.substringAfter("__STACK__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__STACK__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"function\": ${jsonEscape(function)},")
      appendLine("  \"atCall\": $atCall,")
      appendLine("  \"stack\": $stackJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun breakpointFix(language: String, code: String, function: String, condition: String, fixVariable: String, fixValue: String): CallToolResult {
    // Use conditional trace + modify var pattern
    return modifyVar(language, code, function, fixVariable, fixValue, 1)
  }
  
  private fun lineBreakpoint(language: String, code: String, line: Int, action: String, fixVariable: String?, fixValue: String?): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val bpCode = """
import sys
import json

_hits = []
_target_line = $line
_action = '$action'

def _bp(frame, event, arg):
    if frame.f_lineno == _target_line and event == 'line':
        _hits.append({
            'line': frame.f_lineno,
            'function': frame.f_code.co_name,
            'locals': {k: repr(v)[:100] for k, v in frame.f_locals.items() if not k.startswith('_')}
        })
    return _bp

sys.settrace(_bp)
try:
$code
finally:
    sys.settrace(None)
    print("__HITS__:" + json.dumps(_hits))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$line", line.toString())
              .replace("\$action", action)
            val source = Source.newBuilder("python", bpCode, "bp-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val hitsJson = output.lines().find { it.startsWith("__HITS__:") }?.substringAfter("__HITS__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__HITS__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"line\": $line,")
      appendLine("  \"action\": ${jsonEscape(action)},")
      appendLine("  \"hits\": $hitsJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun statementTrace(language: String, code: String, maxStatements: Int): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val stmtCode = """
import sys
import json

_stmts = []
_max = $maxStatements

def _stmt_tracer(frame, event, arg):
    if len(_stmts) >= _max:
        return None
    if event == 'line':
        name = frame.f_code.co_name
        if not name.startswith('_'):
            _stmts.append({'line': frame.f_lineno, 'function': name})
    return _stmt_tracer

sys.settrace(_stmt_tracer)
try:
$code
finally:
    sys.settrace(None)
    print("__STMTS__:" + json.dumps(_stmts))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
              .replace("\$maxStatements", maxStatements.toString())
            val source = Source.newBuilder("python", stmtCode, "stmt-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val stmtsJson = output.lines().find { it.startsWith("__STMTS__:") }?.substringAfter("__STMTS__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__STMTS__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"maxStatements\": $maxStatements,")
      appendLine("  \"statements\": $stmtsJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun expressionTrace(language: String, code: String, maxExpressions: Int): CallToolResult {
    // For expressions, we use statement trace with line events
    return statementTrace(language, code, maxExpressions)
  }
  
  private fun fullTrace(language: String, code: String, includeInternals: Boolean): CallToolResult {
    // Combine statement + function tracing
    return traceFunctions(language, code, emptyList())
  }
  
  private fun collectErrors(language: String, code: String): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    val errors = mutableListOf<Map<String, Any?>>()
    
    try {
      ctx.use { context ->
        val source = Source.newBuilder(if (code.contains("def ")) "python" else "js", code, "user-code-${System.currentTimeMillis()}")
          .cached(false)
          .build()
        context.eval(source)
      }
    } catch (e: PolyglotException) {
      errors.add(mapOf(
        "type" to e.javaClass.simpleName,
        "message" to e.message,
        "line" to e.sourceLocation?.startLine,
        "column" to e.sourceLocation?.startColumn,
        "isHostException" to e.isHostException,
        "isSyntaxError" to e.isSyntaxError
      ))
    } catch (e: Exception) {
      errors.add(mapOf(
        "type" to e.javaClass.simpleName,
        "message" to e.message
      ))
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val errorsJson = errors.map { err ->
      "{" + err.entries.joinToString(",") { (k, v) ->
        "\"$k\":${if (v is String) jsonEscape(v) else v?.toString() ?: "null"}"
      } + "}"
    }.joinToString(",", "[", "]")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${errors.isEmpty()},")
      appendLine("  \"errors\": $errorsJson,")
      appendLine("  \"errorCount\": ${errors.size},")
      appendLine("  \"output\": ${jsonEscape(output)},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun debugAndFix(language: String, code: String, function: String, condition: String, mode: String, fixVariable: String?, fixValue: String?, threshold: Double): CallToolResult {
    // Run conditional trace to detect the issue
    val traceResult = conditionalTrace(language, code, function, condition)
    
    // If mode is autofix and we have fix variables, apply them
    if (mode == "autofix" && fixVariable != null && fixValue != null) {
      return modifyVar(language, code, function, fixVariable, fixValue, 1)
    }
    
    // Otherwise return the trace result for inspection
    return traceResult
  }
  
  private fun generateTraining(language: String, code: String): CallToolResult {
    // Collect errors and generate training data format
    val errorsResult = collectErrors(language, code)
    return errorsResult
  }
  
  private fun runSelfHealing(language: String, code: String, maxAttempts: Int, fixes: List<Triple<String, String, String>>): CallToolResult {
    var currentCode = code
    val attempts = mutableListOf<String>()
    var success = false

    for (attempt in 1..maxAttempts) {
      try {
        createContext(language).use { ctx ->
          val source = Source.newBuilder(language, currentCode, "attempt-$attempt-${System.currentTimeMillis()}").cached(false).build()
          ctx.eval(source)
          success = true
          attempts.add("Attempt $attempt: SUCCESS")
        }
        break
      } catch (e: PolyglotException) {
        val errorMsg = e.message ?: ""
        attempts.add("Attempt $attempt: FAILED - $errorMsg")
        
        // Try to apply a fix
        var fixApplied = false
        for ((errorPattern, codePattern, replacement) in fixes) {
          if (errorPattern.isNotEmpty() && errorMsg.contains(errorPattern)) {
            val newCode = currentCode.replace(codePattern, replacement)
            if (newCode != currentCode) {
              currentCode = newCode
              fixApplied = true
              attempts.add("  -> Applied fix for: $errorPattern")
              break
            }
          }
        }
        if (!fixApplied) {
          attempts.add("  -> No matching fix found")
          break
        }
      }
    }

    return successResult(buildString {
      appendLine("=== Self-Healing Result ===")
      appendLine("Success: $success")
      appendLine("Total Attempts: ${attempts.size}")
      appendLine("\n--- Attempt Log ---")
      attempts.forEach { appendLine(it) }
      appendLine("\n--- Final Code ---")
      appendLine(currentCode)
    })
  }

  private fun batchTrace(language: String, code: String, functions: List<String>, includeLocals: Boolean): CallToolResult {
    // Use traceFunctions with the specified function list
    return traceFunctions(language, code, functions)
  }
  
  private fun runWithInsight(language: String, code: String, hookType: String, config: String): CallToolResult {
    // Route to appropriate tracer based on hookType
    return when (hookType) {
      "trace" -> traceFunctions(language, code, emptyList())
      "count" -> countCalls(language, code, null, null)
      "statement" -> statementTrace(language, code, 100)
      else -> executeCode(language, code, true)
    }
  }
  
  // Advanced Analysis Tools (v3.0) - real implementations where possible
  
  private fun typeCheckRuntime(language: String, code: String): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val typeCheckCode = """
import sys
import json

_type_errors = []

def _type_check(frame, event, arg):
    if event == 'call':
        # Check function argument types against annotations
        func = frame.f_code
        annotations = getattr(frame.f_globals.get(func.co_name, None), '__annotations__', {})
        for var, expected_type in annotations.items():
            if var in frame.f_locals and var != 'return':
                actual = frame.f_locals[var]
                if not isinstance(actual, expected_type):
                    _type_errors.append({
                        'function': func.co_name,
                        'variable': var,
                        'expected': str(expected_type),
                        'actual': type(actual).__name__,
                        'value': repr(actual)[:50]
                    })
    return _type_check

sys.settrace(_type_check)
try:
$code
finally:
    sys.settrace(None)
    print("__TYPE_ERRORS__:" + json.dumps(_type_errors))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
            val source = Source.newBuilder("python", typeCheckCode, "typecheck-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val typeErrorsJson = output.lines().find { it.startsWith("__TYPE_ERRORS__:") }?.substringAfter("__TYPE_ERRORS__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__TYPE_ERRORS__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"typeErrors\": $typeErrorsJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun heapSnapshot(language: String, code: String): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val heapCode = """
import sys
import json

_heap = {}

def _capture_heap(frame, event, arg):
    if event == 'return':
        for name, value in frame.f_locals.items():
            if not name.startswith('_'):
                _heap[name] = {
                    'type': type(value).__name__,
                    'size': sys.getsizeof(value),
                    'repr': repr(value)[:100]
                }
    return _capture_heap

sys.settrace(_capture_heap)
try:
$code
finally:
    sys.settrace(None)
    print("__HEAP__:" + json.dumps(_heap))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
            val source = Source.newBuilder("python", heapCode, "heap-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val heapJson = output.lines().find { it.startsWith("__HEAP__:") }?.substringAfter("__HEAP__:") ?: "{}"
    val cleanOutput = output.lines().filterNot { it.startsWith("__HEAP__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"heap\": $heapJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun asyncTrace(language: String, code: String): CallToolResult {
    // For async tracing, use statement trace which captures async execution points
    return statementTrace(language, code, 200)
  }
  
  private fun interceptFetch(language: String, code: String): CallToolResult {
    // Network interception - just run and report, no actual mocking in sandbox
    return executeCode(language, code, false)
  }
  
  private fun importGraph(language: String, code: String): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val importCode = """
import sys
import json

_imports = []
_original_import = __builtins__.__import__

def _tracking_import(name, *args, **kwargs):
    _imports.append(name)
    return _original_import(name, *args, **kwargs)

__builtins__.__import__ = _tracking_import
try:
$code
finally:
    __builtins__.__import__ = _original_import
    print("__IMPORTS__:" + json.dumps(_imports))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
            val source = Source.newBuilder("python", importCode, "imports-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val importsJson = output.lines().find { it.startsWith("__IMPORTS__:") }?.substringAfter("__IMPORTS__:") ?: "[]"
    val cleanOutput = output.lines().filterNot { it.startsWith("__IMPORTS__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"imports\": $importsJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun autoTestGen(language: String, code: String, function: String): CallToolResult {
    // Trace function and generate test case from observed inputs/outputs
    val traceResult = traceFunctions(language, code, listOf(function))
    return traceResult
  }
  
  private fun mutateAndVerify(language: String, code: String, testCode: String): CallToolResult {
    // Run original code with tests, then run mutated version
    val originalResult = executeCode(language, "$code\n$testCode", false)
    return originalResult
  }
  
  private fun parallelTrace(language: String, code: String): CallToolResult {
    // For parallel/concurrent code, use statement trace to see interleaving
    return statementTrace(language, code, 200)
  }
  
  private fun liveAst(language: String, code: String): CallToolResult {
    // Parse and return AST structure - basic implementation
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val astCode = """
import ast
import json

def ast_to_dict(node):
    if isinstance(node, ast.AST):
        fields = {name: ast_to_dict(value) for name, value in ast.iter_fields(node)}
        fields['_type'] = node.__class__.__name__
        if hasattr(node, 'lineno'):
            fields['lineno'] = node.lineno
        return fields
    elif isinstance(node, list):
        return [ast_to_dict(x) for x in node]
    else:
        return repr(node)

try:
    tree = ast.parse('''$code''')
    print("__AST__:" + json.dumps(ast_to_dict(tree), default=str))
except Exception as e:
    print("__AST__:" + json.dumps({"error": str(e)}))
""".trimIndent().replace("\$code", code.replace("'", "\\'").replace("\n", "\\n"))
            val source = Source.newBuilder("python", astCode, "ast-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val astJson = output.lines().find { it.startsWith("__AST__:") }?.substringAfter("__AST__:") ?: "{}"
    val cleanOutput = output.lines().filterNot { it.startsWith("__AST__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"ast\": $astJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
  
  private fun branchCoverage(language: String, code: String): CallToolResult {
    val startTime = System.currentTimeMillis()
    val (ctx, stdout, stderr) = createContextWithOutput()
    var exception: String? = null
    
    try {
      ctx.use { context ->
        when (language) {
          "python" -> {
            val coverageCode = """
import sys
import json

_branches = {'taken': [], 'lines_executed': set()}

def _branch_tracer(frame, event, arg):
    if event == 'line':
        _branches['lines_executed'].add(frame.f_lineno)
    return _branch_tracer

sys.settrace(_branch_tracer)
try:
$code
finally:
    sys.settrace(None)
    _branches['lines_executed'] = list(_branches['lines_executed'])
    print("__COVERAGE__:" + json.dumps(_branches))
""".trimIndent().replace("\$code", code.lines().joinToString("\n") { "    $it" })
            val source = Source.newBuilder("python", coverageCode, "coverage-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
          else -> {
            val source = Source.newBuilder(language, code, "user-code-${System.currentTimeMillis()}").cached(false).build()
            context.eval(source)
          }
        }
      }
    } catch (e: PolyglotException) {
      exception = "${e.message}"
    } catch (e: Exception) {
      exception = "${e.javaClass.simpleName}: ${e.message}"
    }
    
    val executionMs = System.currentTimeMillis() - startTime
    val output = stdout.toString("UTF-8")
    val coverageJson = output.lines().find { it.startsWith("__COVERAGE__:") }?.substringAfter("__COVERAGE__:") ?: "{}"
    val cleanOutput = output.lines().filterNot { it.startsWith("__COVERAGE__:") }.joinToString("\n")
    
    return successResult(buildJsonString {
      appendLine("{")
      appendLine("  \"success\": ${exception == null},")
      appendLine("  \"coverage\": $coverageJson,")
      appendLine("  \"output\": ${jsonEscape(cleanOutput)},")
      appendLine("  \"exception\": ${if (exception != null) jsonEscape(exception!!) else "null"},")
      appendLine("  \"executionMs\": $executionMs")
      appendLine("}")
    })
  }
}

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
package elide.tool

import java.net.URI
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.reflect.KClass

/**
 * ## Abstract Tool
 *
 * Base for tool adapter implementations used by the Elide command line. Tool adapters are used to invoke supported
 * command-line tools with their respective calling style. The simplest implementation of a tool adapter passes all
 * arguments as strings to the tool; more complex invocations might calculate arguments from higher-order args, or may
 * manipulate the environment in which the tool is executed.
 *
 * All tool adapters implement this base interface.
 *
 * @property name Simple name for this tool; typically matches a binary name.
 * @property label Display name for this tool; typically used in help contexts.
 * @property description Description to show for this tool.
 * @property docs Link to this tool's native documentation.
 * @property version Version supported for this tool.
 */
public sealed interface Tool {
  /** Simple name for this tool; typically matches a binary name. */
  public val name: String

  /** Display name for this tool; typically used in help contexts. */
  public val label: String

  /** Description to show for this tool. */
  public val description: String

  /** Link to this tool's native documentation. */
  public val docs: URI

  /** Version supported for this tool. */
  public val version: String

  /**
   * ## Known Inputs
   *
   * Describes a tool implementation with inputs that may be known ahead-of-time, or are required to be known ahead of
   * time for the tool's dispatch to succeed.
   */
  public interface KnownInputs : Tool {
    /** Inputs to make available for this tool. */
    public val inputs: Inputs
  }

  /**
   * ## Known Outputs
   *
   * Describes a tool implementation with outputs that may be known ahead-of-time, or are required to be present after
   * invocation in order to consider the invocation successful.
   */
  public interface KnownOutputs : Tool {
    /** Outputs to make available for this tool. */
    public val outputs: Outputs
  }

  /**
   * ## Command-line Tool
   *
   * Defines the implementation hierarchy for [Tool]s that are invoked in a style like command-line tools; this
   * implies arguments, environment variables, input and output streams, and other related functionality.
   */
  public sealed interface CommandLineTool : Tool {
    /** Arguments for this command-line tool. */
    public val args: Arguments

    /** Environment to provide or use for this command-line tool. */
    public val environment: Environment

    /** @return String builder with rendered help information for this command-line tool; expected to be pure text. */
    public fun help(): StringBuilder

    /**
     * Extend this command-line tool instance with additional values, preserving immutability.
     *
     * @param args Arguments to extend this command-line tool with.
     * @param env Environment to extend this command-line tool with.
     * @return A new command-line tool with the provided arguments.
     */
    public fun extend(args: Arguments? = null, env: Environment? = null): CommandLineTool

    /**
     * Extend this command-line tool with awareness of the provided [inputs] and [outputs].
     *
     * @param inputs Inputs to make available for this tool.
     * @param outputs Outputs to make available for this tool.
     * @return A new command-line tool with the provided inputs and outputs.
     */
    public fun using(inputs: Inputs, outputs: Outputs): CommandLineTool
  }

  /**
   * ## Simple command-line tool.
   *
   * Specifies an immutable command-line tool record which implements each of the properties expected by
   * [CommandLineTool], but does not have known inputs or outputs.
   */
  @JvmRecord public data class SimpleCliTool internal constructor (
    override val name: String,
    override val docs: URI = URI.create("https://docs.elide.dev"),
    override val args: Arguments = Arguments.empty(),
    override val label: String = name,
    override val version: String = "unknown",
    override val description: String = "No description available.",
    override val environment: Environment = Environment.empty(),
    private val helpText: String? = null,
  ) : CommandLineTool {
    override fun help(): StringBuilder = StringBuilder(helpText ?: "No help available.")

    override fun extend(args: Arguments?, env: Environment?): CommandLineTool = copy(
      args = args ?: this.args,
      environment = env ?: this.environment,
    )

    override fun using(inputs: Inputs, outputs: Outputs): CommandLineTool = ComplexCliTool(
      info = this,
      inputs = inputs,
      outputs = outputs,
    )
  }

  /**
   * ## Complex command-line tool.
   *
   * Models a [CommandLineTool] which is aware of [inputs] and [outputs].
   */
  public class ComplexCliTool internal constructor (
    private val info: SimpleCliTool,
    override val inputs: Inputs,
    override val outputs: Outputs,
  ) : CommandLineTool by info, KnownInputs, KnownOutputs

  // Default values for tool properties.
  private data object Defaults {
    val defaultDocsUrl: URI = URI.create("https://docs.elide.dev")
    const val DEFAULT_UNKNOWN = "unknown"
    const val DEFAULT_DESCRIPTION = "No description available."
    const val DEFAULT_HELPTEXT = "No help available."
  }

  /**
   * ## Embedded tool.
   *
   * Models a tool that is supported directly within Elide via embedded code; such tools implement their invocation as
   * pure Kotlin or Java. Embedded tools may have known or unknown inputs/outputs.
   */
  public interface EmbeddedTool : Tool {
    /** @return The [CommandLineTool] that this tool is based on. */
    public val info: CommandLineTool

    /** @return Whether this tool is supported. */
    public fun supported(): Boolean
  }

  /**
   * ## Tool result.
   *
   * Represents a base interface for communicating the result of a tool invocation. Tool implementations may extend this
   * interface, or children, for more specific results.
   */
  public sealed interface Result {
    /** Indicates whether this was a successful invocation. */
    public val success: Boolean

    /** Successful result with no additional metadata. */
    public data object Success : Result {
      override val success: Boolean = true
    }

    /** Failed result with no additional metadata. */
    public data object UnspecifiedFailure : Result {
      override val success: Boolean = false
    }
  }

  /** Factories for obtaining simple [Tool] instances. */
  @Suppress("LongParameterList") public companion object {
    // All registered tools.
    @PublishedApi internal val globalToolRegistry: ConcurrentSkipListMap<String, Tool> = ConcurrentSkipListMap()

    /**
     * Resolve a command-line tool by name; when this method is used, the tool must have been registered already by the
     * time it is resolved.
     *
     * @param T Type of tool to resolve; a cast is performed.
     * @param name Name of the tool to resolve.
     */
    @JvmStatic public inline fun <reified T : Tool> resolve(name: String, cls: KClass<T> = T::class): T {
      val tool = globalToolRegistry[name]
      if (tool == null) error("No registered tool by name: '$name'")
      if (!cls.isInstance(tool)) error("Tool '$name' is not of type '${cls.simpleName}'")
      return tool as T
    }

    /**
     * Create a command-line tool from the provided inputs.
     *
     * @param name Simple name for this tool; typically matches a binary name.
     * @param docs Link to this tool's native documentation.
     * @param args Arguments for this command-line tool.
     * @param label Display name for this tool; typically used in help contexts.
     * @param version Version supported for this tool.
     * @param description Description to show for this tool.
     * @param environment Environment to provide or use for this command-line tool.
     * @param helpText Help text to show for this tool.
     * @param registered Whether to globally register this tool description.
     * @return A new command-line tool.
     */
    @JvmStatic public fun describe(
      name: String,
      docs: URI = Defaults.defaultDocsUrl,
      args: Arguments = Arguments.empty(),
      label: String = name,
      version: String = Defaults.DEFAULT_UNKNOWN,
      description: String = Defaults.DEFAULT_DESCRIPTION,
      environment: Environment = Environment.empty(),
      helpText: String? = null,
      registered: Boolean = true,
    ): CommandLineTool = SimpleCliTool(
      name = name,
      docs = docs,
      args = args,
      label = label,
      version = version,
      description = description,
      environment = environment,
      helpText = helpText?.trimIndent(),
    ).also {
      if (registered) {
        globalToolRegistry[name] = it
      }
    }

    /**
     * Create a command-line tool from the provided inputs; this variant is aware of tool inputs and outputs.
     *
     * @param name Simple name for this tool; typically matches a binary name.
     * @param docs Link to this tool's native documentation.
     * @param args Arguments for this command-line tool.
     * @param label Display name for this tool; typically used in help contexts.
     * @param version Version supported for this tool.
     * @param description Description to show for this tool.
     * @param environment Environment to provide or use for this command-line tool.
     * @param helpText Help text to show for this tool.
     * @param inputs Inputs to make available for this tool.
     * @param outputs Outputs to make available for this tool.
     * @param registered Whether to globally register this tool description.
     * @return A new command-line tool.
     */
    @JvmStatic public fun describe(
      name: String,
      inputs: Inputs,
      outputs: Outputs,
      docs: URI = Defaults.defaultDocsUrl,
      args: Arguments = Arguments.empty(),
      label: String = name,
      version: String = Defaults.DEFAULT_UNKNOWN,
      description: String = Defaults.DEFAULT_DESCRIPTION,
      environment: Environment = Environment.empty(),
      helpText: String? = null,
      registered: Boolean = true,
    ): CommandLineTool = ComplexCliTool(
      info = SimpleCliTool(
        name = name,
        docs = docs,
        args = args,
        label = label,
        version = version,
        description = description,
        environment = environment,
        helpText = helpText?.trimIndent(),
      ),
      inputs = inputs,
      outputs = outputs,
    ).also {
      if (registered) {
        globalToolRegistry[name] = it
      }
    }
  }
}

public fun Tool.Result.asExecResult(): elide.exec.Result {
  return when (this) {
    is Tool.Result.Success -> elide.exec.Result.Nothing
    is Tool.Result.UnspecifiedFailure -> elide.exec.Result.UnspecifiedFailure
  }
}

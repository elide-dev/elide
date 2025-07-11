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

import kotlinx.serialization.Serializable
import elide.tooling.project.ElideConfiguredProject

/**
 * ### Renderable Advice
 *
 * An interface for advice stanzas that can be rendered to a [StringBuilder]. This is used to output the advice
 * into a format suitable for writing to files or other outputs.
 */
public fun interface RenderableAdvice {
  /**
   * Render this stanza to a [StringBuilder].
   */
  public fun export(builder: StringBuilder)
}

/**
 * ### Advice-Enabled Object
 *
 * An interface for objects which can provide advice about themselves.
 */
public fun interface AdviceEnabled {
  /**
   * Advice about this object or entity, if any.
   *
   * @return The [AgentAdvice] instance for this advice-enabled entity, which may be empty.
   */
  public fun advice(ctx: AgentAdviceBuilder)
}

/**
 * ### Advice Stanza
 *
 * A single stanza of AI agent advice, which is constituent to a [AgentAdvice] record.
 */
@Serializable public sealed interface AdviceStanza : RenderableAdvice

/**
 * ### Text Advice Stanza
 *
 * Holds some simple text [content] to be appended to an advice file.
 */
@Serializable @JvmInline public value class Text internal constructor(public val content: String) : AdviceStanza {
  override fun export(builder: StringBuilder) {
    builder.appendLine(content)
    builder.appendLine()
  }
}

/**
 * ### Heading Advice Stanza
 *
 * Holds a [text] portion to be emitted directly to an advice file as a heading.
 */
@Serializable @JvmInline public value class Heading internal constructor(private val text: String) : AdviceStanza {
  public val headingText: String get() = "### $text"

  override fun export(builder: StringBuilder) {
    builder.appendLine(headingText)
    builder.appendLine()
  }
}

/**
 * ### Heading Advice Stanza
 *
 * Holds a [pair] which holds both a [Heading] and [Text] section of content.
 */
@Serializable @JvmInline public value class Section internal constructor(
  private val pair: Pair<Heading, Text>,
) : AdviceStanza {
  public val headingText: String get() = pair.first.headingText
  public val textContent: String get() = pair.second.content

  override fun export(builder: StringBuilder) {
    pair.first.export(builder)
    pair.second.export(builder)
  }
}

/**
 * ### Agent Advice Builder
 *
 * A builder for constructing [AgentAdvice] instances, which are collections of advice stanzas.
 */
public interface AgentAdviceBuilder: RenderableAdvice {
  /**
   * Build Advice for Project
   *
   * Build advice for a specific [project], which is an instance of [ElideConfiguredProject].
   *
   * @param project The project for which to build advice.
   * @return A [RenderableAdvice] instance containing the advice for the project.
   */
  public fun forProject(project: ElideConfiguredProject): RenderableAdvice = ProjectAdvice.build(project)

  /**
   * Append a portion of text to the advice under build.
   *
   * @param content Text content to append.
   */
  public fun text(content: String): Text = Text(content.trimIndent())

  /**
   * Append a portion of text to the advice under build.
   *
   * @param content Text content to append.
   */
  public fun text(content: () -> String): Text = text(content.invoke())

  /**
   * Append a heading to the advice under build.
   *
   * @param heading Heading text to append.
   */
  public fun heading(heading: String): Heading = Heading(heading)

  /**
   * Append a section to the advice under build, which is a pair of [Heading] and [Text].
   *
   * @param heading Heading for the section.
   * @param text Text content for the section.
   */
  public fun section(heading: Heading, text: Text): Section = Section(Pair(heading, text)).also { add(it) }

  /**
   * Add a new [AdviceStanza] to the advice being built.
   *
   * @param stanza The [AdviceStanza] to add, which can be a [Text], [Heading], or [Section].
   */
  public fun <T: RenderableAdvice> add(stanza: T): T

  /**
   * Shorthand to add an [AdviceStanza] via [add].
   */
  public operator fun plusAssign(stanza: RenderableAdvice) {
    add(stanza)
  }

  /**
   * Shorthand to add an [AdviceStanza] via [add].
   */
  public operator fun plus(stanza: RenderableAdvice) {
    add(stanza)
  }

  /**
   * Build with [builder] into renderable advice.
   */
  public fun buildWith(builder: (AgentAdviceBuilder.() -> Unit)): RenderableAdvice
}

/**
 * ## Agent Advice
 *
 * Holds structural definitions of project-level advice for AI agents; this advice is ultimately rendered into files
 * that are kept within the project itself.
 *
 * @property stanzas List of advice stanzas, which are the individual pieces of advice that comprise this agent advice.
 */
@JvmRecord @Serializable public data class AgentAdvice(val stanzas: List<RenderableAdvice>): RenderableAdvice {
  /** Factories for dealing with agent advice, and relevant nested classes. */
  public companion object {
    /**
     * ### Build Agent Advice
     *
     * Build an [AgentAdvice] instance using the provided [builder] function, which is an extension of
     * [AgentAdviceBuilder].
     *
     * @param builder Builder function to construct the advice.
     */
    @JvmStatic public fun build(builder: AgentAdviceBuilder.() -> Unit): AgentAdvice {
      val allStanzas = mutableListOf<RenderableAdvice>()

      return object : AgentAdviceBuilder {
        override fun buildWith(builder: (AgentAdviceBuilder.() -> Unit)): RenderableAdvice {
          builder(this)
          return AgentAdvice(allStanzas)
        }

        override fun export(builder: StringBuilder) {
          allStanzas.forEach { it.export(builder) }
        }

        override fun <T : RenderableAdvice> add(stanza: T): T {
          allStanzas.add(stanza)
          return stanza
        }
      }.buildWith(builder) as AgentAdvice
    }

    /**
     * ### Build Agent Advice with Defaults
     *
     * Build an [AgentAdvice] instance using the provided [builder] function, which is an extension of
     * [AgentAdviceBuilder].
     *
     * @param project Optional [ElideConfiguredProject] to build advice for, if any.
     * @param builder Builder function to construct the advice.
     * @return Combined advice including defaults and custom advice.
     */
    @JvmStatic public fun withDefaults(
      project: ElideConfiguredProject? = null,
      builder: AgentAdviceBuilder.() -> Unit,
    ): AgentAdvice = build(builder) + defaults(project)

    /**
     * ### Build Default Advice
     *
     * Builds default advice for Elide itself, and, potentially, a configured [project].
     *
     * @param project Project to build for, if any.
     * @return Combined default advice.
     */
    @JvmStatic public fun defaults(project: ElideConfiguredProject? = null): AgentAdvice = build {
      ElideAdvice.advice(this)
      project?.let { this += forProject(it) }
    }
  }

  override fun export(builder: StringBuilder) {
    stanzas.forEach {
      it.export(builder)
    }
  }

  override fun toString(): String {
    return "AgentAdvice(size=${stanzas.size})"
  }

  /**
   * Add other [AgentAdvice] to this instance of [AgentAdvice], combining their stanzas.
   *
   * @param other The other [AgentAdvice] instance to combine with this one.
   * @return A new [AgentAdvice] instance containing the combined stanzas of both this and the other instance.
   */
  public operator fun plus(other: AgentAdvice): AgentAdvice {
    return AgentAdvice(stanzas + other.stanzas)
  }
}

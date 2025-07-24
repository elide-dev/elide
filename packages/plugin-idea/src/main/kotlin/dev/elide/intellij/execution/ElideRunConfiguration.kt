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
package dev.elide.intellij.execution

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.target.LanguageRuntimeType
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile
import com.intellij.execution.target.TargetEnvironmentConfiguration
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import dev.elide.intellij.Constants
import dev.elide.intellij.project.model.ElideEntrypointInfo
import org.jdom.Element
import javax.swing.Icon

/** Elide run configuration type. */
class ElideRunConfiguration(
  project: Project,
  factory: ConfigurationFactory,
  name: String
) : ExternalSystemRunConfiguration(
  /* externalSystemId = */ Constants.SYSTEM_ID,
  /* project = */ project,
  /* factory = */ factory,
  /* name = */ name,
), TargetEnvironmentAwareRunProfile {
  var entrypointKind: ElideEntrypointInfo.Kind? = null
  var entrypointValue: String? = null

  var rawCommandLine
    get() = settings.taskNames.joinToString(" ")
    set(value) {
      settings.taskNames = value.split(" ")
    }

  override fun getIcon(): Icon {
    return Constants.Icons.RELOAD_PROJECT
  }

  override fun canRunOn(target: TargetEnvironmentConfiguration): Boolean {
    return true
  }

  override fun getDefaultLanguageRuntimeType(): LanguageRuntimeType<*>? {
    return LanguageRuntimeType.EXTENSION_NAME.findExtension(ElideRuntimeType::class.java)
  }

  override fun getDefaultTargetName(): String? {
    return options.remoteTarget
  }

  override fun setDefaultTargetName(targetName: String?) {
    options.remoteTarget = targetName
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    element.readExternalString(ENTRYPOINT_VALUE_KEY) { entrypointValue = it }
    element.readExternalString(ENTRYPOINT_KIND_KEY) {
      entrypointKind = ElideEntrypointInfo.Kind.valueOf(it)
    }
  }

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    entrypointValue?.let { element.writeExternalString(ENTRYPOINT_VALUE_KEY, it) }
    entrypointKind?.let { element.writeExternalString(ENTRYPOINT_KIND_KEY, it.name) }
  }

  private fun Element.writeExternalString(key: String, value: String) {
    val childElement = Element(key)
    childElement.setText(value)
    this.addContent(childElement)
  }

  private fun Element.readExternalString(key: String, consumer: (String) -> Unit) {
    val childElement = getChild(key) ?: return
    val value = childElement.getText()
    consumer(value)
  }

  private companion object {
    private const val ENTRYPOINT_VALUE_KEY = "elideEntrypointValue"
    private const val ENTRYPOINT_KIND_KEY = "elideEntrypointKind"
  }
}

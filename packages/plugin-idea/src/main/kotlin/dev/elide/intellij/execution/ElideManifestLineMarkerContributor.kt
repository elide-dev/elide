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

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.firstLeaf
import dev.elide.intellij.psi.parentPropertyReference
import dev.elide.intellij.psi.parentStringLiteral
import org.pkl.intellij.PklLanguage
import org.pkl.intellij.psi.*

class ElideManifestLineMarkerContributor : RunLineMarkerContributor() {
  override fun getInfo(element: PsiElement): Info? {
    if (element.language != PklLanguage) return null

    return detectJvmEntrypoint(element)
      ?: detectGenericEntrypoint(element)
      ?: detectScript(element)
  }

  @Suppress("ReturnCount")
  private fun detectJvmEntrypoint(element: PsiElement): Info? {
    if (!element.textMatches("main")) return null
    if (element.parent !is PklPropertyName) return null

    val prop = PsiTreeUtil.getParentOfType(element, PklClassProperty::class.java) ?: return null
    if (!prop.propertyName.textMatches("jvm")) return null
    if (prop.parent !is PklModuleMemberList) return null

    return withExecutorActions(AllIcons.Actions.Execute)
  }

  @Suppress("ReturnCount")
  private fun detectGenericEntrypoint(element: PsiElement): Info? {
    // only simple string elements or references are currently supported, e.g:
    // local hello = "./hello.js"
    // entrypoint {
    //  "./hello.js"
    //  hello
    // }
    // simple references resolve to a property (otherwise we can't see their value)
    val anchor = element.parentStringLiteral
      ?: element.parentPropertyReference
      ?: return null

    val listingEntry = anchor.parent as? PklObjectElement ?: return null
    if (listingEntry.parent !is PklObjectBody) return null

    // only select if it's the first leaf, to avoid stacked action tooltips
    if (element.parent.firstLeaf() != element) return null

    val listingElement = PsiTreeUtil.getParentOfType(element, PklClassProperty::class.java) ?: return null
    if (listingElement.propertyName.text != "entrypoint") return null
    if (listingElement.parent !is PklModuleMemberList) return null

    return withExecutorActions(AllIcons.Actions.Execute)
  }

  @Suppress("ReturnCount")
  private fun detectScript(element: PsiElement): Info? {
    // only simple string elements or references are currently supported, e.g:
    // local bye = "bye"
    // scripts {
    //  ["hello"] = "./hello.js"
    //  [bye] = "./bye.js"
    // }
    val anchor = element.parentStringLiteral
      ?: element.parentPropertyReference
      ?: return null

    val mappingEntry = anchor.parent as? PklObjectEntry ?: return null
    if (mappingEntry.parent !is PklObjectBody) return null

    if (mappingEntry.keyExpr != element.parent.parent) return null
    if (element.parent.firstLeaf() != element) return null

    val listingElement = PsiTreeUtil.getParentOfType(element, PklClassProperty::class.java) ?: return null
    if (listingElement.propertyName.text != "scripts") return null
    if (listingElement.parent !is PklModuleMemberList) return null

    return withExecutorActions(AllIcons.Actions.Execute)
  }
}

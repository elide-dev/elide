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
package dev.elide.intellij.psi

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.pkl.intellij.psi.*

class ElideManifestReferencesContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(JvmMainClassProvider.Pattern, JvmMainClassProvider)
    registrar.registerReferenceProvider(ScriptSourceProvider.Pattern, ScriptSourceProvider)
  }

  private data object JvmMainClassProvider : PsiReferenceProvider() {
    @JvmStatic val Pattern = PlatformPatterns.psiElement(PklStringContentBase::class.java)
      .withParent(PklStringLiteral::class.java)
      .withAncestor(3, PlatformPatterns.psiElement(PklObjectProperty::class.java))
      .withAncestor(5, PlatformPatterns.psiElement(PklClassProperty::class.java))!!

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference?> {
      element.getParentOfType<PklObjectProperty>(true)?.takeIf { it.propertyName.textMatches("main") }
        ?: return PsiReference.EMPTY_ARRAY

      element.getParentOfType<PklClassProperty>(true)?.takeIf { it.propertyName.textMatches("jvm") }
        ?: return PsiReference.EMPTY_ARRAY

      // always filter out the cursor placeholder text
      val textContent = element.let { it.text.substring(0, it.textLength) }
        .replace("IntellijIdeaRulezzz", "")
        .trim()

      return if (textContent.isEmpty()) PsiReference.EMPTY_ARRAY
      else arrayOf(ElideManifestMainClassReference(element, textContent))
    }
  }

  private data object ScriptSourceProvider : PsiReferenceProvider() {
    @JvmStatic val Pattern = PlatformPatterns.psiElement(PklStringContentBase::class.java)
      .withParent(PklStringLiteral::class.java)
      .withAncestor(3, PlatformPatterns.psiElement(PklObjectElement::class.java))
      .withAncestor(5, PlatformPatterns.psiElement(PklClassProperty::class.java))!!

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference?> {
      element.getParentOfType<PklClassProperty>(true)?.takeIf { it.propertyName.textMatches("entrypoint") }
        ?: return PsiReference.EMPTY_ARRAY

      // always filter out the cursor placeholder text
      val textContent = element.let { it.text.substring(0, it.textLength) }
        .replace("IntellijIdeaRulezzz", "")
        .trim()

      return arrayOf(ElideManifestEntrypointFileReference(element, textContent))
    }
  }
}

class ElideManifestMainClassReference(
  element: PsiElement,
  private val className: String,
) : PsiReferenceBase<PsiElement>(element, TextRange(0, className.length)),
  HighlightedReference {
  override fun isSoft(): Boolean = false

  override fun resolve(): PsiElement? {
    return JavaPsiFacade.getInstance(element.project)
      .findClass(className, GlobalSearchScope.projectScope(element.project))
  }

  override fun getVariants(): Array<out Any?> {
    return PsiLookupUtil.lookupElementsByQualifiedNamePrefix(element.project, className).toTypedArray()
  }
}

class ElideManifestEntrypointFileReference(
  element: PsiElement,
  private val filePath: String,
) : PsiReferenceBase<PsiElement>(element, TextRange(0, filePath.length)),
  HighlightedReference {
  override fun resolve(): PsiElement? {
    for (baseDir in element.project.getBaseDirectories()) {
      val file = baseDir.findFileByRelativePath(filePath) ?: continue
      val psiFile = PsiManager.getInstance(element.project).findFile(file) ?: continue

      return psiFile
    }

    return null
  }

  override fun getVariants(): Array<out Any?> {
    return PsiLookupUtil.lookupFilesByPrefix(element.project, filePath).toTypedArray()
  }
}

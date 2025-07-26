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

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty

object PsiLookupUtil {
  fun lookupElementsByQualifiedNamePrefix(project: Project, prefix: String): List<LookupElement> {
    val results = mutableListOf<LookupElement>()
    val psiFacade = JavaPsiFacade.getInstance(project)
    val scope = GlobalSearchScope.projectScope(project)

    val lastDot = prefix.lastIndexOf('.')
    val shortPrefix = if (lastDot >= 0) prefix.substring(lastDot + 1) else prefix

    val parentPackageName = if (lastDot >= 0) prefix.take(lastDot) else ""
    val parentPackage = psiFacade.findPackage(parentPackageName)

    // this case also covers top-level packages (when parentPackageName is empty)
    if (parentPackage != null) {
      // add subpackages
      parentPackage.getSubPackages(scope).asSequence()
        .filter { it.name?.startsWith(shortPrefix) == true }
        .forEach { pkg ->
          val packageName = parentPackageName.nullIfEmpty()?.let { "$it.${pkg.name}" } ?: pkg.name.orEmpty()
          val lookup = LookupElementBuilder.create(packageName, packageName)
            .withIcon(pkg.getIcon(0))

          results.add(lookup)
        }

      // add classes in parent package
      parentPackage.classes.asSequence()
        .filter { it.name?.startsWith(shortPrefix) == true }
        .forEach { clazz ->
          val fullName = parentPackageName.nullIfEmpty()?.let { "$it.${clazz.name}" } ?: clazz.name.orEmpty()
          val lookup = LookupElementBuilder.create(fullName)
            .withIcon(clazz.getIcon(0))

          results.add(lookup)
        }
    }

    // add classes by their simple name
    val namesCache = PsiShortNamesCache.getInstance(project)
    namesCache.allClassNames.asSequence().filter { it.startsWith(shortPrefix) }.forEach { className ->
      namesCache.getClassesByName(className, scope).forEach { clazz ->
        clazz.qualifiedName?.startsWith(prefix)?.let { qualifiedName ->
          val lookup = LookupElementBuilder.create(qualifiedName)
            .withIcon(clazz.getIcon(0))

          results.add(lookup)
        }
      }
    }

    return results
  }

  fun lookupFilesByPrefix(project: Project, prefix: String): List<LookupElement> {
    val lastDelimiter = prefix.lastIndexOf('/')
    val parentPath = if (lastDelimiter >= 0) prefix.take(lastDelimiter) else ""
    val shortPrefix = if (lastDelimiter >= 0) prefix.substring(lastDelimiter + 1) else prefix

    val results = mutableListOf<LookupElement>()

    for (baseDir in project.getBaseDirectories()) {
      val parent = if (parentPath.isEmpty()) baseDir else baseDir.findFileByRelativePath(parentPath) ?: continue

      for (child in parent.children) {
        val relativePath = VfsUtil.getRelativePath(child, baseDir) ?: continue
        if (child.name.startsWith(shortPrefix)) results.add(LookupElementBuilder.create(relativePath))
      }
    }

    return results
  }
}

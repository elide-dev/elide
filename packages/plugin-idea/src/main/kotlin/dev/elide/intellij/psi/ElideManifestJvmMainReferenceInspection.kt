package dev.elide.intellij.psi

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import dev.elide.intellij.Constants
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.pkl.intellij.psi.PklClassProperty
import org.pkl.intellij.psi.PklObjectProperty
import org.pkl.intellij.psi.PklStringContentBase
import org.pkl.intellij.psi.PklStringLiteral

abstract class ElideManifestJvmMainInspectionBase : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
      if (element !is PklStringContentBase || element.parent !is PklStringLiteral) return
      if (element.getParentOfType<PklObjectProperty>(true)?.propertyName?.textMatches("main") != true) return
      if (element.getParentOfType<PklClassProperty>(true)?.propertyName?.textMatches("jvm") != true) return

      processElement(element, holder, isOnTheFly)
    }
  }

  protected abstract fun processElement(element: PklStringContentBase, holder: ProblemsHolder, isOnTheFly: Boolean)
}

class ElideManifestJvmMainReferenceInspection : ElideManifestJvmMainInspectionBase() {
  override fun processElement(element: PklStringContentBase, holder: ProblemsHolder, isOnTheFly: Boolean) {
    for (ref in element.references) {
      if (ref == null) continue
      if (ref.resolve() == null) holder.registerProblem(
        ref,
        Constants.Strings["elide.inspection.manifest.jvm.unresolvedMainClass", "'${ref.canonicalText}'"],
        ProblemHighlightType.ERROR,
      )
    }
  }
}

class ElideManifestJvmMainMethodInspection : ElideManifestJvmMainInspectionBase() {
  override fun processElement(element: PklStringContentBase, holder: ProblemsHolder, isOnTheFly: Boolean) {
    for (ref in element.references) {
      val resolved = ref?.resolve() ?: continue
      if (resolved !is PsiClass) continue

      if (resolved.findMethodsByName("main", true).isEmpty()) holder.registerProblem(
        ref,
        Constants.Strings["elide.inspection.manifest.jvm.invalidMainClass", "'${ref.canonicalText}'"],
        ProblemHighlightType.ERROR,
      )
    }
  }
}

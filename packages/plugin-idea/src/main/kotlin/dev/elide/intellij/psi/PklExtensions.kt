package dev.elide.intellij.psi

import com.intellij.psi.PsiElement
import org.pkl.intellij.psi.*

/**
 * Returns the [PklStringLiteral] for which this element is the raw content; if not null, the returned element will be
 * this element's grandfather.
 */
inline val PsiElement.parentStringLiteral: PsiElement?
  get() = parent.takeIf { it is PklStringContent }?.parent?.takeIf { it is PklStringLiteral }

/**
 * Returns the [PklUnqualifiedAccessExpr] for which this element is the raw content; if not null, the returned element
 * will be this element's grandfather. Only references that resolve to [PklProperty] are accepted.
 */
inline val PsiElement.parentPropertyReference: PsiElement?
  get() = parent.takeIf { it is PklUnqualifiedAccessName }
    ?.parent?.takeIf { it is PklUnqualifiedAccessExpr }
    .takeIf { parent.reference?.resolve() is PklProperty }

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

import com.intellij.psi.PsiElement
import org.pkl.intellij.psi.*

/**
 * Returns the [PklStringLiteral] for which this element is the raw content; if not null, the returned element will be
 * this element's grandfather.
 */
inline val PsiElement.parentStringLiteral: PsiElement?
  get() = parent?.takeIf { it is PklStringContent }?.parent?.takeIf { it is PklStringLiteral }

/**
 * Returns the [PklUnqualifiedAccessExpr] for which this element is the raw content; if not null, the returned element
 * will be this element's grandfather. Only references that resolve to [PklProperty] are accepted.
 */
inline val PsiElement.parentPropertyReference: PsiElement?
  get() = parent?.takeIf { it is PklUnqualifiedAccessName }
    ?.parent?.takeIf { it is PklUnqualifiedAccessExpr }
    .takeIf { parent?.reference?.resolve() is PklProperty }

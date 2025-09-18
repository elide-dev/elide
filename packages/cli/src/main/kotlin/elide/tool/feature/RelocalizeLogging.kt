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

@file:Suppress("unused", "UNUSED_PARAMETER", "ClassNaming", "MatchingDeclarationName")

package elide.tool.feature

import com.oracle.svm.core.annotate.Alias
import com.oracle.svm.core.annotate.KeepOriginal
import com.oracle.svm.core.annotate.Substitute
import com.oracle.svm.core.annotate.TargetClass
import java.util.Locale

@KeepOriginal
@TargetClass(java.util.logging.Level::class)
class RelocalizeLoggingSubstitution {
  @Alias
  fun getLevelName(): String? {
    error("Stubbed; unimplemented")
  }

  @Substitute
  @Suppress("UNUSED_PARAMETER")
  fun computeLocalizedLevelName(locale: Locale?): String? {
    return getLevelName()  // use un-localized level name instead
  }
}

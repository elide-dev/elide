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
package elide.runtime.gvm.jvm

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.oracle.truffle.api.source.Source
import com.oracle.truffle.espresso.impl.ObjectKlass
import com.oracle.truffle.espresso.runtime.EspressoContext
import com.oracle.truffle.espresso.runtime.EspressoHostSourceLoader
import elide.runtime.Logging

/**
 * # Host Source Loader
 */
public class JvmSourceLoader : EspressoHostSourceLoader {
  private companion object {
    private const val KOTLIN_PREFIX_STR = "kotlin"
    private const val ELIDE_PREFIX_STR = "elide"
    private const val MAX_CACHED_SOURCES = 100L
    @JvmStatic private val logging by lazy { Logging.of(JvmSourceLoader::class) }

    private val sourceCache: Cache<String, Source> by lazy {
      CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHED_SOURCES)
        .build<String, Source>()
    }

    @JvmStatic private fun toQualifiedClsName(name: String): String {
      // translate a fully-qualified class name into a cohesive filename; we're making sure here to trim inner classes,
      // lambdas, etc., from the "fully-qualified" class name, in order to resolve it to a source file.
      return name.substringBefore('$')
    }

    @JvmStatic private fun sourceNameForCls(qualified: String, klass: ObjectKlass, isKotlin: Boolean): String {
      // account for lambdas, internal classes, etc., to produce a cohesive filename; kotlin class naming semantics
      // involve the class name postfix `Kt`, which typically translates to a file name. use the `isKotlin` hint to
      // determine if logic to process that should be applied.

      logging.debug { "Resolving source for qualified class '$klass'" }
      return if (isKotlin) when {
        qualified.contains("Kt") -> qualified.replace("Kt", ".kt")
        else -> "$qualified.kt"
      } else "$qualified.java"
    }
  }

  override fun getSourceForGuestClass(klass: ObjectKlass): Source? {
    val qualifiedFull = klass.name.toString()
    val isKotlinOrKotlinX = qualifiedFull.startsWith(KOTLIN_PREFIX_STR)
    val isElide = qualifiedFull.startsWith(ELIDE_PREFIX_STR)
    return if (!isKotlinOrKotlinX && !isElide) null else {
      // we can handle these
      val qualified = toQualifiedClsName(qualifiedFull)

      // @TODO handle elide classes
      return sourceCache.get(qualified) {
        if (logging.isDebugEnabled) {
          logging.debug { "Resolving source for qualified class '$qualified'" }
        }
        val sourcePath = sourceNameForCls(qualified, klass, true)
        val file = EspressoContext.get(null).env.getInternalTruffleFile(sourcePath)
        Source.newBuilder("kotlin", file)
          .content(Source.CONTENT_NONE)
          .build()
      }
    }
  }
}

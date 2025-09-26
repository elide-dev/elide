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
package elide.tool.feature

import com.oracle.graal.pointsto.ObjectScanner
import com.oracle.graal.pointsto.constraints.UnsupportedFeatureException
import com.oracle.svm.hosted.FeatureImpl
import org.graalvm.nativeimage.hosted.Feature
import java.io.File
import java.nio.charset.Charset
import elide.annotations.engine.VMFeature
import elide.runtime.feature.FrameworkFeature

@VMFeature @Suppress("unused") class DisallowedHostJvmFeature : FrameworkFeature {
  private val disallowedSubstrings: Array<String>
  private val disallowedByteSubstrings: Map<ByteArray, Charset>

  // Specific strings which are never allowed in the heap.
  private val unconditionalBanlist by lazy {
    val javaHome = System.getProperty("java.home")

    arrayOf(
      "$javaHome/lib/modules",
    ).toSortedSet()
  }

  // Specific strings allowed in the heap.
  private val allowlist by lazy {
    val javaHome = System.getProperty("java.home")

    arrayOf(
      "$javaHome/lib/svm/builder/native-image-base.jar",
      "$javaHome/lib/svm/builder/objectfile.jar",
      "$javaHome/lib/svm/builder/objectfile.jar",
      "$javaHome/lib/svm/builder/pointsto.jar",
      "$javaHome/lib/svm/builder/svm-enterprise.jar",
      "$javaHome/lib/svm/builder/svm-foreign.jar",
      "$javaHome/lib/svm/builder/svm-configure.jar",
      "$javaHome/lib/svm/builder/svm.jar",
      "$javaHome/lib/svm/builder/espresso-svm.jar",
      "$javaHome/lib/svm/builder/reporter.jar",
      "$javaHome/lib/svm/library-support.jar",
      "$javaHome/lib/svm/builder/svm-capnproto-runtime.jar",
      "$javaHome/lib/truffle/builder/truffle-enterprise-svm.jar",
      "$javaHome/lib/truffle/builder/truffle-runtime-svm.jar",
    ).flatMap {
      listOf(
        it,
        "file://$it",
      )
    }.toSortedSet()
  }

  override fun getDescription(): String = "Disallows substrings in the heap which match the host JVM's path"

  init {
    disallowedSubstrings = getDisallowedSubstrings(
      System.getProperty("java.home"),
      System.getenv("JAVA_HOME"),
      System.getenv("GRAALVM_HOME"),
    )

    val encodings = setOf(
      Charset.forName("UTF-8"),
      Charset.forName("UTF-16"),
      Charset.forName(System.getProperty("sun.jnu.encoding")),
    )
    disallowedByteSubstrings = disallowedSubstrings.mapNotNull { substring ->
      val bytes = substring.toByteArray()
      val charset = encodings.firstOrNull { encoding ->
        try {
          substring.toByteArray(encoding)
          true
        } catch (_: Exception) {
          false
        }
      }
      if (charset != null) {
        bytes to charset
      } else {
        null
      }
    }.associate { it }
  }

  // Build a sorted set of all tokens which are disallowed in the heap.
  private fun getDisallowedSubstrings(vararg substrings: String?): Array<String> {
    return substrings.filterNotNull().asSequence().distinct().filter {
      it.isNotEmpty() && it.isNotBlank()
    }.filter { s ->
      s.indexOf(File.separatorChar, s.indexOf(File.separatorChar) + 1) != -1
    }.toList().toTypedArray()
  }

  // Called when a string is reachable in the heap.
  private fun onStringReachable(access: Feature.DuringAnalysisAccess, str: String, reason: ObjectScanner.ScanReason) {
    if (disallowedSubstrings.isNotEmpty()) {
      for (disallowedSubstring in disallowedSubstrings) {
        if (str.contains(disallowedSubstring)) {
          if (!unconditionalBanlist.contains(str)) {
            if (allowlist.contains(str)) {
              // This is an allowlisted string, so we can skip it.
              return
            }
          }

          // This is an unconditional banlisted string, or a matching string with no presence in the allowlist, so we
          // should immediately throw.
          val lineSeparator = System.lineSeparator()
          throw UnsupportedFeatureException(
            "Detected a string in the image heap which is disallowed." +
              lineSeparator +
              "String that is problematic: $str" +
              lineSeparator +
              "Disallowed substring: $disallowedSubstring" +
              lineSeparator +
              "Reachable because: $reason"
          )
        }
      }
    }
  }

  // Called when a byte array is reachable in the heap.
  private fun onBytesReachable(access: Feature.DuringAnalysisAccess, str: ByteArray, reason: ObjectScanner.ScanReason) {
    if (disallowedByteSubstrings.isNotEmpty()) {
      for ((disallowedSubstring, charset) in disallowedByteSubstrings) {
        if (str.contentEquals(disallowedSubstring)) {
          val lineSeparator = System.lineSeparator()
          throw UnsupportedFeatureException(
            "Detected a byte[] in the image heap which is disallowed." +
              lineSeparator +
              "byte[] that is problematic: ${String(str, charset)}" +
              lineSeparator +
              "Disallowed substring with user directory: ${String(disallowedSubstring, charset)}" +
              lineSeparator +
              "Reachable because: $reason"
          )
        }
      }
    }
  }

  override fun duringSetup(access: Feature.DuringSetupAccess?) {
    access as FeatureImpl.DuringSetupAccessImpl
    access.registerObjectReachableCallback(String::class.java, this::onStringReachable)
    access.registerObjectReachableCallback(ByteArray::class.java, this::onBytesReachable)
  }
}

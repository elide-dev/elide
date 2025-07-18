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
package jdk.internal.jrtfs;

import com.oracle.svm.core.annotate.KeepOriginal;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import java.util.Objects;

@TargetClass(className = "jdk.internal.jrtfs.SystemImage")
@KeepOriginal
public final class RuntimeSystemImage {
  @Substitute
  private static String findHome() {
    var assigned = System.getProperty("java.home");
    if (assigned != null) {
      return assigned;
    }
    var env = System.getenv("JAVA_HOME");
    if (env != null) {
      return env;
    }
    return Objects.requireNonNull(System.getProperty("elide.java.home"));
  }
}

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
package elide.runtime.gvm;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.graalvm.nativeimage.c.CContext;

/** Utility C directives to load JNI headers. */
public final class JNIHeaderDirectives implements CContext.Directives {
  private static final boolean USE_BUILTIN_HEADERS = true;

  @Override
  public List<String> getOptions() {
    return getHeaderOptions();
  }

  @Override
  public List<String> getHeaderFiles() {
    return getHeaders();
  }

  public static List<String> getHeaders() {
    File[] jnis = findJNIHeaders();
    if (USE_BUILTIN_HEADERS) {
      return Arrays.asList("<" + jnis[0].getName() + ">", "<" + jnis[1].getName() + ">");
    }
    return Arrays.asList(
        "\"" + jnis[0].getAbsolutePath() + "\"", "\"" + jnis[1].getAbsolutePath() + "\"");
  }

  public static List<String> getHeaderOptions() {
    File[] jnis = findJNIHeaders();
    return Arrays.asList("-I" + jnis[0].getParent(), "-I" + jnis[1].getParent());
  }

  private static File[] findJNIHeaders() throws IllegalStateException {
    final File jreHome = new File(System.getProperty("java.home"));
    final File include = new File(jreHome, "include");
    var operatingSystemTag = "linux";
    if ("Mac OS X".equals(System.getProperty("os.name"))) {
      operatingSystemTag = "darwin";
    }
    return new File[] {
      new File(include, "jni.h"), new File(new File(include, operatingSystemTag), "jni_md.h"),
    };
  }
}

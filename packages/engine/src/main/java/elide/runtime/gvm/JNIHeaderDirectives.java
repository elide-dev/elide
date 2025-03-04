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
  @Override
  public List<String> getOptions() {
    File[] jnis = findJNIHeaders();
    return Arrays.asList("-I" + jnis[0].getParent(), "-I" + jnis[1].getParent());
  }

  @Override
  public List<String> getHeaderFiles() {
    File[] jnis = findJNIHeaders();
    return Arrays.asList("<" + jnis[0] + ">", "<" + jnis[1] + ">");
  }

  private static File[] findJNIHeaders() throws IllegalStateException {
    final File jreHome = new File(System.getProperty("java.home"));
    final File include = new File(jreHome.getParentFile(), "include");
    return new File[] {
      new File(include, "jni.h"), new File(new File(include, "linux"), "jni_md.h"),
    };
  }
}

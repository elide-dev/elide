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
package elide.tooling.gvm.nativeImage;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.KeepOriginal;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import elide.tool.cli.Statics;
import java.nio.file.Path;
import java.util.List;

/** Patches Native Image's build configuration to support Elide's call style. */
@KeepOriginal
@TargetClass(className = "com.oracle.svm.driver.NativeImage$BuildConfiguration")
@SuppressWarnings("unused")
public final class ElideNativeBuildConfiguration {
  @Substitute
  ElideNativeBuildConfiguration(List<String> args) {
    // redirects to elide resources root
    this(Statics.INSTANCE.getResourcesPath().resolve("gvm"), (Path) null, args);
  }

  @Alias
  ElideNativeBuildConfiguration(Path rootDir, Path workDir, List<String> args) {
    // no-op; stubbed
  }
}

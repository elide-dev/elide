/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;


/**
 * Maps the native C struct used for guest app configuration. Note that certain values such as the dispatch mode are
 * mapped as {@code int}, mapping to their corresponding enum values must be done manually.
 * <p>
 * Use the {@link NativeInterop} extensions to construct the JVM equivalent of this struct using the native values.
 */
@CStruct("elide_app_config_t")
@CContext(ElideNativeDirectives.class)
interface NativeAppConfig extends PointerBase {
  /**
   * Returns the value of the 'id' struct field, which is a C string.
   */
  @CField("id")
  CCharPointer getId();

  /**
   * Returns the value of the 'entrypoint' struct field, which is a C string.
   */
  @CField("entrypoint")
  CCharPointer getEntrypoint();

  /**
   * Returns the value of the 'language' struct field, which can be mapped to the {@link NativeAppLanguage} enum.
   */
  @CField("language")
  int getLanguage();

  /**
   * Returns the value of the 'mode' struct field, which can be mapped to the {@link NativeAppMode} enum.
   */
  @CField("mode")
  int getMode();
}

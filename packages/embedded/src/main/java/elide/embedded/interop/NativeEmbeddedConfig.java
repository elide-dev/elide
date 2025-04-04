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
 * Maps the native C struct used for runtime configuration. Note that certain values such as the protocol version and
 * format are mapped as {@code int}, mapping to their corresponding enum values must be done manually.
 * <p>
 * Use the {@link NativeInterop} extensions to construct the JVM equivalent of this struct using the native values.
 */
@CStruct("elide_config_t")
@CContext(ElideNativeDirectives.class)
interface NativeEmbeddedConfig extends PointerBase {
  /**
   * Returns the value of the 'version' struct field, which can be mapped to the {@link NativeProtocolVersion} enum.
   */
  @CField("version")
  int getVersion();

  /**
   * Returns the value of the 'format' struct field, which can be mapped to the {@link NativeProtocolFormat} enum.
   */
  @CField("format")
  int getFormat();

  /**
   * Returns the value of the 'guest_root' struct field, which is a C string representing a directory path.
   */
  @CField("guest_root")
  CCharPointer getGuestRoot();

  /**
   * Returns the value of the 'languages' struct field, which is a 32-bit integer treated as a collection of language
   * flags using the {@link NativeAppLanguage} enum ordinal as bit position.
   */
  @CField("languages")
  int getLanguageFlags();
}

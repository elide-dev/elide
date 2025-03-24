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
package elide.runtime.lang.typescript;

import com.oracle.truffle.api.TruffleFile;
import java.nio.charset.Charset;

class TypeScriptFileTypeDetector implements TruffleFile.FileTypeDetector {
  public String findMimeType(TruffleFile file) {
    String fileName = file.getName();
    if (fileName != null) {
      if (fileName.endsWith(TypeScriptLanguage.EXTENSION_TS)) {
        return TypeScriptLanguage.APPLICATION_MIME_TYPE;
      }
      if (fileName.endsWith(TypeScriptLanguage.EXTENSION_MTS)) {
        return TypeScriptLanguage.MODULE_MIME_TYPE;
      }
      if (fileName.endsWith(TypeScriptLanguage.EXTENSION_TSX)) {
        return TypeScriptLanguage.TSX_MIME_TYPE;
      }
      if (fileName.endsWith(TypeScriptLanguage.EXTENSION_JSX)) {
        return TypeScriptLanguage.JSX_MIME_TYPE;
      }
    }
    return null;
  }

  public Charset findEncoding(TruffleFile file) {
    return null;
  }
}

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
package elide.runtime.gvm.internals.js

import com.oracle.truffle.api.TruffleFile
import com.oracle.truffle.js.lang.JavaScriptLanguage
import java.nio.charset.Charset

// Mime type/file type constants.
internal const val JAVASCRIPT_APPLICATION_MIME_TYPE = JavaScriptLanguage.APPLICATION_MIME_TYPE
internal const val JAVASCRIPT_MODULE_MIME_TYPE = JavaScriptLanguage.MODULE_MIME_TYPE
internal const val JAVASCRIPT_TEXT_MIME_TYPE = JavaScriptLanguage.TEXT_MIME_TYPE
internal const val JAVASCRIPT_JSON_MIME_TYPE = JavaScriptLanguage.JSON_MIME_TYPE
internal const val TYPESCRIPT_TEXT_MIME_TYPE = "text/typescript"
internal const val TYPESCRIPT_MIME_TYPE = "application/typescript"
internal const val TYPESCRIPT_MODULE_MIME_TYPE = "application/typescript+module"

// Implements a file type detector for JavaScript and TypeScript file types supported by Elide.
internal class ElideJsFileTypeDetector : TruffleFile.FileTypeDetector {
  override fun findEncoding(file: TruffleFile?): Charset {
    TODO("Not yet implemented")
  }

  override fun findMimeType(file: TruffleFile?): String {
    TODO("Not yet implemented")
  }
}

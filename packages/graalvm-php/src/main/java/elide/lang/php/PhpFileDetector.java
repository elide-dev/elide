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
package elide.lang.php;

import com.oracle.truffle.api.TruffleFile;
import java.io.IOException;
import java.nio.charset.Charset;

/** Detects PHP files based on file extension and content. */
public final class PhpFileDetector implements TruffleFile.FileTypeDetector {
  @Override
  public String findMimeType(TruffleFile file) throws IOException {
    String fileName = file.getName();
    if (fileName != null && fileName.endsWith(PhpLanguage.EXTENSION)) {
      return PhpLanguage.MIME_TYPE;
    }
    return null;
  }

  @Override
  public Charset findEncoding(TruffleFile file) throws IOException {
    // PHP files are UTF-8 by default
    return Charset.forName("UTF-8");
  }
}

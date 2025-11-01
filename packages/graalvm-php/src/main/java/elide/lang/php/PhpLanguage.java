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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import elide.lang.php.nodes.PhpRootNode;
import elide.lang.php.parser.PhpParser;
import elide.lang.php.runtime.PhpContext;

/**
 * TrufflePHP Language Implementation
 *
 * <p>This is the main entry point for the PHP language implementation in Truffle. It handles
 * language initialization, context creation, and program execution.
 */
@TruffleLanguage.Registration(
    id = "php",
    name = "PHP",
    defaultMimeType = PhpLanguage.MIME_TYPE,
    characterMimeTypes = PhpLanguage.MIME_TYPE,
    contextPolicy = TruffleLanguage.ContextPolicy.SHARED,
    fileTypeDetectors = PhpFileDetector.class)
public final class PhpLanguage extends TruffleLanguage<PhpContext> {

  public static final String ID = "php";
  public static final String MIME_TYPE = "application/x-php";
  public static final String EXTENSION = ".php";

  @Override
  protected PhpContext createContext(Env env) {
    return new PhpContext(this, env);
  }

  @Override
  protected CallTarget parse(ParsingRequest request) throws Exception {
    // Get the context to access the global scope
    PhpContext context = getCurrentContext(PhpLanguage.class);
    PhpParser parser = new PhpParser(this, request.getSource(), context.getGlobalScope());
    return parser.parse().getCallTarget();
  }

  /**
   * Parse and execute a source file. This is used by include/require statements to execute included
   * files. The included file shares the same global scope and frame as the parent file.
   */
  public Object parseAndExecute(Source source, VirtualFrame frame) {
    PhpRootNode rootNode = parseSourceFile(source);
    // Execute the included file's body directly in the current frame
    // This allows the included file to access and modify variables from the parent scope
    return rootNode.execute(frame);
  }

  @com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
  private PhpRootNode parseSourceFile(Source source) {
    PhpContext context = getCurrentContext(PhpLanguage.class);
    PhpParser parser = new PhpParser(this, source, context.getGlobalScope());
    return parser.parse();
  }

  /** Get the language instance from a node. */
  public static PhpLanguage get(Node node) {
    return getCurrentLanguage(PhpLanguage.class);
  }

  /** Get the context associated with a node. */
  public static PhpContext getContext(Node node) {
    return getCurrentContext(PhpLanguage.class);
  }
}

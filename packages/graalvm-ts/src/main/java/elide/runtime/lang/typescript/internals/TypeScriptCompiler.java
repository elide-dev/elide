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
package elide.runtime.lang.typescript.internals;

import static com.oracle.truffle.api.TruffleLanguage.Env;

import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;

/** TBD. */
public class TypeScriptCompiler implements AutoCloseable {
  private static final String TYPESCRIPT_COMPILER_PATH =
      "/META-INF/elide/embedded/tools/tsc/typescript.js.gz";
  private static final Source TYPESCRIPT_COMPILER_SOURCE = createTypeScriptCompilerSource();
  //  private static final Source TYPESCRIPT_TRANSPILE_FUNCTION_SOURCE =
  // createTypeScriptTranspileFunctionSource();
  private final TruffleContext context;
  private final Object transpileFunction;

  public TypeScriptCompiler(Env env) {
    this.context = env.newInnerContextBuilder("js").build();
    transpileFunction = context.evalInternal(null, TYPESCRIPT_COMPILER_SOURCE);
    //    transpileFunction = context.evalInternal(null, TYPESCRIPT_TRANSPILE_FUNCTION_SOURCE);
  }

  public String compileToString(CharSequence ts, String name) {
    try {
      var js = (TruffleString) InteropLibrary.getUncached().execute(transpileFunction, ts, name);
      return js.toJavaStringUncached();
    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
      throw new RuntimeException(e);
    }
  }

  public Source compileToNewSource(
      CharSequence ts, String name, boolean isModule, String filePath) {
    var js = compileToString(ts, name);
    if (filePath == null) {
      return Source.newBuilder("js", js, name)
          .mimeType(isModule ? "application/javascript+module" : "application/javascript")
          .build();
    } else {
      try {
        return Source.newBuilder("js", Path.of(filePath).toUri().toURL())
            .name(name)
            .content(js)
            .mimeType(isModule ? "application/javascript+module" : "application/javascript")
            .build();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static Source createTypeScriptCompilerSource() {
    return Source.newBuilder("js", getTypeScriptCompilerCode(), "typescript.js")
        .mimeType("application/javascript")
        //            .cached(true)
        //            .encoding(StandardCharsets.UTF_8)
        //            .canonicalizePath(true)
        //            .interactive(false)
        //            .internal(true)
        .build();
  }

  //  private static Source createTypeScriptTranspileFunctionSource() {
  //    String function = """
  //        (code, fileName) => ts.transpile(code, {
  //          module: "ESNext",
  //          inlineSourceMap: true,
  //          inlineSources: true,
  //        }, fileName);
  //        """;
  //    return Source
  //            .newBuilder("js", function, "typescript-transpile.js")
  //            .mimeType("application/javascript")
  ////            .cached(true)
  ////            .interactive(false)
  ////            .internal(true)
  ////            .encoding(StandardCharsets.UTF_8)
  //            .build();
  //  }

  private static String getTypeScriptCompilerCode() {
    try (var stream = TypeScriptCompiler.class.getResourceAsStream(TYPESCRIPT_COMPILER_PATH)) {
      if (stream == null) {
        throw new RuntimeException("TypeScript compiler not found in resources");
      }
      try (var reader =
          new BufferedReader(
              new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8))) {
        // read all code preserving newlines
        return IOUtils.toString(reader);
      }
    } catch (Exception e) {
      throw new RuntimeException("TypeScript compiler not found in resources", e);
    }
  }

  @Override
  public void close() {
    context.close();
  }
}
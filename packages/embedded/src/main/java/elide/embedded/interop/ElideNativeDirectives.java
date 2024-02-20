package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext.Directives;

import java.util.List;


/**
 * Build-time directives for the shared library housing the embedded runtime. This class includes a header defining C
 * types used at runtime so that they can be used via the GraalVM C interoperability API.
 */
public class ElideNativeDirectives implements Directives {
  @Override
  public List<String> getHeaderFiles() {
    return List.of("<" + BuildConstants.TYPEDEF_HEADER_PATH + ">");
  }
}
package elide.runtime.python;

import elide.runtime.plugins.api.NativePlugin;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.word.Pointer;

/**
 * TBD.
 */
@SuppressWarnings("unused")
public class ElidePythonLanguage extends NativePlugin {
  private static final String ELIDE_PYTHON = "python";
  private static final ElidePythonLanguage SINGLETON = new ElidePythonLanguage();
  private static final Context BASE = initialize(SINGLETON).build();

  ElidePythonLanguage() {
    super(ELIDE_PYTHON);
  }

  public static ElidePythonLanguage get() {
    return SINGLETON;
  }

  @Override
  public void context(Engine engine, Context.Builder builder, String[] args) {
    builder.option("python.EmulateJython", "false");
    builder.option("python.NativeModules", "true");
    builder.option("python.LazyStrings", "true");
    builder.option("python.WithTRegex", "true");
    builder.option("python.WithCachedSources", "true");
    builder.option("python.HPyBackend", "nfi");
    builder.option("python.PosixModuleBackend", "java");
  }

  public static void main(String[] args) {
    get().apply(args);
  }


  @CEntryPoint(name = "Java_elide_runtime_python_ElidePythonLanguage_init")
  public static void init(Pointer jniEnv, Pointer clazz, @CEntryPoint.IsolateThreadContext long isolateId) {
    System.out.println("Native Python plugin initialized (isolate: " + isolateId + ")");
  }
}

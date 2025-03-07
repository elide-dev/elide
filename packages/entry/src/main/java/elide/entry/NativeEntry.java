package elide.entry;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;

public final class NativeEntry {
  @CEntryPoint(name = "elide_entry_init")
  private static int initEntry(IsolateThread thread) {
    return 0;
  }

  @CEntryPoint(name = "elide_entry_run")
  private static int runEntry(IsolateThread thread) {
    String[] args = ProcessHandle.current().info().arguments().orElseThrow();
    return elide.entry.MainKt.entry(args);
  }
}

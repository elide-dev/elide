package elide.runtime.gvm.internals.intrinsics

import org.graalvm.nativeimage.Isolate
import org.graalvm.nativeimage.IsolateThread
import org.graalvm.nativeimage.c.function.CEntryPoint

/**
 * TBD.
 */
internal object NativeRuntime {
  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.CREATE_ISOLATE, name = "create_isolate")
  @JvmStatic
  external fun createIsolate(): IsolateThread?

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.TEAR_DOWN_ISOLATE, name = "tear_down_isolate")
  @JvmStatic
  external fun tearDownIsolate(thread: IsolateThread)

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.ATTACH_THREAD, name = "attach_thread")
  @JvmStatic
  external fun attachThread(isolate: Isolate): IsolateThread?

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.DETACH_THREAD, name = "detach_thread")
  @JvmStatic
  external fun detachThread(thread: IsolateThread): Int

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.GET_CURRENT_THREAD, name = "get_current_thread")
  @JvmStatic
  external fun currentThread(isolate: Isolate): IsolateThread?

  /**
   * TBD.
   */
  @CEntryPoint(builtin = CEntryPoint.Builtin.GET_ISOLATE, name = "get_isolate")
  @JvmStatic
  external fun isolateForThread(thread: IsolateThread): Isolate?
}

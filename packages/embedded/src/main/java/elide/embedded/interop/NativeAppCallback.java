package elide.embedded.interop;

import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.type.CTypedef;


/**
 * Represents a native C function which accepts a single {@code int} argument. This callback type is used for async
 * operations (e.g. app startup and shutdown), allowing C code to receive a notification when the operation completes.
 * <p>
 * Use the {@link #call(int)} method to invoke the native function.
 */
@CTypedef(name = "elide_app_callback_t")
interface NativeAppCallback extends CFunctionPointer {
  /**
   * Invoke the native function held by this pointer using an integer result code.
   */
  @InvokeCFunctionPointer
  void call(int result);
}

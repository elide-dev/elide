package elide.embedded.interop;


import kotlin.Unit;
import kotlin.jvm.functions.Function1;


/**
 * Interoperability helper used to invoke native functions as a response to an application event. This wrapper is
 * necessary to work around the limitations imposed on native pointer values, which may not be freely shared with
 * Kotlin code.
 * <p>
 * The wrapper implements a Kotlin functional type accepting a Boolean parameter and returning Unit (void). Using
 * {@link #invoke} will call the wrapped native function, translating the boolean result (which indicates success)
 * into the proper {@link NativeResultCodes native result code}.
 */
class NativeAppCallbackHolder implements Function1<Boolean, Unit> {
  /**
   * Reference to the native function pointer.
   */
  private final NativeAppCallback nativeCallback;

  /**
   * Constructs a new wrapper around a native function pointer, which will be called when {@link #invoke} is called.
   * The wrapper instance can be freely shared with Kotlin code and passed into lambdas, as a workaround for the
   * restrictions imposed on native word-sized values.
   *
   * @param nativeAppCallback A native function pointer to be invoked by this wrapper.
   */
  NativeAppCallbackHolder(NativeAppCallback nativeAppCallback) {
    nativeCallback = nativeAppCallback;
  }

  @Override
  public Unit invoke(Boolean success) {
    // translate the boolean to a native integer code 
    nativeCallback.call(success ? NativeResultCodes.ok() : NativeResultCodes.unknownError());
    return Unit.INSTANCE;
  }
}
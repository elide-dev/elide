package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;


/**
 * A collection of C constants representing well-known error codes for runtime operations. During a native build,
 * calls to the methods provided by this class will be replaced with their matching constant value.
 */
@CContext(ElideNativeDirectives.class)
final class NativeResultCodes {
  private NativeResultCodes() {
    // sealed constructor for static class
  }

  /**
   * Constant value for a successful result code.
   */
  @CConstant("ELIDE_OK")
  static native int ok();

  /**
   * Constant value for an unknown error code, returned when an exception is thrown during a runtime operation.
   */
  @CConstant("ELIDE_ERR_UNKNOWN")
  static native int unknownError();

  /**
   * Error code used when the runtime is not yet initialized, but initialization is required for the operation.
   */
  @CConstant("ELIDE_ERR_UNINITIALIZED")
  static native int uninitialized();

  /**
   * Error code used when the runtime's initialization function is called more than once.
   */
  @CConstant("ELIDE_ERR_ALREADY_INITIALIZED")
  static native int alreadyInitialized();
}
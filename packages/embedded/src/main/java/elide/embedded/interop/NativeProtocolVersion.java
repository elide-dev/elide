package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;


/**
 * Maps the native C enum representing the version of the protocol used by the runtime for call dispatch and other
 * operations.
 * <p>
 * Use the {@link #nativeValue} and {@link #fromNativeValue} methods to convert to/from native integer values. The
 * {@link NativeInterop} extensions also provide methods to convert values to their counterpart in the JVM API.
 */
@CEnum("elide_protocol_version_t")
@CContext(ElideNativeDirectives.class)
enum NativeProtocolVersion {
  V1_0;

  /**
   * Returns the native integer value for this enum entry.
   */
  @CEnumValue
  public native int nativeValue();

  /**
   * Returns the enum entry for the specified native integer value.
   */
  @CEnumLookup
  public static native NativeProtocolVersion fromNativeValue(int value);
}
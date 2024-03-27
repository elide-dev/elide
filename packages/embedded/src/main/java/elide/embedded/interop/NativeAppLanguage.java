package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;


/**
 * Maps the native C enum representing the guest languages supported by the runtime for applications. The runtime must
 * be configured to enable a specific set of languages, which can then be used by guest apps.
 * <p>
 * Use the {@link #nativeValue} and {@link #fromNativeValue} methods to convert to/from native integer values. The
 * {@link NativeInterop} extensions also provide methods to convert values to their counterpart in the JVM API.
 */
@CEnum("elide_app_lang_t")
@CContext(ElideNativeDirectives.class)
enum NativeAppLanguage {
  /**
   * Selects JavaScript as a guest language.
   */
  JS,

  /**
   * Selects Python as a guest language.
   */
  PYTHON;

  /**
   * Returns the native integer value for this enum entry.
   */
  @CEnumValue
  public native int nativeValue();

  /**
   * Returns the enum entry for the specified native integer value.
   */
  @CEnumLookup
  public static native NativeAppLanguage fromNativeValue(int value);
}
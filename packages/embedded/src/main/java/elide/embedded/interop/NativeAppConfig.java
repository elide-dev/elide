package elide.embedded.interop;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.word.PointerBase;


/**
 * Maps the native C struct used for guest app configuration. Note that certain values such as the dispatch mode are
 * mapped as {@code int}, mapping to their corresponding enum values must be done manually.
 * <p>
 * Use the {@link NativeInterop} extensions to construct the JVM equivalent of this struct using the native values.
 */
@CStruct("elide_app_config_t")
@CContext(ElideNativeDirectives.class)
interface NativeAppConfig extends PointerBase {
  /**
   * Returns the value of the 'id' struct field, which is a C string.
   */
  @CField("id")
  CCharPointer getId();

  /**
   * Returns the value of the 'entrypoint' struct field, which is a C string.
   */
  @CField("entrypoint")
  CCharPointer getEntrypoint();

  /**
   * Returns the value of the 'language' struct field, which can be mapped to the {@link NativeAppLanguage} enum.
   */
  @CField("language")
  int getLanguage();

  /**
   * Returns the value of the 'mode' struct field, which can be mapped to the {@link NativeAppMode} enum.
   */
  @CField("mode")
  int getMode();
}
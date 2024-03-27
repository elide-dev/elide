package elide.embedded.native

import java.lang.foreign.MemoryLayout.PathElement
import java.lang.foreign.MemoryLayout.structLayout
import java.lang.foreign.StructLayout
import java.lang.foreign.ValueLayout
import java.lang.invoke.VarHandle
import elide.embedded.native.ElideNativeLibrary.EmbeddedNativeConfig.LAYOUT
import elide.embedded.native.ElideNativeLibrary.GraalVM
import elide.embedded.test.TestConstants

/**
 * A [NativeLibrary] singleton mapping the native shared library for the Elide Embedded Runtime.
 *
 * Runtime functions require a GraalVM isolate thread which can be obtained with [GraalVM.createIsolate]. See the
 * native headers for API details.
 */
internal object ElideNativeLibrary : NativeLibrary() {
  /** Functions related to the GraalVM C interop API, used to manage native isolates. */
  object GraalVM {
    /**
     * Binds to the `graal_create_isolate` function, which is used to obtain the isolate thread passed to Elide
     * library functions. See the original C declarations for a description of the API.
     */
    val createIsolate = functionHandle(
      name = "graal_create_isolate",
      returns = ValueLayout.JAVA_INT,
      /*params=*/ ValueLayout.ADDRESS,
      /*isolate=*/ ValueLayout.ADDRESS,
      /*thread=*/ ValueLayout.ADDRESS,
    )
  }

  /** Describes the memory [layout][LAYOUT] of the `elide_config_t` struct, and provides var handles for its fields. */
  object EmbeddedNativeConfig {
    /** Name of the 'version' struct field. */
    private const val FIELD_VERSION = "version"

    /** Name of the 'format' struct field. */
    private const val FIELD_FORMAT = "format"

    /** Name of the 'guest_root' struct field. */
    private const val FIELD_GUEST_ROOT = "guest_root"

    /** Memory layout of the `elide_config_t` struct. */
    val LAYOUT: StructLayout = structLayout(
      ValueLayout.JAVA_INT.withName(FIELD_VERSION),
      ValueLayout.JAVA_INT.withName(FIELD_FORMAT),
      ValueLayout.ADDRESS.withName(FIELD_GUEST_ROOT),
    )

    /** A [VarHandle] for the 'version' field of the `elide_config_t` struct. */
    val version: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_VERSION))

    /** A [VarHandle] for the 'format' field of the `elide_config_t` struct. */
    val format: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_FORMAT))

    /** A [VarHandle] for the 'guest_root' field of the `elide_config_t` struct. */
    val guestRoot: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_GUEST_ROOT))
  }

  object EmbeddedAppNativeConfig {
    /** Name of the 'id' struct field. */
    private const val FIELD_ID = "id"

    /** Name of the 'entrypoint' struct field. */
    private const val FIELD_ENTRYPOINT = "entrypoint"

    /** Name of the 'language' struct field. */
    private const val FIELD_LANGUAGE = "language"

    /** Name of the 'mode' struct field. */
    private const val FIELD_MODE = "mode"

    /** Memory layout of the `elide_app_config_t` struct. */
    val LAYOUT: StructLayout = structLayout(
      ValueLayout.ADDRESS.withName(FIELD_ID),
      ValueLayout.ADDRESS.withName(FIELD_ENTRYPOINT),
      ValueLayout.JAVA_INT.withName(FIELD_LANGUAGE),
      ValueLayout.JAVA_INT.withName(FIELD_MODE),
    )

    /** A [VarHandle] for the 'id' field of the `elide_app_config_t` struct. */
    val id: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_ID))

    /** A [VarHandle] for the 'entrypoint' field of the `elide_app_config_t` struct. */
    val entrypoint: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_ENTRYPOINT))

    /** A [VarHandle] for the 'language' field of the `elide_app_config_t` struct. */
    val language: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_LANGUAGE))

    /** A [VarHandle] for the 'mode' field of the `elide_app_config_t` struct. */
    val mode: VarHandle = LAYOUT.varHandle(PathElement.groupElement(FIELD_MODE))
  }

  /** Load the library from the path set at build time. */
  override val libraryPath: String = TestConstants.ELIDE_EMBEDDED_PATH

  /** Binds to the `elide_embedded_init` C function in the native library. */
  val initialize = functionHandle(
    name = "elide_embedded_init",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
    /*config=*/ValueLayout.ADDRESS,
  )

  /** Binds to the `elide_app_create` C function in the native library. */
  val createApp = functionHandle(
    name = "elide_app_create",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
    /*config=*/ValueLayout.ADDRESS,
    /*handle=*/ValueLayout.ADDRESS,
  )

  /** Binds to the `elide_app_start` C function in the native library. */
  val startApp = functionHandle(
    name = "elide_app_start",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
    /*app=*/ValueLayout.ADDRESS,
    /*callback=*/ValueLayout.ADDRESS,
  )

  /** Binds to the `elide_app_stop` C function in the native library. */
  val stopApp = functionHandle(
    name = "elide_app_stop",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
    /*app=*/ValueLayout.ADDRESS,
    /*callback=*/ValueLayout.ADDRESS,
  )

  /** Binds to the `elide_embedded_start` C function in the native library. */
  val start = functionHandle(
    name = "elide_embedded_start",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
  )

  /** Binds to the `elide_embedded_stop` C function in the native library. */
  val stop = functionHandle(
    name = "elide_embedded_stop",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
  )

  /** Binds to the `elide_embedded_dispatch` C function in the native library. */
  val dispatch = functionHandle(
    name = "elide_embedded_dispatch",
    returns = ValueLayout.JAVA_INT,
    /*thread=*/ValueLayout.ADDRESS,
  )
}

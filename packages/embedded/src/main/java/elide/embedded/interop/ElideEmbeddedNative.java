package elide.embedded.interop;

import elide.embedded.ElideEmbedded;
import elide.embedded.EmbeddedApp;
import elide.runtime.Logger;
import elide.runtime.Logging;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.Pointer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Static entrypoint for the Elide Embedded C API.
 * <p>
 * This class uses a shared {@link ElideEmbedded} instance created during the first {@link #initialize} call, and its
 * methods contain only the logic necessary to translate from native data structures to their JVM counterparts. It is
 * meant to serve as a thin wrapper over the embedded runtime instance, exposing its API to native C code.
 *
 * @see ElideNativeDirectives
 */
@CContext(ElideNativeDirectives.class)
final class ElideEmbeddedNative {
  /**
   * Flag to avoid multiple concurrent {@link #initialize} calls.
   * <p>
   * Note that even when this flag returns {@code true}, there is no guarantee that the {@link #runtime} reference will
   * be initialized.
   */
  private static final AtomicBoolean initialized = new AtomicBoolean();

  /**
   * Shared reference to the current {@link ElideEmbedded} instance, or {@code null} if {@link #initialize} has not
   * been called yet.
   */
  private static final AtomicReference<ElideEmbedded> runtime = new AtomicReference<>();

  /**
   * Runtime logger instance, used for internal error reporting and debugging.
   * <p>
   * Exceptions thrown by runtime operations
   * should always be caught and translated into a proper error code for the C host, this logger permits the analysis
   * of such exceptions.
   */
  private static final Logger logging = Logging.of(ElideEmbeddedNative.class);

  private ElideEmbeddedNative() {
    // sealed constructor for static class
  }

  /**
   * Initializes the runtime with the specified configuration. This is a thread-safe function meant to be used from C
   * code via GraalVM's interoperability API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @param config        Pointer to a C struct specifying the configuration for the runtime.
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_embedded_init", documentation = {"Initialize and configure the Elide embedded runtime"})
  public static int initialize(IsolateThread ignoredThread, NativeEmbeddedConfig config) {
    // prevent duplicate initialization, use a recoverable error code
    if (!initialized.compareAndSet(false, true)) return NativeResultCodes.alreadyInitialized();

    var instance = new ElideEmbedded();
    runtime.set(instance);

    try {
      // map configuration to JVM structures and initialize the runtime
      var version = NativeProtocolVersion.fromNativeValue(config.getVersion());
      var format = NativeProtocolFormat.fromNativeValue(config.getFormat());
      var guestRoot = CTypeConversion.toJavaString(config.getGuestRoot());

      var embeddedConfig = NativeInterop.createConfig(version, format, guestRoot);
      instance.initialize(embeddedConfig);
      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }

  /**
   * Starts the runtime, enabling {@link #dispatch} calls. This is a thread-safe function meant to be used from C code
   * via GraalVM's interoperability API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_embedded_start", documentation = {"Start the embedded runtime"})
  public static int start(IsolateThread ignoredThread) {
    var instance = runtime.get();
    if (instance == null) return NativeResultCodes.uninitialized();

    try {
      instance.start();
      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }

  /**
   * Stops the runtime, rejecting all new {@link #dispatch} calls. This is a thread-safe function meant to be used
   * from C code via GraalVM's interoperability API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_embedded_stop", documentation = {"Stop the embedded runtime"})
  public static int stop(IsolateThread ignoredThread) {
    var instance = runtime.get();
    if (instance == null) return NativeResultCodes.uninitialized();

    try {
      instance.stop();
      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }

  /**
   * Dispatch an incoming call with the runtime. This is a thread-safe function meant to be used from C code via
   * GraalVM's interoperability API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_embedded_dispatch", documentation = {"Dispatch a call with the embedded runtime"})
  public static int dispatch(IsolateThread ignoredThread) {
    var instance = runtime.get();
    if (instance == null) return NativeResultCodes.uninitialized();

    try {
      instance.dispatch();
      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }

  /**
   * Registers a guest application with the runtime. The runtime must be initialized for this operation to succeed.
   * This is a thread-safe function meant to be used from C code via GraalVM's interoperability API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @param config        Pointer to a C struct (`elide_app_config_t`) providing configuration for the app.
   * @param handle        Pointer to which the app handle will be written.
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_app_create", documentation = {"Register a guest application with the runtime"})
  public static int createApp(IsolateThread ignoredThread, NativeAppConfig config, Pointer handle) {
    var instance = runtime.get();
    if (instance == null) return NativeResultCodes.uninitialized();

    try {
      // the id is also passed in the config struct
      var appId = CTypeConversion.toJavaString(config.getId());

      // map configuration options
      var entrypoint = CTypeConversion.toJavaString(config.getEntrypoint());
      var lang = CTypeConversion.toJavaString(config.getLanguage());
      var mode = NativeAppMode.fromNativeValue(config.getMode());

      // launch the app and wrap the instance in a native pointer, then write the pointer value
      var app = instance.createApp(appId, NativeInterop.createAppConfig(entrypoint, lang, mode));
      handle.writeWord(0, NativeInterop.handleFor(app));

      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }

  /**
   * Starts a guest application, allowing it to process incoming requests. The runtime must be initialized for this
   * operation to succeed. This is a thread-safe function meant to be used from C code via GraalVM's interoperability
   * API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @param handle        An opaque pointer containing a wrapped app instance as a result of {@link #createApp}
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_app_start", documentation = {"Start a guest application"})
  public static int startApp(IsolateThread ignoredThread, ObjectHandle handle, NativeAppCallback callback) {
    var instance = runtime.get();
    if (instance == null) return NativeResultCodes.uninitialized();

    try {
      // the pointer must be a wrapped guest app instance
      EmbeddedApp app = NativeInterop.unwrapHandle(handle);

      // schedule the app startup, and pass the wrapped native callback if available
      if (callback.isNull()) instance.startApp(app);
      else instance.startApp(app, new NativeAppCallbackHolder(callback));

      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }

  /**
   * Starts a guest application, allowing it to process incoming requests. The runtime must be initialized for this
   * operation to succeed. This is a thread-safe function meant to be used from C code via GraalVM's interoperability
   * API.
   *
   * @param ignoredThread GraalVM isolate thread parameter required by the C API.
   * @param handle        An opaque pointer containing a wrapped app instance as a result of {@link #createApp}
   * @return An int result code, {@code 0} on success, or one of the well-known {@link NativeResultCodes} on failure.
   */
  @CEntryPoint(name = "elide_app_stop", documentation = {"Stop a guest application"})
  public static int stopApp(IsolateThread ignoredThread, ObjectHandle handle, NativeAppCallback callback) {
    var instance = runtime.get();
    if (instance == null) return NativeResultCodes.uninitialized();

    try {
      // the pointer must be a wrapped guest app instance
      EmbeddedApp app = NativeInterop.unwrapHandle(handle);

      // schedule the app startup, and pass the wrapped native callback if available
      if (callback.isNull()) instance.stopApp(app);
      else instance.stopApp(app, new NativeAppCallbackHolder(callback));

      return NativeResultCodes.ok();
    } catch (Throwable cause) {
      logging.error("Unexpected error in native entrypoint", cause);
      return NativeResultCodes.unknownError();
    }
  }
}
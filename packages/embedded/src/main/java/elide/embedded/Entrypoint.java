/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.embedded;

import com.oracle.svm.core.Uninterruptible;
import elide.embedded.NativeApi.*;
import elide.embedded.api.Constants;
import elide.embedded.api.InFlightCallInfo;
import elide.embedded.api.UnaryNativeCall;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Elide Embedded Entrypoint
 *
 * <p>Provides the entrypoint for the Elide runtime when used as a native embedded library. This file defines a pure
 * Java interface which is generated into a natively callable header and implementation.</p>
 *
 * <p>This class operates as a thin layer on top of {@link ElideEmbedded}, which implements the actual entrypoint logic
 * in a way that can be tested from JVM.</p>
 *
 * @see ElideEmbedded for more information.
 */
@SuppressWarnings({"unused", "JavadocDeclaration"})
@CContext(NativeApi.ElideEmbeddedApi.class)
public class Entrypoint {
  private static final String DEFAULT_PROTOCOL_VERSION = Constants.API_VERSION;
  private static final ProtocolMode DEFAULT_PROTOCOL_MODE = ProtocolMode.ELIDE_PROTOBUF;
  private static final AtomicReference<ProtocolMode> protocolMode = new AtomicReference<>(DEFAULT_PROTOCOL_MODE);
  private static final AtomicReference<String> protocolVersion = new AtomicReference<>(DEFAULT_PROTOCOL_VERSION);
  private static final AtomicBoolean configured = new AtomicBoolean(false);
  private static final AtomicReference<ElideEmbedded> runtime = new AtomicReference<>(null);
  private static final AtomicBoolean initialized = new AtomicBoolean(false);
  private static final AtomicBoolean running = new AtomicBoolean(false);
  private static final List<String> args = new LinkedList<>();
  private static final AtomicReference<PrintStream> out = new AtomicReference<>(System.out);
  private static final AtomicReference<PrintStream> err = new AtomicReference<>(System.err);
  private static final AtomicReference<InputStream> in = new AtomicReference<>(System.in);
  private static final UncaughtMainExceptionHandler mainExceptionHandler = new UncaughtMainExceptionHandler();

  static {
    System.setProperty("elide.embedded", "true");
    System.setProperty("elide.host", "true");
    Thread.currentThread().setUncaughtExceptionHandler(mainExceptionHandler);
  }

  /**
   *
   * @param args
   */
  public static void main(String[] args) {
    ElideEmbedded.create().entry(args);
  }

  /**
   *
   * @param thread
   * @param mode
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_initialize",
    documentation = {
      "Initialize the Elide Embedded runtime.",
      "This function must be called before any other Elide Embedded function, in order to setup initial resources.",
      "This function is not thread-safe and must be called from a single thread, ideally at host system boot."
    }
  )
  public static int initialize(IsolateThread thread, ProtocolMode mode) {
    assert !initialized.get() : "Elide Embedded is already initialized";
    assert !configured.get() : "Elide Embedded is already configured";
    assert !running.get() : "Elide Embedded is already running";
    try {
      protocolMode.set(mode);
      final ElideEmbedded instance = ElideEmbedded.create();
      runtime.compareAndSet(null, instance);
      initialized.compareAndSet(false, true);
      return instance.initialize(elide.embedded.api.ProtocolMode.resolve(mode));
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   *
   * @param thread
   * @param capability
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_capability",
    documentation = {
      "Declare a capability for the Elide Embedded runtime.",
      "Called to declare support for a given capability from host software. Must be called after initialization.",
      "Cannot be called after configuration, or after an instance is running."
    }
  )
  public static int capability(IsolateThread thread, Capability capability) {
    assert initialized.get() : "Elide Embedded not initialized; please initialize before declaring capabilities";
    assert !configured.get() : "Elide Embedded is already configured";
    assert !running.get() : "Elide Embedded is already running";
    try {
      return runtime.get().capability(elide.embedded.api.Capability.resolve(capability));
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   *
   * @param thread
   * @param arg
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_arg",
    documentation = {
      "Declare an argument for the Elide Embedded runtime.",
      "Called to declare an entry argument for the Elide Embedded runtime. Must be called after initialization.",
      "Cannot be called after configuration, or after an instance is running."
    }
  )
  public static int arg(IsolateThread thread, CCharPointer arg) {
    assert initialized.get() : "Elide Embedded not initialized; please initialize before declaring arguments";
    assert !configured.get() : "Elide Embedded is already configured";
    assert !running.get() : "Elide Embedded is already running";
    final String argStr = CTypeConversion.toJavaString(arg);
    try {
      args.add(argStr);
      return 0;
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   *
   * @param thread
   * @param config
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_configure",
    documentation = {
      "Configure the Elide Embedded runtime.",
      "Called to configure the Elide Embedded runtime with a given binary configuration payload; must be called after",
      "initialization. Can only be called once per instance, and cannot be called after the instance is running."
    }
  )
  public static int configure(IsolateThread thread, CCharPointer version, InstanceConfiguration config) {
    assert initialized.get() : "Elide Embedded not initialized; please initialize before configuring";
    final ElideEmbedded instance = runtime.get();
    assert instance != null;
    configured.compareAndExchange(false, true);
    try {
      final String apiVersion = CTypeConversion.toJavaString(version);
      protocolVersion.set(apiVersion);
      return instance.configure(apiVersion, elide.embedded.api.InstanceConfiguration.loadNative(
        NativeBytes.inflateConfig(thread, protocolVersion.get(), protocolMode.get(), config)
      ));
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   *
   * @param thread
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_start",
    documentation = {
      "Start the Elide Embedded runtime.",
      "Finishes the initialization and configuration process, and starts the underlying runtime.",
    }
  )
  public static int start(IsolateThread thread) {
    assert initialized.get() : "Elide Embedded not initialized; please initialize before starting";
    assert configured.get() : "Elide Embedded not configured; please configure before starting";
    final ElideEmbedded instance = runtime.get();
    assert instance != null;
    try {
      if (instance.start(args) == 0) {
        running.compareAndExchange(false, true);
        return 0;
      } else {
        return 1;
      }
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   *
   * @param thread
   * @param call
   * @param inflight
   * @param cbk
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_call_enter",
    documentation = {}
  )
  public static int enter(IsolateThread thread, SerializedInvocation call, InFlightCall inflight, CallbackPointer cbk) {
    assert configured.get() : "Elide Embedded not configured; please configure before dispatching";
    final ElideEmbedded instance = getActiveRuntime();
    final long callId = getCallId(inflight);
    final ObjectHandle handle = ObjectHandles.getGlobal().create(callId);
    inflight.setCallHandle(handle);
    try {
      final UnaryNativeCall raw = NativeBytes.inflateCall(thread, callId, call);
      return instance.enterDispatch(raw, InFlightCallInfo.of(callId, raw));
    } catch (Exception e) {
      // immediate failure: de-allocate handle
      ObjectHandles.getGlobal().destroy(handle);
      return -1;
    }
  }

  /**
   *
   * @param thread
   * @param inflight
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_call_cancel",
    documentation = {}
  )
  public static int cancel(IsolateThread thread, InFlightCall inflight) {
    assert configured.get() : "Elide Embedded not configured; please configure before dispatching";
    final ElideEmbedded instance = getActiveRuntime();
    final long callId = getCallId(inflight);
    try {
      return instance.dispatchCancel(InFlightCallInfo.of(callId, null));
    } catch (Exception e) {
      // nothing to do
      return -1;
    }
  }

  /**
   *
   * @param thread
   * @param inflight
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_call_poll",
    documentation = {}
  )
  public static int poll(IsolateThread thread, InFlightCall inflight) {
    assert configured.get() : "Elide Embedded not configured; please configure before dispatching";
    final ElideEmbedded instance = getActiveRuntime();
    final long callId = getCallId(inflight);
    try {
      return instance.dispatchPoll(InFlightCallInfo.of(callId, null));
    } catch (Exception e) {
      // nothing to do
      return -1;
    }
  }

  /**
   *
   * @param thread
   * @param call
   * @param inflight
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_call_exit",
    documentation = {}
  )
  public static int exit(IsolateThread thread, SerializedInvocation call, InFlightCall inflight) {
    final ElideEmbedded instance = getActiveRuntime();
    final long callId = getCallId(inflight);
    ObjectHandle handle = inflight.getCallHandle();
    try {
      final UnaryNativeCall raw = NativeBytes.inflateCall(thread, callId, call);
      return instance.exitDispatch(raw, InFlightCallInfo.of(callId, raw));
    } catch (Exception e) {
      // nothing to do
      return -1;
    } finally {
      ObjectHandles.getGlobal().destroy(handle);
    }
  }

  /**
   *
   * @param thread
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_stop",
    documentation = {}
  )
  public static int stop(IsolateThread thread) {
    assert running.get() : "Elide Embedded not running; can't stop a server which isn't running";
    final ElideEmbedded instance = getActiveRuntime();
    try {
      return instance.stop();
    } catch (Exception e) {
      return 1;
    }
  }

  /**
   *
   * @param thread
   * @return
   */
  @CEntryPoint(
    name = "elide_embedded_teardown",
    documentation = {}
  )
  public static int teardown(IsolateThread thread) {
    final ElideEmbedded instance = getActiveRuntime();
    running.compareAndExchange(true, false);
    configured.compareAndExchange(true, false);
    initialized.compareAndExchange(true, false);
    try {
      Isolates.tearDownIsolate(thread);
      return instance.teardown();
    } catch (Exception e) {
      // nothing to do
      return -1;
    } finally {
      // cleanup gate
      protocolVersion.set(DEFAULT_PROTOCOL_VERSION);
      protocolMode.set(DEFAULT_PROTOCOL_MODE);
      runtime.set(null);
    }
  }

  /**
   * ## Native: Entry Function (`v1alpha1`)
   */
  public final CEntryPointLiteral<EntryPointer> entryFunctionV1alpha1 = CEntryPointLiteral.create(
    Entrypoint.class,
    "enter",
    IsolateThread.class,
    SerializedInvocation.class,
    CallbackPointer.class
  );

  // Safely obtain the active runtime.
  private static ElideEmbedded getActiveRuntime() {
    final ElideEmbedded instance = runtime.get();
    assert instance != null : "Cannot obtain active runtime when not initialized";
    assert running.get() : "Cannot obtain active runtime when not running";
    return instance;
  }

  // Retrieve the call ID from an inflight call handle, using the specified context.
  private static long getCallId(InFlightCall inflight, ObjectHandles handles) {
    final ObjectHandle handle = inflight.getCallHandle();
    assert handle != null;
    final Long callId = ObjectHandles.getGlobal().get(handle);
    assert callId != null;
    assert callId > 0;
    return callId;
  }

  // Retrieve the call ID from an inflight call handle, using the global context.
  private static long getCallId(InFlightCall inflight) {
    return getCallId(inflight, ObjectHandles.getGlobal());
  }

  /**
   * Uncaught Native Exception Handler
   *
   * <p>Responsible for handling native exceptions which surface past the point of no return (literally, since JVM
   * exceptions cannot propagate to C).</p>
   */
  private static class UncaughtNativeExceptionHandler implements CEntryPoint.ExceptionHandler {
    /**
     * Notify the handler singleton of an uncaught native exception.
     *
     * <p>The result of this function must remain assignable to all native entrypoints used by this exception
     * handler.</p>
     *
     * @param err The uncaught exception.
     * @return Exit code; always `1` or greater.
     */
    @Uninterruptible(
      reason = "Called from uninterruptible code.",
      mayBeInlined = true
    )
    static int notify(Object err) {
      System.err.println("ERR! Uncaught exception in native code: " + err.toString());
      return 1;  // always returns `1`, which is assigned as a native exit code
    }
  }

  /**
   * Uncaught Main Exception Handler
   *
   * <p>Responsible for handling JVM exceptions which surface past the point of other handling, within the main thread
   * that starts the application.</p>
   */
  private static class UncaughtMainExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread t, Throwable e) {
      err.get().println("ERR! Uncaught exception in main thread: " + e.getMessage());
      e.printStackTrace(err.get());
    }
  }
}

/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

import elide.embedded.NativeApi.*;
import elide.embedded.api.InFlightCallInfo;
import elide.embedded.api.NativeCall;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.Isolates;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CEntryPointLiteral;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.nio.ByteBuffer;

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
@SuppressWarnings({"unused", "ConstantValue"})
@CContext(NativeApi.ElideEmbeddedApi.class)
public class Entrypoint {
  /**
   *
   * @param args
   */
  public static void main(String[] args) {
    ElideEmbedded.main(args);
  }

  /**
   *
   * @param thread
   * @param mode
   * @return
   */
  @CEntryPoint(name = "elide_embedded_initialize")
  public static int initialize(IsolateThread thread, ProtocolMode mode) {
    try {
      return ElideEmbedded.initialize(elide.embedded.api.ProtocolMode.resolve(mode));
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
  @CEntryPoint(name = "elide_embedded_capability")
  public static int capability(IsolateThread thread, Capability capability) {
    try {
      return ElideEmbedded.capability(elide.embedded.api.Capability.resolve(capability));
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
  @CEntryPoint(name = "elide_embedded_configure")
  public static int configure(IsolateThread thread, CCharPointer version, InstanceConfiguration config) {
    try {
      final String apiVersion = CTypeConversion.toJavaString(version);
      return ElideEmbedded.configure(apiVersion, elide.embedded.api.InstanceConfiguration.create(
        NativeBytes.inflateConfig(thread, apiVersion, config)
      ));
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
  @CEntryPoint(name = "elide_embedded_call_enter")
  public static int enter(IsolateThread thread, SerializedInvocation call, InFlightCall inflight, CallbackPointer cbk) {
    final long callId = getCallId(inflight);
    final ObjectHandle handle = ObjectHandles.getGlobal().create(callId);
    inflight.setCallHandle(handle);

    try {
      final NativeCall raw = NativeBytes.inflateCall(thread, callId, call);
      return ElideEmbedded.enterDispatch(raw, InFlightCallInfo.of(callId, raw));
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
  @CEntryPoint(name = "elide_embedded_call_cancel")
  public static int cancel(IsolateThread thread, InFlightCall inflight) {
    final long callId = getCallId(inflight);
    try {
      return ElideEmbedded.dispatchCancel(InFlightCallInfo.of(callId, null));
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
  @CEntryPoint(name = "elide_embedded_call_poll")
  public static int poll(IsolateThread thread, InFlightCall inflight) {
    final long callId = getCallId(inflight);
    try {
      return ElideEmbedded.dispatchPoll(InFlightCallInfo.of(callId, null));
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
  @CEntryPoint(name = "elide_embedded_call_exit")
  public static int exit(IsolateThread thread, SerializedInvocation call, InFlightCall inflight) {
    final long callId = getCallId(inflight);
    ObjectHandle handle = inflight.getCallHandle();
    try {
      final NativeCall raw = NativeBytes.inflateCall(thread, callId, call);
      return ElideEmbedded.exitDispatch(raw, InFlightCallInfo.of(callId, raw));
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
  @CEntryPoint(name = "elide_embedded_teardown")
  public static int teardown(IsolateThread thread) {
    try {
      Isolates.tearDownIsolate(thread);
      return ElideEmbedded.teardown();
    } catch (Exception e) {
      // nothing to do
      return -1;
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

  // Retrieve the call ID from an inflight call handle.
  private static long getCallId(InFlightCall inflight) {
    final ObjectHandle handle = inflight.getCallHandle();
    assert handle != null;
    final Long callId = ObjectHandles.getGlobal().get(handle);
    assert callId != null;
    assert callId > 0;
    return callId;
  }
}

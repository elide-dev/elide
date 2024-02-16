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

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.PointerBase;

import java.util.Collections;
import java.util.List;

/**
 *
 */
@CContext(NativeApi.ElideEmbeddedApi.class)
public final class NativeApi {
  public final static class ElideEmbeddedApi implements CContext.Directives {
    @Override
    public boolean isInConfiguration() {
      return true;
    }

    @Override
    public List<String> getHeaderFiles() {
      return Collections.singletonList(apiHeader());
    }

    private static String apiHeader() {
      final String path = System.getProperty("elide.embedded.headerPath");
      if (path == null || path.isEmpty()) {
        throw new IllegalStateException("Cannot have zero-length or `null` public header path");
      }
      return "\"" + path + "\"";
    }
  }

  /**
   *
   */
  @CEnum("elide_protocol_mode_t")
  public enum ProtocolMode {
    /**
     *
     */
    ELIDE_PROTOBUF,

    /**
     *
     */
    ELIDE_CAPNPROTO;

    /**
     * @return
     */
    @CEnumValue
    public native int getCValue();

    /**
     * @param value
     * @return
     */
    @CEnumLookup
    public static native ProtocolMode fromCValue(int value);
  }

  /**
   *
   */
  @CEnum("elide_invocation_status_t")
  public enum InvocationStatus {
    /**
     *
     */
    ELIDE_INFLIGHT_PENDING,

    /**
     *
     */
    ELIDE_INFLIGHT_EXECUTING,

    /**
     *
     */
    ELIDE_INFLIGHT_ERR,

    /**
     *
     */
    ELIDE_INFLIGHT_COMPLETED;

    /**
     * @return
     */
    @CEnumValue
    public native int getCValue();

    /**
     * @param value
     * @return
     */
    @CEnumLookup
    public static native InvocationStatus fromCValue(int value);
  }

  /**
   *
   */
  @CEnum("elide_embedded_capability_t")
  public enum Capability {
    /**
     *
     */
    ELIDE_BASELINE;

    /**
     * @return
     */
    @CEnumValue
    public native int getCValue();

    /**
     * @param value
     * @return
     */
    @CEnumLookup
    public static native Capability fromCValue(int value);
  }

  /**
   *
   */
  @CStruct("elide_invocation_t")
  public interface SerializedInvocation extends PointerBase {
    /**
     * Unique request ID.
     */
    @CField("f_rq")
    long getRequestId();

    /**
     * Retrieve the active protocol mode.
     */
    @CField("f_mode")
    int getProtocolMode();

    /**
     * Read access to the expected size of the payload.
     */
    @CField("f_size")
    long getSize();

    /**
     * Read access to the outcome status.
     */
    @CField("f_status")
    int getStatus();

    /**
     * Write access to the outcome status.
     */
    @CField("f_status")
    void setStatus(int status);

    /**
     * Acquire the tip of the payload array.
     */
    @CField("fn_tip")
    PayloadTipSupplier getPayloadTipSupplier();

    /**
     * Consume a byte from the payload array at the provided position.
     */
    @CField("fn_consume")
    PayloadByteConsumer getPayloadByteConsumer();

    /**
     * Retrieves the tip pointer to the cycle payload.
     */
    @CFieldAddress("f_payload_tip")
    WordPointer cyclePayload();
  }

  /**
   *
   */
  public interface PayloadTipSupplier extends CFunctionPointer {
    /**
     *
     */
    @InvokeCFunctionPointer
    WordPointer invoke(IsolateThread thread, SerializedInvocation cycle);
  }

  /**
   *
   */
  public interface PayloadByteConsumer extends CFunctionPointer {
    /**
     *
     */
    @InvokeCFunctionPointer
    byte invoke(IsolateThread thread, SerializedInvocation cycle, int index);
  }

  /**
   *
   */
  public interface ConfigByteConsumer extends CFunctionPointer {
    /**
     *
     */
    @InvokeCFunctionPointer
    byte invoke(IsolateThread thread, InstanceConfiguration cycle, int index);
  }

  /**
   *
   */
  public interface ConfigTipSupplier extends CFunctionPointer {
    /**
     *
     */
    @InvokeCFunctionPointer
    WordPointer invoke(IsolateThread thread, InstanceConfiguration cycle);
  }

  /**
   *
   */
  @CStruct("elide_inflight_call_t")
  public interface InFlightCall extends PointerBase {
    /**
     * @return
     */
    @CField("f_call_handle")
    ObjectHandle getCallHandle();

    /**
     * @param handle
     */
    @CField("f_call_handle")
    void setCallHandle(ObjectHandle handle);
  }

  /**
   *
   */
  public interface CallbackPointer extends CFunctionPointer {
    /**
     * @param thread
     * @param call
     * @return
     */
    @InvokeCFunctionPointer
    int invoke(IsolateThread thread, SerializedInvocation call);
  }

  /**
   *
   */
  public interface EntryPointer extends CFunctionPointer {
    /**
     * @param thread
     * @param call
     * @param callback
     * @return
     */
    @InvokeCFunctionPointer
    int invoke(IsolateThread thread, SerializedInvocation call, CallbackPointer callback);
  }

  /**
   *
   */
  @CStruct("elide_configuration_t")
  public interface InstanceConfiguration extends PointerBase {
    /**
     * Retrieve the active protocol mode.
     */
    @CField("f_version")
    CCharPointer getProtocolVersion();

    /**
     * Retrieve the active protocol mode.
     */
    @CField("f_mode")
    int getProtocolMode();

    /**
     * Read access to the expected size of the payload.
     */
    @CField("f_size")
    long getSize();

    /**
     * Acquire the tip of the configuration data array.
     */
    @CField("fn_tip")
    ConfigTipSupplier getConfigTipSupplier();

    /**
     * Consume a byte from the payload array at the provided position.
     */
    @CField("fn_consume")
    ConfigByteConsumer getConfigByteConsumer();

    /**
     * Retrieves the tip pointer to the configuration payload.
     */
    @CFieldAddress("f_config_tip")
    WordPointer getConfigPayload();
  }
}

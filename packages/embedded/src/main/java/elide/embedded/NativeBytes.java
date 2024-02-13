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

import elide.embedded.NativeApi.ConfigByteConsumer;
import elide.embedded.NativeApi.ConfigTipSupplier;
import elide.embedded.NativeApi.InstanceConfiguration;
import elide.embedded.NativeApi.SerializedInvocation;
import elide.embedded.NativeApi.PayloadByteConsumer;
import elide.embedded.NativeApi.PayloadTipSupplier;
import elide.embedded.NativeApi.ProtocolMode;
import elide.embedded.api.UnaryNativeCall;
import elide.embedded.api.NativeConfiguration;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.WordPointer;

import java.nio.ByteBuffer;

/**
 * Utilities for dealing with native byte arrays.
 */
class NativeBytes {
  private static final boolean DEFAULT_WALK_BYTEARRAY = false;

  private NativeBytes() { /* Static use only. */ }

  /**
   * TBD.
   *
   * @param thread
   * @param call
   * @param walk
   * @return
   */
  public static ByteBuffer inflate(IsolateThread thread, SerializedInvocation call, boolean walk) {
    // pointer to the tip of the bytearray
    final long size = call.getSize();
    assert size > 0 : "Cannot allocate a buffer of size 0";
    assert size < (Integer.MAX_VALUE - 1) : "Cannot allocate a buffer larger than 2GB";

    final ByteBuffer buffer;
    if (walk) {
      final PayloadByteConsumer consumer = call.getPayloadByteConsumer();
      final byte[] bytes = new byte[(int) size];
      final byte firstByte = consumer.invoke(thread, call, 0);
      bytes[0] = firstByte;
      for (int i = 1; i < size; i++) {
        bytes[i] = consumer.invoke(thread, call, i);
      }
      buffer = ByteBuffer.wrap(bytes);
    } else {
      final PayloadTipSupplier tipSupplier = call.getPayloadTipSupplier();
      final WordPointer tip = tipSupplier.invoke(thread, call);
      buffer = CTypeConversion.asByteBuffer(tip, (int) size);
    }
    return buffer.asReadOnlyBuffer();
  }

  /**
   * TBD.
   *
   * @param thread
   * @param config
   * @param walk
   * @return
   */
  public static ByteBuffer inflate(IsolateThread thread, InstanceConfiguration config, boolean walk) {
    // pointer to the tip of the bytearray
    final long size = config.getSize();
    assert size > 0 : "Cannot allocate a buffer of size 0";
    assert size < (Integer.MAX_VALUE - 1) : "Cannot allocate a buffer larger than 2GB";

    final ByteBuffer buffer;
    if (walk) {
      final ConfigByteConsumer consumer = config.getConfigByteConsumer();
      final byte[] bytes = new byte[(int) size];
      final byte firstByte = consumer.invoke(thread, config, 0);
      bytes[0] = firstByte;
      for (int i = 1; i < size; i++) {
        bytes[i] = consumer.invoke(thread, config, i);
      }
      buffer = ByteBuffer.wrap(bytes);
    } else {
      final ConfigTipSupplier tipSupplier = config.getConfigTipSupplier();
      final WordPointer tip = tipSupplier.invoke(thread, config);
      buffer = CTypeConversion.asByteBuffer(tip, (int) size);
    }
    return buffer.asReadOnlyBuffer();
  }

  /**
   *
   *
   * @param thread
   * @param call
   * @param walk
   * @return
   */
  public static UnaryNativeCall inflateCall(IsolateThread thread, long callId, SerializedInvocation call, boolean walk) {
    final ByteBuffer buffer = inflate(thread, call, walk);
    final ProtocolMode mode = ProtocolMode.fromCValue(call.getProtocolMode());
    return UnaryNativeCall.of(callId, elide.embedded.api.ProtocolMode.resolve(mode), buffer);
  }

  /**
   *
   *
   * @param thread
   * @param call
   * @return
   */
  public static UnaryNativeCall inflateCall(IsolateThread thread, long callId, SerializedInvocation call) {
    return inflateCall(thread, callId, call, DEFAULT_WALK_BYTEARRAY);
  }

  /**
   *
   *
   * @param thread
   * @param config
   * @param version
   * @param walk
   * @return
   */
  public static NativeConfiguration inflateConfig(
      IsolateThread thread,
      InstanceConfiguration config,
      String version,
      ProtocolMode mode,
      boolean walk
  ) {
    ByteBuffer buffer = inflate(thread, config, walk);
    return NativeConfiguration.of(version, elide.embedded.api.ProtocolMode.resolve(mode), buffer);
  }

  /**
   *
   *
   * @param thread
   * @param version
   * @param config
   * @return
   */
  public static NativeConfiguration inflateConfig(
      IsolateThread thread,
      String version,
      ProtocolMode mode,
      InstanceConfiguration config
  ) {
    return inflateConfig(thread, config, version, mode, DEFAULT_WALK_BYTEARRAY);
  }
}

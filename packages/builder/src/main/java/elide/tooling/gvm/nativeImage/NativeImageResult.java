/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.tooling.gvm.nativeImage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Raised when embedded native-image finishes execution; either expresses an error or a successful
 * result.
 */
public class NativeImageResult extends RuntimeException {
  private final Throwable error;
  private final boolean success;

  private NativeImageResult(Throwable err) {
    super("Native Image execution failed", err, false, true);
    this.error = err;
    this.success = false;
  }

  private NativeImageResult() {
    super("", null, false, false);
    this.error = null;
    this.success = true;
  }

  static @NonNull NativeImageResult success() {
    return new NativeImageResult();
  }

  static @NonNull NativeImageResult failure(Throwable err) {
    return new NativeImageResult(err);
  }

  public @Nullable Throwable getError() {
    return error;
  }

  public boolean isSuccess() {
    return success;
  }
}

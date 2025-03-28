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
package elide.runtime.localai

import java.nio.file.Path

/**
 * ### Model Specification
 *
 * Model specification objects describe the AI model which is used for inference. The engine supports local models
 * which are available on-disk, as well as remote models hosted on platforms like HuggingFace. If a remote model is
 * selected, it will be downloaded and cached locally for use.
 *
 * See [OnDiskModel] and [HuggingFaceModel] for more information.
 *
 * When selecting a model, certain parameters or other inputs may need alignment with expected values. Consult the
 * model's documentation for more information.
 */
public sealed interface Model {
  /**
   * Local (On-disk) Model
   *
   * Describes a model which already resides on-disk at a known path.
   *
   * @property path The path to the model on disk.
   * @constructor Create a local model specification.
   */
  @JvmInline public value class OnDiskModel internal constructor (public val path: Path) : Model

  /**
   * Remote (HuggingFace) model.
   *
   * Describes a model which is hosted on HuggingFace, and must be downloaded and cached for local use; the parameters
   * [repo] and [name] are used to identify the model, and [path] is the local path where the model is cached.
   *
   * @property repo The HuggingFace repository where the model is hosted.
   * @property name The name of the model within the repository.
   * @property path The local path where the model is cached; if `null`, one will be calculated at runtime.
   * @constructor Create a HuggingFace model specification.
   */
  @JvmRecord public data class HuggingFaceModel internal constructor (
    public val repo: String,
    public val name: String,
    public val path: Path? = null,
  ) : Model

  /** Factories for model specifications. */
  public companion object {
    /** @return Configured local path for a model. */
    @JvmStatic public fun atPath(path: Path): OnDiskModel = OnDiskModel(path)

    /** @return Configured HuggingFace parameters for a model. */
    @JvmStatic public fun huggingface(repo: String, name: String, path: Path? = null): HuggingFaceModel {
      return HuggingFaceModel(
        repo = repo,
        name = name,
        path = path,
      )
    }
  }
}

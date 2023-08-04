/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.intrinsics.js

import io.micronaut.http.HttpRequest
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchRequestIntrinsic
import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
public interface FetchMutableRequest : FetchRequest {
  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var headers: FetchHeaders

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var method: String

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var url: String

  /**
   * TBD.
   */
  public interface RequestFactory<Impl: FetchRequest> {
    /**
     * TBD.
     */
    public fun forRequest(request: HttpRequest<*>): Impl
  }

  /**
   * TBD.
   */
  public companion object DefaultFactory : RequestFactory<FetchMutableRequest> {
    /**
     * TBD.
     */
    @JvmStatic override fun forRequest(request: HttpRequest<*>): FetchMutableRequest {
      return FetchRequestIntrinsic.forRequest(request)
    }
  }
}

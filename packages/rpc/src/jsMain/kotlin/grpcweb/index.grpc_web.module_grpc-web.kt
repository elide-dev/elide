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

@file:JsModule("grpc-web")
@file:JsNonModule
@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "ENUM_CLASS_IN_EXTERNAL_DECLARATION_WARNING",
  "DEPRECATION",
  "unused",
)

package grpcweb

import kotlin.js.Json
import kotlin.js.Promise

public external interface Metadata {
    @nativeGetter
    public operator fun get(s: String): String?
    @nativeSetter
    public operator fun set(s: String, value: String)
}

public open external class AbstractClientBase {
  public open fun <REQ, RESP> thenableCall(method: String, request: REQ, metadata: Metadata, methodDescriptor: MethodDescriptor<REQ, RESP>): Promise<RESP>
  public open fun <REQ, RESP> rpcCall(method: String, request: REQ, metadata: Metadata, methodDescriptor: MethodDescriptor<REQ, RESP>, callback: (err: RpcError, response: RESP) -> Unit): ClientReadableStream<RESP>
  public open fun <REQ, RESP> serverStreaming(method: String, request: REQ, metadata: Metadata, methodDescriptor: MethodDescriptor<REQ, RESP>): ClientReadableStream<RESP>
}

public open external class ClientReadableStream<RESP> {
  public open fun on(eventType: String /* "error" */, callback: (err: RpcError) -> Unit): ClientReadableStream<RESP>
  public open fun on(eventType: String /* "status" */, callback: (status: Status) -> Unit): ClientReadableStream<RESP>
  public open fun on(eventType: String /* "metadata" */, callback: (status: Metadata) -> Unit): ClientReadableStream<RESP>
  public open fun on(eventType: String /* "data" */, callback: (response: RESP) -> Unit): ClientReadableStream<RESP>
  public open fun on(eventType: String /* "end" */, callback: () -> Unit): ClientReadableStream<RESP>
  public open fun removeListener(eventType: String /* "error" */, callback: (err: RpcError) -> Unit)
  public open fun removeListener(eventType: String /* "status" */, callback: (status: Status) -> Unit)
  public open fun removeListener(eventType: String /* "metadata" */, callback: (status: Metadata) -> Unit)
  public open fun removeListener(eventType: String /* "data" */, callback: (response: RESP) -> Unit)
  public open fun removeListener(eventType: String /* "end" */, callback: () -> Unit)
  public open fun cancel()
}

public external interface StreamInterceptor<REQ, RESP> {
  public fun intercept(request: Request<REQ, RESP>, invoker: (request: Request<REQ, RESP>) -> ClientReadableStream<RESP>): ClientReadableStream<RESP>
}

public external interface UnaryInterceptor<REQ, RESP> {
  public fun intercept(request: Request<REQ, RESP>, invoker: (request: Request<REQ, RESP>) -> Promise<UnaryResponse<REQ, RESP>>): Promise<UnaryResponse<REQ, RESP>>
}

public open external class CallOptions(options: Json)

public open external class MethodDescriptor<REQ, RESP>(name: String, methodType: String, requestType: Any, responseType: Any, requestSerializeFn: Any, responseDeserializeFn: Any) {
  public open fun createRequest(requestMessage: REQ, metadata: Metadata = definedExternally, callOptions: CallOptions = definedExternally): Request<REQ, RESP>
  public open fun createUnaryResponse(responseMessage: RESP, metadata: Metadata = definedExternally, status: Status = definedExternally): UnaryResponse<REQ, RESP>
  public open fun getName(): String
  public open fun getMethodType(): String
  public open fun getRequestMessageCtor(): Any
  public open fun getResponseMessageCtor(): Any
  public open fun getRequestSerializeFn(): Any
  public open fun getResponseDeserializeFn(): Any
}

public open external class Request<REQ, RESP> {
  public open fun getRequestMessage(): REQ
  public open fun getMethodDescriptor(): MethodDescriptor<REQ, RESP>
  public open fun getMetadata(): Metadata
  public open fun getCallOptions(): CallOptions
}

public open external class UnaryResponse<REQ, RESP> {
  public open fun getResponseMessage(): RESP
  public open fun getMetadata(): Metadata
  public open fun getMethodDescriptor(): MethodDescriptor<REQ, RESP>
  public open fun getStatus(): Status
}

public external interface GrpcWebClientBaseOptions {
  public var format: String?
        get() = definedExternally
        set(value) = definedExternally
  public var suppressCorsPreflight: Boolean?
        get() = definedExternally
        set(value) = definedExternally
  public var withCredentials: Boolean?
        get() = definedExternally
        set(value) = definedExternally
  public var unaryInterceptors: Array<UnaryInterceptor<Any, Any>>?
        get() = definedExternally
        set(value) = definedExternally
  public var streamInterceptors: Array<StreamInterceptor<Any, Any>>?
        get() = definedExternally
        set(value) = definedExternally
}

public open external class GrpcWebClientBase(options: GrpcWebClientBaseOptions = definedExternally) : AbstractClientBase

public external interface Status {
  public var code: Number
  public var details: String
  public var metadata: Metadata?
        get() = definedExternally
        set(value) = definedExternally
}

public external enum class StatusCode {
    ABORTED,
    ALREADY_EXISTS,
    CANCELLED,
    DATA_LOSS,
    DEADLINE_EXCEEDED,
    FAILED_PRECONDITION,
    INTERNAL,
    INVALID_ARGUMENT,
    NOT_FOUND,
    OK,
    OUT_OF_RANGE,
    PERMISSION_DENIED,
    RESOURCE_EXHAUSTED,
    UNAUTHENTICATED,
    UNAVAILABLE,
    UNIMPLEMENTED,
    UNKNOWN
}

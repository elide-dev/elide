@file:JsModule("grpc-web")
@file:JsNonModule
@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
  "DEPRECATION",
)

package lib.grpcweb

import kotlin.js.*

external interface Metadata {
    @nativeGetter
    operator fun get(s: String): String?
    @nativeSetter
    operator fun set(s: String, value: String)
}

open external class AbstractClientBase {
    open fun <REQ, RESP> thenableCall(method: String, request: REQ, metadata: Metadata, methodDescriptor: MethodDescriptor<REQ, RESP>): Promise<RESP>
    open fun <REQ, RESP> rpcCall(method: String, request: REQ, metadata: Metadata, methodDescriptor: MethodDescriptor<REQ, RESP>, callback: (err: RpcError, response: RESP) -> Unit): ClientReadableStream<RESP>
    open fun <REQ, RESP> serverStreaming(method: String, request: REQ, metadata: Metadata, methodDescriptor: MethodDescriptor<REQ, RESP>): ClientReadableStream<RESP>
}

open external class ClientReadableStream<RESP> {
    open fun on(eventType: String /* "error" */, callback: (err: RpcError) -> Unit): ClientReadableStream<RESP>
    open fun on(eventType: String /* "status" */, callback: (status: Status) -> Unit): ClientReadableStream<RESP>
    open fun on(eventType: String /* "metadata" */, callback: (status: Metadata) -> Unit): ClientReadableStream<RESP>
    open fun on(eventType: String /* "data" */, callback: (response: RESP) -> Unit): ClientReadableStream<RESP>
    open fun on(eventType: String /* "end" */, callback: () -> Unit): ClientReadableStream<RESP>
    open fun removeListener(eventType: String /* "error" */, callback: (err: RpcError) -> Unit)
    open fun removeListener(eventType: String /* "status" */, callback: (status: Status) -> Unit)
    open fun removeListener(eventType: String /* "metadata" */, callback: (status: Metadata) -> Unit)
    open fun removeListener(eventType: String /* "data" */, callback: (response: RESP) -> Unit)
    open fun removeListener(eventType: String /* "end" */, callback: () -> Unit)
    open fun cancel()
}

external interface StreamInterceptor<REQ, RESP> {
    fun intercept(request: Request<REQ, RESP>, invoker: (request: Request<REQ, RESP>) -> ClientReadableStream<RESP>): ClientReadableStream<RESP>
}

external interface UnaryInterceptor<REQ, RESP> {
    fun intercept(request: Request<REQ, RESP>, invoker: (request: Request<REQ, RESP>) -> Promise<UnaryResponse<REQ, RESP>>): Promise<UnaryResponse<REQ, RESP>>
}

open external class CallOptions(options: Json)

open external class MethodDescriptor<REQ, RESP>(name: String, methodType: String, requestType: Any, responseType: Any, requestSerializeFn: Any, responseDeserializeFn: Any) {
    open fun createRequest(requestMessage: REQ, metadata: Metadata = definedExternally, callOptions: CallOptions = definedExternally): Request<REQ, RESP>
    open fun createUnaryResponse(responseMessage: RESP, metadata: Metadata = definedExternally, status: Status = definedExternally): UnaryResponse<REQ, RESP>
    open fun getName(): String
    open fun getMethodType(): String
    open fun getRequestMessageCtor(): Any
    open fun getResponseMessageCtor(): Any
    open fun getRequestSerializeFn(): Any
    open fun getResponseDeserializeFn(): Any
}

open external class Request<REQ, RESP> {
    open fun getRequestMessage(): REQ
    open fun getMethodDescriptor(): MethodDescriptor<REQ, RESP>
    open fun getMetadata(): Metadata
    open fun getCallOptions(): CallOptions
}

open external class UnaryResponse<REQ, RESP> {
    open fun getResponseMessage(): RESP
    open fun getMetadata(): Metadata
    open fun getMethodDescriptor(): MethodDescriptor<REQ, RESP>
    open fun getStatus(): Status
}

external interface GrpcWebClientBaseOptions {
    var format: String?
        get() = definedExternally
        set(value) = definedExternally
    var suppressCorsPreflight: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var withCredentials: Boolean?
        get() = definedExternally
        set(value) = definedExternally
    var unaryInterceptors: Array<UnaryInterceptor<Any, Any>>?
        get() = definedExternally
        set(value) = definedExternally
    var streamInterceptors: Array<StreamInterceptor<Any, Any>>?
        get() = definedExternally
        set(value) = definedExternally
}

open external class GrpcWebClientBase(options: GrpcWebClientBaseOptions = definedExternally) : AbstractClientBase

external interface Status {
    var code: Number
    var details: String
    var metadata: Metadata?
        get() = definedExternally
        set(value) = definedExternally
}

external enum class StatusCode {
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

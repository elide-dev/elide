@file:JsModule("uuid")
@file:JsNonModule

@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
  "PropertyName",
  "ClassName",
)

package lib.uuid

import kotlin.js.*
import lib.tsstdlib.ArrayLike

external interface RandomOptions {
    var random: ArrayLike<Number>?
        get() = definedExternally
        set(value) = definedExternally
}

external interface RngOptions {
    var rng: (() -> ArrayLike<Number>)?
        get() = definedExternally
        set(value) = definedExternally
}

external interface V1BaseOptions {
    var node: ArrayLike<Number>?
        get() = definedExternally
        set(value) = definedExternally
    var clockseq: Number?
        get() = definedExternally
        set(value) = definedExternally
    var msecs: dynamic /* Number? | Date? */
        get() = definedExternally
        set(value) = definedExternally
    var nsecs: Number?
        get() = definedExternally
        set(value) = definedExternally
}

external interface V1RandomOptions : V1BaseOptions, RandomOptions

external interface V1RngOptions : V1BaseOptions, RngOptions

external interface v3Static {
  var DNS: String
  var URL: String
}

external interface v5Static {
  var DNS: String
  var URL: String
}

external var NIL: String

external var parse: (uuid: String) -> ArrayLike<Number>

external var stringify: (buffer: ArrayLike<Number>, offset: Number) -> String

external var v1: (options: dynamic /* V1RandomOptions | V1RngOptions */) -> String /* v1String */

external var v3: (name: dynamic /* String | InputBuffer */, namespace: dynamic /* String | InputBuffer */) -> String /* v3String */

external var v4: (options: dynamic /* RandomOptions | RngOptions */) -> String /* v4String */

external var v5: (name: dynamic /* String | InputBuffer */, namespace: dynamic /* String | InputBuffer */) -> String /* v5String */

external var validate: (uuid: String) -> Boolean

external var version: (uuid: String) -> Number

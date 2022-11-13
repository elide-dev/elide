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

public external interface RandomOptions {
  public var random: ArrayLike<Number>?
    get() = definedExternally
    set(value) = definedExternally
}

public external interface RngOptions {
  public var rng: (() -> ArrayLike<Number>)?
    get() = definedExternally
    set(value) = definedExternally
}

public external interface V1BaseOptions {
  public var node: ArrayLike<Number>?
    get() = definedExternally
    set(value) = definedExternally
  public var clockseq: Number?
    get() = definedExternally
    set(value) = definedExternally
  public var msecs: dynamic /* Number? | Date? */
    get() = definedExternally
    set(value) = definedExternally
  public var nsecs: Number?
    get() = definedExternally
    set(value) = definedExternally
}

public external interface V1RandomOptions : V1BaseOptions, RandomOptions

public external interface V1RngOptions : V1BaseOptions, RngOptions

public external interface v3Static {
  public var DNS: String
  public var URL: String
}

public external interface v5Static {
  public var DNS: String
  public var URL: String
}

public external var NIL: String

public external var parse: (uuid: String) -> ArrayLike<Number>

public external var stringify: (buffer: ArrayLike<Number>, offset: Number) -> String

public external var v1: (options: dynamic /* V1RandomOptions | V1RngOptions */) -> String /* v1String */

public external var v3: (name: dynamic /* String | InputBuffer */, namespace: dynamic /* String | InputBuffer */) -> String /* v3String */

public external var v4: (options: dynamic /* RandomOptions | RngOptions */) -> String /* v4String */

public external var v5: (name: dynamic /* String | InputBuffer */, namespace: dynamic /* String | InputBuffer */) -> String /* v5String */

public external var validate: (uuid: String) -> Boolean

public external var version: (uuid: String) -> Number

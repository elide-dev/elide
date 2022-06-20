@file:JsQualifier("BinaryConstants")
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")
package lib.protobuf

import kotlin.js.*

external enum class FieldType {
    INVALID /* = -1 */,
    DOUBLE /* = 1 */,
    FLOAT /* = 2 */,
    INT64 /* = 3 */,
    UINT64 /* = 4 */,
    INT32 /* = 5 */,
    FIXED64 /* = 6 */,
    FIXED32 /* = 7 */,
    BOOL /* = 8 */,
    STRING /* = 9 */,
    GROUP /* = 10 */,
    MESSAGE /* = 11 */,
    BYTES /* = 12 */,
    UINT32 /* = 13 */,
    ENUM /* = 14 */,
    SFIXED32 /* = 15 */,
    SFIXED64 /* = 16 */,
    SINT32 /* = 17 */,
    SINT64 /* = 18 */,
    FHASH64 /* = 30 */,
    VHASH64 /* = 31 */
}

external enum class WireType {
    INVALID /* = -1 */,
    VARINT /* = 0 */,
    FIXED64 /* = 1 */,
    DELIMITED /* = 2 */,
    START_GROUP /* = 3 */,
    END_GROUP /* = 4 */,
    FIXED32 /* = 5 */
}

external var FieldTypeToWireType: (fieldType: FieldType) -> WireType

external var INVALID_FIELD_NUMBER: Number

external var FLOAT32_EPS: Number

external var FLOAT32_MIN: Number

external var FLOAT32_MAX: Number

external var FLOAT64_EPS: Number

external var FLOAT64_MIN: Number

external var FLOAT64_MAX: Number

external var TWO_TO_20: Number

external var TWO_TO_23: Number

external var TWO_TO_31: Number

external var TWO_TO_32: Number

external var TWO_TO_52: Number

external var TWO_TO_63: Number

external var TWO_TO_64: Number

external var ZERO_HASH: String
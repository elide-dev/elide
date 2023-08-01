@file:JsQualifier("BinaryConstants")
@file:Suppress(
  "INTERFACE_WITH_SUPERCLASS",
  "OVERRIDING_FINAL_MEMBER",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
  "CONFLICTING_OVERLOADS",
  "unused",
)

package lib.protobuf

import kotlin.js.*

public external enum class FieldType {
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
  VHASH64, /* = 31 */
}

public external enum class WireType {
  INVALID /* = -1 */,
  VARINT /* = 0 */,
  FIXED64 /* = 1 */,
  DELIMITED /* = 2 */,
  START_GROUP /* = 3 */,
  END_GROUP /* = 4 */,
  FIXED32, /* = 5 */
}

public external var FieldTypeToWireType: (fieldType: FieldType) -> WireType

public external var INVALID_FIELD_NUMBER: Number

public external var FLOAT32_EPS: Number

public external var FLOAT32_MIN: Number

public external var FLOAT32_MAX: Number

public external var FLOAT64_EPS: Number

public external var FLOAT64_MIN: Number

public external var FLOAT64_MAX: Number

public external var TWO_TO_20: Number

public external var TWO_TO_23: Number

public external var TWO_TO_31: Number

public external var TWO_TO_32: Number

public external var TWO_TO_52: Number

public external var TWO_TO_63: Number

public external var TWO_TO_64: Number

public external var ZERO_HASH: String

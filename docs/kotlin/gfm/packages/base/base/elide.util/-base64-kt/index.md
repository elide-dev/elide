//[base](../../../index.md)/[elide.util](../index.md)/[Base64Kt](index.md)

# Base64Kt

[common]\
object [Base64Kt](index.md)

This class consists exclusively of static methods for obtaining encoders and decoders for the Base64 encoding scheme. The implementation of this class supports the following types of Base64 as specified in [RFC 4648](http://www.ietf.org/rfc/rfc4648.txt) and [RFC 2045](http://www.ietf.org/rfc/rfc2045.txt).

- 
   <a id="basic">**Basic**</a>

Uses &quot;The Base64 Alphabet&quot; as specified in Table 1 of RFC 4648 and RFC 2045 for encoding and decoding operation. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.

- 
   <a id="url">**URL and Filename safe**</a>

Uses the &quot;URL and Filename safe Base64 Alphabet&quot; as specified in Table 2 of RFC 4648 for encoding and decoding. The encoder does not add any line feed (line separator) character. The decoder rejects data that contains characters outside the base64 alphabet.

- 
   <a id="mime">**MIME**</a>

Uses &quot;The Base64 Alphabet&quot; as specified in Table 1 of RFC 2045 for encoding and decoding operation. The encoded output must be represented in lines of no more than 76 characters each and uses a carriage return `'\r'` followed immediately by a linefeed `'\n'` as the line separator. No line separator is added to the end of the encoded output. All line separators or other characters not found in the base64 alphabet table are ignored in decoding operation.

Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to be thrown.

#### Author

Xueming Shen

#### Since

1.8

## Types

| Name | Summary |
|---|---|
| [Decoder](-decoder/index.md) | [common]<br>class [Decoder](-decoder/index.md)<br>This class implements a decoder for decoding byte data using the Base64 encoding scheme as specified in RFC 4648 and RFC 2045. |
| [Encoder](-encoder/index.md) | [common]<br>class [Encoder](-encoder/index.md)<br>This class implements an encoder for encoding byte data using the Base64 encoding scheme as specified in RFC 4648 and RFC 2045. |

## Properties

| Name | Summary |
|---|---|
| [decoder](decoder.md) | [common]<br>val [decoder](decoder.md): [Base64Kt.Decoder](-decoder/index.md)<br>Returns a [Decoder](-decoder/index.md) that decodes using the #basic type base64 encoding scheme. |
| [encoder](encoder.md) | [common]<br>val [encoder](encoder.md): [Base64Kt.Encoder](-encoder/index.md)<br>Returns a [Encoder](-encoder/index.md) that encodes using the #basic type base64 encoding scheme. |

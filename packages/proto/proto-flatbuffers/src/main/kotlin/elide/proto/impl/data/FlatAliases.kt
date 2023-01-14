package elide.proto.impl.data


/** Use a native Kotlin enumeration for hash algorithms. */
internal typealias HashAlgorithm = FlatHashAlgorithm

/** Symbol for different hash algorithms (integer used). */
internal typealias HashAlgorithmSymbol = Int

/** Use a native Kotlin enumeration for encodings. */
internal typealias Encoding = FlatEncoding

/** Symbol for different encodings (integer used). */
internal typealias EncodingSymbol = Int

/** Unwrap the provided [FlatHashAlgorithm] enum to its expected constant value. */
internal fun FlatHashAlgorithm.unwrap(): HashAlgorithmSymbol {
  return this.symbol
}

/** Unwrap the provided [FlatEncoding] enum to its expected constant value. */
internal fun FlatEncoding.unwrap(): HashAlgorithmSymbol {
  return this.symbol
}

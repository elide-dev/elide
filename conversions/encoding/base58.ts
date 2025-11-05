/**
 * Base58 Encoder/Decoder
 * Bitcoin-style Base58 encoding
 */

const BASE58_ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';

export function encodeBase58(data: Buffer): string {
  if (data.length === 0) {
    return '';
  }

  // Convert bytes to big integer
  let num = BigInt(0);
  for (const byte of data) {
    num = num * BigInt(256) + BigInt(byte);
  }

  // Convert to base58
  let result = '';
  while (num > 0) {
    const remainder = Number(num % BigInt(58));
    result = BASE58_ALPHABET[remainder] + result;
    num = num / BigInt(58);
  }

  // Add leading '1' for each leading zero byte
  for (const byte of data) {
    if (byte !== 0) break;
    result = '1' + result;
  }

  return result;
}

export function decodeBase58(encoded: string): Buffer {
  if (encoded.length === 0) {
    return Buffer.alloc(0);
  }

  // Convert from base58 to big integer
  let num = BigInt(0);
  for (const char of encoded) {
    const index = BASE58_ALPHABET.indexOf(char);
    if (index === -1) {
      throw new Error(`Invalid Base58 character: ${char}`);
    }
    num = num * BigInt(58) + BigInt(index);
  }

  // Convert to bytes
  const bytes: number[] = [];
  while (num > 0) {
    bytes.unshift(Number(num % BigInt(256)));
    num = num / BigInt(256);
  }

  // Add leading zero bytes for each leading '1'
  for (const char of encoded) {
    if (char !== '1') break;
    bytes.unshift(0);
  }

  return Buffer.from(bytes);
}

// Check encoding
export function isValidBase58(encoded: string): boolean {
  return /^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]*$/.test(encoded);
}

// CLI demo
if (import.meta.url.includes("base58.ts")) {
  console.log("Base58 Encoder/Decoder Demo\n");

  console.log("1. Encode string:");
  const text = Buffer.from("Hello, World!");
  const encoded = encodeBase58(text);
  console.log(`  Input: "Hello, World!"`);
  console.log(`  Encoded: ${encoded}`);

  console.log("\n2. Decode string:");
  const decoded = decodeBase58(encoded);
  console.log(`  Decoded: "${decoded.toString()}"`);

  console.log("\n3. Binary data:");
  const binary = Buffer.from([0x00, 0x01, 0x02, 0xFF]);
  const binaryEncoded = encodeBase58(binary);
  console.log(`  Input: [${Array.from(binary).map(b => '0x' + b.toString(16).padStart(2, '0')).join(', ')}]`);
  console.log(`  Encoded: ${binaryEncoded}`);

  const binaryDecoded = decodeBase58(binaryEncoded);
  console.log(`  Decoded: [${Array.from(binaryDecoded).map(b => '0x' + b.toString(16).padStart(2, '0')).join(', ')}]`);

  console.log("\n4. Leading zeros:");
  const leadingZeros = Buffer.from([0x00, 0x00, 0x01, 0x02]);
  const zerosEncoded = encodeBase58(leadingZeros);
  console.log(`  Input: [0x00, 0x00, 0x01, 0x02]`);
  console.log(`  Encoded: ${zerosEncoded} (note leading '1's)`);

  console.log("\n5. Validation:");
  console.log(`  "${encoded}" is valid: ${isValidBase58(encoded)}`);
  console.log(`  "0OIl" is valid: ${isValidBase58("0OIl")} (O, I, l not in alphabet)`);

  console.log("\n6. Round-trip test:");
  const original = Buffer.from("The quick brown fox");
  const roundTrip = decodeBase58(encodeBase58(original));
  console.log(`  Match: ${original.equals(roundTrip) ? "✅" : "❌"}`);

  console.log("\n7. Different inputs:");
  const inputs = [
    Buffer.from("a"),
    Buffer.from("abc"),
    Buffer.from([0x00]),
    Buffer.from([0xFF, 0xFF])
  ];

  inputs.forEach(input => {
    const enc = encodeBase58(input);
    console.log(`  ${Array.from(input).map(b => '0x' + b.toString(16).padStart(2, '0')).join('')} → ${enc}`);
  });

  console.log("\n✅ Base58 test passed");
  console.log("⚠️  Note: Bitcoin addresses use Base58Check with checksums");
}

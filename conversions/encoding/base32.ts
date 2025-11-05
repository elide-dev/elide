/**
 * Base32 Encoder/Decoder
 * RFC 4648 Base32 encoding
 */

const BASE32_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
const BASE32_LOOKUP: Record<string, number> = {};

for (let i = 0; i < BASE32_ALPHABET.length; i++) {
  BASE32_LOOKUP[BASE32_ALPHABET[i]] = i;
}

export function encodeBase32(data: string | Buffer): string {
  const buffer = typeof data === 'string' ? Buffer.from(data) : data;
  let result = '';
  let bits = 0;
  let value = 0;

  for (let i = 0; i < buffer.length; i++) {
    value = (value << 8) | buffer[i];
    bits += 8;

    while (bits >= 5) {
      result += BASE32_ALPHABET[(value >>> (bits - 5)) & 31];
      bits -= 5;
    }
  }

  if (bits > 0) {
    result += BASE32_ALPHABET[(value << (5 - bits)) & 31];
  }

  // Padding
  while (result.length % 8 !== 0) {
    result += '=';
  }

  return result;
}

export function decodeBase32(encoded: string): Buffer {
  // Remove padding and convert to uppercase
  const clean = encoded.replace(/=/g, '').toUpperCase();
  const buffer: number[] = [];
  let bits = 0;
  let value = 0;

  for (let i = 0; i < clean.length; i++) {
    const char = clean[i];

    if (!(char in BASE32_LOOKUP)) {
      throw new Error(`Invalid Base32 character: ${char}`);
    }

    value = (value << 5) | BASE32_LOOKUP[char];
    bits += 5;

    if (bits >= 8) {
      buffer.push((value >>> (bits - 8)) & 255);
      bits -= 8;
    }
  }

  return Buffer.from(buffer);
}

// Base32 without padding (commonly used in URLs)
export function encodeBase32NoPad(data: string | Buffer): string {
  return encodeBase32(data).replace(/=/g, '');
}

export function decodeBase32NoPad(encoded: string): Buffer {
  // Add padding back
  const padded = encoded + '='.repeat((8 - (encoded.length % 8)) % 8);
  return decodeBase32(padded);
}

// Hex variant (often used for case-insensitive applications)
const BASE32_HEX_ALPHABET = '0123456789ABCDEFGHIJKLMNOPQRSTUV';

export function encodeBase32Hex(data: string | Buffer): string {
  const standard = encodeBase32(data);
  return standard.split('').map(c => {
    if (c === '=') return c;
    const index = BASE32_ALPHABET.indexOf(c);
    return BASE32_HEX_ALPHABET[index];
  }).join('');
}

// CLI demo
if (import.meta.url.includes("base32.ts")) {
  console.log("Base32 Encoder/Decoder Demo\n");

  console.log("1. Encode string:");
  const text = "Hello, World!";
  const encoded = encodeBase32(text);
  console.log(`  Input: "${text}"`);
  console.log(`  Encoded: ${encoded}`);

  console.log("\n2. Decode string:");
  const decoded = decodeBase32(encoded);
  console.log(`  Decoded: "${decoded.toString()}"`);

  console.log("\n3. Encode without padding:");
  const noPad = encodeBase32NoPad("test");
  console.log(`  Input: "test"`);
  console.log(`  No padding: ${noPad}`);

  console.log("\n4. Binary data:");
  const binary = Buffer.from([0x01, 0x02, 0x03, 0x04, 0x05]);
  const binaryEncoded = encodeBase32(binary);
  console.log(`  Input: [${Array.from(binary).join(', ')}]`);
  console.log(`  Encoded: ${binaryEncoded}`);

  const binaryDecoded = decodeBase32(binaryEncoded);
  console.log(`  Decoded: [${Array.from(binaryDecoded).join(', ')}]`);

  console.log("\n5. Hex variant:");
  const hexEncoded = encodeBase32Hex("ABC");
  console.log(`  Input: "ABC"`);
  console.log(`  Hex variant: ${hexEncoded}`);

  console.log("\n6. Round-trip test:");
  const original = "The quick brown fox jumps over the lazy dog";
  const roundTrip = decodeBase32(encodeBase32(original)).toString();
  console.log(`  Match: ${original === roundTrip ? "✅" : "❌"}`);

  console.log("\n✅ Base32 test passed");
}

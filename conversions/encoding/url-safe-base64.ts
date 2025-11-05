/**
 * URL-Safe Base64
 * RFC 4648 Base64URL encoding/decoding
 */

export function encodeBase64URL(data: string | Buffer): string {
  const buffer = typeof data === 'string' ? Buffer.from(data) : data;

  return buffer.toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

export function decodeBase64URL(encoded: string): Buffer {
  // Restore standard base64
  let base64 = encoded
    .replace(/-/g, '+')
    .replace(/_/g, '/');

  // Add padding
  while (base64.length % 4) {
    base64 += '=';
  }

  return Buffer.from(base64, 'base64');
}

// Standard base64 with padding
export function encodeBase64(data: string | Buffer): string {
  const buffer = typeof data === 'string' ? Buffer.from(data) : data;
  return buffer.toString('base64');
}

export function decodeBase64(encoded: string): Buffer {
  return Buffer.from(encoded, 'base64');
}

// Check if string is valid base64
export function isValidBase64(str: string): boolean {
  try {
    const decoded = Buffer.from(str, 'base64');
    return Buffer.from(decoded.toString('base64')) .toString('base64') === str;
  } catch {
    return false;
  }
}

// Check if string is valid base64url
export function isValidBase64URL(str: string): boolean {
  return /^[A-Za-z0-9_-]*$/.test(str);
}

// Convert between standard and URL-safe
export function toBase64URL(standard: string): string {
  return standard
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

export function fromBase64URL(urlSafe: string): string {
  let base64 = urlSafe
    .replace(/-/g, '+')
    .replace(/_/g, '/');

  while (base64.length % 4) {
    base64 += '=';
  }

  return base64;
}

// Data URI helpers
export function createDataURI(data: Buffer | string, mimeType: string = 'text/plain'): string {
  const base64 = encodeBase64(data);
  return `data:${mimeType};base64,${base64}`;
}

export function parseDataURI(uri: string): { mimeType: string; data: Buffer } | null {
  const match = uri.match(/^data:([^;]+);base64,(.+)$/);

  if (!match) {
    return null;
  }

  return {
    mimeType: match[1],
    data: decodeBase64(match[2])
  };
}

// CLI demo
if (import.meta.url.includes("url-safe-base64.ts")) {
  console.log("URL-Safe Base64 Demo\n");

  console.log("1. Standard Base64:");
  const text = "Hello, World!";
  const standard = encodeBase64(text);
  console.log(`  Input: "${text}"`);
  console.log(`  Base64: ${standard}`);

  console.log("\n2. URL-safe Base64:");
  const urlSafe = encodeBase64URL(text);
  console.log(`  URL-safe: ${urlSafe}`);
  console.log(`  (no +, /, or = characters)`);

  console.log("\n3. Decode URL-safe:");
  const decoded = decodeBase64URL(urlSafe);
  console.log(`  Decoded: "${decoded.toString()}"`);

  console.log("\n4. Special characters:");
  const special = ">>>???<<<";
  const specialStd = encodeBase64(special);
  const specialURL = encodeBase64URL(special);
  console.log(`  Input: "${special}"`);
  console.log(`  Standard: ${specialStd}`);
  console.log(`  URL-safe: ${specialURL}`);

  console.log("\n5. Convert between formats:");
  const converted = toBase64URL(specialStd);
  console.log(`  Converted: ${converted}`);
  console.log(`  Match: ${converted === specialURL ? "✅" : "❌"}`);

  console.log("\n6. Validation:");
  console.log(`  "${urlSafe}" is valid URL-safe: ${isValidBase64URL(urlSafe)}`);
  console.log(`  "${specialStd}" is valid URL-safe: ${isValidBase64URL(specialStd)}`);

  console.log("\n7. Data URI:");
  const image = Buffer.from([0xFF, 0xD8, 0xFF, 0xE0]); // JPEG header
  const dataURI = createDataURI(image, 'image/jpeg');
  console.log(`  Data URI: ${dataURI.substring(0, 50)}...`);

  const parsed = parseDataURI(dataURI);
  console.log(`  MIME type: ${parsed?.mimeType}`);
  console.log(`  Data length: ${parsed?.data.length} bytes`);

  console.log("\n8. Round-trip test:");
  const original = "The quick brown fox jumps over the lazy dog";
  const roundTrip = decodeBase64URL(encodeBase64URL(original)).toString();
  console.log(`  Match: ${original === roundTrip ? "✅" : "❌"}`);

  console.log("\n9. Binary data:");
  const binary = Buffer.from([0x00, 0x01, 0x02, 0xFF, 0xFE, 0xFD]);
  const binaryEncoded = encodeBase64URL(binary);
  console.log(`  Input: [${Array.from(binary).map(b => '0x' + b.toString(16).padStart(2, '0')).join(', ')}]`);
  console.log(`  Encoded: ${binaryEncoded}`);

  const binaryDecoded = decodeBase64URL(binaryEncoded);
  console.log(`  Match: ${binary.equals(binaryDecoded) ? "✅" : "❌"}`);

  console.log("\n✅ URL-safe Base64 test passed");
}

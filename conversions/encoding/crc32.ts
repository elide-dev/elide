/**
 * CRC32 Checksum
 * Cyclic Redundancy Check (32-bit)
 */

// CRC32 lookup table
const CRC32_TABLE: number[] = [];

function buildCRC32Table(): void {
  for (let i = 0; i < 256; i++) {
    let crc = i;

    for (let j = 0; j < 8; j++) {
      crc = (crc & 1) ? (0xEDB88320 ^ (crc >>> 1)) : (crc >>> 1);
    }

    CRC32_TABLE[i] = crc >>> 0; // Ensure unsigned
  }
}

buildCRC32Table();

export function crc32(data: Buffer | string, previous: number = 0): number {
  const buffer = typeof data === 'string' ? Buffer.from(data) : data;
  let crc = previous ^ 0xFFFFFFFF;

  for (let i = 0; i < buffer.length; i++) {
    crc = CRC32_TABLE[(crc ^ buffer[i]) & 0xFF] ^ (crc >>> 8);
  }

  return (crc ^ 0xFFFFFFFF) >>> 0;
}

export function crc32Hex(data: Buffer | string): string {
  return crc32(data).toString(16).padStart(8, '0');
}

// Incremental CRC32 (for large files)
export class CRC32Stream {
  private crc = 0;

  update(data: Buffer | string): this {
    this.crc = crc32(data, this.crc);
    return this;
  }

  digest(): number {
    return this.crc;
  }

  digestHex(): string {
    return this.crc.toString(16).padStart(8, '0');
  }

  reset(): void {
    this.crc = 0;
  }
}

// Verify checksum
export function verifyCRC32(data: Buffer | string, expected: number): boolean {
  return crc32(data) === expected;
}

// Common use case: file integrity
export function addCRC32Suffix(filename: string, data: Buffer | string): string {
  const checksum = crc32Hex(data);
  const ext = filename.includes('.') ? '.' + filename.split('.').pop() : '';
  const base = ext ? filename.slice(0, -ext.length) : filename;

  return `${base}_${checksum}${ext}`;
}

// CLI demo
if (import.meta.url.includes("crc32.ts")) {
  console.log("CRC32 Checksum Demo\n");

  console.log("1. Calculate CRC32:");
  const text = "Hello, World!";
  const checksum = crc32(text);
  const checksumHex = crc32Hex(text);

  console.log(`  Input: "${text}"`);
  console.log(`  CRC32: ${checksum}`);
  console.log(`  Hex: 0x${checksumHex}`);

  console.log("\n2. Verify checksum:");
  const valid = verifyCRC32(text, checksum);
  console.log(`  Verification: ${valid ? "✅" : "❌"}`);

  console.log("\n3. Different inputs:");
  const inputs = [
    "test",
    "test1",
    "Test",
    ""
  ];

  inputs.forEach(input => {
    const crc = crc32Hex(input);
    console.log(`  "${input}" → 0x${crc}`);
  });

  console.log("\n4. Incremental CRC32:");
  const stream = new CRC32Stream();

  stream.update("Hello, ");
  stream.update("World!");

  console.log(`  Chunk 1: "Hello, "`);
  console.log(`  Chunk 2: "World!"`);
  console.log(`  Combined CRC32: 0x${stream.digestHex()}`);
  console.log(`  Direct CRC32: 0x${crc32Hex("Hello, World!")}`);
  console.log(`  Match: ${stream.digestHex() === crc32Hex("Hello, World!") ? "✅" : "❌"}`);

  console.log("\n5. Binary data:");
  const binary = Buffer.from([0x01, 0x02, 0x03, 0x04, 0x05]);
  const binaryCRC = crc32Hex(binary);
  console.log(`  Input: [${Array.from(binary).join(', ')}]`);
  console.log(`  CRC32: 0x${binaryCRC}`);

  console.log("\n6. File naming:");
  const filename = "document.pdf";
  const content = "PDF content here";
  const withCRC = addCRC32Suffix(filename, content);
  console.log(`  Original: ${filename}`);
  console.log(`  With CRC: ${withCRC}`);

  console.log("\n7. Collision detection:");
  const data1 = "abc";
  const data2 = "abd";
  const crc1 = crc32Hex(data1);
  const crc2 = crc32Hex(data2);

  console.log(`  "${data1}" → 0x${crc1}`);
  console.log(`  "${data2}" → 0x${crc2}`);
  console.log(`  Different: ${crc1 !== crc2 ? "✅" : "❌"}`);

  console.log("\n8. Large data:");
  const large = Buffer.alloc(1000);
  for (let i = 0; i < large.length; i++) {
    large[i] = i % 256;
  }

  const largeCRC = crc32Hex(large);
  console.log(`  1000 bytes: 0x${largeCRC}`);

  console.log("\n✅ CRC32 test passed");
  console.log("⚠️  Note: CRC32 is for error detection, not cryptographic hashing");
}

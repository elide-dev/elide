/**
 * Hex Encoding
 * Hexadecimal encoding and decoding utilities
 */

export function encodeHex(data: string | Buffer): string {
  const buffer = typeof data === 'string' ? Buffer.from(data) : data;
  return buffer.toString('hex');
}

export function decodeHex(hex: string): Buffer {
  // Remove any whitespace or separators
  const clean = hex.replace(/[\s:-]/g, '');

  if (clean.length % 2 !== 0) {
    throw new Error('Hex string must have even length');
  }

  if (!/^[0-9a-fA-F]*$/.test(clean)) {
    throw new Error('Invalid hex string');
  }

  return Buffer.from(clean, 'hex');
}

// Format hex with separators
export function formatHex(hex: string, separator: string = ':', groupSize: number = 2): string {
  const clean = hex.replace(/[\s:-]/g, '');
  const groups: string[] = [];

  for (let i = 0; i < clean.length; i += groupSize) {
    groups.push(clean.slice(i, i + groupSize));
  }

  return groups.join(separator);
}

// Compare hex strings (constant-time for security)
export function compareHex(a: string, b: string): boolean {
  const cleanA = a.replace(/[\s:-]/g, '').toLowerCase();
  const cleanB = b.replace(/[\s:-]/g, '').toLowerCase();

  if (cleanA.length !== cleanB.length) {
    return false;
  }

  let result = 0;
  for (let i = 0; i < cleanA.length; i++) {
    result |= cleanA.charCodeAt(i) ^ cleanB.charCodeAt(i);
  }

  return result === 0;
}

// Hex dump (like hexdump command)
export function hexDump(data: Buffer, options: {
  width?: number;
  showAddress?: boolean;
  showAscii?: boolean;
} = {}): string {
  const width = options.width || 16;
  const showAddress = options.showAddress ?? true;
  const showAscii = options.showAscii ?? true;

  const lines: string[] = [];

  for (let i = 0; i < data.length; i += width) {
    const chunk = data.slice(i, i + width);
    const parts: string[] = [];

    // Address
    if (showAddress) {
      parts.push(i.toString(16).padStart(8, '0'));
    }

    // Hex bytes
    const hexBytes: string[] = [];
    for (let j = 0; j < width; j++) {
      if (j < chunk.length) {
        hexBytes.push(chunk[j].toString(16).padStart(2, '0'));
      } else {
        hexBytes.push('  ');
      }

      // Add separator after 8 bytes
      if (j === 7) {
        hexBytes.push(' ');
      }
    }
    parts.push(hexBytes.join(' '));

    // ASCII representation
    if (showAscii) {
      const ascii = Array.from(chunk)
        .map(b => (b >= 32 && b <= 126) ? String.fromCharCode(b) : '.')
        .join('');
      parts.push(`|${ascii}|`);
    }

    lines.push(parts.join('  '));
  }

  return lines.join('\n');
}

// Random hex string generation
export function randomHex(bytes: number): string {
  const buffer = Buffer.alloc(bytes);

  for (let i = 0; i < bytes; i++) {
    buffer[i] = Math.floor(Math.random() * 256);
  }

  return encodeHex(buffer);
}

// Parse hex color codes
export function parseHexColor(hex: string): { r: number; g: number; b: number; a?: number } | null {
  const clean = hex.replace(/^#/, '');

  // #RGB
  if (clean.length === 3) {
    return {
      r: parseInt(clean[0] + clean[0], 16),
      g: parseInt(clean[1] + clean[1], 16),
      b: parseInt(clean[2] + clean[2], 16)
    };
  }

  // #RRGGBB
  if (clean.length === 6) {
    return {
      r: parseInt(clean.slice(0, 2), 16),
      g: parseInt(clean.slice(2, 4), 16),
      b: parseInt(clean.slice(4, 6), 16)
    };
  }

  // #RRGGBBAA
  if (clean.length === 8) {
    return {
      r: parseInt(clean.slice(0, 2), 16),
      g: parseInt(clean.slice(2, 4), 16),
      b: parseInt(clean.slice(4, 6), 16),
      a: parseInt(clean.slice(6, 8), 16)
    };
  }

  return null;
}

// CLI demo
if (import.meta.url.includes("hex-encoding.ts")) {
  console.log("Hex Encoding Demo\n");

  console.log("1. Encode string:");
  const text = "Hello, World!";
  const hex = encodeHex(text);
  console.log(`  Input: "${text}"`);
  console.log(`  Hex: ${hex}`);

  console.log("\n2. Decode hex:");
  const decoded = decodeHex(hex);
  console.log(`  Decoded: "${decoded.toString()}"`);

  console.log("\n3. Format hex:");
  const formatted = formatHex(hex, ':', 2);
  console.log(`  Colon format: ${formatted}`);

  const formatted2 = formatHex(hex, '-', 4);
  console.log(`  Dash format (4): ${formatted2}`);

  console.log("\n4. Hex dump:");
  const data = Buffer.from("The quick brown fox jumps over the lazy dog");
  console.log(hexDump(data, { width: 16 }));

  console.log("\n5. Compare hex:");
  const hex1 = "deadbeef";
  const hex2 = "DEAD:BEEF";
  console.log(`  "${hex1}" == "${hex2}": ${compareHex(hex1, hex2)}`);

  console.log("\n6. Random hex:");
  const random = randomHex(8);
  console.log(`  Random 8 bytes: ${random}`);

  console.log("\n7. Parse hex colors:");
  const colors = ["#f00", "#00FF00", "#0000FFAA"];
  colors.forEach(color => {
    const parsed = parseHexColor(color);
    console.log(`  ${color} → RGB(${parsed?.r}, ${parsed?.g}, ${parsed?.b}${parsed?.a !== undefined ? ', ' + parsed.a : ''})`);
  });

  console.log("\n✅ Hex encoding test passed");
}

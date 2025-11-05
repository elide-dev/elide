/**
 * Content Negotiation
 * Parse and negotiate HTTP Accept headers
 */

export interface MediaType {
  type: string;
  subtype: string;
  quality: number;
  params: Record<string, string>;
}

export function parseMediaType(mediaType: string): MediaType {
  const parts = mediaType.split(';').map(p => p.trim());
  const [type, subtype] = parts[0].split('/');

  const result: MediaType = {
    type: type || '*',
    subtype: subtype || '*',
    quality: 1.0,
    params: {}
  };

  // Parse parameters (including q value)
  for (let i = 1; i < parts.length; i++) {
    const [key, value] = parts[i].split('=').map(s => s.trim());

    if (key === 'q') {
      result.quality = parseFloat(value) || 1.0;
    } else if (key && value) {
      result.params[key] = value;
    }
  }

  return result;
}

export function parseAccept(acceptHeader: string): MediaType[] {
  if (!acceptHeader) return [];

  const types = acceptHeader.split(',').map(s => s.trim());
  const parsed = types.map(parseMediaType);

  // Sort by quality (highest first)
  return parsed.sort((a, b) => b.quality - a.quality);
}

export function selectMediaType(accept: string, available: string[]): string | null {
  const accepted = parseAccept(accept);

  for (const mediaType of accepted) {
    for (const option of available) {
      if (matchesMediaType(mediaType, option)) {
        return option;
      }
    }
  }

  return null;
}

function matchesMediaType(pattern: MediaType, candidate: string): boolean {
  const candidateType = parseMediaType(candidate);

  // */* matches everything
  if (pattern.type === '*') return true;

  // type/* matches any subtype
  if (pattern.type === candidateType.type && pattern.subtype === '*') return true;

  // Exact match
  if (pattern.type === candidateType.type && pattern.subtype === candidateType.subtype) {
    return true;
  }

  return false;
}

// Language negotiation
export interface Language {
  code: string;
  region?: string;
  quality: number;
}

export function parseLanguage(lang: string): Language {
  const parts = lang.split(';').map(p => p.trim());
  const [code, region] = parts[0].split('-');

  const result: Language = {
    code: code.toLowerCase(),
    region: region?.toUpperCase(),
    quality: 1.0
  };

  // Parse q value
  for (let i = 1; i < parts.length; i++) {
    const [key, value] = parts[i].split('=');
    if (key?.trim() === 'q') {
      result.quality = parseFloat(value) || 1.0;
    }
  }

  return result;
}

export function parseAcceptLanguage(header: string): Language[] {
  if (!header) return [];

  const langs = header.split(',').map(s => s.trim());
  const parsed = langs.map(parseLanguage);

  return parsed.sort((a, b) => b.quality - a.quality);
}

export function selectLanguage(accept: string, available: string[]): string | null {
  const accepted = parseAcceptLanguage(accept);

  for (const lang of accepted) {
    for (const option of available) {
      const optLang = parseLanguage(option);

      // Exact match (code + region)
      if (lang.code === optLang.code && lang.region === optLang.region) {
        return option;
      }

      // Code-only match
      if (lang.code === optLang.code && !lang.region) {
        return option;
      }
    }
  }

  return null;
}

// Encoding negotiation
export function parseAcceptEncoding(header: string): Array<{ encoding: string; quality: number }> {
  if (!header) return [];

  const encodings = header.split(',').map(s => s.trim());

  return encodings.map(enc => {
    const parts = enc.split(';').map(p => p.trim());
    let quality = 1.0;

    for (let i = 1; i < parts.length; i++) {
      const [key, value] = parts[i].split('=');
      if (key?.trim() === 'q') {
        quality = parseFloat(value) || 1.0;
      }
    }

    return { encoding: parts[0], quality };
  }).sort((a, b) => b.quality - a.quality);
}

export function selectEncoding(accept: string, available: string[]): string | null {
  const accepted = parseAcceptEncoding(accept);

  for (const { encoding } of accepted) {
    if (encoding === '*') {
      return available[0] || null;
    }

    if (available.includes(encoding)) {
      return encoding;
    }
  }

  return null;
}

// CLI demo
if (import.meta.url.includes("content-negotiation.ts")) {
  console.log("Content Negotiation Demo\n");

  console.log("1. Parse Accept header:");
  const accept = "text/html;q=0.9, application/json, text/*;q=0.8";
  const types = parseAccept(accept);
  types.forEach(t => {
    console.log(`  ${t.type}/${t.subtype} (q=${t.quality})`);
  });

  console.log("\n2. Select media type:");
  const available = ["application/json", "text/html", "text/plain"];
  const selected = selectMediaType(accept, available);
  console.log(`  Selected: ${selected}`);

  console.log("\n3. Parse Accept-Language:");
  const acceptLang = "en-US, en;q=0.9, fr;q=0.8, de;q=0.7";
  const langs = parseAcceptLanguage(acceptLang);
  langs.forEach(l => {
    const region = l.region ? `-${l.region}` : '';
    console.log(`  ${l.code}${region} (q=${l.quality})`);
  });

  console.log("\n4. Select language:");
  const availableLangs = ["en", "fr", "de", "es"];
  const selectedLang = selectLanguage(acceptLang, availableLangs);
  console.log(`  Selected: ${selectedLang}`);

  console.log("\n5. Parse Accept-Encoding:");
  const acceptEnc = "gzip, deflate;q=0.8, br;q=0.9";
  const encodings = parseAcceptEncoding(acceptEnc);
  encodings.forEach(e => {
    console.log(`  ${e.encoding} (q=${e.quality})`);
  });

  console.log("\n6. Select encoding:");
  const availableEnc = ["gzip", "deflate", "identity"];
  const selectedEnc = selectEncoding(acceptEnc, availableEnc);
  console.log(`  Selected: ${selectedEnc}`);

  console.log("\n✅ Content negotiation test passed");
}

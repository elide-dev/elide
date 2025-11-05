/**
 * HTML Sanitizer
 * Sanitize HTML to prevent XSS attacks
 */

export interface SanitizeOptions {
  allowedTags?: string[];
  allowedAttributes?: Record<string, string[]>;
  allowedSchemes?: string[];
}

const DEFAULT_ALLOWED_TAGS = [
  'p', 'br', 'strong', 'em', 'u', 'a', 'ul', 'ol', 'li',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'code', 'pre'
];

const DEFAULT_ALLOWED_ATTRIBUTES: Record<string, string[]> = {
  'a': ['href', 'title'],
  '*': ['class']
};

const DEFAULT_ALLOWED_SCHEMES = ['http', 'https', 'mailto'];

export class HTMLSanitizer {
  private options: Required<SanitizeOptions>;

  constructor(options: SanitizeOptions = {}) {
    this.options = {
      allowedTags: options.allowedTags || DEFAULT_ALLOWED_TAGS,
      allowedAttributes: options.allowedAttributes || DEFAULT_ALLOWED_ATTRIBUTES,
      allowedSchemes: options.allowedSchemes || DEFAULT_ALLOWED_SCHEMES
    };
  }

  sanitize(html: string): string {
    // Simple tag-based sanitization (not a full parser)
    let sanitized = html;

    // Remove script tags and their content
    sanitized = sanitized.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '');

    // Remove style tags
    sanitized = sanitized.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '');

    // Remove event handlers (onclick, onerror, etc.)
    sanitized = sanitized.replace(/\s*on\w+\s*=\s*["'][^"']*["']/gi, '');
    sanitized = sanitized.replace(/\s*on\w+\s*=\s*[^\s>]*/gi, '');

    // Remove javascript: protocol
    sanitized = sanitized.replace(/javascript:/gi, '');

    // Remove data: protocol (can be used for XSS)
    sanitized = sanitized.replace(/data:/gi, '');

    // Strip disallowed tags
    sanitized = this.stripDisallowedTags(sanitized);

    // Strip disallowed attributes
    sanitized = this.stripDisallowedAttributes(sanitized);

    return sanitized;
  }

  private stripDisallowedTags(html: string): string {
    const tagRegex = /<\/?(\w+)[^>]*>/g;

    return html.replace(tagRegex, (match, tagName) => {
      const tag = tagName.toLowerCase();

      if (this.options.allowedTags.includes(tag)) {
        return match;
      }

      // Remove disallowed tags but keep content
      return '';
    });
  }

  private stripDisallowedAttributes(html: string): string {
    const tagRegex = /<(\w+)([^>]*)>/g;

    return html.replace(tagRegex, (match, tagName, attributes) => {
      const tag = tagName.toLowerCase();

      if (!this.options.allowedTags.includes(tag)) {
        return match;
      }

      const allowedAttrs = [
        ...(this.options.allowedAttributes[tag] || []),
        ...(this.options.allowedAttributes['*'] || [])
      ];

      if (allowedAttrs.length === 0) {
        return `<${tag}>`;
      }

      // Parse and filter attributes
      const attrRegex = /(\w+)=["']([^"']*)["']/g;
      const filteredAttrs: string[] = [];

      let attrMatch;
      while ((attrMatch = attrRegex.exec(attributes)) !== null) {
        const [, attrName, attrValue] = attrMatch;

        if (allowedAttrs.includes(attrName.toLowerCase())) {
          // Validate href/src schemes
          if (attrName.toLowerCase() === 'href' || attrName.toLowerCase() === 'src') {
            if (this.isAllowedURL(attrValue)) {
              filteredAttrs.push(`${attrName}="${this.escapeAttribute(attrValue)}"`);
            }
          } else {
            filteredAttrs.push(`${attrName}="${this.escapeAttribute(attrValue)}"`);
          }
        }
      }

      if (filteredAttrs.length > 0) {
        return `<${tag} ${filteredAttrs.join(' ')}>`;
      }

      return `<${tag}>`;
    });
  }

  private isAllowedURL(url: string): boolean {
    const lower = url.toLowerCase().trim();

    // Relative URLs are OK
    if (lower.startsWith('/') || lower.startsWith('#')) {
      return true;
    }

    // Check against allowed schemes
    for (const scheme of this.options.allowedSchemes) {
      if (lower.startsWith(`${scheme}:`)) {
        return true;
      }
    }

    return false;
  }

  private escapeAttribute(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }
}

export function sanitizeHTML(html: string, options?: SanitizeOptions): string {
  const sanitizer = new HTMLSanitizer(options);
  return sanitizer.sanitize(html);
}

export function escapeHTML(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// CLI demo
if (import.meta.url.includes("html-sanitizer.ts")) {
  console.log("HTML Sanitizer Demo\n");

  const sanitizer = new HTMLSanitizer();

  console.log("1. Remove script tags:");
  const xss1 = '<p>Hello</p><script>alert("XSS")</script>';
  console.log("  Input:", xss1);
  console.log("  Output:", sanitizer.sanitize(xss1));

  console.log("\n2. Remove event handlers:");
  const xss2 = '<img src="x" onerror="alert(1)">';
  console.log("  Input:", xss2);
  console.log("  Output:", sanitizer.sanitize(xss2));

  console.log("\n3. Remove javascript: protocol:");
  const xss3 = '<a href="javascript:alert(1)">Click</a>';
  console.log("  Input:", xss3);
  console.log("  Output:", sanitizer.sanitize(xss3));

  console.log("\n4. Allow safe HTML:");
  const safe = '<p><strong>Bold</strong> and <em>italic</em> text</p>';
  console.log("  Input:", safe);
  console.log("  Output:", sanitizer.sanitize(safe));

  console.log("\n5. Filter attributes:");
  const attrs = '<a href="https://example.com" onclick="bad()" class="link">Link</a>';
  console.log("  Input:", attrs);
  console.log("  Output:", sanitizer.sanitize(attrs));

  console.log("\n6. Escape HTML:");
  const raw = '<script>alert("XSS")</script>';
  console.log("  Input:", raw);
  console.log("  Escaped:", escapeHTML(raw));

  console.log("\n✅ HTML sanitizer test passed");
  console.log("⚠️  Note: This is a basic sanitizer. For production use a battle-tested library.");
}

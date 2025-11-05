/**
 * ENV Parser
 * Parse .env files and environment variables
 */

export interface EnvOptions {
  override?: boolean;
  multiline?: boolean;
  interpolation?: boolean;
}

export class EnvParser {
  private options: Required<EnvOptions>;

  constructor(options: EnvOptions = {}) {
    this.options = {
      override: options.override ?? true,
      multiline: options.multiline ?? true,
      interpolation: options.interpolation ?? true
    };
  }

  parse(content: string): Record<string, string> {
    const result: Record<string, string> = {};
    const lines = content.split('\n');
    let i = 0;

    while (i < lines.length) {
      const line = lines[i].trim();

      // Skip empty lines and comments
      if (!line || line.startsWith('#')) {
        i++;
        continue;
      }

      // Parse key-value pair
      const equalIndex = line.indexOf('=');
      if (equalIndex === -1) {
        i++;
        continue;
      }

      const key = line.slice(0, equalIndex).trim();
      let value = line.slice(equalIndex + 1).trim();

      // Handle quoted values
      if ((value.startsWith('"') && value.endsWith('"')) ||
          (value.startsWith("'") && value.endsWith("'"))) {
        value = value.slice(1, -1);

        // Handle multiline (only for double quotes)
        if (this.options.multiline && line.includes('"') && !value.endsWith('"')) {
          // Collect multiline value
          i++;
          while (i < lines.length && !lines[i].includes('"')) {
            value += '\n' + lines[i];
            i++;
          }
        }

        // Unescape special characters
        value = this.unescape(value);
      }

      // Variable interpolation
      if (this.options.interpolation) {
        value = this.interpolate(value, result);
      }

      result[key] = value;
      i++;
    }

    return result;
  }

  stringify(data: Record<string, string>): string {
    const lines: string[] = [];

    for (const [key, value] of Object.entries(data)) {
      const escaped = this.escape(value);

      // Use quotes if value contains special characters
      const needsQuotes = value.includes(' ') ||
                          value.includes('=') ||
                          value.includes('#') ||
                          value.includes('\n');

      if (needsQuotes) {
        lines.push(`${key}="${escaped}"`);
      } else {
        lines.push(`${key}=${value}`);
      }
    }

    return lines.join('\n') + '\n';
  }

  private unescape(value: string): string {
    return value
      .replace(/\\n/g, '\n')
      .replace(/\\r/g, '\r')
      .replace(/\\t/g, '\t')
      .replace(/\\\\/g, '\\')
      .replace(/\\"/g, '"');
  }

  private escape(value: string): string {
    return value
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"')
      .replace(/\n/g, '\\n')
      .replace(/\r/g, '\\r')
      .replace(/\t/g, '\\t');
  }

  private interpolate(value: string, env: Record<string, string>): string {
    return value.replace(/\$\{([^}]+)\}/g, (match, varName) => {
      return env[varName] || match;
    }).replace(/\$([A-Z_][A-Z0-9_]*)/g, (match, varName) => {
      return env[varName] || match;
    });
  }

  load(content: string, target: Record<string, string> = {}): Record<string, string> {
    const parsed = this.parse(content);

    for (const [key, value] of Object.entries(parsed)) {
      if (this.options.override || !(key in target)) {
        target[key] = value;
      }
    }

    return target;
  }
}

// Helper functions
export function parseEnv(content: string, options?: EnvOptions): Record<string, string> {
  const parser = new EnvParser(options);
  return parser.parse(content);
}

export function stringifyEnv(data: Record<string, string>): string {
  const parser = new EnvParser();
  return parser.stringify(data);
}

// CLI demo
if (import.meta.url.includes("env-parser.ts")) {
  console.log("ENV Parser Demo\n");

  console.log("1. Parse simple .env:");
  const env1 = `
# Database configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=myapp
`;

  const data1 = parseEnv(env1);
  console.log("  Parsed:", JSON.stringify(data1, null, 2));

  console.log("\n2. Quoted values:");
  const env2 = `
MESSAGE="Hello, World!"
PATH_WITH_SPACES="/path/to/my directory"
SINGLE_QUOTE='value with "double quotes"'
`;

  const data2 = parseEnv(env2);
  console.log("  Parsed:", JSON.stringify(data2, null, 2));

  console.log("\n3. Special characters:");
  const env3 = `
ESCAPED="Line 1\\nLine 2\\tTabbed"
HASH="Value with # hash"
`;

  const data3 = parseEnv(env3);
  console.log("  Parsed:");
  console.log(`  ESCAPED: "${data3.ESCAPED}"`);

  console.log("\n4. Variable interpolation:");
  const env4 = `
BASE_URL=https://example.com
API_URL=\${BASE_URL}/api
CDN_URL=\${BASE_URL}/static
`;

  const data4 = parseEnv(env4);
  console.log("  Parsed:");
  console.log(`  BASE_URL: ${data4.BASE_URL}`);
  console.log(`  API_URL: ${data4.API_URL}`);
  console.log(`  CDN_URL: ${data4.CDN_URL}`);

  console.log("\n5. Generate .env:");
  const config = {
    NODE_ENV: 'production',
    PORT: '3000',
    DATABASE_URL: 'postgres://localhost/db',
    SECRET_KEY: 'my-secret-key'
  };

  const output = stringifyEnv(config);
  console.log("  Generated:");
  console.log(output);

  console.log("\n6. Override behavior:");
  const parser = new EnvParser({ override: false });
  const existing = { PORT: '8080' };
  const loaded = parser.load('PORT=3000\nHOST=localhost', existing);
  console.log("  Existing PORT not overridden:", loaded.PORT);
  console.log("  New HOST added:", loaded.HOST);

  console.log("✅ ENV parser test passed");
}

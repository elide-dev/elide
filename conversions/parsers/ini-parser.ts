/**
 * INI Parser
 * Parse and generate INI configuration files
 */

export interface INIData {
  [section: string]: {
    [key: string]: string;
  };
}

export class INIParser {
  parse(content: string): INIData {
    const result: INIData = {};
    let currentSection = '';

    const lines = content.split('\n');

    for (let line of lines) {
      line = line.trim();

      // Skip empty lines and comments
      if (!line || line.startsWith(';') || line.startsWith('#')) {
        continue;
      }

      // Section header [section]
      if (line.startsWith('[') && line.endsWith(']')) {
        currentSection = line.slice(1, -1).trim();
        if (!result[currentSection]) {
          result[currentSection] = {};
        }
        continue;
      }

      // Key-value pair
      const equalIndex = line.indexOf('=');
      if (equalIndex !== -1) {
        const key = line.slice(0, equalIndex).trim();
        const value = line.slice(equalIndex + 1).trim();

        // Remove quotes if present
        const unquoted = value.replace(/^["']|["']$/g, '');

        if (currentSection) {
          result[currentSection][key] = unquoted;
        } else {
          // Global section
          if (!result['']) {
            result[''] = {};
          }
          result[''][key] = unquoted;
        }
      }
    }

    return result;
  }

  stringify(data: INIData): string {
    const lines: string[] = [];

    // Global section first (if exists)
    if (data['']) {
      for (const [key, value] of Object.entries(data[''])) {
        lines.push(`${key} = ${this.escapeValue(value)}`);
      }
      if (lines.length > 0) {
        lines.push('');
      }
    }

    // Other sections
    for (const [section, values] of Object.entries(data)) {
      if (section === '') continue; // Already handled

      lines.push(`[${section}]`);

      for (const [key, value] of Object.entries(values)) {
        lines.push(`${key} = ${this.escapeValue(value)}`);
      }

      lines.push('');
    }

    return lines.join('\n').trim() + '\n';
  }

  private escapeValue(value: string): string {
    // Quote if contains special characters
    if (value.includes(';') || value.includes('#') || value.includes('=')) {
      return `"${value.replace(/"/g, '\\"')}"`;
    }

    return value;
  }

  get(data: INIData, section: string, key: string, defaultValue?: string): string | undefined {
    return data[section]?.[key] ?? defaultValue;
  }

  set(data: INIData, section: string, key: string, value: string): void {
    if (!data[section]) {
      data[section] = {};
    }
    data[section][key] = value;
  }
}

// Helper functions
export function parseINI(content: string): INIData {
  const parser = new INIParser();
  return parser.parse(content);
}

export function stringifyINI(data: INIData): string {
  const parser = new INIParser();
  return parser.stringify(data);
}

// CLI demo
if (import.meta.url.includes("ini-parser.ts")) {
  console.log("INI Parser Demo\n");

  console.log("1. Parse INI file:");
  const ini = `
; Database configuration
[database]
host = localhost
port = 5432
username = admin
password = secret123

[server]
port = 8080
debug = true

# Global settings
timeout = 30
`;

  const data = parseINI(ini);
  console.log("  Parsed:", JSON.stringify(data, null, 2));

  console.log("\n2. Get values:");
  const parser = new INIParser();
  console.log(`  database.host: ${parser.get(data, 'database', 'host')}`);
  console.log(`  server.port: ${parser.get(data, 'server', 'port')}`);
  console.log(`  timeout: ${parser.get(data, '', 'timeout')}`);

  console.log("\n3. Set values:");
  parser.set(data, 'database', 'max_connections', '100');
  parser.set(data, 'cache', 'enabled', 'true');
  console.log("  Added database.max_connections and cache.enabled");

  console.log("\n4. Generate INI:");
  const config: INIData = {
    '': {
      version: '1.0',
      encoding: 'utf-8'
    },
    app: {
      name: 'MyApp',
      debug: 'false'
    },
    database: {
      url: 'postgres://localhost/mydb'
    }
  };

  const output = stringifyINI(config);
  console.log("  Generated:");
  console.log(output);

  console.log("\n5. Special characters:");
  const special: INIData = {
    test: {
      'comment-like': 'value ; with semicolon',
      'equals': 'a=b',
      'hash': 'value # with hash'
    }
  };

  const escaped = stringifyINI(special);
  console.log("  Escaped values:");
  console.log(escaped);

  console.log("✅ INI parser test passed");
}

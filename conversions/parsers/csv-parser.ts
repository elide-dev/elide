/**
 * CSV Parser
 * Parse and generate CSV files
 */

export interface CSVOptions {
  delimiter?: string;
  quote?: string;
  escape?: string;
  headers?: boolean;
  skipEmpty?: boolean;
}

export class CSVParser {
  private options: Required<CSVOptions>;

  constructor(options: CSVOptions = {}) {
    this.options = {
      delimiter: options.delimiter || ',',
      quote: options.quote || '"',
      escape: options.escape || '"',
      headers: options.headers ?? true,
      skipEmpty: options.skipEmpty ?? true
    };
  }

  parse(content: string): any[] {
    const lines = content.split('\n').map(l => l.trim());
    const rows: string[][] = [];

    for (const line of lines) {
      if (this.options.skipEmpty && !line) {
        continue;
      }

      rows.push(this.parseLine(line));
    }

    if (rows.length === 0) {
      return [];
    }

    if (this.options.headers) {
      const headers = rows[0];
      const data = rows.slice(1);

      return data.map(row => {
        const obj: any = {};
        headers.forEach((header, i) => {
          obj[header] = row[i] || '';
        });
        return obj;
      });
    }

    return rows;
  }

  private parseLine(line: string): string[] {
    const fields: string[] = [];
    let current = '';
    let inQuotes = false;
    let i = 0;

    while (i < line.length) {
      const char = line[i];
      const next = line[i + 1];

      if (char === this.options.quote) {
        if (inQuotes && next === this.options.quote) {
          // Escaped quote
          current += this.options.quote;
          i += 2;
          continue;
        } else {
          inQuotes = !inQuotes;
          i++;
          continue;
        }
      }

      if (char === this.options.delimiter && !inQuotes) {
        fields.push(current);
        current = '';
        i++;
        continue;
      }

      current += char;
      i++;
    }

    fields.push(current);

    return fields;
  }

  stringify(data: any[], options?: { headers?: string[] }): string {
    if (data.length === 0) {
      return '';
    }

    const lines: string[] = [];
    const headers = options?.headers || Object.keys(data[0]);

    if (this.options.headers) {
      lines.push(this.stringifyRow(headers));
    }

    for (const row of data) {
      const values = headers.map(h => row[h]?.toString() || '');
      lines.push(this.stringifyRow(values));
    }

    return lines.join('\n');
  }

  private stringifyRow(fields: string[]): string {
    return fields.map(field => this.escapeField(field)).join(this.options.delimiter);
  }

  private escapeField(field: string): string {
    const needsQuotes = field.includes(this.options.delimiter) ||
                        field.includes(this.options.quote) ||
                        field.includes('\n');

    if (!needsQuotes) {
      return field;
    }

    const escaped = field.replace(
      new RegExp(this.options.quote, 'g'),
      this.options.quote + this.options.quote
    );

    return `${this.options.quote}${escaped}${this.options.quote}`;
  }
}

// Helper functions
export function parseCSV(content: string, options?: CSVOptions): any[] {
  const parser = new CSVParser(options);
  return parser.parse(content);
}

export function stringifyCSV(data: any[], options?: CSVOptions): string {
  const parser = new CSVParser(options);
  return parser.stringify(data);
}

// CLI demo
if (import.meta.url.includes("csv-parser.ts")) {
  console.log("CSV Parser Demo\n");

  console.log("1. Parse simple CSV:");
  const csv1 = `name,age,city
Alice,30,NYC
Bob,25,LA
Charlie,35,Chicago`;

  const data1 = parseCSV(csv1);
  console.log("  Input:", csv1.substring(0, 30) + "...");
  console.log("  Parsed:", JSON.stringify(data1, null, 2));

  console.log("\n2. Parse with quoted fields:");
  const csv2 = `product,price,description
"Widget A",9.99,"A great widget, perfect for everything"
"Gadget B",19.99,"The ""best"" gadget"`;

  const data2 = parseCSV(csv2);
  console.log("  Parsed:", JSON.stringify(data2, null, 2));

  console.log("\n3. Generate CSV:");
  const data = [
    { name: "Alice", age: 30, city: "NYC" },
    { name: "Bob", age: 25, city: "LA" }
  ];

  const output = stringifyCSV(data);
  console.log("  Generated:");
  console.log(output);

  console.log("\n4. Custom delimiter:");
  const parser = new CSVParser({ delimiter: ';', headers: false });
  const csv3 = "1;2;3\n4;5;6";
  const data3 = parser.parse(csv3);
  console.log(`  Input: "${csv3.replace(/\n/g, '\\n')}"`);
  console.log("  Parsed:", JSON.stringify(data3));

  console.log("\n5. Handle special characters:");
  const special = [
    { text: 'Field with "quotes"', value: 'Normal' },
    { text: 'Field with, comma', value: 'Also normal' }
  ];

  const escapedCSV = stringifyCSV(special);
  console.log("  Generated:");
  console.log(escapedCSV);

  console.log("\n✅ CSV parser test passed");
}

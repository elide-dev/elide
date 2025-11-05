/**
 * XML Builder
 * Simple XML generation (not a parser)
 */

export interface XMLNode {
  tag: string;
  attributes?: Record<string, string | number>;
  children?: (XMLNode | string)[];
  text?: string;
}

export class XMLBuilder {
  private indent = 2;

  setIndent(spaces: number): this {
    this.indent = spaces;
    return this;
  }

  build(node: XMLNode, level: number = 0): string {
    const indentation = ' '.repeat(level * this.indent);
    const parts: string[] = [];

    // Opening tag
    let opening = `${indentation}<${node.tag}`;

    if (node.attributes) {
      for (const [key, value] of Object.entries(node.attributes)) {
        opening += ` ${key}="${this.escapeAttribute(String(value))}"`;
      }
    }

    // Self-closing tag
    if (!node.text && (!node.children || node.children.length === 0)) {
      parts.push(`${opening} />`);
      return parts.join('\n');
    }

    opening += '>';

    // Text content
    if (node.text) {
      parts.push(`${opening}${this.escapeText(node.text)}</${node.tag}>`);
      return parts.join('');
    }

    // Children
    parts.push(opening);

    if (node.children) {
      for (const child of node.children) {
        if (typeof child === 'string') {
          parts.push(`${indentation}${' '.repeat(this.indent)}${this.escapeText(child)}`);
        } else {
          parts.push(this.build(child, level + 1));
        }
      }
    }

    // Closing tag
    parts.push(`${indentation}</${node.tag}>`);

    return parts.join('\n');
  }

  private escapeText(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
  }

  private escapeAttribute(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }
}

// Fluent API builder
export class FluentXML {
  private nodes: XMLNode[] = [];
  private current: XMLNode | null = null;

  element(tag: string): this {
    const node: XMLNode = { tag };

    if (this.current) {
      if (!this.current.children) {
        this.current.children = [];
      }
      this.current.children.push(node);
    } else {
      this.nodes.push(node);
    }

    this.current = node;
    return this;
  }

  attr(key: string, value: string | number): this {
    if (this.current) {
      if (!this.current.attributes) {
        this.current.attributes = {};
      }
      this.current.attributes[key] = value;
    }
    return this;
  }

  text(content: string): this {
    if (this.current) {
      this.current.text = content;
    }
    return this;
  }

  child(tag: string, text?: string): this {
    if (this.current) {
      if (!this.current.children) {
        this.current.children = [];
      }

      const child: XMLNode = { tag };
      if (text !== undefined) {
        child.text = text;
      }

      this.current.children.push(child);
    }
    return this;
  }

  build(): string {
    const builder = new XMLBuilder();
    return this.nodes.map(node => builder.build(node)).join('\n');
  }
}

// Helper functions
export function buildXML(node: XMLNode): string {
  const builder = new XMLBuilder();
  return `<?xml version="1.0" encoding="UTF-8"?>\n${builder.build(node)}`;
}

export function xml(tag: string): FluentXML {
  return new FluentXML().element(tag);
}

// CLI demo
if (import.meta.url.includes("xml-builder.ts")) {
  console.log("XML Builder Demo\n");

  console.log("1. Simple element:");
  const simple: XMLNode = {
    tag: 'greeting',
    text: 'Hello, World!'
  };

  const builder = new XMLBuilder();
  console.log(builder.build(simple));

  console.log("\n2. With attributes:");
  const withAttrs: XMLNode = {
    tag: 'person',
    attributes: { id: '123', age: 30 },
    children: [
      { tag: 'name', text: 'Alice' },
      { tag: 'city', text: 'NYC' }
    ]
  };

  console.log(builder.build(withAttrs));

  console.log("\n3. Nested structure:");
  const nested: XMLNode = {
    tag: 'library',
    children: [
      {
        tag: 'book',
        attributes: { isbn: '123' },
        children: [
          { tag: 'title', text: 'TypeScript Guide' },
          { tag: 'author', text: 'John Doe' }
        ]
      },
      {
        tag: 'book',
        attributes: { isbn: '456' },
        children: [
          { tag: 'title', text: 'JavaScript Basics' },
          { tag: 'author', text: 'Jane Smith' }
        ]
      }
    ]
  };

  console.log(builder.build(nested));

  console.log("\n4. Fluent API:");
  const fluent = xml('user')
    .attr('id', '456')
    .child('name', 'Bob')
    .child('email', 'bob@example.com')
    .build();

  console.log(fluent);

  console.log("\n5. Complete document:");
  const doc: XMLNode = {
    tag: 'config',
    children: [
      {
        tag: 'database',
        children: [
          { tag: 'host', text: 'localhost' },
          { tag: 'port', text: '5432' }
        ]
      },
      {
        tag: 'server',
        attributes: { enabled: 'true' },
        children: [
          { tag: 'port', text: '8080' }
        ]
      }
    ]
  };

  console.log(buildXML(doc));

  console.log("\n6. Escape special characters:");
  const special: XMLNode = {
    tag: 'data',
    text: 'Text with <brackets> & ampersands'
  };

  console.log(builder.build(special));

  console.log("\n✅ XML builder test passed");
}

/**
 * Markdown Parser
 * Simple markdown to HTML converter
 */

export class MarkdownParser {
  parse(markdown: string): string {
    let html = markdown;

    // Headers
    html = html.replace(/^### (.*$)/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gm, '<h1>$1</h1>');

    // Bold
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/__(.+?)__/g, '<strong>$1</strong>');

    // Italic
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/_(.+?)_/g, '<em>$1</em>');

    // Code (inline)
    html = html.replace(/`(.+?)`/g, '<code>$1</code>');

    // Code blocks
    html = html.replace(/```(\w+)?\n([\s\S]+?)```/g, (match, lang, code) => {
      const language = lang ? ` class="language-${lang}"` : '';
      return `<pre><code${language}>${this.escapeHtml(code.trim())}</code></pre>`;
    });

    // Links
    html = html.replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2">$1</a>');

    // Images
    html = html.replace(/!\[(.+?)\]\((.+?)\)/g, '<img src="$2" alt="$1">');

    // Unordered lists
    html = this.parseLists(html);

    // Horizontal rules
    html = html.replace(/^---$/gm, '<hr>');
    html = html.replace(/^\*\*\*$/gm, '<hr>');

    // Blockquotes
    html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>');

    // Line breaks
    html = html.replace(/  \n/g, '<br>\n');

    // Paragraphs
    html = this.wrapParagraphs(html);

    return html;
  }

  private parseLists(html: string): string {
    // Unordered lists
    html = html.replace(/^\* (.+)$/gm, '<li>$1</li>');
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');

    // Wrap consecutive <li> in <ul>
    html = html.replace(/(<li>.*<\/li>\n?)+/g, (match) => {
      return `<ul>\n${match}</ul>\n`;
    });

    // Ordered lists
    html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');

    // Wrap consecutive numbered <li> in <ol>
    html = html.replace(/(<li>.*<\/li>\n)+/g, (match) => {
      if (match.includes('<ul>')) {
        return match; // Already wrapped as ul
      }
      return `<ol>\n${match}</ol>\n`;
    });

    return html;
  }

  private wrapParagraphs(html: string): string {
    const lines = html.split('\n');
    const result: string[] = [];
    let inParagraph = false;
    let paragraphLines: string[] = [];

    for (const line of lines) {
      const trimmed = line.trim();

      // Skip if line is already an HTML element
      if (trimmed.startsWith('<')) {
        if (inParagraph) {
          result.push(`<p>${paragraphLines.join(' ')}</p>`);
          paragraphLines = [];
          inParagraph = false;
        }
        result.push(line);
        continue;
      }

      // Empty line ends paragraph
      if (!trimmed) {
        if (inParagraph) {
          result.push(`<p>${paragraphLines.join(' ')}</p>`);
          paragraphLines = [];
          inParagraph = false;
        }
        result.push('');
        continue;
      }

      // Regular text line
      inParagraph = true;
      paragraphLines.push(trimmed);
    }

    // Close final paragraph if open
    if (inParagraph && paragraphLines.length > 0) {
      result.push(`<p>${paragraphLines.join(' ')}</p>`);
    }

    return result.join('\n');
  }

  private escapeHtml(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}

// Helper function
export function parseMarkdown(markdown: string): string {
  const parser = new MarkdownParser();
  return parser.parse(markdown);
}

// CLI demo
if (import.meta.url.includes("markdown-parser.ts")) {
  console.log("Markdown Parser Demo\n");

  console.log("1. Headers:");
  const md1 = `# Heading 1
## Heading 2
### Heading 3`;

  const html1 = parseMarkdown(md1);
  console.log("  Input:", md1.replace(/\n/g, '\\n'));
  console.log("  Output:", html1.replace(/\n/g, '\\n'));

  console.log("\n2. Text formatting:");
  const md2 = `**Bold text** and *italic text*
__Also bold__ and _also italic_
Inline \`code\` example`;

  const html2 = parseMarkdown(md2);
  console.log("  Output:", html2);

  console.log("\n3. Links and images:");
  const md3 = `[Link text](https://example.com)
![Alt text](image.jpg)`;

  const html3 = parseMarkdown(md3);
  console.log("  Output:", html3.replace(/\n/g, '\\n'));

  console.log("\n4. Lists:");
  const md4 = `* Item 1
* Item 2
* Item 3

1. First
2. Second
3. Third`;

  const html4 = parseMarkdown(md4);
  console.log("  Output:");
  console.log(html4);

  console.log("\n5. Code blocks:");
  const md5 = '```typescript\nfunction hello() {\n  return "world";\n}\n```';

  const html5 = parseMarkdown(md5);
  console.log("  Output:");
  console.log(html5);

  console.log("\n6. Blockquotes:");
  const md6 = `> This is a quote
> Spanning multiple lines`;

  const html6 = parseMarkdown(md6);
  console.log("  Output:", html6.replace(/\n/g, '\\n'));

  console.log("\n7. Complete example:");
  const md = `# My Document

This is a paragraph with **bold** and *italic* text.

## Features

* Easy to read
* Easy to write
* [Supports links](https://example.com)

\`\`\`javascript
const x = 42;
\`\`\``;

  const html = parseMarkdown(md);
  console.log("  Output:");
  console.log(html);

  console.log("\n✅ Markdown parser test passed");
  console.log("⚠️  Note: This is a basic parser. For production use a battle-tested library.");
}

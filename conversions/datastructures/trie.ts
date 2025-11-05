/**
 * Trie (Prefix Tree)
 * Efficient string search and prefix matching
 */

class TrieNode {
  children = new Map<string, TrieNode>();
  isEndOfWord = false;
  value?: any;
}

export class Trie {
  private root = new TrieNode();

  insert(word: string, value?: any): void {
    let node = this.root;

    for (const char of word) {
      if (!node.children.has(char)) {
        node.children.set(char, new TrieNode());
      }
      node = node.children.get(char)!;
    }

    node.isEndOfWord = true;
    if (value !== undefined) {
      node.value = value;
    }
  }

  search(word: string): boolean {
    const node = this.findNode(word);
    return node !== null && node.isEndOfWord;
  }

  get(word: string): any | undefined {
    const node = this.findNode(word);
    return node?.isEndOfWord ? node.value : undefined;
  }

  startsWith(prefix: string): boolean {
    return this.findNode(prefix) !== null;
  }

  delete(word: string): boolean {
    return this.deleteHelper(this.root, word, 0);
  }

  private deleteHelper(node: TrieNode, word: string, index: number): boolean {
    if (index === word.length) {
      if (!node.isEndOfWord) {
        return false;
      }

      node.isEndOfWord = false;
      node.value = undefined;

      return node.children.size === 0;
    }

    const char = word[index];
    const child = node.children.get(char);

    if (!child) {
      return false;
    }

    const shouldDeleteChild = this.deleteHelper(child, word, index + 1);

    if (shouldDeleteChild) {
      node.children.delete(char);
      return node.children.size === 0 && !node.isEndOfWord;
    }

    return false;
  }

  findAllWithPrefix(prefix: string): string[] {
    const node = this.findNode(prefix);

    if (!node) {
      return [];
    }

    const results: string[] = [];
    this.collectWords(node, prefix, results);

    return results;
  }

  private collectWords(node: TrieNode, current: string, results: string[]): void {
    if (node.isEndOfWord) {
      results.push(current);
    }

    for (const [char, child] of node.children) {
      this.collectWords(child, current + char, results);
    }
  }

  private findNode(word: string): TrieNode | null {
    let node = this.root;

    for (const char of word) {
      const child = node.children.get(char);

      if (!child) {
        return null;
      }

      node = child;
    }

    return node;
  }

  getAllWords(): string[] {
    const results: string[] = [];
    this.collectWords(this.root, '', results);
    return results;
  }

  size(): number {
    return this.getAllWords().length;
  }

  clear(): void {
    this.root = new TrieNode();
  }
}

// Autocomplete helper
export class Autocomplete {
  private trie = new Trie();

  add(word: string, metadata?: any): void {
    this.trie.insert(word.toLowerCase(), metadata);
  }

  search(prefix: string, limit: number = 10): string[] {
    const matches = this.trie.findAllWithPrefix(prefix.toLowerCase());
    return matches.slice(0, limit);
  }

  has(word: string): boolean {
    return this.trie.search(word.toLowerCase());
  }
}

// CLI demo
if (import.meta.url.includes("trie.ts")) {
  console.log("Trie (Prefix Tree) Demo\n");

  const trie = new Trie();

  console.log("1. Insert words:");
  const words = ["apple", "app", "application", "apply", "banana", "band", "bandana"];
  words.forEach(w => trie.insert(w));
  console.log(`  Inserted: ${words.join(", ")}`);
  console.log(`  Size: ${trie.size()}`);

  console.log("\n2. Search exact words:");
  console.log(`  "apple": ${trie.search("apple") ? "✅" : "❌"}`);
  console.log(`  "app": ${trie.search("app") ? "✅" : "❌"}`);
  console.log(`  "appl": ${trie.search("appl") ? "✅" : "❌"}`);

  console.log("\n3. Check prefixes:");
  console.log(`  startsWith("app"): ${trie.startsWith("app")}`);
  console.log(`  startsWith("ban"): ${trie.startsWith("ban")}`);
  console.log(`  startsWith("cat"): ${trie.startsWith("cat")}`);

  console.log("\n4. Find all with prefix:");
  const appWords = trie.findAllWithPrefix("app");
  console.log(`  Prefix "app": [${appWords.join(", ")}]`);

  const banWords = trie.findAllWithPrefix("ban");
  console.log(`  Prefix "ban": [${banWords.join(", ")}]`);

  console.log("\n5. Store values:");
  const valueTrie = new Trie();
  valueTrie.insert("apple", { color: "red", price: 1.50 });
  valueTrie.insert("banana", { color: "yellow", price: 0.75 });

  const appleData = valueTrie.get("apple");
  console.log(`  "apple": ${JSON.stringify(appleData)}`);

  console.log("\n6. Delete word:");
  trie.delete("app");
  console.log(`  Deleted "app"`);
  console.log(`  search("app"): ${trie.search("app")}`);
  console.log(`  search("apple"): ${trie.search("apple")}`);

  console.log("\n7. Autocomplete:");
  const autocomplete = new Autocomplete();
  ["hello", "help", "hero", "heap", "world"].forEach(w => autocomplete.add(w));

  const suggestions = autocomplete.search("he");
  console.log(`  Suggestions for "he": [${suggestions.join(", ")}]`);

  console.log("\n✅ Trie test passed");
}

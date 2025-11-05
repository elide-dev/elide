/**
 * Redirect Manager
 * Manage HTTP redirects (301, 302, 307, 308)
 */

export type RedirectType = 301 | 302 | 307 | 308;

export interface RedirectRule {
  from: string | RegExp;
  to: string;
  status: RedirectType;
  permanent?: boolean;
}

export interface RedirectResult {
  shouldRedirect: boolean;
  status?: RedirectType;
  location?: string;
}

export class RedirectManager {
  private rules: RedirectRule[] = [];

  add(from: string | RegExp, to: string, status: RedirectType = 302): this {
    this.rules.push({ from, to, status, permanent: status === 301 || status === 308 });
    return this;
  }

  permanent(from: string | RegExp, to: string): this {
    return this.add(from, to, 301);
  }

  temporary(from: string | RegExp, to: string): this {
    return this.add(from, to, 302);
  }

  // 307 preserves method and body
  temporaryPreserve(from: string | RegExp, to: string): this {
    return this.add(from, to, 307);
  }

  // 308 is permanent + preserves method and body
  permanentPreserve(from: string | RegExp, to: string): this {
    return this.add(from, to, 308);
  }

  match(path: string): RedirectResult {
    for (const rule of this.rules) {
      const match = this.matchRule(path, rule);

      if (match) {
        return {
          shouldRedirect: true,
          status: rule.status,
          location: match
        };
      }
    }

    return { shouldRedirect: false };
  }

  private matchRule(path: string, rule: RedirectRule): string | null {
    if (typeof rule.from === 'string') {
      if (path === rule.from) {
        return rule.to;
      }
    } else {
      const match = path.match(rule.from);

      if (match) {
        // Replace $1, $2, etc. with captured groups
        let result = rule.to;

        for (let i = 1; i < match.length; i++) {
          result = result.replace(`$${i}`, match[i]);
        }

        return result;
      }
    }

    return null;
  }

  remove(from: string | RegExp): boolean {
    const index = this.rules.findIndex(r => {
      if (typeof r.from === 'string' && typeof from === 'string') {
        return r.from === from;
      }
      if (r.from instanceof RegExp && from instanceof RegExp) {
        return r.from.source === from.source;
      }
      return false;
    });

    if (index !== -1) {
      this.rules.splice(index, 1);
      return true;
    }

    return false;
  }

  clear(): void {
    this.rules = [];
  }

  getRules(): ReadonlyArray<RedirectRule> {
    return this.rules;
  }
}

// Common redirect patterns
export function createRedirectManager(): RedirectManager {
  return new RedirectManager();
}

// Helper to build redirect response
export function buildRedirectResponse(location: string, status: RedirectType = 302): {
  status: number;
  headers: Record<string, string>;
  body: string;
} {
  return {
    status,
    headers: {
      'Location': location,
      'Content-Type': 'text/html'
    },
    body: `<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="refresh" content="0;url=${location}">
</head>
<body>
  <p>Redirecting to <a href="${location}">${location}</a>...</p>
</body>
</html>`
  };
}

// Redirect status descriptions
export function getRedirectDescription(status: RedirectType): string {
  const descriptions: Record<RedirectType, string> = {
    301: 'Moved Permanently',
    302: 'Found (Temporary Redirect)',
    307: 'Temporary Redirect (preserve method)',
    308: 'Permanent Redirect (preserve method)'
  };

  return descriptions[status];
}

// CLI demo
if (import.meta.url.includes("redirect-manager.ts")) {
  console.log("Redirect Manager Demo\n");

  const manager = new RedirectManager();

  console.log("Add redirect rules:");
  manager
    .permanent('/old-page', '/new-page')
    .temporary('/temp', '/temporary-location')
    .permanentPreserve('/api/v1/users', '/api/v2/users')
    .add(/^\/blog\/(\d+)$/, '/posts/$1', 301);

  console.log(`  Rules: ${manager.getRules().length}`);

  console.log("\n1. Exact match:");
  const result1 = manager.match('/old-page');
  console.log(`  Path: /old-page`);
  console.log(`  Redirect: ${result1.shouldRedirect ? 'Yes' : 'No'}`);
  console.log(`  Status: ${result1.status} (${getRedirectDescription(result1.status!)})`);
  console.log(`  Location: ${result1.location}`);

  console.log("\n2. Regex match with capture:");
  const result2 = manager.match('/blog/123');
  console.log(`  Path: /blog/123`);
  console.log(`  Redirect: ${result2.shouldRedirect ? 'Yes' : 'No'}`);
  console.log(`  Location: ${result2.location}`);

  console.log("\n3. No match:");
  const result3 = manager.match('/about');
  console.log(`  Path: /about`);
  console.log(`  Redirect: ${result3.shouldRedirect ? 'Yes' : 'No'}`);

  console.log("\n4. Build redirect response:");
  const response = buildRedirectResponse('/new-location', 301);
  console.log(`  Status: ${response.status}`);
  console.log(`  Location: ${response.headers['Location']}`);
  console.log(`  Body: ${response.body.substring(0, 50)}...`);

  console.log("\n5. Remove rule:");
  manager.remove('/temp');
  console.log(`  Rules after removal: ${manager.getRules().length}`);

  console.log("\n✅ Redirect manager test passed");
}

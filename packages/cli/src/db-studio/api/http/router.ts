/**
 * Match a route pattern against a path and extract parameters
 */
export function matchRoute(pattern: string, path: string): Record<string, string> | null {
  const patternParts = pattern.split("/").filter(p => p);
  const pathParts = path.split("/").filter(p => p);

  if (patternParts.length !== pathParts.length) return null;

  const params: Record<string, string> = {};

  for (let i = 0; i < patternParts.length; i++) {
    const patternPart = patternParts[i];
    const pathPart = pathParts[i];

    if (patternPart.startsWith(":")) {
      // Parameter segment
      const paramName = patternPart.slice(1);
      params[paramName] = pathPart;
    } else if (patternPart !== pathPart) {
      // Literal segment doesn't match
      return null;
    }
  }

  return params;
}


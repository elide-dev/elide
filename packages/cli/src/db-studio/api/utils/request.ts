/**
 * Parse query parameters from URL string
 * Extracts and decodes query parameters without using URL API (not available in Elide runtime)
 */
export function parseQueryParams(url: string): Map<string, string> {
  const queryParams = new Map<string, string>();
  const queryIndex = url.indexOf('?');
  
  if (queryIndex < 0) {
    return queryParams;
  }
  
  const queryString = url.substring(queryIndex + 1);
  if (!queryString) {
    return queryParams;
  }
  
  const pairs = queryString.split('&');
  for (const pair of pairs) {
    const [key, value] = pair.split('=');
    if (key && value !== undefined) {
      queryParams.set(decodeURIComponent(key), decodeURIComponent(value));
    }
  }
  
  return queryParams;
}

/**
 * Parse request body as JSON
 */
export function parseRequestBody(body: string): Record<string, unknown> {
  if (!body || body.trim() === "") {
    return {};
  }
  try {
    return JSON.parse(body);
  } catch {
    return {};
  }
}

/**
 * Build SQL WHERE clause from object
 */
export function buildWhereClause(where: Record<string, unknown>): { clause: string; values: unknown[] } {
  const conditions: string[] = [];
  const values: unknown[] = [];

  for (const [key, value] of Object.entries(where)) {
    conditions.push(`"${key}" = ?`);
    values.push(value);
  }

  return {
    clause: conditions.length > 0 ? `WHERE ${conditions.join(" AND ")}` : "",
    values,
  };
}


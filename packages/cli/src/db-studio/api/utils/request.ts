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
    conditions.push(`${key} = ?`);
    values.push(value);
  }

  return {
    clause: conditions.length > 0 ? `WHERE ${conditions.join(" AND ")}` : "",
    values,
  };
}


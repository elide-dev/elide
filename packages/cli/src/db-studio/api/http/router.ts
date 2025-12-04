/**
 * Represents a captured route parameter
 */
type RouteParameter = {
  name: string;
  value: string;
};

/**
 * Result of matching a single path segment
 */
type SegmentMatchResult = 
  | { matched: true; parameter: RouteParameter | null }
  | { matched: false };

/**
 * Splits a URL path into segments, filtering out empty strings
 */
function splitPathIntoSegments(path: string): string[] {
  return path.split("/").filter(segment => segment.length > 0);
}

/**
 * Checks if a pattern segment is a parameter (starts with ':')
 */
function isParameterSegment(segment: string): boolean {
  return segment.startsWith(":");
}

/**
 * Extracts the parameter name from a pattern segment (removes the ':' prefix)
 */
function extractParameterName(segment: string): string {
  return segment.slice(1);
}

/**
 * Matches a single pattern segment against a path segment
 * Returns match result indicating success and any captured parameter
 */
function matchPathSegment(
  patternSegment: string,
  pathSegment: string
): SegmentMatchResult {
  if (isParameterSegment(patternSegment)) {
    return {
      matched: true,
      parameter: {
        name: extractParameterName(patternSegment),
        value: decodeURIComponent(pathSegment)
      }
    };
  }

  // Literal segment - must match exactly
  if (patternSegment === pathSegment) {
    return { matched: true, parameter: null };
  }

  return { matched: false };
}

/**
 * Matches a route pattern against a path and extracts parameters
 * 
 * Pattern segments starting with ':' are treated as parameters.
 * 
 * @example
 * matchRoute("/api/:dbId/query", "/api/0/query") 
 * // Returns: { dbId: "0" }
 */
export function matchRoute(pattern: string, path: string): Record<string, string> | null {
  const patternSegments = splitPathIntoSegments(pattern);
  const pathSegments = splitPathIntoSegments(path);

  // Paths must have the same number of segments to match
  if (patternSegments.length !== pathSegments.length) {
    return null;
  }

  const params: Record<string, string> = {};

  for (let i = 0; i < patternSegments.length; i++) {
    const result = matchPathSegment(patternSegments[i], pathSegments[i]);
    
    if (!result.matched) {
      return null;
    }
    
    if (result.parameter) {
      params[result.parameter.name] = result.parameter.value;
    }
  }

  return params;
}


/**
 * Shared bell detection logic for minders and recorders
 * Single source of truth for detecting build completion signals
 */

export interface BellDetectionResult {
  bellRung: boolean;
  buildStatus: 'success' | 'failure' | 'unknown';
  rawOutput?: string;
}

/**
 * Detect if the bell has been rung in terminal output
 *
 * Expected format (from CLAUDE-*.md instructions):
 * ```
 * 🔔 BUILD COMPLETE 🔔
 * Status: SUCCESS
 * ```
 *
 * @param data Terminal output data
 * @returns Bell detection result
 */
export function detectBell(data: string): BellDetectionResult {
  const result: BellDetectionResult = {
    bellRung: false,
    buildStatus: 'unknown',
  };

  // Primary detection: Look for the canonical bell format
  if (data.includes('🔔 BUILD COMPLETE 🔔')) {
    result.bellRung = true;
    result.rawOutput = data;

    // Extract status from the output
    const lowerData = data.toLowerCase();
    if (lowerData.includes('status: success') || lowerData.includes('status:success')) {
      result.buildStatus = 'success';
    } else if (lowerData.includes('status: failure') || lowerData.includes('status:failure')) {
      result.buildStatus = 'failure';
    }

    return result;
  }

  // Fallback detection: Check for Maven/Gradle build output
  // This catches cases where Claude didn't follow instructions perfectly
  // but the underlying build tool indicated success/failure
  const lowerData = data.toLowerCase();

  if (lowerData.includes('build success') || lowerData.includes('build successful')) {
    result.bellRung = true;
    result.buildStatus = 'success';
    result.rawOutput = data;
    return result;
  }

  if (lowerData.includes('build failure') || lowerData.includes('build failed')) {
    result.bellRung = true;
    result.buildStatus = 'failure';
    result.rawOutput = data;
    return result;
  }

  return result;
}

/**
 * Check if a bell detection result represents a successful build
 */
export function isBuildSuccessful(result: BellDetectionResult): boolean {
  return result.bellRung && result.buildStatus === 'success';
}

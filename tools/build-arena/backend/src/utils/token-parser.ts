/**
 * Token Usage Parser
 *
 * Parses Claude Code's local JSONL logs to extract token usage statistics.
 * Claude Code stores conversation history in ~/.claude/projects/<workspace>/*.jsonl
 */

import { spawn } from 'child_process';

export interface TokenUsage {
  inputTokens: number;
  outputTokens: number;
  cacheCreationTokens: number;
  cacheReadTokens: number;
  totalTokens: number;
}

interface ClaudeMessage {
  role: string;
  content: any;
  usage?: {
    input_tokens?: number;
    output_tokens?: number;
    cache_creation_input_tokens?: number;
    cache_read_input_tokens?: number;
  };
}

/**
 * Extract token usage from a container's Claude Code logs
 *
 * @param containerId Docker container ID
 * @returns Token usage statistics
 */
export async function extractTokenUsage(containerId: string): Promise<TokenUsage> {
  try {
    console.log(`[TokenParser] Extracting token usage from container ${containerId.substring(0, 12)}`);

    // Find JSONL files in the .claude directory
    const findResult = await execInContainer(
      containerId,
      'find /home/builder/.claude/projects -name "*.jsonl" 2>/dev/null || true'
    );

    const jsonlFiles = findResult.trim().split('\n').filter(f => f.length > 0);
    console.log(`[TokenParser] Found ${jsonlFiles.length} JSONL files`);

    if (jsonlFiles.length === 0) {
      console.log('[TokenParser] No JSONL files found, returning zero usage');
      return {
        inputTokens: 0,
        outputTokens: 0,
        cacheCreationTokens: 0,
        cacheReadTokens: 0,
        totalTokens: 0,
      };
    }

    let totalInputTokens = 0;
    let totalOutputTokens = 0;
    let totalCacheCreationTokens = 0;
    let totalCacheReadTokens = 0;

    // Parse each JSONL file
    for (const jsonlPath of jsonlFiles) {
      try {
        // Read the JSONL file
        const content = await execInContainer(containerId, `cat "${jsonlPath}"`);

        // Parse each line as JSON
        const lines = content.split('\n').filter(line => line.trim().length > 0);

        for (const line of lines) {
          try {
            const entry = JSON.parse(line);

            // Check if this is a message entry with usage data
            if (entry.message && entry.message.role === 'assistant' && entry.message.usage) {
              const usage = entry.message.usage;

              totalInputTokens += usage.input_tokens || 0;
              totalOutputTokens += usage.output_tokens || 0;
              totalCacheCreationTokens += usage.cache_creation_input_tokens || 0;
              totalCacheReadTokens += usage.cache_read_input_tokens || 0;
            }
          } catch (parseError) {
            // Skip malformed lines
            continue;
          }
        }
      } catch (fileError) {
        console.error(`[TokenParser] Error reading file ${jsonlPath}:`, fileError);
        continue;
      }
    }

    const result = {
      inputTokens: totalInputTokens,
      outputTokens: totalOutputTokens,
      cacheCreationTokens: totalCacheCreationTokens,
      cacheReadTokens: totalCacheReadTokens,
      totalTokens: totalInputTokens + totalOutputTokens,
    };

    console.log(`[TokenParser] Extracted usage:`, result);
    return result;
  } catch (error) {
    console.error('[TokenParser] Error extracting token usage:', error);
    return {
      inputTokens: 0,
      outputTokens: 0,
      cacheCreationTokens: 0,
      cacheReadTokens: 0,
      totalTokens: 0,
    };
  }
}

/**
 * Execute a command in a Docker container and return stdout
 */
function execInContainer(containerId: string, command: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const docker = spawn('docker', ['exec', containerId, 'sh', '-c', command]);

    let stdout = '';
    let stderr = '';

    docker.stdout.on('data', (data) => {
      stdout += data.toString();
    });

    docker.stderr.on('data', (data) => {
      stderr += data.toString();
    });

    docker.on('close', (code) => {
      if (code === 0) {
        resolve(stdout);
      } else {
        reject(new Error(`Command failed with code ${code}: ${stderr}`));
      }
    });

    docker.on('error', (error) => {
      reject(error);
    });
  });
}

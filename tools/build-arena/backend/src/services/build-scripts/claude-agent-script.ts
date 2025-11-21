import { generateElideScript } from './elide-script.js';
import { generateStandardScript } from './standard-script.js';

type BuildTool = 'elide' | 'standard';

/**
 * Generate startup script for Claude Code agent
 */
export function generateClaudeAgentScript(
  tool: BuildTool,
  repositoryUrl: string,
  instructions: string
): string {
  // Escape instructions for embedding in bash script
  const escapedInstructions = instructions.replace(/`/g, '\\`').replace(/\$/g, '\\$');

  // Get fallback script for this tool
  const fallbackScript = tool === 'elide' ? generateElideScript() : generateStandardScript();

  return `
    set -e
    cd /workspace

    # Create CLAUDE.md with instructions
    cat > CLAUDE.md << 'INSTRUCTIONS_EOF'
${escapedInstructions}
INSTRUCTIONS_EOF

    # Export environment variables
    export REPO_URL="${repositoryUrl}"
    export BUILD_TOOL="${tool}"

    echo "======================================"
    echo "Build Arena - ${tool.toUpperCase()} Team"
    echo "======================================"
    echo ""
    echo "Repository: $REPO_URL"
    echo "Tool: $BUILD_TOOL"
    echo ""
    echo "Starting Claude Code agent..."
    echo ""

    # Start Claude Code in non-interactive mode with proper flags
    # Use --print for non-interactive mode
    # --output-format json for structured output
    # --max-turns to limit iterations
    # The ANTHROPIC_API_KEY is pre-configured in the environment

    claude --print --output-format json --max-turns 50 "$(cat CLAUDE.md)" 2>&1 | tee /workspace/claude-output.log

    CLAUDE_EXIT_CODE=\${PIPESTATUS[0]}

    if [ \$CLAUDE_EXIT_CODE -ne 0 ]; then
      echo "Claude Code exited with code: \$CLAUDE_EXIT_CODE"
      echo "Falling back to direct execution..."

      ${fallbackScript}
    else
      echo "Claude Code execution completed successfully"
    fi

    echo ""
    echo "======================================"
    echo "Agent execution complete"
    echo "======================================"
  `;
}

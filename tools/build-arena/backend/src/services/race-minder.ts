/**
 * Race Minder Service
 *
 * Connects to a race container's terminal WebSocket and:
 * 1. Sends the initial Claude Code command
 * 2. Auto-approves all permission prompts
 * 3. Detects completion signals (bell ringing)
 *
 * The frontend connects to the same WebSocket in view-only mode to display output.
 */

import WebSocket from 'ws';

export interface MinderConfig {
  containerId: string;
  repoUrl: string;
  buildType: 'elide' | 'standard';
  wsUrl: string;
}

export interface MinderResult {
  success: boolean;
  bellRung: boolean;
  duration: number;
  error?: string;
}

// Global registry of active minders for debugging
const activeMinders = new Map<string, RaceMinder>();

export function getActiveMinders(): Map<string, RaceMinder> {
  return activeMinders;
}

export class RaceMinder {
  private ws: WebSocket | null = null;
  private config: MinderConfig;
  private startTime: number;
  private outputBuffer: string = '';
  private lastOutputSnippet: string = ''; // For debugging - last 200 chars of output

  // State tracking
  private themeHandled = false;
  private apiKeyHandled = false;
  private trustHandled = false;
  private workspaceTrustHandled = false;
  private gitCloneHandled = false;
  private claudeStarted = false;
  private lastApprovalTime = 0;
  private lastErrorTime = 0;
  private bellRung = false;
  private lastActivity: string = 'Initializing';
  private approvalCount = 0;

  constructor(config: MinderConfig) {
    this.config = config;
    this.startTime = Date.now();
    // Register in global map
    activeMinders.set(config.containerId, this);
  }

  /**
   * Get current minder status for debugging
   */
  getStatus() {
    return {
      containerId: this.config.containerId,
      buildType: this.config.buildType,
      repoUrl: this.config.repoUrl,
      connected: this.ws !== null && this.ws.readyState === 1,
      uptime: Math.floor((Date.now() - this.startTime) / 1000),
      lastActivity: this.lastActivity,
      approvalCount: this.approvalCount,
      state: {
        themeHandled: this.themeHandled,
        apiKeyHandled: this.apiKeyHandled,
        trustHandled: this.trustHandled,
        workspaceTrustHandled: this.workspaceTrustHandled,
        claudeStarted: this.claudeStarted,
        bellRung: this.bellRung,
      },
      lastOutputSnippet: this.lastOutputSnippet,
    };
  }

  /**
   * Start the minder - connect and begin auto-approval
   */
  async start(): Promise<MinderResult> {
    return new Promise((resolve, reject) => {
      console.log(`[Minder:${this.config.buildType}] Starting for container ${this.config.containerId.substring(0, 12)}`);

      this.ws = new WebSocket(this.config.wsUrl);

      this.ws.on('open', () => {
        console.log(`[Minder:${this.config.buildType}] Connected to WebSocket`);

        // Wait for bash to initialize, then send claude command
        setTimeout(() => {
          const command = `claude "Clone ${this.config.repoUrl}, analyze the project structure, then build it following the instructions in /workspace/CLAUDE.md. Time the build and report results with a bell signal."\n`;

          console.log(`[Minder:${this.config.buildType}] Sending Claude command`);
          this.sendInput(command);
        }, 2000);
      });

      this.ws.on('message', (data: Buffer) => {
        this.handleMessage(data, resolve);
      });

      this.ws.on('error', (error) => {
        console.error(`[Minder:${this.config.buildType}] WebSocket error:`, error.message);
        reject(error);
      });

      this.ws.on('close', () => {
        const duration = Date.now() - this.startTime;
        console.log(`[Minder:${this.config.buildType}] WebSocket closed. Duration: ${duration}ms, Bell rung: ${this.bellRung}`);

        resolve({
          success: this.bellRung,
          bellRung: this.bellRung,
          duration: Math.round(duration / 1000),
        });
      });

      // Timeout after 10 minutes
      setTimeout(() => {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
          console.log(`[Minder:${this.config.buildType}] Timeout after 10 minutes`);
          this.ws.close();
          resolve({
            success: false,
            bellRung: this.bellRung,
            duration: 600,
            error: 'Timeout',
          });
        }
      }, 600000);
    });
  }

  /**
   * Handle incoming WebSocket messages
   */
  private handleMessage(data: Buffer, resolve: (result: MinderResult) => void): void {
    try {
      const message = JSON.parse(data.toString());

      if (message.type === 'output') {
        const output = message.data.toString();

        // Track last output snippet for debugging
        this.lastOutputSnippet = output.slice(-200);

        // Detect when Claude Code starts
        if (!this.claudeStarted && output.includes('Welcome to Claude Code')) {
          this.claudeStarted = true;
          console.log(`[Minder:${this.config.buildType}] Claude Code started`);
        }

        // Check if Claude is thinking (don't fill buffer during this time)
        const isThinking = /(?:Swooping|Pollinating|Calculating|Stewing|Thinking)…/i.test(output);

        // Add to buffer for approval detection (only after Claude starts and not thinking)
        if (this.claudeStarted && this.themeHandled && this.apiKeyHandled && !isThinking) {
          this.outputBuffer += output;
          if (this.outputBuffer.length > 1000) {
            this.outputBuffer = this.outputBuffer.slice(-1000);
          }
        }

        // Clear buffer when thinking to prevent stale matches
        if (isThinking) {
          this.outputBuffer = '';
        }

        // Handle API errors - auto-retry
        if (this.claudeStarted &&
            (output.includes('API Error: 500') ||
             output.includes('Internal server error') ||
             output.includes('"type":"api_error"'))) {

          const now = Date.now();
          if (now - this.lastErrorTime > 5000) {
            console.log(`[Minder:${this.config.buildType}] API Error - retrying`);
            setTimeout(() => {
              this.sendInput('let\'s try that again\n');
              this.lastErrorTime = now;
            }, 1000);
          }
        }

        // Auto-handle setup prompts
        this.handleSetupPrompts(output);

        // Auto-handle permission prompts (includes workspace trust)
        this.handlePermissionPrompts(output);

        // Check for completion signals
        this.checkCompletion(output, resolve);
      }
    } catch (err) {
      // Ignore parse errors
    }
  }

  /**
   * Handle initial setup prompts (theme, API key confirmation, workspace trust)
   */
  private handleSetupPrompts(output: string): void {
    // Theme selection
    if (!this.themeHandled && output.includes('Choose the text style')) {
      console.log(`[Minder:${this.config.buildType}] Auto-selecting theme`);
      setTimeout(() => {
        this.sendInput('\r');
        this.themeHandled = true;
      }, 1000);
    }

    // If we see "Welcome to Claude Code", theme is done
    if (!this.themeHandled && output.includes('Welcome to Claude Code')) {
      console.log(`[Minder:${this.config.buildType}] Claude Code started`);
      this.themeHandled = true;
    }

    // API key confirmation - "Do you want to use this API key?"
    // Select "Yes" (option 1)
    // Check buffer for the full context since messages come in fragments
    // Note: With .claude.json, onboarding is skipped so themeHandled may never be set
    if (!this.apiKeyHandled) {
      // Look for ANTHROPIC_API_KEY specifically (not just box borders - workspace trust also has boxes!)
      const hasApiKeyPrompt = output.includes('ANTHROPIC_API_KEY:') ||
                              output.includes('Detected a custom API key') ||
                              output.includes('Do you want to use this API key?') ||
                              this.outputBuffer.includes('ANTHROPIC_API_KEY:') ||
                              this.outputBuffer.includes('Detected a custom API key');

      if (hasApiKeyPrompt) {
        // Mark as handled IMMEDIATELY to prevent duplicate approvals from subsequent fragments
        this.apiKeyHandled = true;
        this.lastActivity = 'Approving API key';
        this.approvalCount++;
        console.log(`[Minder:${this.config.buildType}] ✅ API KEY PROMPT DETECTED! Auto-approving...`);

        setTimeout(() => {
          // Press UP arrow to select "1. Yes" then Enter
          this.sendInput('\x1b[A');  // UP arrow
          setTimeout(() => {
            this.sendInput('\r');    // Enter
            console.log(`[Minder:${this.config.buildType}] API key approved - auth successful`);
            // Mark theme/trust as handled since we're past onboarding
            this.themeHandled = true;
            this.trustHandled = true;
            this.gitCloneHandled = true;
          }, 500);
        }, 1000);
      }
    }

    // Workspace trust
    if (this.themeHandled && this.apiKeyHandled && !this.trustHandled &&
        (/trust|Trust|workspace|Workspace/.test(output))) {
      console.log(`[Minder:${this.config.buildType}] Auto-trusting workspace`);
      setTimeout(() => {
        this.sendInput('\r');
        this.trustHandled = true;
        this.gitCloneHandled = true; // Enable generic approvals
      }, 1000);
    }
  }

  /**
   * Handle generic permission prompts (after setup is complete)
   */
  private handlePermissionPrompts(output: string): void {
    // Check if this is the workspace trust prompt
    const isWorkspaceTrust = output.includes('Do you trust the files') ||
                             output.includes('Do you want to work in this folder');

    // WORKSPACE TRUST: Handle separately with immediate approval
    if (isWorkspaceTrust) {
      // Skip if already handled
      if (this.workspaceTrustHandled) {
        return;
      }

      // Mark as handled IMMEDIATELY to prevent duplicate approvals from subsequent fragments
      this.workspaceTrustHandled = true;
      this.gitCloneHandled = true;
      this.lastActivity = 'Approving workspace trust';
      this.approvalCount++;

      console.log(`[Minder:${this.config.buildType}] Workspace trust prompt detected - auto-approving`);

      // Send Enter immediately (assume "Yes" is default selection)
      // No setTimeout, no delays - just approve and move on
      this.sendInput('\r');
      this.lastApprovalTime = Date.now();

      // Clear output buffer to prevent stale matches
      this.outputBuffer = '';

      console.log(`[Minder:${this.config.buildType}] Workspace trust approved`);

      // Return early - don't fall through to generic approval logic
      return;
    }

    // GENERIC PERMISSION PROMPTS: Handle other prompts (git clone, bash commands, etc.)
    // Skip if we're still in workspace trust prompt (fragments may arrive separately)
    if (this.workspaceTrustHandled && Date.now() - this.lastApprovalTime < 5000) {
      // Within 5 seconds of workspace trust approval - ignore other prompts
      // This prevents fragments of the workspace trust prompt from triggering generic handler
      return;
    }

    // Only detect actual permission prompts, not UI refreshes
    // Must have BOTH a prompt question AND option choices
    const hasPromptQuestion = output.includes('Do you want to proceed?') ||
                              output.includes('proceed?') ||
                              output.includes('Enter to confirm');

    // Look for numbered options (1. Yes, 2. Yes/No, etc.)
    const hasOptionChoices = /[❯\s]*1\.\s*(Yes|Allow|Approve)/i.test(output);

    const hasPermissionPrompt = hasPromptQuestion && hasOptionChoices;

    if (hasPermissionPrompt) {
      const now = Date.now();
      if (now - this.lastApprovalTime > 2000) { // 2 second debounce
        // Update lastApprovalTime immediately to prevent duplicate attempts
        this.lastApprovalTime = now;

        // Extract command from output for logging
        const commandMatch = output.match(/Bash command\s+([^\n]+)/i) ||
                             output.match(/Bash\(([^)]+)\)/);
        const command = commandMatch ? commandMatch[1].trim() : 'permission request';

        this.lastActivity = `Approving: ${command.substring(0, 50)}`;
        this.approvalCount++;

        console.log(`[Minder:${this.config.buildType}] Auto-approving: ${command}`);

        // Clear output buffer to prevent stale matches
        this.outputBuffer = '';

        setTimeout(() => {
          // Check which option is currently selected by looking for the marker
          const yesIsSelected = /❯\s*1\..*Yes/i.test(output) ||
                                output.includes('❯ 1. Yes');

          const noIsSelected = /❯\s*2\..*No/i.test(output) ||
                               output.includes('❯ 2. No');

          if (yesIsSelected) {
            // "Yes" is already selected, just press Enter
            console.log(`[Minder:${this.config.buildType}] "Yes" already selected, pressing Enter`);
            this.sendInput('\r');
          } else if (noIsSelected) {
            // "No" is selected, press UP arrow then Enter
            console.log(`[Minder:${this.config.buildType}] "No" selected, sending UP arrow first`);
            this.sendInput('\x1b[A');  // UP arrow to select "1. Yes"
            setTimeout(() => {
              this.sendInput('\r');  // Then Enter
            }, 500);
          } else {
            // Can't determine selection, just press Enter (hope for the best)
            console.log(`[Minder:${this.config.buildType}] Selection unclear, pressing Enter`);
            this.sendInput('\r');
          }
        }, 1000);
      }
    }
  }

  /**
   * Check for completion signals (bell ringing)
   */
  private checkCompletion(output: string, _resolve: (result: MinderResult) => void): void {
    const completionPatterns = [
      /🔔/,
      /BUILD COMPLETE/i,
      /\[BUILD COMPLETE\]/i,
      /Build succeeded/i,
      /Build failed/i,
      /BUILD SUCCESS/i,
      /BUILD FAILURE/i,
      /BUILD SUCCEEDED/i,
      /BUILD FAILED/i,
      /\[SUCCESS\]/i,
      /\[FAILED\]/i,
      /Total time:/i,
      /BUILD SUCCESSFUL/i,
    ];

    const isComplete = completionPatterns.some(p => p.test(output));
    if (isComplete && !this.bellRung) {
      this.bellRung = true;
      const match = completionPatterns.find(p => p.test(output));
      console.log(`[Minder:${this.config.buildType}] Bell rung! Pattern: ${match}`);

      // Wait a bit to capture final output, then close
      setTimeout(() => {
        if (this.ws) {
          this.ws.close();
        }
      }, 3000);
    }
  }

  /**
   * Send input to the terminal
   */
  private sendInput(data: string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify({
        type: 'input',
        data
      }));
    }
  }

  /**
   * Stop the minder
   */
  stop(): void {
    if (this.ws) {
      this.ws.close();
    }
    // Remove from registry
    activeMinders.delete(this.config.containerId);
  }
}

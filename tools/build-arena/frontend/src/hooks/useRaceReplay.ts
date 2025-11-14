import { Terminal } from '@xterm/xterm';

interface RecordingMessage {
  ts: number;
  msg: {
    type: string;
    data?: string;
  };
}

interface Recording {
  messages: RecordingMessage[];
  duration: number;
  metadata: Record<string, any>;
}

/**
 * Play recorded messages in a terminal with timing
 */
async function playMessages(terminal: Terminal, messages: RecordingMessage[]): Promise<void> {
  let lastTs = 0;

  for (const { ts, msg } of messages) {
    // Wait for the time difference (capped for faster replay)
    const delay = ts - lastTs;
    if (delay > 0) {
      await new Promise(resolve => setTimeout(resolve, Math.min(delay, 100)));
    }

    // Write message to terminal
    if (msg.type === 'output' && msg.data) {
      terminal.write(msg.data);
    }

    lastTs = ts;
  }
}

/**
 * Load and play a race recording
 */
export async function playRaceRecording(
  jobId: string,
  elideTerminal: Terminal | null,
  standardTerminal: Terminal | null
): Promise<void> {
  if (!elideTerminal || !standardTerminal) {
    throw new Error('Terminals not initialized');
  }

  try {
    // Fetch recordings for both runners
    const [elideRecording, standardRecording] = await Promise.all([
      fetch(`/api/races/${jobId}/recording/elide`).then(r => r.json()) as Promise<Recording>,
      fetch(`/api/races/${jobId}/recording/standard`).then(r => r.json()) as Promise<Recording>
    ]);

    // Clear terminals
    elideTerminal.clear();
    standardTerminal.clear();

    // Write headers
    elideTerminal.writeln('\x1b[1;36m🏁 ELIDE RUNNER - Replay Mode\x1b[0m');
    elideTerminal.writeln('\x1b[90m' + '='.repeat(50) + '\x1b[0m\n');

    standardTerminal.writeln('\x1b[1;36m🏁 MAVEN/GRADLE RUNNER - Replay Mode\x1b[0m');
    standardTerminal.writeln('\x1b[90m' + '='.repeat(50) + '\x1b[0m\n');

    // Play both recordings simultaneously
    await Promise.all([
      playMessages(elideTerminal, elideRecording.messages),
      playMessages(standardTerminal, standardRecording.messages)
    ]);
  } catch (err) {
    console.error('Error playing recording:', err);
    throw new Error('Failed to load race replay');
  }
}

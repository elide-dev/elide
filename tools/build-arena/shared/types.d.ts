/**
 * Shared types for Build Arena frontend and backend
 */
export type BuildTool = 'elide' | 'standard';
export type JobStatus = 'queued' | 'running' | 'completed' | 'failed' | 'cancelled';
export interface BuildJob {
    id: string;
    repositoryUrl: string;
    repositoryName: string;
    createdAt: string;
    status: JobStatus;
    elideResult?: BuildResult;
    standardResult?: BuildResult;
}
export interface BuildResult {
    tool: BuildTool;
    status: 'running' | 'success' | 'failure';
    startTime: string;
    endTime?: string;
    duration?: number;
    exitCode?: number;
    metrics?: BuildMetrics;
}
export interface BuildMetrics {
    buildTime: number;
    memoryUsage: number;
    cpuUsage: number;
    artifactSize?: number;
}
export interface TerminalOutput {
    jobId: string;
    tool: BuildTool;
    type: 'stdout' | 'stderr';
    data: string;
    timestamp: string;
}
export interface SubmitJobRequest {
    repositoryUrl: string;
}
export interface SubmitJobResponse {
    jobId: string;
    message: string;
}
export interface JobStatusResponse {
    job: BuildJob;
}
export type WebSocketMessageType = 'subscribe' | 'unsubscribe' | 'terminal_output' | 'terminal_input' | 'build_started' | 'build_completed' | 'build_failed' | 'build_bell' | 'error';
export interface WebSocketMessage {
    type: WebSocketMessageType;
    payload: unknown;
}
export interface SubscribeMessage extends WebSocketMessage {
    type: 'subscribe';
    payload: {
        jobId: string;
    };
}
export interface TerminalOutputMessage extends WebSocketMessage {
    type: 'terminal_output';
    payload: TerminalOutput;
}
export interface BuildStartedMessage extends WebSocketMessage {
    type: 'build_started';
    payload: {
        jobId: string;
        tool: BuildTool;
        timestamp: string;
    };
}
export interface BuildCompletedMessage extends WebSocketMessage {
    type: 'build_completed';
    payload: {
        jobId: string;
        tool: BuildTool;
        result: BuildResult;
    };
}
export interface ErrorMessage extends WebSocketMessage {
    type: 'error';
    payload: {
        message: string;
        code?: string;
    };
}
export interface TerminalInputMessage extends WebSocketMessage {
    type: 'terminal_input';
    payload: {
        jobId: string;
        tool: BuildTool;
        data: string;
    };
}
export interface BuildBellMessage extends WebSocketMessage {
    type: 'build_bell';
    payload: {
        jobId: string;
        tool: BuildTool;
        timestamp: string;
        message?: string;
    };
}
//# sourceMappingURL=types.d.ts.map
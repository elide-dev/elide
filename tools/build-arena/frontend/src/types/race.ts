export interface RaceRunnerStatus {
  status: 'pending' | 'running' | 'completed' | 'failed';
  duration?: number;
  containerId?: string;
}

export interface StandardRunnerStatus extends RaceRunnerStatus {
  buildTool?: 'maven' | 'gradle';
}

export interface RaceStatistics {
  totalRaces: number;
  elideWins: number;
  standardWins: number;
  ties: number;
  elideAvgDuration: number;
  standardAvgDuration: number;
}

export interface RaceStatus {
  jobId: string;
  repositoryUrl: string;
  repositoryName: string;
  mode: 'replay' | 'live';
  status: 'pending' | 'running' | 'completed' | 'failed';
  startedAt?: string;
  ready?: boolean; // Whether containers are ready for WebSocket connections
  elide: RaceRunnerStatus;
  standard: StandardRunnerStatus;
  winner?: 'elide' | 'standard' | 'tie';
  stats?: RaceStatistics;
}

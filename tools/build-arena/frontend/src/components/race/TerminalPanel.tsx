import { formatTime } from '../../hooks/useRaceTimer';

interface TerminalPanelProps {
  title: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  duration?: number;
  elapsedTime: number;
  terminalRef: React.RefObject<HTMLDivElement>;
}

export function TerminalPanel({
  title,
  status,
  duration,
  elapsedTime,
  terminalRef,
}: TerminalPanelProps) {
  const getStatusColor = () => {
    switch (status) {
      case 'running':
        return 'bg-green-500 animate-pulse';
      case 'completed':
        return 'bg-blue-500';
      case 'failed':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  const displayTime = duration !== undefined ? `${duration.toFixed(0)}s` : formatTime(elapsedTime);

  return (
    <div className="bg-slate-800 rounded-lg shadow-xl overflow-hidden">
      <div className="bg-slate-700 px-4 py-2 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className={`w-3 h-3 rounded-full ${getStatusColor()}`} />
          <span className="font-medium">{title}</span>
        </div>
        <span className="text-sm text-gray-300 font-mono">⏱️ {displayTime}</span>
      </div>
      <div ref={terminalRef} className="p-2 overflow-x-auto" />
    </div>
  );
}

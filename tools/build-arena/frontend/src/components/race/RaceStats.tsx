import { RaceStatistics } from '../../types/race';

interface RaceStatsProps {
  stats: RaceStatistics;
}

export function RaceStats({ stats }: RaceStatsProps) {
  const winRate = Math.round((stats.elideWins / stats.totalRaces) * 100);

  return (
    <div className="mt-4 grid grid-cols-4 gap-4 text-center text-sm">
      <div>
        <p className="text-gray-400">Total Races</p>
        <p className="text-xl font-bold">{stats.totalRaces}</p>
      </div>
      <div>
        <p className="text-gray-400">Elide Wins</p>
        <p className="text-xl font-bold text-green-400">{stats.elideWins}</p>
      </div>
      <div>
        <p className="text-gray-400">Standard Wins</p>
        <p className="text-xl font-bold text-blue-400">{stats.standardWins}</p>
      </div>
      <div>
        <p className="text-gray-400">Win Rate</p>
        <p className="text-xl font-bold text-indigo-400">{winRate}%</p>
      </div>
    </div>
  );
}

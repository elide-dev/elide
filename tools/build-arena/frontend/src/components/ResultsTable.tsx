import useSWR from 'swr';
import type { BuildJob } from '../../../shared/types';

const fetcher = (url: string) => fetch(url).then((res) => res.json());

interface ResultsTableProps {
  limit?: number;
}

/**
 * Calculate speedup percentage
 * Positive = Elide is faster
 * Negative = Standard is faster
 */
function calculateSpeedup(elideDuration: number, standardDuration: number): number {
  return ((standardDuration - elideDuration) / standardDuration) * 100;
}

/**
 * Format duration in seconds to human-readable format
 */
function formatDuration(seconds: number): string {
  if (seconds < 60) {
    return `${seconds.toFixed(1)}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes}m ${secs.toFixed(0)}s`;
}

export function ResultsTable({ limit = 20 }: ResultsTableProps) {
  const { data, error, isLoading } = useSWR<{ jobs: BuildJob[] }>(
    `/api/jobs/recent/results?limit=${limit}`,
    fetcher,
    {
      refreshInterval: 30000, // Refresh every 30 seconds
    }
  );

  if (isLoading) {
    return (
      <div className="w-full max-w-6xl mx-auto mb-8">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">Recent Build Comparisons</h2>
        <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
          Loading recent results...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full max-w-6xl mx-auto mb-8">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">Recent Build Comparisons</h2>
        <div className="bg-white rounded-lg shadow p-8 text-center text-red-600">
          Failed to load recent results
        </div>
      </div>
    );
  }

  const jobs = data?.jobs || [];

  if (jobs.length === 0) {
    return (
      <div className="w-full max-w-6xl mx-auto mb-8">
        <h2 className="text-2xl font-bold mb-4 text-gray-800">Recent Build Comparisons</h2>
        <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
          No completed builds yet. Submit a repository to see results!
        </div>
      </div>
    );
  }

  return (
    <div className="w-full max-w-6xl mx-auto mb-8">
      <h2 className="text-2xl font-bold mb-4 text-gray-800">Recent Build Comparisons</h2>
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Repository
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Elide
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Standard
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Speedup
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Date
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {jobs.map((job) => {
                const elideDuration = job.elideResult?.duration || 0;
                const standardDuration = job.standardResult?.duration || 0;
                const speedup = calculateSpeedup(elideDuration, standardDuration);
                const isElideWinner = speedup > 0;

                return (
                  <tr key={job.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{job.repositoryName}</div>
                      <div className="text-xs text-gray-500 truncate max-w-xs">
                        {job.repositoryUrl}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{formatDuration(elideDuration)}</div>
                      {job.elideResult?.status === 'success' && (
                        <div className="text-xs text-green-600">Success</div>
                      )}
                      {job.elideResult?.status === 'failure' && (
                        <div className="text-xs text-red-600">Failed</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{formatDuration(standardDuration)}</div>
                      {job.standardResult?.status === 'success' && (
                        <div className="text-xs text-green-600">Success</div>
                      )}
                      {job.standardResult?.status === 'failure' && (
                        <div className="text-xs text-red-600">Failed</div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {isElideWinner ? (
                        <div className="inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold bg-green-100 text-green-800">
                          {speedup.toFixed(1)}% faster
                        </div>
                      ) : (
                        <div className="inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold bg-red-100 text-red-800">
                          {Math.abs(speedup).toFixed(1)}% slower
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(job.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

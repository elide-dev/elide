'use client'

import type { BuildResult } from '@shared/types'

interface BuildMetricsProps {
  elideResult?: BuildResult
  standardResult?: BuildResult
}

function formatDuration(ms?: number): string {
  if (!ms) return 'N/A'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

function formatBytes(bytes?: number): string {
  if (!bytes) return 'N/A'
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(2)} MB`
}

function calculateSpeedup(elideMs?: number, standardMs?: number): string {
  if (!elideMs || !standardMs) return 'N/A'
  const speedup = ((standardMs - elideMs) / standardMs) * 100
  if (speedup > 0) {
    return `${speedup.toFixed(1)}% faster`
  } else if (speedup < 0) {
    return `${Math.abs(speedup).toFixed(1)}% slower`
  }
  return 'Same speed'
}

export function BuildMetrics({ elideResult, standardResult }: BuildMetricsProps) {
  const speedup = calculateSpeedup(elideResult?.duration, standardResult?.duration)

  return (
    <div className="bg-slate-800 rounded-lg shadow-xl p-6">
      <h3 className="text-xl font-bold text-white mb-4">Performance Comparison</h3>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Build Time */}
        <div className="bg-slate-700 rounded-lg p-4">
          <h4 className="text-sm font-medium text-gray-400 mb-2">Build Time</h4>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-bold text-elide-primary">
              {formatDuration(elideResult?.duration)}
            </span>
            <span className="text-sm text-gray-400">Elide</span>
          </div>
          <div className="flex items-baseline space-x-2 mt-2">
            <span className="text-2xl font-bold text-gray-300">
              {formatDuration(standardResult?.duration)}
            </span>
            <span className="text-sm text-gray-400">Standard</span>
          </div>
          <div className="mt-3 pt-3 border-t border-slate-600">
            <span className="text-sm font-semibold text-green-400">{speedup}</span>
          </div>
        </div>

        {/* Memory Usage */}
        <div className="bg-slate-700 rounded-lg p-4">
          <h4 className="text-sm font-medium text-gray-400 mb-2">Peak Memory</h4>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-bold text-elide-primary">
              {formatBytes(elideResult?.metrics?.memoryUsage)}
            </span>
            <span className="text-sm text-gray-400">Elide</span>
          </div>
          <div className="flex items-baseline space-x-2 mt-2">
            <span className="text-2xl font-bold text-gray-300">
              {formatBytes(standardResult?.metrics?.memoryUsage)}
            </span>
            <span className="text-sm text-gray-400">Standard</span>
          </div>
        </div>

        {/* CPU Usage */}
        <div className="bg-slate-700 rounded-lg p-4">
          <h4 className="text-sm font-medium text-gray-400 mb-2">Avg CPU Usage</h4>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-bold text-elide-primary">
              {elideResult?.metrics?.cpuUsage?.toFixed(1) || 'N/A'}
              {elideResult?.metrics?.cpuUsage && '%'}
            </span>
            <span className="text-sm text-gray-400">Elide</span>
          </div>
          <div className="flex items-baseline space-x-2 mt-2">
            <span className="text-2xl font-bold text-gray-300">
              {standardResult?.metrics?.cpuUsage?.toFixed(1) || 'N/A'}
              {standardResult?.metrics?.cpuUsage && '%'}
            </span>
            <span className="text-sm text-gray-400">Standard</span>
          </div>
        </div>
      </div>

      {/* Winner Banner */}
      {elideResult && standardResult && (
        <div className="mt-6 p-4 bg-gradient-to-r from-elide-primary/20 to-elide-secondary/20 border border-elide-primary/50 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-lg font-bold text-white">
                {elideResult.duration && standardResult.duration &&
                 elideResult.duration < standardResult.duration
                  ? '🏆 Elide Wins!'
                  : standardResult.duration < elideResult.duration
                  ? '🏆 Standard Wins!'
                  : '🤝 Tie!'}
              </p>
              <p className="text-sm text-gray-300 mt-1">
                {elideResult.duration && standardResult.duration &&
                  `Elide ${
                    elideResult.duration < standardResult.duration
                      ? 'completed the build faster'
                      : 'took longer this time'
                  }`}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

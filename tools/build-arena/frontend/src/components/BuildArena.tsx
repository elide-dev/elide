'use client'

import { useEffect, useState } from 'react'
import useSWR from 'swr'
import { Terminal } from './Terminal'
import { BuildMetrics } from './BuildMetrics'
import type { BuildJob, BuildResult } from '@shared/types'

interface BuildArenaProps {
  jobId: string
}

const fetcher = (url: string) => fetch(url).then((res) => res.json())

export function BuildArena({ jobId }: BuildArenaProps) {
  const { data, error } = useSWR<{ job: BuildJob }>(
    `http://localhost:3001/api/jobs/${jobId}`,
    fetcher,
    { refreshInterval: 1000 }
  )

  if (error) {
    return (
      <div className="bg-red-900/50 border border-red-700 text-red-200 px-4 py-3 rounded-lg">
        Failed to load job data
      </div>
    )
  }

  if (!data) {
    return (
      <div className="text-center text-gray-300">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-elide-primary mx-auto mb-4"></div>
        Loading...
      </div>
    )
  }

  const { job } = data

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-slate-800 rounded-lg shadow-xl p-6">
        <h2 className="text-2xl font-bold text-white mb-2">{job.repositoryName}</h2>
        <p className="text-gray-400">{job.repositoryUrl}</p>
        <div className="mt-4 flex items-center space-x-4">
          <span className="text-sm text-gray-400">Status:</span>
          <span
            className={`px-3 py-1 rounded-full text-sm font-semibold ${
              job.status === 'running'
                ? 'bg-yellow-500/20 text-yellow-300'
                : job.status === 'completed'
                ? 'bg-green-500/20 text-green-300'
                : job.status === 'failed'
                ? 'bg-red-500/20 text-red-300'
                : 'bg-blue-500/20 text-blue-300'
            }`}
          >
            {job.status}
          </span>
        </div>
      </div>

      {/* Metrics Comparison */}
      {(job.elideResult || job.standardResult) && (
        <BuildMetrics elideResult={job.elideResult} standardResult={job.standardResult} />
      )}

      {/* Dual Terminal View */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Elide Terminal */}
        <div className="bg-slate-800 rounded-lg shadow-xl overflow-hidden">
          <div className="bg-elide-primary px-6 py-3">
            <h3 className="text-lg font-bold text-white">Elide</h3>
          </div>
          <div className="h-[500px]">
            <Terminal jobId={jobId} tool="elide" />
          </div>
        </div>

        {/* Standard Terminal */}
        <div className="bg-slate-800 rounded-lg shadow-xl overflow-hidden">
          <div className="bg-slate-600 px-6 py-3">
            <h3 className="text-lg font-bold text-white">Standard Toolchain</h3>
          </div>
          <div className="h-[500px]">
            <Terminal jobId={jobId} tool="standard" />
          </div>
        </div>
      </div>
    </div>
  )
}

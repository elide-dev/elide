import { useQuery } from '@tanstack/react-query'
import { API_BASE_URL } from '../config'

export type DiscoveredDatabase = {
  path: string
  size: number
  lastModified: number
  isLocal: boolean
}

type DatabasesResponse = {
  databases: DiscoveredDatabase[]
}

async function fetchDatabases(): Promise<DiscoveredDatabase[]> {
  const res = await fetch(`${API_BASE_URL}/api/databases`)
  if (!res.ok) {
    throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  }
  const data: DatabasesResponse = await res.json()
  return data.databases
}

export function useDatabases() {
  return useQuery({
    queryKey: ['databases'],
    queryFn: fetchDatabases,
  })
}

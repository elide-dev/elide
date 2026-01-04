import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`
}

export function formatDate(timestamp: number) {
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) return `${minutes}m ago`
  if (hours < 24) return `${hours}h ago`
  return `${days}d ago`
}

export function formatRowCount(count: number): string {
  if (count < 1000) return count.toString()
  if (count < 1000000) {
    const k = Math.floor(count / 100) / 10
    return k % 1 === 0 ? `${k}K` : `${k.toFixed(1)}K`
  }
  const m = Math.floor(count / 100000) / 10
  return m % 1 === 0 ? `${m}M` : `${m.toFixed(1)}M`
}

/**
 * Utilities for parsing and building URL search parameters
 */

import type { Filter, PaginationParams, SortingParams } from './types'
import { DEFAULT_LIMIT, DEFAULT_OFFSET } from './constants'

/**
 * Parse pagination parameters from URLSearchParams
 */
export function parsePaginationParams(searchParams: URLSearchParams | null): PaginationParams {
  if (!searchParams) {
    return { limit: DEFAULT_LIMIT, offset: DEFAULT_OFFSET }
  }

  return {
    limit: parseInt(searchParams.get('limit') || String(DEFAULT_LIMIT), 10),
    offset: parseInt(searchParams.get('offset') || String(DEFAULT_OFFSET), 10),
  }
}

/**
 * Parse sorting parameters from URLSearchParams
 */
export function parseSortingParams(searchParams: URLSearchParams | null): SortingParams {
  if (!searchParams) {
    return { column: null, direction: null }
  }

  const sort = searchParams.get('sort')
  const order = searchParams.get('order')

  if (sort && order && (order === 'asc' || order === 'desc')) {
    return { column: sort, direction: order }
  }

  return { column: null, direction: null }
}

/**
 * Parse filter parameters from URLSearchParams
 */
export function parseFilterParams(searchParams: URLSearchParams | null): Filter[] {
  if (!searchParams) {
    return []
  }

  const whereParam = searchParams.get('where')
  if (!whereParam) {
    return []
  }

  try {
    const decoded = decodeURIComponent(whereParam)
    const parsed = JSON.parse(decoded)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

/**
 * Build URL search params from table state
 */
export function buildSearchParams(
  pagination: PaginationParams,
  sorting: SortingParams,
  filters: Filter[]
): Record<string, string> {
  const params: Record<string, string> = {}

  // Always include pagination
  params.limit = pagination.limit.toString()
  params.offset = pagination.offset.toString()

  // Include sorting if set
  if (sorting.column && sorting.direction) {
    params.sort = sorting.column
    params.order = sorting.direction
  }

  // Include filters if any
  if (filters.length > 0) {
    params.where = encodeURIComponent(JSON.stringify(filters))
  }

  return params
}

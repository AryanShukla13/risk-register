import type { Severity } from '../api/types'

/**
 * Used ONLY for the live preview in the risk form. For stored risks the server
 * returns scores and severities, and is the source of truth.
 */
export const inherentScore = (likelihood: number, impact: number) => likelihood * impact

export function severityBand(score: number): Severity {
  if (score >= 20) return 'CRITICAL'
  if (score >= 13) return 'HIGH'
  if (score >= 6) return 'MEDIUM'
  return 'LOW'
}

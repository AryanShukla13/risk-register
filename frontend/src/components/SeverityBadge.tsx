import type { Severity } from '../api/types'

const LABEL: Record<Severity, string> = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' }

/** Colour is reinforced with the band name so it never relies on colour alone. */
export function SeverityBadge({ score, severity }: { score: number; severity: Severity }) {
  return (
    <span className={`badge badge-${severity.toLowerCase()}`} data-testid="severity-badge">
      {score} · {LABEL[severity]}
    </span>
  )
}

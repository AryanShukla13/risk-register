import { Link } from 'react-router-dom'
import { SeverityBadge } from '../../components/SeverityBadge'
import type { RiskSummary } from '../../api/types'

export function RiskTable({ risks }: { risks: RiskSummary[] }) {
  if (risks.length === 0) return <p>No risks match. Try different filters or create one.</p>
  return (
    <table>
      <thead>
        <tr>
          <th>Title</th>
          <th>Category</th>
          <th>Status</th>
          <th>Inherent</th>
          <th>Residual</th>
          <th>Mitigations</th>
        </tr>
      </thead>
      <tbody>
        {risks.map((r) => (
          <tr key={r.id} className={`row-${r.residualSeverity.toLowerCase()}`}>
            <td>
              <Link to={`/risks/${r.id}`}>{r.title}</Link>
            </td>
            <td>{r.category}</td>
            <td>{r.status}</td>
            <td>
              <SeverityBadge score={r.inherentScore} severity={r.inherentSeverity} />
            </td>
            <td>
              <SeverityBadge score={r.residualScore} severity={r.residualSeverity} />
            </td>
            <td>{r.mitigationCount}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

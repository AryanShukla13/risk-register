import { Link, useNavigate, useParams } from 'react-router-dom'
import { SeverityBadge } from '../../components/SeverityBadge'
import { ErrorMessage, Loading } from '../../components/Status'
import { useDeleteRisk, useRisk } from './hooks'
import { MitigationForm } from './MitigationForm'
import { MitigationList } from './MitigationList'

export function RiskDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const { data: risk, isPending, error, refetch } = useRisk(id)
  const remove = useDeleteRisk()

  if (isPending) return <Loading />
  if (error) return <ErrorMessage error={error} onRetry={() => refetch()} />

  const onDelete = async () => {
    if (!window.confirm(`Delete "${risk.title}" and its mitigations?`)) return
    await remove.mutateAsync(risk.id)
    navigate('/')
  }

  return (
    <>
      <Link to="/">← All risks</Link>
      <div className="page-header">
        <h1>{risk.title}</h1>
        <div className="actions">
          <Link className="button" to={`/risks/${risk.id}/edit`}>Edit</Link>
          <button type="button" className="secondary" onClick={onDelete}>Delete</button>
        </div>
      </div>
      {remove.error && <ErrorMessage error={remove.error} />}

      <div className="scores">
        <div>Inherent <SeverityBadge score={risk.inherentScore} severity={risk.inherentSeverity} />
          <div className="muted">{risk.likelihood} × {risk.impact}</div></div>
        <div>Residual <SeverityBadge score={risk.residualScore} severity={risk.residualSeverity} /></div>
      </div>

      <dl>
        <dt>Category</dt><dd>{risk.category}</dd>
        <dt>Status</dt><dd>{risk.status}</dd>
        <dt>Owner</dt><dd>{risk.owner}</dd>
        {risk.description && (<><dt>Description</dt><dd>{risk.description}</dd></>)}
      </dl>

      <h2>Mitigations ({risk.mitigations.length})</h2>
      <MitigationList riskId={risk.id} mitigations={risk.mitigations} />
      <MitigationForm riskId={risk.id} />
    </>
  )
}

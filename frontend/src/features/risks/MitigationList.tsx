import type { Mitigation } from '../../api/types'
import { ErrorMessage } from '../../components/Status'
import { useDeleteMitigation } from './hooks'
import { EFFECTIVENESS_LABELS } from './MitigationForm'

export function MitigationList({ riskId, mitigations }: { riskId: number; mitigations: Mitigation[] }) {
  const remove = useDeleteMitigation(riskId)
  if (mitigations.length === 0) return <p>No mitigations yet, so residual score equals inherent score.</p>
  return (
    <>
      {remove.error && <ErrorMessage error={remove.error} />}
      <ul className="mitigations">
        {mitigations.map((m) => (
          <li key={m.id}>
            <div>
              <strong>{m.description}</strong>
              <div className="muted">Effectiveness: {EFFECTIVENESS_LABELS[m.effectiveness]}</div>
            </div>
            <button type="button" className="secondary" disabled={remove.isPending} onClick={() => remove.mutate(m.id)}>
              Delete
            </button>
          </li>
        ))}
      </ul>
    </>
  )
}

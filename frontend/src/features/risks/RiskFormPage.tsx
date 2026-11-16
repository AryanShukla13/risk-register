import { useNavigate, useParams } from 'react-router-dom'
import { ErrorMessage, Loading } from '../../components/Status'
import type { RiskInput } from '../../api/types'
import { useCreateRisk, useRisk, useUpdateRisk } from './hooks'
import { RiskForm } from './RiskForm'

export function CreateRiskPage() {
  const navigate = useNavigate()
  const create = useCreateRisk()
  return (
    <>
      <h1>New risk</h1>
      <RiskForm
        submitLabel="Create risk"
        onSubmit={async (values) => {
          const risk = await create.mutateAsync(values)
          navigate(`/risks/${risk.id}`)
        }}
      />
    </>
  )
}

export function EditRiskPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const { data, isPending, error } = useRisk(id)
  const update = useUpdateRisk(id)

  if (isPending) return <Loading />
  if (error) return <ErrorMessage error={error} />

  const initial: RiskInput = { ...data, description: data.description ?? '' }
  return (
    <>
      <h1>Edit risk</h1>
      <RiskForm
        initial={initial}
        submitLabel="Save changes"
        onSubmit={async (values) => {
          await update.mutateAsync(values)
          navigate(`/risks/${id}`)
        }}
      />
    </>
  )
}

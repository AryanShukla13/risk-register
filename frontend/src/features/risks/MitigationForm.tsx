import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { SCALE } from '../../api/types'
import { applyServerErrors } from './formErrors'
import { useAddMitigation } from './hooks'
import { mitigationSchema, type MitigationFormValues } from './schema'

export const EFFECTIVENESS_LABELS: Record<number, string> = {
  1: '1 – Barely effective', 2: '2 – Slightly effective', 3: '3 – Moderately effective',
  4: '4 – Very effective', 5: '5 – Nearly eliminates the risk',
}

export function MitigationForm({ riskId }: { riskId: number }) {
  const add = useAddMitigation(riskId)
  const [banner, setBanner] = useState('')
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } =
    useForm<MitigationFormValues>({
      resolver: zodResolver(mitigationSchema),
      defaultValues: { description: '', effectiveness: 3 },
    })

  const submit = handleSubmit(async (values) => {
    setBanner('')
    try {
      await add.mutateAsync(values)
      reset()
    } catch (e) {
      setBanner(applyServerErrors(e, setError, ['description', 'effectiveness']))
    }
  })

  return (
    <form onSubmit={submit} noValidate className="mitigation-form">
      <h3>Add mitigation</h3>
      {banner && <div role="alert" className="error">{banner}</div>}
      <label>Description<input {...register('description')} />
        {errors.description && <span className="field-error">{errors.description.message}</span>}</label>
      <label>Effectiveness
        <select {...register('effectiveness', { valueAsNumber: true })}>
          {SCALE.map((n) => <option key={n} value={n}>{EFFECTIVENESS_LABELS[n]}</option>)}</select>
        {errors.effectiveness && <span className="field-error">{errors.effectiveness.message}</span>}</label>
      <button type="submit" disabled={isSubmitting}>Add mitigation</button>
    </form>
  )
}

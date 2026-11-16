import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { CATEGORIES, SCALE, STATUSES, type RiskInput } from '../../api/types'
import { SeverityBadge } from '../../components/SeverityBadge'
import { inherentScore, severityBand } from '../../lib/scoring'
import { applyServerErrors } from './formErrors'
import { riskSchema, type RiskFormValues } from './schema'

interface Props {
  initial?: RiskInput
  submitLabel: string
  onSubmit: (values: RiskInput) => Promise<unknown>
}

const DEFAULTS: RiskFormValues = {
  title: '', description: '', category: 'OPERATIONAL', owner: '', likelihood: 3, impact: 3, status: 'OPEN',
}

export function RiskForm({ initial, submitLabel, onSubmit }: Props) {
  const [banner, setBanner] = useState('')
  const {
    register, handleSubmit, watch, setError, formState: { errors, isSubmitting },
  } = useForm<RiskFormValues>({ resolver: zodResolver(riskSchema), defaultValues: initial ?? DEFAULTS })

  const likelihood = watch('likelihood')
  const impact = watch('impact')
  const score = inherentScore(likelihood, impact)
  const validScore = Number.isInteger(score) && score >= 1 && score <= 25

  const submit = handleSubmit(async (values) => {
    setBanner('')
    try {
      await onSubmit(values)
    } catch (e) {
      setBanner(applyServerErrors(e, setError, Object.keys(DEFAULTS)))
    }
  })

  return (
    <form onSubmit={submit} noValidate>
      {banner && <div role="alert" className="error">{banner}</div>}

      <label>Title<input {...register('title')} />
        {errors.title && <span className="field-error">{errors.title.message}</span>}</label>
      <label>Description<textarea rows={3} {...register('description')} />
        {errors.description && <span className="field-error">{errors.description.message}</span>}</label>
      <label>Category
        <select {...register('category')}>{CATEGORIES.map((c) => <option key={c}>{c}</option>)}</select></label>
      <label>Owner<input {...register('owner')} />
        {errors.owner && <span className="field-error">{errors.owner.message}</span>}</label>

      <div className="row">
        <label>Likelihood
          <select {...register('likelihood', { valueAsNumber: true })}>
            {SCALE.map((n) => <option key={n}>{n}</option>)}</select>
          {errors.likelihood && <span className="field-error">{errors.likelihood.message}</span>}</label>
        <label>Impact
          <select {...register('impact', { valueAsNumber: true })}>
            {SCALE.map((n) => <option key={n}>{n}</option>)}</select>
          {errors.impact && <span className="field-error">{errors.impact.message}</span>}</label>
        <div className="preview">
          Inherent score
          {validScore && <SeverityBadge score={score} severity={severityBand(score)} />}
        </div>
      </div>

      <label>Status
        <select {...register('status')}>{STATUSES.map((s) => <option key={s}>{s}</option>)}</select>
        {errors.status && <span className="field-error">{errors.status.message}</span>}</label>

      <button type="submit" disabled={isSubmitting}>{submitLabel}</button>
    </form>
  )
}

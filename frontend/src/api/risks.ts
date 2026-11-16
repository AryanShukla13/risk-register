import { request } from './client'
import type { Mitigation, MitigationInput, Risk, RiskFilters, RiskInput, RiskSummary } from './types'

const json = (method: string, body: unknown): RequestInit => ({ method, body: JSON.stringify(body) })

export const listRisks = ({ category, status, sort }: RiskFilters) => {
  const params = new URLSearchParams({ sort })
  if (category) params.set('category', category)
  if (status) params.set('status', status)
  return request<RiskSummary[]>(`/risks?${params}`)
}

export const getRisk = (id: number) => request<Risk>(`/risks/${id}`)
export const createRisk = (input: RiskInput) => request<Risk>('/risks', json('POST', input))
export const updateRisk = (id: number, input: RiskInput) => request<Risk>(`/risks/${id}`, json('PUT', input))
export const deleteRisk = (id: number) => request<void>(`/risks/${id}`, { method: 'DELETE' })

export const addMitigation = (riskId: number, input: MitigationInput) =>
  request<Mitigation>(`/risks/${riskId}/mitigations`, json('POST', input))
export const deleteMitigation = (riskId: number, id: number) =>
  request<void>(`/risks/${riskId}/mitigations/${id}`, { method: 'DELETE' })

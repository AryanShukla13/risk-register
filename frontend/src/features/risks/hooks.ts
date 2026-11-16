import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as api from '../../api/risks'
import type { MitigationInput, RiskFilters, RiskInput } from '../../api/types'

export const riskKeys = {
  all: ['risks'] as const,
  list: (filters: RiskFilters) => ['risks', 'list', filters] as const,
  detail: (id: number) => ['risks', 'detail', id] as const,
}

export const useRisks = (filters: RiskFilters) =>
  useQuery({ queryKey: riskKeys.list(filters), queryFn: () => api.listRisks(filters) })

export const useRisk = (id: number) =>
  useQuery({ queryKey: riskKeys.detail(id), queryFn: () => api.getRisk(id), enabled: Number.isFinite(id) })

/** Any write can change a score or count, so invalidate every risk query. */
function useInvalidatingMutation<TVars, TResult>(fn: (vars: TVars) => Promise<TResult>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: riskKeys.all }),
  })
}

export const useCreateRisk = () => useInvalidatingMutation((input: RiskInput) => api.createRisk(input))
export const useUpdateRisk = (id: number) => useInvalidatingMutation((input: RiskInput) => api.updateRisk(id, input))
export const useDeleteRisk = () => useInvalidatingMutation((id: number) => api.deleteRisk(id))
export const useAddMitigation = (riskId: number) =>
  useInvalidatingMutation((input: MitigationInput) => api.addMitigation(riskId, input))
export const useDeleteMitigation = (riskId: number) =>
  useInvalidatingMutation((id: number) => api.deleteMitigation(riskId, id))

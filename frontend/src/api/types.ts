export const CATEGORIES = ['OPERATIONAL', 'FINANCIAL', 'COMPLIANCE', 'SECURITY', 'STRATEGIC'] as const
export const STATUSES = ['OPEN', 'MITIGATING', 'CLOSED'] as const
export const SCALE = [1, 2, 3, 4, 5] as const

export type Category = (typeof CATEGORIES)[number]
export type Status = (typeof STATUSES)[number]
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type SortParam = 'residualScore,desc' | 'residualScore,asc' | 'createdAt,desc'

interface Scored {
  inherentScore: number
  inherentSeverity: Severity
  residualScore: number
  residualSeverity: Severity
}

export interface RiskSummary extends Scored {
  id: number
  title: string
  category: Category
  owner: string
  likelihood: number
  impact: number
  status: Status
  mitigationCount: number
}

export interface Mitigation {
  id: number
  riskId: number
  description: string
  effectiveness: number
  createdAt: string
}

export interface Risk extends Scored {
  id: number
  title: string
  description: string | null
  category: Category
  owner: string
  likelihood: number
  impact: number
  status: Status
  mitigations: Mitigation[]
}

export interface RiskInput {
  title: string
  description?: string
  category: Category
  owner: string
  likelihood: number
  impact: number
  status: Status
}

export interface MitigationInput {
  description: string
  effectiveness: number
}

export interface RiskFilters {
  category?: Category
  status?: Status
  sort: SortParam
}

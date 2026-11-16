import { z } from 'zod'
import { CATEGORIES, STATUSES } from '../../api/types'

const scale = (label: string) =>
  z
    .number({ error: `${label} is required` })
    .int()
    .min(1, `${label} must be between 1 and 5`)
    .max(5, `${label} must be between 1 and 5`)

export const riskSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(200),
  description: z.string().max(2000).optional(),
  category: z.enum(CATEGORIES),
  owner: z.string().trim().min(1, 'Owner is required').max(100),
  likelihood: scale('Likelihood'),
  impact: scale('Impact'),
  status: z.enum(STATUSES),
})

export type RiskFormValues = z.infer<typeof riskSchema>

export const mitigationSchema = z.object({
  description: z.string().trim().min(1, 'Description is required').max(1000),
  effectiveness: scale('Effectiveness'),
})

export type MitigationFormValues = z.infer<typeof mitigationSchema>

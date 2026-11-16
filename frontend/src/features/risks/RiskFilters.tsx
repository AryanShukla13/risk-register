import { CATEGORIES, STATUSES, type Category, type RiskFilters as Filters, type SortParam, type Status } from '../../api/types'

interface Props {
  filters: Filters
  onChange: (next: Filters) => void
}

export function RiskFilters({ filters, onChange }: Props) {
  return (
    <div className="toolbar">
      <label>
        Category
        <select
          value={filters.category ?? ''}
          onChange={(e) => onChange({ ...filters, category: (e.target.value || undefined) as Category | undefined })}
        >
          <option value="">All</option>
          {CATEGORIES.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
      </label>
      <label>
        Status
        <select
          value={filters.status ?? ''}
          onChange={(e) => onChange({ ...filters, status: (e.target.value || undefined) as Status | undefined })}
        >
          <option value="">All</option>
          {STATUSES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
      </label>
      <label>
        Sort
        <select value={filters.sort} onChange={(e) => onChange({ ...filters, sort: e.target.value as SortParam })}>
          <option value="residualScore,desc">Residual score (high to low)</option>
          <option value="residualScore,asc">Residual score (low to high)</option>
          <option value="createdAt,desc">Newest first</option>
        </select>
      </label>
    </div>
  )
}

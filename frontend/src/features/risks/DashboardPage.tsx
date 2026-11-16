import { Link, useSearchParams } from 'react-router-dom'
import { CATEGORIES, STATUSES, type RiskFilters as Filters, type SortParam } from '../../api/types'
import { ErrorMessage, Loading } from '../../components/Status'
import { useRisks } from './hooks'
import { RiskFilters } from './RiskFilters'
import { RiskTable } from './RiskTable'

const SORTS: SortParam[] = ['residualScore,desc', 'residualScore,asc', 'createdAt,desc']

/** Filter and sort state lives in the URL so views are shareable and survive refresh. */
function readFilters(params: URLSearchParams): Filters {
  const category = CATEGORIES.find((c) => c === params.get('category'))
  const status = STATUSES.find((s) => s === params.get('status'))
  const sort = SORTS.find((s) => s === params.get('sort')) ?? 'residualScore,desc'
  return { category, status, sort }
}

export function DashboardPage() {
  const [params, setParams] = useSearchParams()
  const filters = readFilters(params)
  const { data, isPending, error, refetch } = useRisks(filters)

  const onChange = (next: Filters) => {
    const p = new URLSearchParams({ sort: next.sort })
    if (next.category) p.set('category', next.category)
    if (next.status) p.set('status', next.status)
    setParams(p)
  }

  return (
    <>
      <div className="page-header">
        <h1>Risk Register</h1>
        <Link className="button" to="/risks/new">
          New risk
        </Link>
      </div>
      <RiskFilters filters={filters} onChange={onChange} />
      {isPending && <Loading />}
      {error && <ErrorMessage error={error} onRetry={() => refetch()} />}
      {data && <RiskTable risks={data} />}
    </>
  )
}

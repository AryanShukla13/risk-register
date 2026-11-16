import { Navigate, Route, Routes } from 'react-router-dom'
import { DashboardPage } from './features/risks/DashboardPage'
import { EditRiskPage, CreateRiskPage } from './features/risks/RiskFormPage'
import { RiskDetailPage } from './features/risks/RiskDetailPage'

export default function App() {
  return (
    <main>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/risks/new" element={<CreateRiskPage />} />
        <Route path="/risks/:id" element={<RiskDetailPage />} />
        <Route path="/risks/:id/edit" element={<EditRiskPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </main>
  )
}

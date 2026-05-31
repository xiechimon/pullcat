import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ErrorBoundary } from './components/ErrorBoundary'
import { HomePage } from './pages/HomePage'
import { ReviewPage } from './pages/ReviewPage'
import { DashboardPage } from './pages/DashboardPage'
import { HistoryPage } from './pages/HistoryPage'
import { RepoPage } from './pages/RepoPage'
import { LoginPage } from './pages/LoginPage'
import { SettingsPage } from './pages/SettingsPage'

export default function App() {
  return (
    <BrowserRouter>
      <ErrorBoundary>
        <Layout>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/review/:id" element={<ReviewPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/history" element={<HistoryPage />} />
            <Route path="/repos/:owner/:repo" element={<RepoPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/logout" element={<LoginPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Routes>
        </Layout>
      </ErrorBoundary>
    </BrowserRouter>
  )
}

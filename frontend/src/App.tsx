import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './components/layout/MainLayout'
import LoginPage from './pages/auth/LoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import CodeReviewPage from './pages/review/CodeReviewPage'
import ProjectPage from './pages/project/ProjectPage'
import DocumentListPage from './pages/knowledge/DocumentListPage'
import DocumentDetailPage from './pages/knowledge/DocumentDetailPage'
import QAPage from './pages/knowledge/QAPage'
import SearchPage from './pages/knowledge/SearchPage'
import ProfilePage from './pages/profile/ProfilePage'

// Protected Route wrapper
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const token = localStorage.getItem('token')
  const user = localStorage.getItem('user')

  if (!token || !user) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

// Public Route wrapper (redirect to dashboard if already logged in)
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const token = localStorage.getItem('token')
  const user = localStorage.getItem('user')
  if (token && user) {
    return <Navigate to="/dashboard" replace />
  }
  return <>{children}</>
}

function App() {
  return (
    <Routes>
        {/* Public Routes */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <LoginPage />
            </PublicRoute>
          }
        />
        <Route
          path="/register"
          element={
            <PublicRoute>
              <RegisterPage />
            </PublicRoute>
          }
        />

        {/* Protected Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />

          <Route path="review" element={<CodeReviewPage />} />
          <Route path="projects" element={<ProjectPage />} />

          {/* Knowledge Base Routes */}
          <Route path="knowledge" element={<Navigate to="/knowledge/documents" replace />} />
          <Route path="knowledge/documents" element={<DocumentListPage />} />
          <Route path="knowledge/documents/:documentId" element={<DocumentDetailPage />} />
          <Route path="knowledge/qa" element={<QAPage />} />
          <Route path="knowledge/search" element={<SearchPage />} />

          <Route path="profile" element={<ProfilePage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
  )
}

export default App

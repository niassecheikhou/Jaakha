import { Route, Routes } from 'react-router-dom'
import { AppLayout } from '../../components/AppLayout'
import { ProtectedRoute } from '../../components/ProtectedRoute'
import { AboutPage } from '../../pages/AboutPage'
import { AdminPage } from '../../pages/AdminPage'
import { EmployerPage } from '../../pages/EmployerPage'
import { HomePage } from '../../pages/HomePage'
import { ContactPage } from '../../pages/ContactPage'
import { ForgotPasswordPage } from '../../pages/ForgotPasswordPage'
import { LoginPage } from '../../pages/LoginPage'
import { NotFoundPage } from '../../pages/NotFoundPage'
import { ProviderDashboardPage } from '../../pages/ProviderDashboardPage'
import { ProviderDetailPage } from '../../pages/ProviderDetailPage'
import { ProvidersPage } from '../../pages/ProvidersPage'
import { RegisterPage } from '../../pages/RegisterPage'
import { ResetPasswordPage } from '../../pages/ResetPasswordPage'
import { ServicesPage } from '../../pages/ServicesPage'

export function AppRouter() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="services" element={<ServicesPage />} />
        <Route path="contact" element={<ContactPage />} />
        <Route path="providers" element={<ProvidersPage />} />
        <Route path="providers/:id" element={<ProviderDetailPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route
          path="employer"
          element={
            <ProtectedRoute roles={['EMPLOYEUR']}>
              <EmployerPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="provider"
          element={
            <ProtectedRoute roles={['PRESTATAIRE']}>
              <ProviderDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin"
          element={
            <ProtectedRoute roles={['ADMIN', 'SUPER_ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

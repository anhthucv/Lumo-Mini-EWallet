import { Navigate, Route, Routes } from 'react-router-dom';

import { useAuth } from './auth/AuthContext';
import ProtectedRoute from './auth/ProtectedRoute';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import WalletPage from './pages/WalletPage';
import ProfilePage from './pages/ProfilePage';
import BeneficiariesPage from './pages/BeneficiariesPage';
import NotificationsPage from './pages/NotificationsPage';
import ActivityPage from './pages/ActivityPage';
import PaymentResultPage from './pages/PaymentResultPage';
import AdminUsersPage from './pages/AdminUsersPage';

function RootRedirect() {
  const { isAuthenticated, isHydrating } = useAuth();

  if (isHydrating) {
    return (
      <div className="auth-loading" role="status" aria-live="polite">
        Restoring your session...
      </div>
    );
  }

  return <Navigate to={isAuthenticated ? '/dashboard' : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/wallet" element={<WalletPage />} />
        <Route path="/activity" element={<ActivityPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/beneficiaries" element={<BeneficiariesPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/payment-result" element={<PaymentResultPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
      </Route>
      <Route path="*" element={<RootRedirect />} />
    </Routes>
  );
}

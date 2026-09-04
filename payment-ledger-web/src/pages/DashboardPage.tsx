import { Link, useNavigate } from 'react-router-dom';

import { useAuth } from '../auth/AuthContext';
import './register.css';

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user, tokenType, expiresIn, logout } = useAuth();

  function handleSignOut() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <main className="register-shell">
      <section className="register-grid">
        <aside className="hero-panel">
          <div className="brand-row">
            <div className="brand-mark">PL</div>
            <div className="brand-copy">
              <small>Payment Ledger</small>
              <strong>Authenticated Area</strong>
            </div>
          </div>
          <span className="hero-kicker">Signed in successfully</span>
          <h1>Welcome back, {user?.fullName}.</h1>
          <p>
            Your JWT session is stored locally and ready for the next authenticated screens in the project.
          </p>
          <ul className="feature-list">
            <li>
              <span className="feature-badge">✓</span>
              Access token and safe user profile are persisted in localStorage
            </li>
            <li>
              <span className="feature-badge">✓</span>
              Protected wallet requests reuse the shared Authorization helper
            </li>
            <li>
              <span className="feature-badge">✓</span>
              Money movement and transaction history remain out of scope
            </li>
          </ul>
        </aside>

        <section className="form-panel" aria-label="Dashboard summary">
          <div className="form-header">
            <div>
              <h2>Session</h2>
              <p>Your sign-in details are ready for future authenticated pages.</p>
            </div>
            <div className="status-pill">JWT active</div>
          </div>

          <div className="success-summary">
            <div className="summary-grid">
              <div className="summary-item">
                <span>Full name</span>
                <strong>{user?.fullName}</strong>
              </div>
              <div className="summary-item">
                <span>Email</span>
                <strong>{user?.email}</strong>
              </div>
              <div className="summary-item">
                <span>Role</span>
                <strong>{user?.role}</strong>
              </div>
              <div className="summary-item">
                <span>Status</span>
                <strong>{user?.status}</strong>
              </div>
              <div className="summary-item">
                <span>Token type</span>
                <strong>{tokenType}</strong>
              </div>
              <div className="summary-item">
                <span>Expires in</span>
                <strong>{expiresIn ? `${Math.round(expiresIn / 1000 / 60)} minutes` : 'Unknown'}</strong>
              </div>
            </div>
          </div>

          <div className="banner success">
            You are signed in. Open My Wallet to review your current account balance.
          </div>

          <div className="banner warning">
            Access token stored locally and ready for future authenticated requests.
          </div>

          <div className="session-actions">
            <Link to="/wallet" className="primary-button secondary-button-link">
              My Wallet
            </Link>
            <Link to="/profile" className="secondary-button secondary-button-link">
              Profile
            </Link>
            <Link to="/beneficiaries" className="secondary-button secondary-button-link">
              Beneficiaries
            </Link>
            <button type="button" className="primary-button" onClick={handleSignOut}>
              Sign out
            </button>
            <Link to="/register" className="secondary-button secondary-button-link">
              Register new account
            </Link>
          </div>
        </section>
      </section>
    </main>
  );
}

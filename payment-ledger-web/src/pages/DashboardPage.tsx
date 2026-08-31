import { Navigate, Link, useNavigate } from 'react-router-dom';

import { clearAuthSession, loadAuthSession } from '../auth/session';
import './register.css';

export default function DashboardPage() {
  const navigate = useNavigate();
  const session = loadAuthSession();

  if (!session) {
    return <Navigate to="/login" replace />;
  }

  function handleSignOut() {
    clearAuthSession();
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
          <h1>Welcome back, {session.user.fullName}.</h1>
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
              Protected endpoints can reuse the Authorization helper later
            </li>
            <li>
              <span className="feature-badge">✓</span>
              No wallet, transfer, or admin features were added ahead of scope
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
                <strong>{session.user.fullName}</strong>
              </div>
              <div className="summary-item">
                <span>Email</span>
                <strong>{session.user.email}</strong>
              </div>
              <div className="summary-item">
                <span>Role</span>
                <strong>{session.user.role}</strong>
              </div>
              <div className="summary-item">
                <span>Status</span>
                <strong>{session.user.status}</strong>
              </div>
              <div className="summary-item">
                <span>Token type</span>
                <strong>{session.tokenType}</strong>
              </div>
              <div className="summary-item">
                <span>Expires in</span>
                <strong>{Math.round(session.expiresIn / 1000 / 60)} minutes</strong>
              </div>
            </div>
          </div>

          <div className="banner success">
            You are signed in. This is a minimal landing state until the wallet pages are added.
          </div>

          <div className="banner warning">
            Access token stored locally and ready for future authenticated requests.
          </div>

          <div className="session-actions">
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

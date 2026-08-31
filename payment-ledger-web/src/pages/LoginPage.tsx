import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { loginUser } from '../api/authApi';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import type { LoginRequest } from '../types/auth';
import './register.css';

type FieldName = 'email' | 'password';
type FieldErrors = Partial<Record<FieldName, string>>;

const initialForm = {
  email: '',
  password: '',
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function normalizeEmail(value: string): string {
  return value.trim().toLowerCase();
}

function mapBackendError(error: unknown): { field: FieldName | 'form'; message: string } {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'INVALID_CREDENTIALS':
        return { field: 'form', message: 'Invalid email or password.' };
      case 'USER_LOCKED':
        return { field: 'form', message: error.message || 'Your account is locked.' };
      case 'VALIDATION_ERROR':
        return { field: 'form', message: error.message };
      default:
        return { field: 'form', message: error.message || 'Something went wrong. Please try again.' };
    }
  }

  return { field: 'form', message: 'Something went wrong. Please try again.' };
}

export default function LoginPage() {
  const navigate = useNavigate();
  const { isAuthenticated, isHydrating, login } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [generalError, setGeneralError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const trimmedEmail = useMemo(() => normalizeEmail(form.email), [form.email]);

  useEffect(() => {
    if (!isHydrating && isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, isHydrating, navigate]);

  function setField<K extends FieldName>(field: K, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
    setGeneralError('');
  }

  function validateForm(): boolean {
    const nextErrors: FieldErrors = {};

    if (!emailPattern.test(trimmedEmail)) {
      nextErrors.email = 'Please enter a valid email address.';
    }

    if (!form.password.trim()) {
      nextErrors.password = 'Password is required.';
    }

    setFieldErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (loading) {
      return;
    }

    setGeneralError('');

    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      const payload: LoginRequest = {
        email: normalizeEmail(form.email),
        password: form.password,
      };
      const response = await loginUser(payload);
      login(response);
      setForm(initialForm);
      setFieldErrors({});
      navigate('/dashboard', { replace: true });
    } catch (error) {
      const mapped = mapBackendError(error);
      if (mapped.field === 'form') {
        setGeneralError(mapped.message);
      } else {
        setFieldErrors((current) => ({ ...current, [mapped.field]: mapped.message }));
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="register-shell">
      <section className="register-grid">
        <aside className="hero-panel">
          <div className="brand-row">
            <div className="brand-mark">PL</div>
            <div className="brand-copy">
              <small>Payment Ledger</small>
              <strong>Secure Login</strong>
            </div>
          </div>
          <span className="hero-kicker">Fast access to your wallet</span>
          <h1>Sign in to continue to Payment Ledger.</h1>
          <p>
            Use your registered email and password to receive a signed JWT session for upcoming authenticated
            wallet features.
          </p>
          <ul className="feature-list">
            <li>
              <span className="feature-badge">✓</span>
              Stateless JWT session stored locally for the current browser
            </li>
            <li>
              <span className="feature-badge">✓</span>
              Clear validation and error feedback before and after submit
            </li>
            <li>
              <span className="feature-badge">✓</span>
              Direct link back to registration if you need a new account
            </li>
          </ul>
        </aside>

        <section className="form-panel" aria-label="Login form section">
          <div className="form-header">
            <div>
              <h2>Login</h2>
              <p>Access your account with your email and password.</p>
            </div>
            <div className="status-pill">JWT ready</div>
          </div>

          {generalError ? <div className="banner error">{generalError}</div> : null}

          <form className="form" onSubmit={handleSubmit} noValidate>
            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="email">
                  Email
                </label>
                <span className="field-hint">Your registered login email</span>
              </div>
              <input
                id="email"
                className={`input ${fieldErrors.email ? 'error' : ''}`}
                type="email"
                autoComplete="email"
                placeholder="user@example.com"
                value={form.email}
                onChange={(event) => setField('email', event.target.value)}
              />
              {fieldErrors.email ? <div className="error-text">{fieldErrors.email}</div> : null}
            </div>

            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="password">
                  Password
                </label>
                <span className="field-hint">Keep it private</span>
              </div>
              <div className="input-row">
                <input
                  id="password"
                  className={`input ${fieldErrors.password ? 'error' : ''}`}
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  value={form.password}
                  onChange={(event) => setField('password', event.target.value)}
                />
                <button
                  type="button"
                  className="icon-button"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
              {fieldErrors.password ? <div className="error-text">{fieldErrors.password}</div> : null}
            </div>

            <div className="field-row">
              <button type="submit" className="primary-button" disabled={loading}>
                {loading ? 'Signing in...' : 'Login'}
              </button>
              <div className="helper-text">Your JWT session will be stored locally after a successful sign-in.</div>
            </div>

            <div className="auth-link-row">
              <span>Need an account?</span>
              <Link className="auth-link" to="/register">
                Register here
              </Link>
            </div>
          </form>
        </section>
      </section>
    </main>
  );
}

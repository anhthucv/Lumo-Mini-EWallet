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
    <main className="auth-shell auth-login-shell">
      <section className="auth-grid">
        <aside className="auth-hero">
          <div className="brand-row">
            <div className="brand-mark" aria-hidden="true">
              <span className="brand-mark-notch" />
            </div>
            <div className="brand-copy">
              <strong>Lumo</strong>
            </div>
          </div>
          <span className="auth-eyebrow">Welcome back</span>
          <h1>Money moves. You stay in control.</h1>
          <p>Send, receive and keep track of your wallet in one place.</p>
          <div className="wallet-art" aria-hidden="true">
            <div className="wallet-art-card wallet-art-card-back" />
            <div className="wallet-art-card-wrapper">
              <div className="wallet-art-card wallet-art-card-front">
                <div className="wallet-art-heading">
                  <strong className="wallet-art-brand">Lumo Wallet <i aria-hidden="true" /></strong>
                </div>
                <div className="wallet-art-balance-block">
                  <span className="wallet-art-label">Available balance</span>
                  <strong className="wallet-art-balance">₫ 12,480,000</strong>
                </div>
                <div className="wallet-art-actions">
                  <span className="wallet-art-action-send"><b>↗</b> Send</span>
                  <span className="wallet-art-action-receive"><b>↙</b> Receive</span>
                </div>
                <div className="wallet-art-transaction">
                  <span className="wallet-art-transaction-icon">↙</span>
                  <div>
                    <span className="wallet-art-label">Received</span>
                    <strong>Minh</strong>
                  </div>
                  <b>+ ₫ 850,000</b>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <section className="auth-card" aria-label="Login form section">
          <div className="form-header">
            <div>
              <span className="auth-card-kicker">Sign in</span>
              <h2>Welcome back</h2>
              <p>Sign in to access your wallet.</p>
            </div>
          </div>

          {generalError ? <div className="banner error">{generalError}</div> : null}

          <form className="form" onSubmit={handleSubmit} noValidate>
            <div className="field-group">
              <label className="field-label" htmlFor="email">Email</label>
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
              <label className="field-label" htmlFor="password">Password</label>
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
                {loading ? 'Signing in...' : 'Sign in'}
              </button>
            </div>

            <div className="auth-link-row">
              <span>New here?</span>
              <Link className="auth-link" to="/register">
                Create an account
              </Link>
            </div>
          </form>
        </section>
      </section>
    </main>
  );
}

import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { registerUser, sendRegistrationCode } from '../api/authApi';
import { ApiError } from '../api/http';
import { useAuth } from '../auth/AuthContext';
import type { RegisterRequest, RegisterResponse } from '../types/auth';
import './register.css';

type FieldName = 'fullName' | 'email' | 'password' | 'confirmPassword' | 'code';

type FieldErrors = Partial<Record<FieldName, string>>;

const RESEND_SECONDS = 60;

const initialForm = {
  fullName: '',
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const otpPattern = /^\d{6}$/;

function normalizeEmail(value: string): string {
  return value.trim().toLowerCase();
}

function formatCurrency(value: number | string): string {
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return String(value);
  }
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount);
}

function mapBackendError(error: unknown): { field: FieldName | 'form'; message: string } {
  if (error instanceof ApiError) {
    switch (error.code) {
      case 'DUPLICATE_EMAIL':
        return { field: 'email', message: 'This email is already registered.' };
      case 'INVALID_VERIFICATION_CODE':
        return { field: 'code', message: 'The verification code is incorrect.' };
      case 'VERIFICATION_CODE_EXPIRED':
        return { field: 'code', message: 'The verification code has expired. Please request a new code.' };
      case 'VERIFICATION_CODE_NOT_FOUND':
        return { field: 'code', message: 'No active verification code was found. Please request a new code.' };
      case 'VERIFICATION_CODE_RESEND_TOO_SOON':
        return { field: 'email', message: 'Please wait 60 seconds before requesting another code.' };
      case 'EMAIL_DELIVERY_FAILED':
        return { field: 'form', message: 'Unable to send verification email. Please try again.' };
      case 'VALIDATION_ERROR':
        return { field: 'form', message: error.message };
      default:
        return { field: 'form', message: error.message || 'Something went wrong. Please try again.' };
    }
  }

  return { field: 'form', message: 'Something went wrong. Please try again.' };
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const { isAuthenticated, isHydrating } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [generalError, setGeneralError] = useState('');
  const [statusMessage, setStatusMessage] = useState('');
  const [resendSeconds, setResendSeconds] = useState(0);
  const [sendingCode, setSendingCode] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [otpRequestedFor, setOtpRequestedFor] = useState('');
  const [registrationResult, setRegistrationResult] = useState<RegisterResponse | null>(null);

  const trimmedEmail = useMemo(() => normalizeEmail(form.email), [form.email]);

  useEffect(() => {
    if (!isHydrating && isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, isHydrating, navigate]);

  useEffect(() => {
    if (!otpRequestedFor) {
      return;
    }

    if (trimmedEmail === otpRequestedFor) {
      return;
    }

    setForm((current) => ({ ...current, code: '' }));
    setFieldErrors((current) => ({ ...current, code: undefined, email: undefined }));
    setStatusMessage('');
    setGeneralError('');
    setResendSeconds(0);
    setOtpRequestedFor('');
  }, [otpRequestedFor, trimmedEmail]);

  useEffect(() => {
    if (resendSeconds <= 0) {
      return;
    }

    const timerId = window.setInterval(() => {
      setResendSeconds((current) => (current <= 1 ? 0 : current - 1));
    }, 1000);

    return () => window.clearInterval(timerId);
  }, [resendSeconds]);

  function setField<K extends FieldName>(field: K, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: undefined }));
    setGeneralError('');
  }

  function validateEmail(showMessage = true): boolean {
    const email = normalizeEmail(form.email);
    if (!emailPattern.test(email)) {
      if (showMessage) {
        setFieldErrors((current) => ({ ...current, email: 'Please enter a valid email address.' }));
      }
      return false;
    }
    return true;
  }

  function validateForm(): boolean {
    const nextErrors: FieldErrors = {};
    const normalized = normalizeEmail(form.email);

    if (!form.fullName.trim()) {
      nextErrors.fullName = 'Full name is required.';
    }

    if (!emailPattern.test(normalized)) {
      nextErrors.email = 'Please enter a valid email address.';
    }

    if (form.password.length < 8) {
      nextErrors.password = 'Password must be at least 8 characters.';
    }

    if (form.confirmPassword !== form.password) {
      nextErrors.confirmPassword = 'Passwords do not match.';
    }

    if (!otpPattern.test(form.code)) {
      nextErrors.code = 'Verification code must be exactly 6 digits.';
    }

    setFieldErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSendCode() {
    if (sendingCode || resendSeconds > 0) {
      return;
    }

    setRegistrationResult(null);
    setStatusMessage('');
    setGeneralError('');

    if (!validateEmail()) {
      return;
    }

    setSendingCode(true);
    try {
      const response = await sendRegistrationCode({ email: normalizeEmail(form.email) });
      setOtpRequestedFor(normalizeEmail(form.email));
      setResendSeconds(RESEND_SECONDS);
      setFieldErrors((current) => ({ ...current, email: undefined, code: undefined }));
      setStatusMessage(response.message || 'Verification code sent.');
    } catch (error) {
      const mapped = mapBackendError(error);
      if (mapped.field === 'email') {
        setFieldErrors((current) => ({ ...current, email: mapped.message }));
        setStatusMessage('');
      } else {
        setGeneralError(mapped.message);
      }
      setResendSeconds(0);
      setOtpRequestedFor('');
    } finally {
      setSendingCode(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting) {
      return;
    }

    setRegistrationResult(null);
    setStatusMessage('');
    setGeneralError('');

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      const payload: RegisterRequest = {
        email: normalizeEmail(form.email),
        password: form.password,
        fullName: form.fullName.trim(),
        code: form.code,
      };
      const response = await registerUser(payload);
      setRegistrationResult(response);
      setForm(initialForm);
      setFieldErrors({});
      setStatusMessage('');
      setGeneralError('');
      setOtpRequestedFor('');
      setResendSeconds(0);
    } catch (error) {
      const mapped = mapBackendError(error);
      if (mapped.field === 'form') {
        setGeneralError(mapped.message);
      } else {
        setFieldErrors((current) => ({ ...current, [mapped.field]: mapped.message }));
      }
    } finally {
      setSubmitting(false);
    }
  }

  function handleReset() {
    setForm(initialForm);
    setFieldErrors({});
    setGeneralError('');
    setStatusMessage('');
    setRegistrationResult(null);
    setOtpRequestedFor('');
    setResendSeconds(0);
  }

  const resendLabel = resendSeconds > 0 ? `Resend in ${resendSeconds}s` : 'Send verification code';

  if (registrationResult) {
    return (
      <main className="auth-shell auth-register-shell">
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
            <span className="auth-eyebrow">You are all set</span>
            <h1>Your account is ready.</h1>
            <p>
              Your wallet is ready. Sign in whenever you are ready to get started.
            </p>
          </aside>

          <section className="auth-card">
            <div className="success-panel">
              <div>
                <h3>Registration successful</h3>
                <p className="secondary-note">
                  Here are the details for your new wallet.
                </p>
              </div>

              <div className="success-summary">
                <div className="summary-grid">
                  <div className="summary-item">
                    <span>Full name</span>
                    <strong>{registrationResult.fullName}</strong>
                  </div>
                  <div className="summary-item">
                    <span>Email</span>
                    <strong>{registrationResult.email}</strong>
                  </div>
                  <div className="summary-item">
                    <span>Account number</span>
                    <strong>{registrationResult.accountNumber}</strong>
                  </div>
                  <div className="summary-item">
                    <span>Balance</span>
                    <strong>{formatCurrency(registrationResult.balance)}</strong>
                  </div>
                </div>
              </div>

              <div className="banner success">
                Your account is ready. Sign in to access your wallet.
              </div>

              <div className="field-row">
                <button type="button" className="secondary-button" onClick={handleReset}>
                  Register another account
                </button>
              </div>

              <div className="auth-link-row">
                <span>Already have an account?</span>
                <Link className="auth-link" to="/login">
                  Log in
                </Link>
              </div>
            </div>
          </section>
        </section>
      </main>
    );
  }

  return (
    <main className="auth-shell auth-register-shell">
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
          <span className="auth-eyebrow">A better way to pay</span>
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

        <section className="auth-card" aria-label="Register form section">
          <div className="form-header">
            <div>
              <span className="auth-card-kicker">Get started</span>
              <h2>Create your account</h2>
              <p>Start using your wallet in minutes.</p>
            </div>
          </div>

          {generalError ? <div className="banner error">{generalError}</div> : null}
          {statusMessage ? <div className="banner success">{statusMessage}</div> : null}

          <form className="form" onSubmit={handleSubmit} noValidate>
            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="fullName">
                  Full name
                </label>
              </div>
              <input
                id="fullName"
                className={`input ${fieldErrors.fullName ? 'error' : ''}`}
                type="text"
                autoComplete="name"
                placeholder="Nguyen Van A"
                value={form.fullName}
                onChange={(event) => setField('fullName', event.target.value)}
              />
              {fieldErrors.fullName ? <div className="error-text">{fieldErrors.fullName}</div> : null}
            </div>

            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="email">
                  Email
                </label>
              </div>
              <div className="field-row">
                <input
                  id="email"
                  className={`input ${fieldErrors.email ? 'error' : ''}`}
                  type="email"
                  autoComplete="email"
                  placeholder="user@example.com"
                  value={form.email}
                  onChange={(event) => setField('email', event.target.value)}
                />
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleSendCode}
                  disabled={sendingCode || resendSeconds > 0}
                >
                  {sendingCode ? 'Sending...' : resendLabel}
                </button>
              </div>
              {fieldErrors.email ? <div className="error-text">{fieldErrors.email}</div> : null}
            </div>

            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="code">
                  Verification code
                </label>
              </div>
              <input
                id="code"
                className={`input ${fieldErrors.code ? 'error' : ''}`}
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder="123456"
                value={form.code}
                onChange={(event) => setField('code', event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
              {fieldErrors.code ? <div className="error-text">{fieldErrors.code}</div> : null}
            </div>

            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="password">
                  Password
                </label>
              </div>
              <div className="input-row">
                <input
                  id="password"
                  className={`input ${fieldErrors.password ? 'error' : ''}`}
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  placeholder="Create a password"
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

            <div className="field-group">
              <div className="label-row">
                <label className="field-label" htmlFor="confirmPassword">
                  Confirm password
                </label>
              </div>
              <input
                id="confirmPassword"
                className={`input ${fieldErrors.confirmPassword ? 'error' : ''}`}
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                placeholder="Repeat your password"
                value={form.confirmPassword}
                onChange={(event) => setField('confirmPassword', event.target.value)}
              />
              {fieldErrors.confirmPassword ? <div className="error-text">{fieldErrors.confirmPassword}</div> : null}
            </div>

            <div className="field-row">
              <button type="submit" className="primary-button" disabled={submitting}>
                {submitting ? 'Registering...' : 'Register'}
              </button>
              <div className="helper-text">
                {otpRequestedFor ? (
                  <>
                    Code sent to <strong>{otpRequestedFor}</strong>
                  </>
                ) : (
                  'We will send a code to verify your email.'
                )}
              </div>
            </div>

            <div className="auth-link-row">
              <span>Already have an account?</span>
              <Link className="auth-link" to="/login">Sign in</Link>
            </div>
          </form>
        </section>
      </section>
    </main>
  );
}

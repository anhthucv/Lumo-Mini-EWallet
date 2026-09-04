import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import NotificationBell from '../components/NotificationBell';
import { changeMyPassword, getMyProfile, updateMyProfile } from '../api/profileApi';
import { useAuth } from '../auth/AuthContext';
import type { ProfileResponse } from '../types/profile';
import './profile.css';
import './register.css';

function formatCreatedAt(createdAt: string | null): string {
  if (!createdAt) return 'Not available';
  const date = new Date(createdAt);
  return Number.isNaN(date.getTime())
    ? createdAt
    : new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function getProfileErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 404) return 'Your profile could not be found.';
  if (error instanceof ApiError) return 'Your profile could not be loaded. Please try again.';
  return 'The server could not be reached. Please check your connection and try again.';
}

function getUpdateErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 400) return error.message || 'Enter a valid full name.';
  if (error instanceof ApiError && error.status === 404) return 'Your profile could not be found.';
  if (error instanceof ApiError) return 'Your profile could not be updated. Please try again.';
  return 'The server could not be reached. Please check your connection and try again.';
}

function getPasswordErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 400) {
    return error.message || 'The current password is incorrect or the new password is invalid.';
  }
  if (error instanceof ApiError) return 'Your password could not be changed. Please try again.';
  return 'The server could not be reached. Please check your connection and try again.';
}

export default function ProfilePage() {
  const navigate = useNavigate();
  const { logout, updateUser } = useAuth();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [fullName, setFullName] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    async function loadProfile() {
      setLoading(true);
      setError(null);
      try {
        const response = await getMyProfile(controller.signal);
        setProfile(response);
        setFullName(response.fullName);
      } catch (requestError) {
        if (controller.signal.aborted) return;
        if (requestError instanceof ApiError && requestError.status === 401) {
          logout();
          navigate('/login', { replace: true });
          return;
        }
        setProfile(null);
        setError(getProfileErrorMessage(requestError));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadProfile();
    return () => controller.abort();
  }, [logout, navigate, retryKey]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);
    setSuccess(null);
    const trimmedName = fullName.trim();
    if (!trimmedName) {
      setFormError('Full name must not be blank.');
      return;
    }
    if (trimmedName.length > 100) {
      setFormError('Full name must be at most 100 characters long.');
      return;
    }

    setSaving(true);
    try {
      const response = await updateMyProfile({ fullName: trimmedName });
      setProfile(response);
      setFullName(response.fullName);
      updateUser({
        userId: response.userId,
        email: response.email,
        fullName: response.fullName,
        role: response.role,
        status: response.status,
      });
      setSuccess('Your profile was updated successfully.');
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setFormError(getUpdateErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(null);

    if (!currentPassword.trim()) {
      setPasswordError('Current password must not be blank.');
      return;
    }
    if (!newPassword.trim()) {
      setPasswordError('New password must not be blank.');
      return;
    }
    if (newPassword.length < 8) {
      setPasswordError('New password must be at least 8 characters long.');
      return;
    }
    if (!confirmPassword) {
      setPasswordError('Please confirm your new password.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirmation do not match.');
      return;
    }

    setPasswordSaving(true);
    try {
      await changeMyPassword({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordSuccess('Your password was changed successfully.');
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setPasswordError(getPasswordErrorMessage(requestError));
    } finally {
      setPasswordSaving(false);
    }
  }

  return (
    <main className="register-shell profile-shell">
      <section className="profile-card" aria-labelledby="profile-title">
        <div className="profile-topbar">
          <Link to="/dashboard" className="brand-row profile-brand">
            <div className="brand-mark">PL</div>
            <div className="brand-copy"><small>Payment Ledger</small><strong>My Profile</strong></div>
          </Link>
          <div className="profile-navigation">
            <NotificationBell />
            <Link to="/dashboard" className="secondary-button secondary-button-link">Dashboard</Link>
            <Link to="/wallet" className="secondary-button secondary-button-link">My Wallet</Link>
            <Link to="/beneficiaries" className="secondary-button secondary-button-link">Beneficiaries</Link>
          </div>
        </div>

        <div className="profile-heading">
          <span className="hero-kicker">Authenticated profile</span>
          <h1 id="profile-title">Keep your account details current.</h1>
          <p>Review your verified account information and update your display name.</p>
        </div>

        {loading && <div className="profile-state" role="status" aria-live="polite"><span className="loading-dot" aria-hidden="true" />Loading your profile...</div>}
        {!loading && error && (
          <div className="profile-state profile-error" role="alert">
            <strong>{error}</strong>
            <button type="button" className="primary-button" onClick={() => setRetryKey((key) => key + 1)}>Try again</button>
          </div>
        )}

        {!loading && !error && profile && (
          <div className="profile-content">
            <section className="profile-panel" aria-labelledby="profile-information-title">
              <div className="profile-panel-heading">
                <div><span className="profile-kicker">Read-only information</span><h2 id="profile-information-title">Account profile</h2></div>
                <span className="status-pill">{profile.status}</span>
              </div>
              <dl className="profile-details">
                <div><dt>User ID</dt><dd>{profile.userId}</dd></div>
                <div><dt>Email</dt><dd>{profile.email}</dd></div>
                <div><dt>Role</dt><dd>{profile.role}</dd></div>
                <div><dt>Status</dt><dd>{profile.status}</dd></div>
                <div><dt>Created</dt><dd>{formatCreatedAt(profile.createdAt)}</dd></div>
              </dl>
            </section>

            <form className="profile-panel profile-edit" onSubmit={handleSubmit} noValidate>
              <div className="profile-panel-heading"><div><span className="profile-kicker">Editable information</span><h2>Display name</h2></div></div>
              <label className="field-group" htmlFor="profile-full-name">
                <span className="field-label">Full name</span>
                <input id="profile-full-name" className="input" value={fullName} onChange={(event) => { setFullName(event.target.value); setFormError(null); setSuccess(null); }} maxLength={100} disabled={saving} />
              </label>
              {formError && <div className="banner error" role="alert">{formError}</div>}
              {success && <div className="banner success" role="status">{success}</div>}
              <button type="submit" className="primary-button profile-save" disabled={saving}>{saving ? 'Saving profile...' : 'Save changes'}</button>
            </form>

            <form className="profile-panel profile-edit" onSubmit={handlePasswordSubmit} noValidate>
              <div className="profile-panel-heading"><div><span className="profile-kicker">Account security</span><h2>Change password</h2></div></div>
              <label className="field-group" htmlFor="current-password">
                <span className="field-label">Current password</span>
                <input id="current-password" className="input" type="password" autoComplete="current-password" value={currentPassword} onChange={(event) => { setCurrentPassword(event.target.value); setPasswordError(null); setPasswordSuccess(null); }} disabled={passwordSaving} />
              </label>
              <label className="field-group" htmlFor="new-password">
                <span className="field-label">New password</span>
                <input id="new-password" className="input" type="password" autoComplete="new-password" minLength={8} value={newPassword} onChange={(event) => { setNewPassword(event.target.value); setPasswordError(null); setPasswordSuccess(null); }} disabled={passwordSaving} />
              </label>
              <label className="field-group" htmlFor="confirm-password">
                <span className="field-label">Confirm new password</span>
                <input id="confirm-password" className="input" type="password" autoComplete="new-password" minLength={8} value={confirmPassword} onChange={(event) => { setConfirmPassword(event.target.value); setPasswordError(null); setPasswordSuccess(null); }} disabled={passwordSaving} />
              </label>
              {passwordError && <div className="banner error" role="alert">{passwordError}</div>}
              {passwordSuccess && <div className="banner success" role="status">{passwordSuccess}</div>}
              <button type="submit" className="primary-button profile-save" disabled={passwordSaving}>
                {passwordSaving ? 'Changing password...' : 'Change password'}
              </button>
            </form>
          </div>
        )}
      </section>
    </main>
  );
}

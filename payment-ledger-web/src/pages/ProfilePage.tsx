import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { getMyProfile, updateMyProfile } from '../api/profileApi';
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

  return (
    <main className="register-shell profile-shell">
      <section className="profile-card" aria-labelledby="profile-title">
        <div className="profile-topbar">
          <Link to="/dashboard" className="brand-row profile-brand">
            <div className="brand-mark">PL</div>
            <div className="brand-copy"><small>Payment Ledger</small><strong>My Profile</strong></div>
          </Link>
          <div className="profile-navigation">
            <Link to="/dashboard" className="secondary-button secondary-button-link">Dashboard</Link>
            <Link to="/wallet" className="secondary-button secondary-button-link">My Wallet</Link>
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
          </div>
        )}
      </section>
    </main>
  );
}

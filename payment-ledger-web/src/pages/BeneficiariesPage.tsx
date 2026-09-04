import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import {
  createBeneficiary,
  deleteBeneficiary,
  getBeneficiaries,
  updateBeneficiary,
} from '../api/beneficiaryApi';
import { useAuth } from '../auth/AuthContext';
import type { Beneficiary } from '../types/beneficiary';
import './beneficiaries.css';

function getLoadErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error instanceof ApiError) return error.message || 'Saved beneficiaries could not be loaded.';
  return 'The server could not be reached. Please check your connection and try again.';
}

function getCreateErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.code === 'ACCOUNT_NOT_FOUND') return 'No account was found with that account number.';
  if (error instanceof ApiError && error.code === 'BENEFICIARY_ALREADY_EXISTS') return 'That account is already in your saved beneficiaries.';
  if (error instanceof ApiError && error.status === 400) return error.message || 'Enter a valid account number and nickname.';
  if (error instanceof ApiError && error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error instanceof ApiError) return error.message || 'The beneficiary could not be saved.';
  return 'The server could not be reached. Please try again.';
}

function getUpdateErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error instanceof ApiError && error.status === 404) return 'This beneficiary is no longer available.';
  if (error instanceof ApiError && error.status === 400) return error.message || 'Enter a valid nickname.';
  if (error instanceof ApiError) return error.message || 'The beneficiary could not be updated.';
  return 'The server could not be reached. Please try again.';
}

function getDeleteErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) return 'Your session has expired. Please sign in again.';
  if (error instanceof ApiError && error.status === 404) return 'This beneficiary is no longer available.';
  if (error instanceof ApiError) return error.message || 'The beneficiary could not be deleted.';
  return 'The server could not be reached. Please try again.';
}

export default function BeneficiariesPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingError, setLoadingError] = useState<string | null>(null);
  const [accountNumber, setAccountNumber] = useState('');
  const [nickname, setNickname] = useState('');
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editNickname, setEditNickname] = useState('');
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadBeneficiaries() {
      setLoading(true);
      setLoadingError(null);
      try {
        setBeneficiaries(await getBeneficiaries(controller.signal));
      } catch (error) {
        if (controller.signal.aborted) return;
        if (error instanceof ApiError && error.status === 401) {
          logout();
          navigate('/login', { replace: true });
          return;
        }
        setLoadingError(getLoadErrorMessage(error));
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    void loadBeneficiaries();
    return () => controller.abort();
  }, [logout, navigate, retryKey]);

  function handleUnauthorized(error: unknown): boolean {
    if (error instanceof ApiError && error.status === 401) {
      logout();
      navigate('/login', { replace: true });
      return true;
    }
    return false;
  }

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreateError(null);
    setActionError(null);
    setSuccess(null);
    const trimmedAccountNumber = accountNumber.trim();
    const trimmedNickname = nickname.trim();
    if (!trimmedAccountNumber || !trimmedNickname) {
      setCreateError('Account number and nickname are required.');
      return;
    }

    setCreating(true);
    try {
      await createBeneficiary({ accountNumber: trimmedAccountNumber, nickname: trimmedNickname });
      setAccountNumber('');
      setNickname('');
      setSuccess('Beneficiary saved successfully.');
      setRetryKey((key) => key + 1);
    } catch (error) {
      if (!handleUnauthorized(error)) setCreateError(getCreateErrorMessage(error));
    } finally {
      setCreating(false);
    }
  }

  function startEditing(beneficiary: Beneficiary) {
    setActionError(null);
    setSuccess(null);
    setEditingId(beneficiary.id);
    setEditNickname(beneficiary.nickname);
  }

  async function handleUpdate(id: number) {
    const trimmedNickname = editNickname.trim();
    if (!trimmedNickname) {
      setActionError('Nickname must not be blank.');
      return;
    }

    setUpdatingId(id);
    setActionError(null);
    setSuccess(null);
    try {
      const updated = await updateBeneficiary(id, { nickname: trimmedNickname });
      setBeneficiaries((current) => current.map((item) => (item.id === id ? updated : item)));
      setEditingId(null);
      setSuccess('Beneficiary nickname updated.');
    } catch (error) {
      if (!handleUnauthorized(error)) setActionError(getUpdateErrorMessage(error));
    } finally {
      setUpdatingId(null);
    }
  }

  async function handleDelete(beneficiary: Beneficiary) {
    if (!window.confirm(`Remove ${beneficiary.nickname} from your saved beneficiaries?`)) return;

    setDeletingId(beneficiary.id);
    setActionError(null);
    setSuccess(null);
    try {
      await deleteBeneficiary(beneficiary.id);
      setBeneficiaries((current) => current.filter((item) => item.id !== beneficiary.id));
      setSuccess('Beneficiary removed.');
    } catch (error) {
      if (!handleUnauthorized(error)) setActionError(getDeleteErrorMessage(error));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <main className="register-shell beneficiary-shell">
      <section className="beneficiary-card" aria-labelledby="beneficiary-title">
        <div className="beneficiary-topbar">
          <Link to="/dashboard" className="brand-row">
            <div className="brand-mark">PL</div>
            <div className="brand-copy"><small>Payment Ledger</small><strong>Saved Beneficiaries</strong></div>
          </Link>
          <div className="beneficiary-navigation">
            <Link to="/dashboard" className="secondary-button secondary-button-link">Dashboard</Link>
            <Link to="/wallet" className="secondary-button secondary-button-link">My Wallet</Link>
            <Link to="/profile" className="secondary-button secondary-button-link">Profile</Link>
          </div>
        </div>

        <div className="beneficiary-heading">
          <div>
            <span className="hero-kicker">Authenticated address book</span>
            <h1 id="beneficiary-title">Keep trusted recipients close.</h1>
            <p>Save account details for your own reference. Beneficiaries are private to your account.</p>
          </div>
          <span className="status-pill">Private list</span>
        </div>

        <div className="beneficiary-content">
          <section className="beneficiary-panel" aria-labelledby="add-beneficiary-title">
            <div className="beneficiary-panel-heading">
              <div><span className="beneficiary-kicker">New contact</span><h2 id="add-beneficiary-title">Add a beneficiary</h2></div>
            </div>
            <form className="beneficiary-form" onSubmit={handleCreate} noValidate>
              <div className="beneficiary-field">
                <label htmlFor="beneficiary-account-number">Account number</label>
                <input id="beneficiary-account-number" className="input" value={accountNumber} onChange={(event) => setAccountNumber(event.target.value)} disabled={creating} autoComplete="off" />
              </div>
              <div className="beneficiary-field">
                <label htmlFor="beneficiary-nickname">Nickname</label>
                <input id="beneficiary-nickname" className="input" value={nickname} onChange={(event) => setNickname(event.target.value)} disabled={creating} autoComplete="off" />
              </div>
              <button type="submit" className="primary-button" disabled={creating}>{creating ? 'Saving...' : 'Save beneficiary'}</button>
            </form>
            {createError && <div className="banner error" role="alert">{createError}</div>}
            {success && <div className="banner success" role="status">{success}</div>}
            <p className="beneficiary-note">The recipient account must already exist.</p>
          </section>

          <section className="beneficiary-panel" aria-labelledby="saved-beneficiaries-title">
            <div className="beneficiary-panel-heading">
              <div><span className="beneficiary-kicker">Your contacts</span><h2 id="saved-beneficiaries-title">Saved beneficiaries</h2></div>
              {!loading && !loadingError && <span className="status-pill">{beneficiaries.length} saved</span>}
            </div>

            {loading && <div className="beneficiary-state" role="status" aria-live="polite"><span className="loading-dot" aria-hidden="true" />Loading saved beneficiaries...</div>}
            {!loading && loadingError && <div className="beneficiary-state error" role="alert"><strong>{loadingError}</strong><button type="button" className="primary-button" onClick={() => setRetryKey((key) => key + 1)}>Try again</button></div>}
            {!loading && !loadingError && beneficiaries.length === 0 && <div className="beneficiary-empty">No beneficiaries saved yet. Add a trusted recipient above.</div>}
            {!loading && !loadingError && beneficiaries.length > 0 && (
              <div className="beneficiary-list">
                {beneficiaries.map((beneficiary) => (
                  <article className="beneficiary-row" key={beneficiary.id}>
                    {editingId === beneficiary.id ? (
                      <div className="beneficiary-edit">
                        <input className="input" aria-label={`Nickname for ${beneficiary.accountNumber}`} value={editNickname} onChange={(event) => setEditNickname(event.target.value)} disabled={updatingId === beneficiary.id} autoFocus />
                        <button type="button" className="primary-button" onClick={() => void handleUpdate(beneficiary.id)} disabled={updatingId === beneficiary.id}>{updatingId === beneficiary.id ? 'Updating...' : 'Save'}</button>
                        <button type="button" className="secondary-button" onClick={() => setEditingId(null)} disabled={updatingId === beneficiary.id}>Cancel</button>
                      </div>
                    ) : (
                      <div className="beneficiary-info">
                        <span className="beneficiary-name">{beneficiary.nickname}</span>
                        <span className="beneficiary-account">{beneficiary.accountNumber}</span>
                        <span className="beneficiary-owner">Recipient: {beneficiary.recipientOwnerName}</span>
                      </div>
                    )}
                    {editingId !== beneficiary.id && <div className="beneficiary-actions"><button type="button" className="secondary-button" onClick={() => startEditing(beneficiary)} disabled={creating || updatingId !== null || deletingId !== null}>Edit nickname</button><button type="button" className="danger-button" onClick={() => void handleDelete(beneficiary)} disabled={creating || updatingId !== null || deletingId !== null}> {deletingId === beneficiary.id ? 'Removing...' : 'Remove'} </button></div>}
                  </article>
                ))}
              </div>
            )}
            {actionError && <div className="banner error" role="alert">{actionError}</div>}
          </section>
        </div>
      </section>
    </main>
  );
}

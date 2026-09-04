import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { getTransaction, getTransactions } from '../api/walletApi';
import NotificationBell from '../components/NotificationBell';
import { useAuth } from '../auth/AuthContext';
import type { TransactionFilters, TransactionResponse, TransactionType } from '../types/wallet';
import './activity.css';
import './dashboard.css';

const vndFormatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });

function formatBalance(value: number | string) {
  const amount = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(amount) ? vndFormatter.format(amount) : 'Unavailable';
}

function formatType(type: TransactionResponse['transactionType']) {
  return ({ DEPOSIT: 'Deposit', WITHDRAW: 'Withdraw', TRANSFER_IN: 'Transfer received', TRANSFER_OUT: 'Transfer sent' })[type] ?? type;
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function validateFilters(filters: TransactionFilters) {
  if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) return 'From date must be before or equal to To date.';
  const min = filters.minAmount ? Number(filters.minAmount) : null;
  const max = filters.maxAmount ? Number(filters.maxAmount) : null;
  if (min !== null && (!Number.isFinite(min) || min < 0)) return 'Minimum amount must be a non-negative number.';
  if (max !== null && (!Number.isFinite(max) || max < 0)) return 'Maximum amount must be a non-negative number.';
  if (min !== null && max !== null && min > max) return 'Minimum amount must be less than or equal to maximum amount.';
  return null;
}

function errorMessage(error: unknown, detail = false) {
  if (error instanceof ApiError && error.status === 404) return detail ? 'Transaction could not be found.' : 'No activity was found.';
  if (error instanceof ApiError && error.status === 403) return 'You do not have permission to view activity.';
  if (error instanceof ApiError) return detail ? 'Transaction details could not be loaded.' : 'Activity could not be loaded. Please try again.';
  return 'The server could not be reached. Check your connection and try again.';
}

export default function ActivityPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [first, setFirst] = useState(true);
  const [last, setLast] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [draft, setDraft] = useState<TransactionFilters>({});
  const [active, setActive] = useState<TransactionFilters>({});
  const [filterError, setFilterError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selected, setSelected] = useState<TransactionResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    async function load() {
      setLoading(true); setError(null);
      try {
        const response = await getTransactions({ ...active, page, size: 10, sort: 'createdAt,desc', signal: controller.signal });
        setTransactions(response.content); setTotalPages(response.totalPages); setTotalElements(response.totalElements);
        setFirst(response.first); setLast(response.last);
      } catch (requestError) {
        if (controller.signal.aborted) return;
        if (requestError instanceof ApiError && requestError.status === 401) { logout(); navigate('/login', { replace: true }); return; }
        setError(errorMessage(requestError));
      } finally { if (!controller.signal.aborted) setLoading(false); }
    }
    void load();
    return () => controller.abort();
  }, [active, page, refreshKey, logout, navigate]);

  useEffect(() => {
    if (selectedId === null) { setSelected(null); setDetailError(null); return; }
    const transactionId = selectedId;
    const controller = new AbortController();
    async function loadDetail() {
      setDetailLoading(true); setDetailError(null); setSelected(null);
      try { setSelected(await getTransaction(transactionId, controller.signal)); }
      catch (requestError) {
        if (controller.signal.aborted) return;
        if (requestError instanceof ApiError && requestError.status === 401) { logout(); navigate('/login', { replace: true }); return; }
        setDetailError(errorMessage(requestError, true));
      } finally { if (!controller.signal.aborted) setDetailLoading(false); }
    }
    void loadDetail();
    return () => controller.abort();
  }, [selectedId, logout, navigate]);

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const message = validateFilters(draft);
    setFilterError(message);
    if (message) return;
    setActive({ ...draft }); setPage(0);
  }

  function resetFilters() { setDraft({}); setActive({}); setFilterError(null); setPage(0); }

  return (
    <main className="dashboard-shell activity-shell">
      <header className="dashboard-header">
        <Link to="/dashboard" className="dashboard-brand" aria-label="Lumo home">
          <span className="dashboard-brand-mark" aria-hidden="true"><i /></span>
          <strong>Lumo</strong>
        </Link>
        <nav className="dashboard-nav" aria-label="Main navigation">
          <Link to="/dashboard">Home</Link>
          <Link to="/wallet">Wallet</Link>
          <Link className="active" to="/activity">Activity</Link>
        </nav>
        <div className="dashboard-header-actions">
          <NotificationBell />
          <Link to="/profile" className="dashboard-wallet-profile">Profile</Link>
        </div>
      </header>
      <section className="dashboard-container activity-container">
        <div className="activity-page-heading"><div><span className="activity-kicker">Activity</span><h1>Activity</h1><p>Your wallet history, all in one place.</p></div></div>
        <section className="activity-panel" aria-labelledby="activity-title">
          <div className="activity-panel-heading"><h2 id="activity-title">All activity</h2><span className="activity-count">{totalElements} {totalElements === 1 ? 'transaction' : 'transactions'}</span></div>
          <div className="activity-filter-bar">
            <label className="activity-type-control" htmlFor="activity-type"><span>Show</span><select id="activity-type" value={draft.type ?? ''} disabled={loading} onChange={(event) => { const type = event.target.value ? event.target.value as TransactionType : undefined; setDraft((current) => ({ ...current, type })); setActive((current) => ({ ...current, type })); setPage(0); }}><option value="">Everything</option><option value="DEPOSIT">Deposits</option><option value="WITHDRAW">Withdrawals</option><option value="TRANSFER_IN">Money received</option><option value="TRANSFER_OUT">Money sent</option></select></label>
            <button type="button" className="filter-toggle" aria-expanded={filtersOpen} onClick={() => setFiltersOpen((open) => !open)}>Filters <span aria-hidden="true">{filtersOpen ? '−' : '+'}</span></button>
          </div>
          {filtersOpen && <form className="activity-advanced-filters" onSubmit={applyFilters} noValidate><label>From date<input type="date" value={draft.fromDate ?? ''} onChange={(event) => setDraft((current) => ({ ...current, fromDate: event.target.value || undefined }))} /></label><label>To date<input type="date" value={draft.toDate ?? ''} onChange={(event) => setDraft((current) => ({ ...current, toDate: event.target.value || undefined }))} /></label><label>Minimum amount<input type="number" min="0" step="1" value={draft.minAmount ?? ''} onChange={(event) => setDraft((current) => ({ ...current, minAmount: event.target.value || undefined }))} /></label><label>Maximum amount<input type="number" min="0" step="1" value={draft.maxAmount ?? ''} onChange={(event) => setDraft((current) => ({ ...current, maxAmount: event.target.value || undefined }))} /></label><div className="activity-filter-actions"><button className="primary-button" type="submit" disabled={loading}>Apply</button><button className="secondary-button" type="button" onClick={resetFilters} disabled={loading}>Reset</button></div>{filterError && <p className="activity-error" role="alert">{filterError}</p>}</form>}
          {loading && <div className="activity-state" role="status">Loading activity...</div>}
          {!loading && error && <div className="activity-state activity-error" role="alert">{error}<button className="secondary-button" type="button" onClick={() => setRefreshKey((key) => key + 1)}>Try again</button></div>}
          {!loading && !error && transactions.length === 0 && <div className="activity-state">{Object.values(active).some(Boolean) ? 'No transactions match these filters.' : 'No transactions yet.'}</div>}
          {!loading && !error && transactions.length > 0 && <div className="activity-list" role="list">{transactions.map((transaction) => { const incoming = transaction.transactionType === 'DEPOSIT' || transaction.transactionType === 'TRANSFER_IN'; return <article className="activity-row" role="listitem" key={transaction.id}><span className={`activity-direction ${incoming ? 'incoming' : 'outgoing'}`} aria-hidden="true">{incoming ? '↙' : '↗'}</span><div className="activity-row-copy"><strong>{formatType(transaction.transactionType)}</strong><small>{formatDate(transaction.createdAt)}</small></div><div className={`activity-row-amount ${incoming ? 'incoming' : 'outgoing'}`}><strong>{incoming ? '+' : '-'}{formatBalance(transaction.amount)}</strong><small>Balance {formatBalance(transaction.balanceAfterTransaction)}</small></div><button className="activity-detail-trigger" type="button" onClick={() => setSelectedId(transaction.id)} aria-label={`View details for transaction ${transaction.id}`}>›</button></article>; })}</div>}
          {!loading && !error && transactions.length > 0 && <div className="activity-pagination"><button className="secondary-button" type="button" disabled={first || loading} onClick={() => setPage((current) => Math.max(0, current - 1))}>Previous</button><span>Page {page + 1} of {Math.max(totalPages, 1)}</span><button className="secondary-button" type="button" disabled={last || loading} onClick={() => setPage((current) => current + 1)}>Next</button></div>}
          {selectedId !== null && <section className="activity-detail" aria-labelledby="activity-detail-title"><div className="activity-detail-heading"><div><span className="activity-kicker">Selected movement</span><h3 id="activity-detail-title">Transaction #{selectedId}</h3></div><button className="secondary-button" type="button" onClick={() => setSelectedId(null)}>Close</button></div>{detailLoading && <div className="activity-state">Loading details...</div>}{!detailLoading && detailError && <div className="activity-state activity-error">{detailError}</div>}{!detailLoading && !detailError && selected && <dl><div><dt>Type</dt><dd>{formatType(selected.transactionType)}</dd></div><div><dt>Amount</dt><dd>{formatBalance(selected.amount)}</dd></div><div><dt>Date</dt><dd>{formatDate(selected.createdAt)}</dd></div><div><dt>Balance after</dt><dd>{formatBalance(selected.balanceAfterTransaction)}</dd></div><div><dt>Related account</dt><dd>{selected.relatedAccountId === null ? 'This wallet' : `Account #${selected.relatedAccountId}`}</dd></div></dl>}</section>}
        </section>
      </section>
    </main>
  );
}

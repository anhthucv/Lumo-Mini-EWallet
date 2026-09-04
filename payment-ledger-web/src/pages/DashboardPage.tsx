import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { getBeneficiaries } from '../api/beneficiaryApi';
import { ApiError } from '../api/http';
import { getMyWallet, getTransactions } from '../api/walletApi';
import { useAuth } from '../auth/AuthContext';
import NotificationBell from '../components/NotificationBell';
import type { Beneficiary } from '../types/beneficiary';
import type { MyWalletResponse, TransactionResponse } from '../types/wallet';
import './dashboard.css';

const vndFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
});

function formatMoney(value: number | string): string {
  const amount = Number(value);
  return Number.isFinite(amount) ? vndFormatter.format(amount) : 'Unavailable';
}

function getFirstName(fullName: string | undefined): string {
  const name = fullName?.trim();
  return name ? (name.split(/\s+/)[0] ?? 'there') : 'there';
}

function getInitials(name: string | undefined): string {
  const value = name?.trim();
  if (!value) return 'LU';
  return value.split(/\s+/).slice(0, 2).map((part) => part[0] ?? '').join('').toUpperCase();
}

function maskAccountNumber(accountNumber: string): string {
  const compact = accountNumber.replace(/\s/g, '');
  return compact.length > 4 ? `•••• ${compact.slice(-4)}` : compact;
}

function formatTransactionType(type: TransactionResponse['transactionType']): string {
  switch (type) {
    case 'DEPOSIT': return 'Money added';
    case 'WITHDRAW': return 'Money withdrawn';
    case 'TRANSFER_IN': return 'Money received';
    case 'TRANSFER_OUT': return 'Transfer sent';
  }
}

function isIncoming(type: TransactionResponse['transactionType']): boolean {
  return type === 'DEPOSIT' || type === 'TRANSFER_IN';
}

function formatTransactionDate(createdAt: string): string {
  const date = new Date(createdAt);
  return Number.isFinite(date.getTime())
    ? new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
    : createdAt;
}

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiError && error.status !== 401 && error.message ? error.message : fallback;
}

function ActionIcon({ children }: { children: string }) {
  return <span className="dashboard-action-icon" aria-hidden="true">{children}</span>;
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [wallet, setWallet] = useState<MyWalletResponse | null>(null);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [walletLoading, setWalletLoading] = useState(true);
  const [activityLoading, setActivityLoading] = useState(true);
  const [peopleLoading, setPeopleLoading] = useState(true);
  const [walletError, setWalletError] = useState<string | null>(null);
  const [activityError, setActivityError] = useState<string | null>(null);
  const [peopleError, setPeopleError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    async function loadDashboard() {
      setWalletLoading(true); setActivityLoading(true); setPeopleLoading(true);
      setWalletError(null); setActivityError(null); setPeopleError(null);
      const [walletResult, activityResult, peopleResult] = await Promise.allSettled([
        getMyWallet(controller.signal),
        getTransactions({ page: 0, size: 5, signal: controller.signal }),
        getBeneficiaries(controller.signal),
      ]);
      if (!active || controller.signal.aborted) return;

      if (walletResult.status === 'fulfilled') setWallet(walletResult.value);
      else if (walletResult.reason instanceof ApiError && walletResult.reason.status === 401) {
        logout(); navigate('/login', { replace: true }); return;
      } else setWalletError(getErrorMessage(walletResult.reason, 'Could not load your wallet.'));
      setWalletLoading(false);

      if (activityResult.status === 'fulfilled') setTransactions(activityResult.value.content);
      else if (activityResult.reason instanceof ApiError && activityResult.reason.status === 401) {
        logout(); navigate('/login', { replace: true }); return;
      } else setActivityError(getErrorMessage(activityResult.reason, 'Could not load recent activity.'));
      setActivityLoading(false);

      if (peopleResult.status === 'fulfilled') setBeneficiaries(peopleResult.value);
      else if (peopleResult.reason instanceof ApiError && peopleResult.reason.status === 401) {
        logout(); navigate('/login', { replace: true }); return;
      } else setPeopleError(getErrorMessage(peopleResult.reason, 'Could not load saved people.'));
      setPeopleLoading(false);
    }

    void loadDashboard();
    return () => { active = false; controller.abort(); };
  }, [logout, navigate, retryKey]);

  function handleSignOut() {
    logout(); navigate('/login', { replace: true });
  }

  return (
    <main className="dashboard-shell">
      <div className="dashboard-container">
        <header className="dashboard-header">
          <Link to="/dashboard" className="dashboard-brand" aria-label="Lumo home">
            <span className="dashboard-brand-mark" aria-hidden="true"><i /></span>
            <strong>Lumo</strong>
          </Link>
          <nav className="dashboard-nav" aria-label="Main navigation">
            <Link className="active" to="/dashboard">Home</Link>
            <Link to="/wallet">Wallet</Link>
            <Link to="/activity">Activity</Link>
          </nav>
          <div className="dashboard-header-actions">
            <NotificationBell />
            <details className="dashboard-profile-menu">
              <summary aria-label="Open profile menu">
                <span className="dashboard-avatar">{getInitials(user?.fullName)}</span>
                <span className="dashboard-avatar-name">{getFirstName(user?.fullName)}</span>
              </summary>
              <div className="dashboard-profile-dropdown">
                <span className="dashboard-profile-label">Signed in as</span>
                <strong>{user?.email}</strong>
                <Link to="/profile">Profile</Link>
                <button type="button" onClick={handleSignOut}>Sign out</button>
              </div>
            </details>
          </div>
        </header>

        <section className="dashboard-welcome" aria-labelledby="dashboard-title">
          <span className="dashboard-eyebrow">Your wallet</span>
          <h1 id="dashboard-title">Good day, {getFirstName(user?.fullName)}.</h1>
          <p>Here is what is happening with your money.</p>
        </section>

        <section className="dashboard-wallet-card" aria-labelledby="balance-title">
          <div className="dashboard-wallet-topline">
            <span className="dashboard-card-label">Available balance</span>
            {wallet && <span className="dashboard-wallet-status"><i /> {wallet.status.toLowerCase()}</span>}
          </div>
          {walletLoading && <div className="dashboard-skeleton dashboard-balance-skeleton" role="status" aria-label="Loading balance" />}
          {!walletLoading && walletError && <div className="dashboard-inline-error" role="alert"><span>{walletError}</span><button type="button" onClick={() => setRetryKey((key) => key + 1)}>Try again</button></div>}
          {!walletLoading && !walletError && wallet && <><strong id="balance-title" className="dashboard-balance">{formatMoney(wallet.balance)}</strong><span className="dashboard-account">Wallet {maskAccountNumber(wallet.accountNumber)}</span></>}
          <div className="dashboard-quick-actions" aria-label="Wallet actions">
            <Link to="/wallet" className="dashboard-action dashboard-action-send"><ActionIcon>↗</ActionIcon><span>Send</span></Link>
            <Link to="/wallet" className="dashboard-action dashboard-action-add"><ActionIcon>+</ActionIcon><span>Add money</span></Link>
            <Link to="/wallet" className="dashboard-action dashboard-action-withdraw"><ActionIcon>↙</ActionIcon><span>Withdraw</span></Link>
          </div>
        </section>

        <div className="dashboard-content-grid">
          <section className="dashboard-surface dashboard-activity" aria-labelledby="activity-title">
            <div className="dashboard-section-heading"><div><span className="dashboard-section-kicker">Money trail</span><h2 id="activity-title">Recent activity</h2></div><Link to="/wallet">View all</Link></div>
            {activityLoading && <div className="dashboard-section-state" role="status">Loading activity...</div>}
            {!activityLoading && activityError && <div className="dashboard-section-state dashboard-section-error">{activityError}</div>}
            {!activityLoading && !activityError && transactions.length === 0 && <div className="dashboard-section-state">No activity yet.</div>}
            {!activityLoading && !activityError && transactions.length > 0 && <div className="dashboard-transaction-list" role="list">{transactions.map((transaction) => {
              const incoming = isIncoming(transaction.transactionType);
              return <article className="dashboard-transaction-row" role="listitem" key={transaction.id}><span className={`dashboard-transaction-icon ${incoming ? 'incoming' : 'outgoing'}`} aria-hidden="true">{incoming ? '↙' : '↗'}</span><div className="dashboard-transaction-copy"><strong>{formatTransactionType(transaction.transactionType)}</strong><span>{formatTransactionDate(transaction.createdAt)}</span></div><strong className={`dashboard-transaction-amount ${incoming ? 'incoming' : 'outgoing'}`}>{incoming ? '+' : '-'} {formatMoney(transaction.amount)}</strong></article>;
            })}</div>}
          </section>

          <section className="dashboard-surface dashboard-people" aria-labelledby="people-title">
            <div className="dashboard-section-heading"><div><span className="dashboard-section-kicker">Quick people</span><h2 id="people-title">Send again</h2></div><Link to="/wallet">Manage</Link></div>
            {peopleLoading && <div className="dashboard-section-state" role="status">Loading saved people...</div>}
            {!peopleLoading && peopleError && <div className="dashboard-section-state dashboard-section-error">{peopleError}</div>}
            {!peopleLoading && !peopleError && beneficiaries.length === 0 && <div className="dashboard-section-state"><span>No saved recipients yet.</span><Link to="/wallet">Add someone</Link></div>}
            {!peopleLoading && !peopleError && beneficiaries.length > 0 && <div className="dashboard-people-list">{beneficiaries.slice(0, 4).map((beneficiary) => <Link className="dashboard-person" to="/wallet" key={beneficiary.id}><span className="dashboard-person-avatar">{getInitials(beneficiary.nickname)}</span><span><strong>{beneficiary.nickname}</strong><small>{beneficiary.recipientOwnerName}</small></span></Link>)}</div>}
          </section>
        </div>
      </div>
    </main>
  );
}

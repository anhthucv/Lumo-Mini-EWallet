import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { deposit, getMyWallet } from '../api/walletApi';
import { useAuth } from '../auth/AuthContext';
import type { DepositResponse, MyWalletResponse } from '../types/wallet';
import './register.css';
import './wallet.css';

const vndFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});

function formatBalance(balance: number | string): string {
  const numericBalance = typeof balance === 'number' ? balance : Number(balance);
  return Number.isFinite(numericBalance) ? vndFormatter.format(numericBalance) : 'Unavailable';
}

function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 404) {
    return 'No wallet is associated with this account yet.';
  }
  if (error instanceof ApiError) {
    return 'The wallet could not be loaded. Please try again.';
  }
  return 'The server could not be reached. Check your connection and try again.';
}

function getDepositErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 400) {
    return error.message || 'Enter a valid deposit amount of at least 1,000 VND.';
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'Your wallet could not be found. Please try again later.';
  }
  if (error instanceof ApiError) {
    return 'The deposit could not be completed. Please try again.';
  }
  return 'The server could not be reached. Please check your connection and try again.';
}

function toWalletResponse(wallet: MyWalletResponse, response: DepositResponse): MyWalletResponse {
  return {
    ...wallet,
    accountId: response.id,
    accountNumber: response.accountNumber,
    ownerName: response.ownerName,
    balance: response.balance,
    status: response.status,
  };
}

export default function WalletPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [wallet, setWallet] = useState<MyWalletResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [amount, setAmount] = useState('');
  const [depositError, setDepositError] = useState<string | null>(null);
  const [depositSuccess, setDepositSuccess] = useState<string | null>(null);
  const [depositing, setDepositing] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    async function loadWallet() {
      setLoading(true);
      setError(null);

      try {
        const response = await getMyWallet(controller.signal);
        setWallet(response);
      } catch (requestError) {
        if (controller.signal.aborted) {
          return;
        }

        if (requestError instanceof ApiError && requestError.status === 401) {
          logout();
          navigate('/login', { replace: true });
          return;
        }

        setWallet(null);
        setError(getErrorMessage(requestError));
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    void loadWallet();

    return () => controller.abort();
  }, [logout, navigate, retryKey]);

  async function handleDeposit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setDepositError(null);
    setDepositSuccess(null);

    const numericAmount = Number(amount);
    if (!amount.trim() || !Number.isFinite(numericAmount) || numericAmount <= 0) {
      setDepositError('Enter a valid amount greater than zero.');
      return;
    }
    if (numericAmount < 1000) {
      setDepositError('The minimum deposit is 1,000 VND.');
      return;
    }

    setDepositing(true);
    try {
      const response = await deposit(numericAmount);
      setWallet((currentWallet) => currentWallet && toWalletResponse(currentWallet, response));
      setAmount('');
      setDepositSuccess(
        `Deposited ${formatBalance(numericAmount)}. Updated balance: ${formatBalance(response.balance)}.`,
      );
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setDepositError(getDepositErrorMessage(requestError));
    } finally {
      setDepositing(false);
    }
  }

  return (
    <main className="register-shell wallet-shell">
      <section className="wallet-card" aria-labelledby="wallet-title">
        <div className="wallet-topbar">
          <Link to="/dashboard" className="brand-row wallet-brand">
            <div className="brand-mark">PL</div>
            <div className="brand-copy">
              <small>Payment Ledger</small>
              <strong>My Wallet</strong>
            </div>
          </Link>
          <Link to="/dashboard" className="secondary-button secondary-button-link">
            Dashboard
          </Link>
        </div>

        <div className="wallet-heading">
          <div>
            <span className="hero-kicker">Authenticated wallet</span>
            <h1 id="wallet-title">Your money, clearly accounted for.</h1>
            <p>Review the wallet connected to your authenticated Payment Ledger account.</p>
          </div>
          {wallet && <div className="status-pill">{wallet.status}</div>}
        </div>

        {loading && (
          <div className="wallet-state" role="status" aria-live="polite">
            <span className="loading-dot" aria-hidden="true" />
            Loading your wallet...
          </div>
        )}

        {!loading && error && (
          <div className="wallet-state wallet-state-error" role="alert">
            <strong>{error}</strong>
            <button type="button" className="primary-button" onClick={() => setRetryKey((key) => key + 1)}>
              Try again
            </button>
          </div>
        )}

        {!loading && !error && wallet && (
          <div className="wallet-content">
            <div className="balance-panel">
              <span>Current balance</span>
              <strong>{formatBalance(wallet.balance)}</strong>
              <small>Available balance in your wallet</small>
            </div>
            <div className="wallet-details" aria-label="Wallet details">
              <div className="summary-item">
                <span>Account number</span>
                <strong>{wallet.accountNumber}</strong>
              </div>
              <div className="summary-item">
                <span>Owner name</span>
                <strong>{wallet.ownerName}</strong>
              </div>
              <div className="summary-item">
                <span>Account status</span>
                <strong>{wallet.status}</strong>
              </div>
              <div className="summary-item">
                <span>Account ID</span>
                <strong>{wallet.accountId}</strong>
              </div>
            </div>
            <form className="deposit-panel" onSubmit={handleDeposit} noValidate>
              <div className="deposit-heading">
                <div>
                  <span className="deposit-kicker">Add funds</span>
                  <h2>Deposit to this wallet</h2>
                </div>
                <span className="deposit-minimum">Minimum 1,000 VND</span>
              </div>
              <label className="field-group" htmlFor="deposit-amount">
                <span className="field-label">Amount in VND</span>
                <input
                  id="deposit-amount"
                  className="input"
                  type="number"
                  inputMode="decimal"
                  min="1000"
                  step="1"
                  value={amount}
                  onChange={(event) => setAmount(event.target.value)}
                  placeholder="100,000"
                  disabled={depositing}
                />
              </label>
              {depositError && <div className="banner error" role="alert">{depositError}</div>}
              {depositSuccess && <div className="banner success" role="status">{depositSuccess}</div>}
              <button type="submit" className="primary-button deposit-button" disabled={depositing}>
                {depositing ? 'Processing deposit...' : 'Deposit funds'}
              </button>
            </form>
          </div>
        )}
      </section>
    </main>
  );
}

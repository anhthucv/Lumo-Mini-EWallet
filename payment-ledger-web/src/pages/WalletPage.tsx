import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { deposit, getMyWallet, getRecipient, transfer, withdraw } from '../api/walletApi';
import { useAuth } from '../auth/AuthContext';
import type { DepositResponse, MyWalletResponse, RecipientResponse } from '../types/wallet';
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
    return error.message || 'Enter a valid deposit amount of at least 1 VND.';
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'Your wallet could not be found. Please try again later.';
  }
  if (error instanceof ApiError) {
    return 'The deposit could not be completed. Please try again.';
  }
  return 'The server could not be reached. Please check your connection and try again.';
}

function getWithdrawErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 400) {
    return error.message || 'The withdrawal amount is not valid or exceeds the available balance.';
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'Your wallet could not be found. Please try again later.';
  }
  if (error instanceof ApiError) {
    return 'The withdrawal could not be completed. Please try again.';
  }
  return 'The server could not be reached. Please check your connection and try again.';
}

function getRecipientErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 404) {
    return 'No recipient was found for that account number.';
  }
  if (error instanceof ApiError && error.status === 400) {
    return error.message || 'Enter a valid recipient account number.';
  }
  if (error instanceof ApiError) {
    return 'The recipient could not be looked up. Please try again.';
  }
  return 'The server could not be reached. Please check your connection and try again.';
}

function getTransferErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 400) {
    return error.message || 'The transfer could not be completed with these details.';
  }
  if (error instanceof ApiError && error.status === 404) {
    return 'The recipient wallet could not be found. Look it up again and retry.';
  }
  if (error instanceof ApiError) {
    return 'The transfer could not be completed. Please try again.';
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
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [withdrawError, setWithdrawError] = useState<string | null>(null);
  const [withdrawSuccess, setWithdrawSuccess] = useState<string | null>(null);
  const [withdrawing, setWithdrawing] = useState(false);
  const [recipientAccountNumber, setRecipientAccountNumber] = useState('');
  const [recipient, setRecipient] = useState<RecipientResponse | null>(null);
  const [recipientError, setRecipientError] = useState<string | null>(null);
  const [lookingUpRecipient, setLookingUpRecipient] = useState(false);
  const [transferAmount, setTransferAmount] = useState('');
  const [transferError, setTransferError] = useState<string | null>(null);
  const [transferSuccess, setTransferSuccess] = useState<string | null>(null);
  const [transferring, setTransferring] = useState(false);

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
      setDepositError('The minimum deposit is 1 VND.');
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

  async function handleWithdraw(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setWithdrawError(null);
    setWithdrawSuccess(null);

    const numericAmount = Number(withdrawAmount);
    if (!withdrawAmount.trim() || !Number.isFinite(numericAmount) || numericAmount <= 0) {
      setWithdrawError('Enter a valid amount greater than zero.');
      return;
    }
    if (numericAmount < 1000) {
      setWithdrawError('The minimum withdrawal is 1 VND.');
      return;
    }

    setWithdrawing(true);
    try {
      const response = await withdraw(numericAmount);
      setWallet((currentWallet) => currentWallet && toWalletResponse(currentWallet, response));
      setWithdrawAmount('');
      setWithdrawSuccess(
        `Withdrew ${formatBalance(numericAmount)}. Updated balance: ${formatBalance(response.balance)}.`,
      );
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setWithdrawError(getWithdrawErrorMessage(requestError));
    } finally {
      setWithdrawing(false);
    }
  }

  function handleRecipientChange(value: string) {
    setRecipientAccountNumber(value);
    setRecipient(null);
    setRecipientError(null);
    setTransferError(null);
    setTransferSuccess(null);
  }

  async function handleRecipientLookup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setRecipientError(null);
    setRecipient(null);
    setTransferError(null);
    setTransferSuccess(null);

    const accountNumber = recipientAccountNumber.trim();
    if (!accountNumber) {
      setRecipientError('Enter a recipient account number.');
      return;
    }
    if (wallet && accountNumber === wallet.accountNumber) {
      setRecipientError('You cannot transfer money to your own account.');
      return;
    }

    setLookingUpRecipient(true);
    try {
      setRecipient(await getRecipient(accountNumber));
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setRecipientError(getRecipientErrorMessage(requestError));
    } finally {
      setLookingUpRecipient(false);
    }
  }

  async function handleTransfer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setTransferError(null);
    setTransferSuccess(null);

    if (!recipient || recipient.accountNumber !== recipientAccountNumber.trim()) {
      setTransferError('Look up and confirm the recipient before transferring.');
      return;
    }
    const numericAmount = Number(transferAmount);
    if (!transferAmount.trim() || !Number.isFinite(numericAmount) || numericAmount <= 0) {
      setTransferError('Enter a valid amount greater than zero.');
      return;
    }
    if (numericAmount < 1000) {
      setTransferError('The minimum transfer is 1 VND.');
      return;
    }

    setTransferring(true);
    try {
      const response = await transfer(recipient.accountNumber, numericAmount);
      setWallet((currentWallet) => currentWallet && toWalletResponse(currentWallet, response));
      setRecipientAccountNumber('');
      setTransferAmount('');
      setRecipient(null);
      setTransferSuccess(
        `Transferred ${formatBalance(numericAmount)} to ${response.accountNumber}. Updated balance: ${formatBalance(response.balance)}.`,
      );
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setTransferError(getTransferErrorMessage(requestError));
    } finally {
      setTransferring(false);
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
                <span className="deposit-minimum">Minimum 1 VND</span>
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
            <form className="deposit-panel withdraw-panel" onSubmit={handleWithdraw} noValidate>
              <div className="deposit-heading">
                <div>
                  <span className="deposit-kicker">Move money out</span>
                  <h2>Withdraw from this wallet</h2>
                </div>
                <span className="deposit-minimum">Minimum 1 VND</span>
              </div>
              <label className="field-group" htmlFor="withdraw-amount">
                <span className="field-label">Amount in VND</span>
                <input
                  id="withdraw-amount"
                  className="input"
                  type="number"
                  inputMode="decimal"
                  min="1000"
                  step="1"
                  value={withdrawAmount}
                  onChange={(event) => setWithdrawAmount(event.target.value)}
                  placeholder="100,000"
                  disabled={withdrawing}
                />
              </label>
              {withdrawError && <div className="banner error" role="alert">{withdrawError}</div>}
              {withdrawSuccess && <div className="banner success" role="status">{withdrawSuccess}</div>}
              <button type="submit" className="primary-button deposit-button" disabled={withdrawing}>
                {withdrawing ? 'Processing withdrawal...' : 'Withdraw funds'}
              </button>
            </form>
            <section className="deposit-panel transfer-panel" aria-labelledby="transfer-title">
              <div className="deposit-heading">
                <div>
                  <span className="deposit-kicker">Send money</span>
                  <h2 id="transfer-title">Transfer to another wallet</h2>
                </div>
                <span className="deposit-minimum">Minimum 1 VND</span>
              </div>
              <form className="transfer-lookup" onSubmit={handleRecipientLookup} noValidate>
                <label className="field-group" htmlFor="recipient-account-number">
                  <span className="field-label">Recipient account number</span>
                  <input
                    id="recipient-account-number"
                    className="input"
                    value={recipientAccountNumber}
                    onChange={(event) => handleRecipientChange(event.target.value)}
                    placeholder="ACC-123456"
                    disabled={lookingUpRecipient || transferring}
                  />
                </label>
                <button type="submit" className="secondary-button" disabled={lookingUpRecipient || transferring}>
                  {lookingUpRecipient ? 'Looking up...' : 'Look up recipient'}
                </button>
              </form>
              {recipientError && <div className="banner error" role="alert">{recipientError}</div>}
              {recipient && (
                <div className="recipient-confirmation" role="status">
                  <span className="deposit-kicker">Recipient</span>
                  <strong>Account: {recipient.accountNumber}</strong>
                  <span>Name: {recipient.ownerName}</span>
                </div>
              )}
              <form onSubmit={handleTransfer} noValidate>
                <label className="field-group" htmlFor="transfer-amount">
                  <span className="field-label">Amount in VND</span>
                  <input
                    id="transfer-amount"
                    className="input"
                    type="number"
                    inputMode="decimal"
                    min="1000"
                    step="1"
                    value={transferAmount}
                    onChange={(event) => {
                      setTransferAmount(event.target.value);
                      setTransferError(null);
                      setTransferSuccess(null);
                    }}
                    placeholder="100,000"
                    disabled={!recipient || transferring}
                  />
                </label>
                {transferError && <div className="banner error" role="alert">{transferError}</div>}
                {transferSuccess && <div className="banner success" role="status">{transferSuccess}</div>}
                <button type="submit" className="primary-button deposit-button" disabled={!recipient || transferring}>
                  {transferring ? 'Processing transfer...' : 'Transfer funds'}
                </button>
              </form>
            </section>
          </div>
        )}
      </section>
    </main>
  );
}

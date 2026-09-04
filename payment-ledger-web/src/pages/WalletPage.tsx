import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { getBeneficiaries } from '../api/beneficiaryApi';
import { getOrCreateIdempotencyAttempt, type IdempotencyAttempt } from '../api/idempotency';
import {
  deposit,
  getMyWallet,
  getRecipient,
  getTransaction,
  getTransactions,
  getWalletLimits,
  transfer,
  withdraw,
} from '../api/walletApi';
import { useAuth } from '../auth/AuthContext';
import type {
  Beneficiary,
} from '../types/beneficiary';
import type {
  DepositResponse,
  MyWalletResponse,
  RecipientResponse,
  TransactionFilters,
  TransactionResponse,
  TransactionType,
  TransactionLimit,
  WalletLimitsResponse,
} from '../types/wallet';
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

function formatTransactionType(type: TransactionResponse['transactionType']): string {
  switch (type) {
    case 'TRANSFER_IN':
      return 'Transfer received';
    case 'TRANSFER_OUT':
      return 'Transfer sent';
    case 'DEPOSIT':
      return 'Deposit';
    case 'WITHDRAW':
      return 'Withdraw';
    default:
      return type;
  }
}

function formatTransactionDate(createdAt: string): string {
  const date = new Date(createdAt);
  return Number.isNaN(date.getTime())
    ? createdAt
    : new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
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
  if (error instanceof ApiError && error.code === 'RISK_REJECTED') {
    return 'This deposit was rejected by transaction risk controls.';
  }
  if (error instanceof ApiError && error.code === 'PER_TRANSACTION_LIMIT_EXCEEDED') {
    return 'This deposit exceeds the per-transaction limit.';
  }
  if (error instanceof ApiError && error.code === 'DAILY_TRANSACTION_LIMIT_EXCEEDED') {
    return 'This deposit exceeds the remaining daily limit.';
  }
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
  if (error instanceof ApiError && error.code === 'RISK_REJECTED') {
    return 'This withdrawal was rejected by transaction risk controls.';
  }
  if (error instanceof ApiError && error.code === 'PER_TRANSACTION_LIMIT_EXCEEDED') {
    return 'This withdrawal exceeds the per-transaction limit.';
  }
  if (error instanceof ApiError && error.code === 'DAILY_TRANSACTION_LIMIT_EXCEEDED') {
    return 'This withdrawal exceeds the remaining daily limit.';
  }
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
  if (error instanceof ApiError && error.code === 'RISK_REJECTED') {
    return 'This transfer was rejected by transaction risk controls.';
  }
  if (error instanceof ApiError && error.code === 'PER_TRANSACTION_LIMIT_EXCEEDED') {
    return 'This transfer exceeds the per-transaction limit.';
  }
  if (error instanceof ApiError && error.code === 'DAILY_TRANSACTION_LIMIT_EXCEEDED') {
    return 'This transfer exceeds the remaining daily limit.';
  }
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

function getHistoryErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 403) {
    return 'You do not have permission to view transaction history.';
  }
  if (error instanceof ApiError) {
    return 'Transaction history could not be loaded. Please try again.';
  }
  return 'The server could not be reached. Please check your connection and try again.';
}

function getTransactionDetailErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 404) {
    return 'Transaction could not be found.';
  }
  if (error instanceof ApiError) {
    return 'Transaction details could not be loaded. Please try again.';
  }
  return 'The server could not be reached. Please check your connection and try again.';
}

function validateTransactionFilters(filters: TransactionFilters): string | null {
  if (filters.fromDate && filters.toDate && filters.fromDate > filters.toDate) {
    return 'From date must be before or equal to To date.';
  }

  const minAmount = filters.minAmount ? Number(filters.minAmount) : null;
  const maxAmount = filters.maxAmount ? Number(filters.maxAmount) : null;
  if (filters.minAmount && (minAmount === null || !Number.isFinite(minAmount) || minAmount < 0)) {
    return 'Minimum amount must be a non-negative number.';
  }
  if (filters.maxAmount && (maxAmount === null || !Number.isFinite(maxAmount) || maxAmount < 0)) {
    return 'Maximum amount must be a non-negative number.';
  }
  if (minAmount !== null && maxAmount !== null && minAmount > maxAmount) {
    return 'Minimum amount must be less than or equal to maximum amount.';
  }
  return null;
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

function getLimitValidationMessage(amount: number, limit: TransactionLimit, operation: string): string | null {
  if (amount > Number(limit.perTransactionLimit)) {
    return `This ${operation} exceeds the per-transaction limit of ${formatBalance(limit.perTransactionLimit)}.`;
  }
  if (amount > Number(limit.remainingToday)) {
    return `This ${operation} exceeds the remaining daily limit of ${formatBalance(limit.remainingToday)}.`;
  }
  return null;
}

export default function WalletPage() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const [wallet, setWallet] = useState<MyWalletResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [limits, setLimits] = useState<WalletLimitsResponse | null>(null);
  const [limitsError, setLimitsError] = useState<string | null>(null);
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
  const [beneficiaries, setBeneficiaries] = useState<Beneficiary[]>([]);
  const [beneficiariesLoading, setBeneficiariesLoading] = useState(true);
  const [beneficiariesError, setBeneficiariesError] = useState<string | null>(null);
  const [selectedBeneficiaryId, setSelectedBeneficiaryId] = useState<number | null>(null);
  const [transferAmount, setTransferAmount] = useState('');
  const [transferError, setTransferError] = useState<string | null>(null);
  const [transferSuccess, setTransferSuccess] = useState<string | null>(null);
  const [transferring, setTransferring] = useState(false);
  const [history, setHistory] = useState<TransactionResponse[]>([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const [historyTotalElements, setHistoryTotalElements] = useState(0);
  const [historyFirst, setHistoryFirst] = useState(true);
  const [historyLast, setHistoryLast] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [historyRefreshKey, setHistoryRefreshKey] = useState(0);
  const [beneficiariesRetryKey, setBeneficiariesRetryKey] = useState(0);
  const [draftFilters, setDraftFilters] = useState<TransactionFilters>({});
  const [activeFilters, setActiveFilters] = useState<TransactionFilters>({});
  const [filterError, setFilterError] = useState<string | null>(null);
  const [selectedTransactionId, setSelectedTransactionId] = useState<number | null>(null);
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionResponse | null>(null);
  const [transactionDetailLoading, setTransactionDetailLoading] = useState(false);
  const [transactionDetailError, setTransactionDetailError] = useState<string | null>(null);
  const depositAttempt = useRef<IdempotencyAttempt | null>(null);
  const withdrawAttempt = useRef<IdempotencyAttempt | null>(null);
  const transferAttempt = useRef<IdempotencyAttempt | null>(null);
  const recipientInputRef = useRef<HTMLInputElement>(null);

  async function refreshLimits() {
    try {
      setLimits(await getWalletLimits());
      setLimitsError(null);
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setLimitsError('Transaction limits could not be loaded. Backend limits still apply.');
    }
  }

  useEffect(() => {
    void refreshLimits();
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    async function loadBeneficiaries() {
      setBeneficiariesLoading(true);
      setBeneficiariesError(null);

      try {
        setBeneficiaries(await getBeneficiaries(controller.signal));
      } catch (requestError) {
        if (controller.signal.aborted) {
          return;
        }
        if (requestError instanceof ApiError && requestError.status === 401) {
          logout();
          navigate('/login', { replace: true });
          return;
        }
        setBeneficiariesError('Saved recipients could not be loaded. You can still enter an account number manually.');
      } finally {
        if (!controller.signal.aborted) {
          setBeneficiariesLoading(false);
        }
      }
    }

    void loadBeneficiaries();

    return () => controller.abort();
  }, [beneficiariesRetryKey, logout, navigate]);

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

  useEffect(() => {
    const controller = new AbortController();

    async function loadHistory() {
      setHistoryLoading(true);
      setHistoryError(null);

      try {
        const response = await getTransactions({
          ...activeFilters,
          page: historyPage,
          size: 10,
          sort: 'createdAt,desc',
          signal: controller.signal,
        });
        setHistory(response.content);
        setHistoryTotalPages(response.totalPages);
        setHistoryTotalElements(response.totalElements);
        setHistoryFirst(response.first);
        setHistoryLast(response.last);
      } catch (requestError) {
        if (controller.signal.aborted) {
          return;
        }
        if (requestError instanceof ApiError && requestError.status === 401) {
          logout();
          navigate('/login', { replace: true });
          return;
        }
        setHistoryError(getHistoryErrorMessage(requestError));
      } finally {
        if (!controller.signal.aborted) {
          setHistoryLoading(false);
        }
      }
    }

    void loadHistory();

    return () => controller.abort();
  }, [activeFilters, historyPage, historyRefreshKey, logout, navigate]);

  useEffect(() => {
    if (selectedTransactionId === null) {
      setSelectedTransaction(null);
      setTransactionDetailError(null);
      return;
    }
    const transactionId = selectedTransactionId;

    const controller = new AbortController();

    async function loadTransactionDetail() {
      setTransactionDetailLoading(true);
      setSelectedTransaction(null);
      setTransactionDetailError(null);

      try {
        setSelectedTransaction(await getTransaction(transactionId, controller.signal));
      } catch (requestError) {
        if (controller.signal.aborted) {
          return;
        }
        if (requestError instanceof ApiError && requestError.status === 401) {
          logout();
          navigate('/login', { replace: true });
          return;
        }
        setTransactionDetailError(getTransactionDetailErrorMessage(requestError));
      } finally {
        if (!controller.signal.aborted) {
          setTransactionDetailLoading(false);
        }
      }
    }

    void loadTransactionDetail();

    return () => controller.abort();
  }, [logout, navigate, selectedTransactionId]);

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
    if (limits) {
      const limitError = getLimitValidationMessage(numericAmount, limits.deposit, 'deposit');
      if (limitError) {
        setDepositError(limitError);
        return;
      }
    }

    const attempt = getOrCreateIdempotencyAttempt(depositAttempt.current, String(numericAmount));
    depositAttempt.current = attempt;
    setDepositing(true);
    try {
      const response = await deposit(numericAmount, attempt.key);
      setWallet((currentWallet) => currentWallet && toWalletResponse(currentWallet, response));
      setAmount('');
      depositAttempt.current = null;
      setDepositSuccess(
        `Deposited ${formatBalance(numericAmount)}. Updated balance: ${formatBalance(response.balance)}.`,
      );
      setHistoryRefreshKey((key) => key + 1);
      void refreshLimits();
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        depositAttempt.current = null;
        logout();
        navigate('/login', { replace: true });
        return;
      }
      if (requestError instanceof ApiError) {
        depositAttempt.current = null;
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
    if (limits) {
      const limitError = getLimitValidationMessage(numericAmount, limits.withdraw, 'withdrawal');
      if (limitError) {
        setWithdrawError(limitError);
        return;
      }
    }

    const attempt = getOrCreateIdempotencyAttempt(withdrawAttempt.current, String(numericAmount));
    withdrawAttempt.current = attempt;
    setWithdrawing(true);
    try {
      const response = await withdraw(numericAmount, attempt.key);
      setWallet((currentWallet) => currentWallet && toWalletResponse(currentWallet, response));
      setWithdrawAmount('');
      withdrawAttempt.current = null;
      setWithdrawSuccess(
        `Withdrew ${formatBalance(numericAmount)}. Updated balance: ${formatBalance(response.balance)}.`,
      );
      setHistoryRefreshKey((key) => key + 1);
      void refreshLimits();
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        withdrawAttempt.current = null;
        logout();
        navigate('/login', { replace: true });
        return;
      }
      if (requestError instanceof ApiError) {
        withdrawAttempt.current = null;
      }
      setWithdrawError(getWithdrawErrorMessage(requestError));
    } finally {
      setWithdrawing(false);
    }
  }

  function handleRecipientChange(value: string) {
    transferAttempt.current = null;
    setRecipientAccountNumber(value);
    setSelectedBeneficiaryId(null);
    setRecipient(null);
    setRecipientError(null);
    setTransferError(null);
    setTransferSuccess(null);
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errorMessage = validateTransactionFilters(draftFilters);
    setFilterError(errorMessage);
    if (errorMessage) {
      return;
    }
    setActiveFilters({ ...draftFilters });
    setHistoryPage(0);
  }

  function resetFilters() {
    setDraftFilters({});
    setActiveFilters({});
    setFilterError(null);
    setHistoryPage(0);
  }

  async function lookupRecipient(accountNumberInput = recipientAccountNumber) {
    setRecipientError(null);
    setRecipient(null);
    setTransferError(null);
    setTransferSuccess(null);

    const accountNumber = accountNumberInput.trim();
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
      setSelectedBeneficiaryId(null);
      setRecipientError(getRecipientErrorMessage(requestError));
    } finally {
      setLookingUpRecipient(false);
    }
  }

  async function handleRecipientLookup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await lookupRecipient();
  }

  function selectBeneficiary(beneficiary: Beneficiary) {
    handleRecipientChange(beneficiary.accountNumber);
    setSelectedBeneficiaryId(beneficiary.id);
    recipientInputRef.current?.focus();
    void lookupRecipient(beneficiary.accountNumber);
  }

  function formatBeneficiaryAccount(accountNumber: string): string {
    return accountNumber.length > 4 ? `**** ${accountNumber.slice(-4)}` : accountNumber;
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
    if (limits) {
      const limitError = getLimitValidationMessage(numericAmount, limits.transfer, 'transfer');
      if (limitError) {
        setTransferError(limitError);
        return;
      }
    }

    const payload = JSON.stringify({
      recipientAccountNumber: recipient.accountNumber,
      amount: numericAmount,
    });
    const attempt = getOrCreateIdempotencyAttempt(transferAttempt.current, payload);
    transferAttempt.current = attempt;
    setTransferring(true);
    try {
      const response = await transfer(recipient.accountNumber, numericAmount, attempt.key);
      setWallet((currentWallet) => currentWallet && toWalletResponse(currentWallet, response));
      setRecipientAccountNumber('');
      setTransferAmount('');
      setRecipient(null);
      setSelectedBeneficiaryId(null);
      transferAttempt.current = null;
      setTransferSuccess(
        `Transferred ${formatBalance(numericAmount)} to ${response.accountNumber}. Updated balance: ${formatBalance(response.balance)}.`,
      );
      setHistoryRefreshKey((key) => key + 1);
      void refreshLimits();
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        transferAttempt.current = null;
        logout();
        navigate('/login', { replace: true });
        return;
      }
      if (requestError instanceof ApiError) {
        transferAttempt.current = null;
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
          <Link to="/profile" className="secondary-button secondary-button-link">
            Profile
          </Link>
          <Link to="/beneficiaries" className="secondary-button secondary-button-link">
            Beneficiaries
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
                <strong>{user?.fullName ?? wallet.ownerName}</strong>
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
            {limitsError && <div className="banner error" role="status">{limitsError}</div>}
            <form className="deposit-panel" onSubmit={handleDeposit} noValidate>
              <div className="deposit-heading">
                <div>
                  <span className="deposit-kicker">Add funds</span>
                  <h2>Deposit to this wallet</h2>
                </div>
                <span className="deposit-minimum">Minimum 1 VND</span>
              </div>
              {limits && <div className="limit-summary">
                <span>Per-transaction limit: {formatBalance(limits.deposit.perTransactionLimit)}</span>
                <span>Remaining today: {formatBalance(limits.deposit.remainingToday)}</span>
              </div>}
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
                  onChange={(event) => {
                    depositAttempt.current = null;
                    setAmount(event.target.value);
                  }}
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
              {limits && <div className="limit-summary">
                <span>Per-transaction limit: {formatBalance(limits.withdraw.perTransactionLimit)}</span>
                <span>Remaining today: {formatBalance(limits.withdraw.remainingToday)}</span>
              </div>}
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
                  onChange={(event) => {
                    withdrawAttempt.current = null;
                    setWithdrawAmount(event.target.value);
                  }}
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
              {limits && <div className="limit-summary">
                <span>Per-transaction limit: {formatBalance(limits.transfer.perTransactionLimit)}</span>
                <span>Remaining today: {formatBalance(limits.transfer.remainingToday)}</span>
              </div>}
              <div className="saved-recipients" aria-labelledby="saved-recipients-title">
                <div className="saved-recipients-heading">
                  <div>
                    <span className="deposit-kicker">Quick select</span>
                    <h3 id="saved-recipients-title">Saved recipients</h3>
                  </div>
                  {!beneficiariesLoading && beneficiaries.length > 0 && (
                    <span className="history-count">{beneficiaries.length} saved</span>
                  )}
                </div>
                {beneficiariesLoading && (
                  <div className="saved-recipients-state" role="status">Loading saved recipients...</div>
                )}
                {!beneficiariesLoading && beneficiariesError && (
                  <div className="saved-recipients-state saved-recipients-error" role="status">
                    <span>{beneficiariesError}</span>
                    <button
                      type="button"
                      className="secondary-button saved-recipients-retry"
                      onClick={() => setBeneficiariesRetryKey((key) => key + 1)}
                    >
                      Try again
                    </button>
                  </div>
                )}
                {!beneficiariesLoading && !beneficiariesError && beneficiaries.length === 0 && (
                  <div className="saved-recipients-state">No saved recipients yet.</div>
                )}
                {!beneficiariesLoading && beneficiaries.length > 0 && (
                  <div className="saved-recipient-list">
                    {beneficiaries.map((beneficiary) => (
                      <button
                        key={beneficiary.id}
                        type="button"
                        className={`saved-recipient ${selectedBeneficiaryId === beneficiary.id ? 'selected' : ''}`}
                        aria-pressed={selectedBeneficiaryId === beneficiary.id}
                        onClick={() => selectBeneficiary(beneficiary)}
                        disabled={lookingUpRecipient || transferring}
                        title={`Use account ${beneficiary.accountNumber}`}
                      >
                        <strong>{beneficiary.nickname}</strong>
                        <span>{beneficiary.recipientOwnerName}</span>
                        <small>{formatBeneficiaryAccount(beneficiary.accountNumber)}</small>
                      </button>
                    ))}
                  </div>
                )}
                {lookingUpRecipient && (
                  <div className="saved-recipients-status" role="status" aria-live="polite">
                    Checking recipient details...
                  </div>
                )}
              </div>
              <form className="transfer-lookup" onSubmit={handleRecipientLookup} noValidate>
                <label className="field-group" htmlFor="recipient-account-number">
                  <span className="field-label">Recipient account number</span>
                  <input
                    id="recipient-account-number"
                    className="input"
                    ref={recipientInputRef}
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
                      transferAttempt.current = null;
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
            <section className="history-panel" aria-labelledby="history-title">
              <div className="history-heading">
                <div>
                  <span className="deposit-kicker">Ledger activity</span>
                  <h2 id="history-title">Transaction history</h2>
                </div>
                <span className="history-count">
                  {historyTotalElements} {historyTotalElements === 1 ? 'transaction' : 'transactions'}
                </span>
              </div>
              <form className="history-filters" onSubmit={applyFilters} noValidate>
                <div className="history-filter-grid">
                  <label className="field-group" htmlFor="transaction-type">
                    <span className="field-label">Transaction type</span>
                    <select
                      id="transaction-type"
                      className="input"
                      value={draftFilters.type ?? ''}
                      onChange={(event) => setDraftFilters((filters) => ({
                        ...filters,
                        type: event.target.value ? event.target.value as TransactionType : undefined,
                      }))}
                      disabled={historyLoading}
                    >
                      <option value="">All</option>
                      <option value="DEPOSIT">Deposit</option>
                      <option value="WITHDRAW">Withdraw</option>
                      <option value="TRANSFER_IN">Transfer received</option>
                      <option value="TRANSFER_OUT">Transfer sent</option>
                    </select>
                  </label>
                  <label className="field-group" htmlFor="history-from-date">
                    <span className="field-label">From date</span>
                    <input
                      id="history-from-date"
                      className="input"
                      type="date"
                      value={draftFilters.fromDate ?? ''}
                      onChange={(event) => setDraftFilters((filters) => ({ ...filters, fromDate: event.target.value || undefined }))}
                      disabled={historyLoading}
                    />
                  </label>
                  <label className="field-group" htmlFor="history-to-date">
                    <span className="field-label">To date</span>
                    <input
                      id="history-to-date"
                      className="input"
                      type="date"
                      value={draftFilters.toDate ?? ''}
                      onChange={(event) => setDraftFilters((filters) => ({ ...filters, toDate: event.target.value || undefined }))}
                      disabled={historyLoading}
                    />
                  </label>
                  <label className="field-group" htmlFor="history-min-amount">
                    <span className="field-label">Minimum amount</span>
                    <input
                      id="history-min-amount"
                      className="input"
                      type="number"
                      min="0"
                      step="1"
                      value={draftFilters.minAmount ?? ''}
                      onChange={(event) => setDraftFilters((filters) => ({ ...filters, minAmount: event.target.value || undefined }))}
                      placeholder="0"
                      disabled={historyLoading}
                    />
                  </label>
                  <label className="field-group" htmlFor="history-max-amount">
                    <span className="field-label">Maximum amount</span>
                    <input
                      id="history-max-amount"
                      className="input"
                      type="number"
                      min="0"
                      step="1"
                      value={draftFilters.maxAmount ?? ''}
                      onChange={(event) => setDraftFilters((filters) => ({ ...filters, maxAmount: event.target.value || undefined }))}
                      placeholder="500,000"
                      disabled={historyLoading}
                    />
                  </label>
                </div>
                {filterError && <div className="banner error" role="alert">{filterError}</div>}
                <div className="history-filter-actions">
                  <button type="submit" className="primary-button" disabled={historyLoading}>Apply filters</button>
                  <button type="button" className="secondary-button" onClick={resetFilters} disabled={historyLoading}>Reset filters</button>
                </div>
              </form>
              {historyLoading && (
                <div className="history-state" role="status" aria-live="polite">
                  <span className="loading-dot" aria-hidden="true" />
                  Loading transaction history...
                </div>
              )}
              {!historyLoading && historyError && (
                <div className="history-state history-error" role="alert">
                  <span>{historyError}</span>
                  <button type="button" className="secondary-button" onClick={() => setHistoryRefreshKey((key) => key + 1)}>
                    Try again
                  </button>
                </div>
              )}
              {!historyLoading && !historyError && history.length === 0 && (
                <div className="history-state" role="status">
                  {Object.values(activeFilters).some(Boolean)
                    ? 'No transactions match these filters.'
                    : 'No transactions yet.'}
                </div>
              )}
              {!historyLoading && !historyError && history.length > 0 && (
                <>
                  <div className="transaction-list" role="list" aria-label="Transaction history">
                    {history.map((transaction) => {
                      const incoming = transaction.transactionType === 'DEPOSIT'
                        || transaction.transactionType === 'TRANSFER_IN';
                      return (
                        <article className="transaction-row" role="listitem" key={transaction.id}>
                          <div className="transaction-main">
                            <span className={`transaction-direction ${incoming ? 'incoming' : 'outgoing'}`}>
                              {incoming ? '+' : '-'}
                            </span>
                            <div>
                              <strong>{formatTransactionType(transaction.transactionType)}</strong>
                              <small>{formatTransactionDate(transaction.createdAt)}</small>
                            </div>
                          </div>
                          <div className="transaction-values">
                            <strong className={incoming ? 'incoming-text' : 'outgoing-text'}>
                              {incoming ? '+' : '-'}{formatBalance(transaction.amount)}
                            </strong>
                            <small>Balance after: {formatBalance(transaction.balanceAfterTransaction)}</small>
                          </div>
                          <span className="transaction-related">
                            {transaction.relatedAccountId === null
                              ? 'This wallet'
                              : `Related account #${transaction.relatedAccountId}`}
                          </span>
                          <button
                            type="button"
                            className="secondary-button transaction-detail-button"
                            onClick={() => setSelectedTransactionId(transaction.id)}
                          >
                            View details
                          </button>
                        </article>
                      );
                    })}
                  </div>
                  <div className="history-pagination" aria-label="Transaction history pagination">
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={historyFirst || historyPage === 0 || historyLoading}
                      onClick={() => setHistoryPage((page) => Math.max(0, page - 1))}
                    >
                      Previous
                    </button>
                    <span>Page {historyPage + 1} of {Math.max(historyTotalPages, 1)}</span>
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={historyLast || historyLoading || historyPage + 1 >= historyTotalPages}
                      onClick={() => setHistoryPage((page) => page + 1)}
                    >
                      Next
                    </button>
                  </div>
                </>
              )}
              {selectedTransactionId !== null && (
                <section className="transaction-detail" aria-labelledby="transaction-detail-title">
                  <div className="history-heading">
                    <div>
                      <span className="deposit-kicker">Transaction detail</span>
                      <h3 id="transaction-detail-title">Transaction #{selectedTransactionId}</h3>
                    </div>
                    <button type="button" className="secondary-button" onClick={() => setSelectedTransactionId(null)}>
                      Close
                    </button>
                  </div>
                  {transactionDetailLoading && (
                    <div className="history-state" role="status" aria-live="polite">
                      <span className="loading-dot" aria-hidden="true" />
                      Loading transaction details...
                    </div>
                  )}
                  {!transactionDetailLoading && transactionDetailError && (
                    <div className="history-state history-error" role="alert">{transactionDetailError}</div>
                  )}
                  {!transactionDetailLoading && !transactionDetailError && selectedTransaction && (
                    <dl className="transaction-detail-grid">
                      <div><dt>Transaction ID</dt><dd>{selectedTransaction.id}</dd></div>
                      <div><dt>Type</dt><dd>{formatTransactionType(selectedTransaction.transactionType)}</dd></div>
                      <div><dt>Amount</dt><dd>{formatBalance(selectedTransaction.amount)}</dd></div>
                      <div><dt>Date</dt><dd>{formatTransactionDate(selectedTransaction.createdAt)}</dd></div>
                      <div><dt>Balance after</dt><dd>{formatBalance(selectedTransaction.balanceAfterTransaction)}</dd></div>
                      <div>
                        <dt>Related account</dt>
                        <dd>{selectedTransaction.relatedAccountId === null
                          ? 'This wallet'
                          : `Related account #${selectedTransaction.relatedAccountId}`}</dd>
                      </div>
                    </dl>
                  )}
                </section>
              )}
            </section>
          </div>
        )}
      </section>
    </main>
  );
}

import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import { ApiError } from '../api/http';
import { getTopUp, syncTopUp } from '../api/topUpApi';
import { clearPendingTopUp, loadPendingTopUp } from '../api/pendingTopUp';
import { useAuth } from '../auth/AuthContext';
import type { TopUpResponse } from '../types/wallet';
import './payment-result.css';

type ResultState = 'CHECKING' | 'SUCCESS' | 'PENDING' | 'CANCELLED' | 'ERROR';

const moneyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0,
});

function formatMoney(value: number | string): string {
  const amount = Number(value);
  return Number.isFinite(amount) ? moneyFormatter.format(amount) : 'your payment amount';
}

function wait(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const timeout = window.setTimeout(resolve, ms);
    signal.addEventListener('abort', () => {
      window.clearTimeout(timeout);
      reject(new DOMException('Request cancelled', 'AbortError'));
    }, { once: true });
  });
}

function stateFor(status: TopUpResponse['status']): ResultState {
  if (status === 'SUCCESS') return 'SUCCESS';
  if (status === 'CANCELLED') return 'CANCELLED';
  return 'PENDING';
}

export default function PaymentResultPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [state, setState] = useState<ResultState>('CHECKING');
  const [payment, setPayment] = useState<TopUpResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  const handleUnauthorized = useCallback((requestError: unknown): boolean => {
    if (requestError instanceof ApiError && requestError.status === 401) {
      logout();
      navigate('/login', { replace: true, state: { from: '/payment-result' } });
      return true;
    }
    return false;
  }, [logout, navigate]);

  useEffect(() => {
    const context = loadPendingTopUp();
    const controller = new AbortController();
    let active = true;

    async function checkPayment() {
      if (!context) {
        setState('ERROR');
        setError("We couldn't identify this payment in the current session.");
        return;
      }
      setState('CHECKING');
      setError(null);
      try {
        let latest = await getTopUp(context.topUpId, controller.signal);
        if (latest.status === 'PENDING') {
          latest = await syncTopUp(context.topUpId, controller.signal);
        }
        for (let attempt = 1; active && latest.status === 'PENDING' && attempt < 3; attempt += 1) {
          await wait(2000, controller.signal);
          latest = await syncTopUp(context.topUpId, controller.signal);
        }
        if (!active) return;
        setPayment(latest);
        setState(stateFor(latest.status));
        if (latest.status === 'SUCCESS' || latest.status === 'CANCELLED') {
          clearPendingTopUp();
        }
      } catch (requestError) {
        if (!active || requestError instanceof DOMException && requestError.name === 'AbortError') return;
        if (handleUnauthorized(requestError)) return;
        setState('ERROR');
        setError("We couldn't confirm this payment yet.");
      }
    }

    void checkPayment();
    return () => { active = false; controller.abort(); };
  }, [handleUnauthorized, retryKey]);

  const title = state === 'CHECKING' ? 'Checking your payment...'
    : state === 'SUCCESS' ? 'Money added'
    : state === 'CANCELLED' ? 'Payment cancelled'
    : state === 'PENDING' ? 'Still confirming payment'
    : 'We could not confirm this payment';

  const description = state === 'SUCCESS' && payment
    ? `${formatMoney(payment.amount)} was added to your Lumo wallet.`
    : state === 'CANCELLED' ? 'No money was added to your Lumo wallet.'
    : state === 'PENDING' ? "We're checking with the payment provider. Your wallet will update once the payment is confirmed."
    : state === 'ERROR' ? (error ?? "We couldn't confirm this payment yet.")
    : 'We are checking the latest status with Lumo.';

  return (
    <main className="payment-result-shell">
      <header className="payment-result-header">
        <Link to="/dashboard" className="payment-result-brand" aria-label="Lumo home">
          <img src="/brand/lumo-icon.svg" alt="" />
          <strong>Lumo</strong>
        </Link>
      </header>
      <section className={`payment-result-card payment-result-${state.toLowerCase()}`} aria-live="polite">
        <div className="payment-result-icon" aria-hidden="true">
          {state === 'SUCCESS' ? '✓' : state === 'CANCELLED' ? '×' : state === 'ERROR' ? '!' : '•'}
        </div>
        <p className="payment-result-kicker">Top-up payment</p>
        <h1>{title}</h1>
        <p className="payment-result-description">{description}</p>
        <div className="payment-result-actions">
          {state === 'PENDING' && <button type="button" className="payment-result-button" onClick={() => setRetryKey((key) => key + 1)}>Check again</button>}
          {state === 'CANCELLED' && <Link className="payment-result-button" to="/wallet">Try again</Link>}
          {state === 'ERROR' && <button type="button" className="payment-result-button" onClick={() => setRetryKey((key) => key + 1)}>Try again</button>}
          <Link className="payment-result-link" to="/wallet">Back to wallet</Link>
        </div>
      </section>
    </main>
  );
}

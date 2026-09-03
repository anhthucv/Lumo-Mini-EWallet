export interface IdempotencyAttempt {
  key: string;
  payload: string;
}

export function createIdempotencyKey(): string {
  if (typeof globalThis.crypto?.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }

  if (typeof globalThis.crypto?.getRandomValues === 'function') {
    const bytes = new Uint8Array(16);
    globalThis.crypto.getRandomValues(bytes);
    return Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');
  }

  throw new Error('Secure random UUID generation is unavailable.');
}

export function getOrCreateIdempotencyAttempt(
  current: IdempotencyAttempt | null,
  payload: string,
): IdempotencyAttempt {
  if (current?.payload === payload) {
    return current;
  }

  return { key: createIdempotencyKey(), payload };
}

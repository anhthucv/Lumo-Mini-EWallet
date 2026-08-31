import type { ApiErrorResponse } from '../types/auth';
import { getAuthHeaders } from '../auth/session';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details?: ApiErrorResponse;

  constructor(details: ApiErrorResponse) {
    super(details.message);
    this.name = 'ApiError';
    this.status = details.status;
    this.code = details.error;
    this.details = details;
  }
}

function getApiBaseUrl(): string {
  const baseUrl = import.meta.env.VITE_API_BASE_URL as string | undefined;
  return baseUrl?.trim() ?? '';
}

function buildUrl(path: string): string {
  const baseUrl = getApiBaseUrl();
  if (!baseUrl) {
    return path;
  }
  return `${baseUrl.replace(/\/$/, '')}${path}`;
}

function parseJson<T>(text: string): T | string {
  try {
    return JSON.parse(text) as T;
  } catch {
    return text;
  }
}

export async function requestJson<TResponse>(
  path: string,
  init: RequestInit,
): Promise<TResponse> {
  const response = await fetch(buildUrl(path), {
    ...init,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...getAuthHeaders(),
      ...(init.headers ?? {}),
    },
  });

  const rawBody = await response.text();
  const parsedBody = rawBody ? parseJson<Record<string, unknown>>(rawBody) : null;

  if (!response.ok) {
    const errorResponse: ApiErrorResponse = {
      status: response.status,
      error:
        parsedBody && typeof parsedBody === 'object' && typeof parsedBody.error === 'string'
          ? parsedBody.error
          : `HTTP_${response.status}`,
      message:
        parsedBody && typeof parsedBody === 'object' && typeof parsedBody.message === 'string'
          ? parsedBody.message
          : response.statusText || 'Request failed',
      path:
        parsedBody && typeof parsedBody === 'object' && typeof parsedBody.path === 'string'
          ? parsedBody.path
          : undefined,
      timestamp:
        parsedBody && typeof parsedBody === 'object' && typeof parsedBody.timestamp === 'string'
          ? parsedBody.timestamp
          : undefined,
    };
    throw new ApiError(errorResponse);
  }

  if (!rawBody) {
    return undefined as TResponse;
  }

  return parseJson<TResponse>(rawBody) as TResponse;
}

export { getAuthHeaders };

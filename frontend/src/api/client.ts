import type { ApiErrorBody, FinancialPositionResponse, IncomeStream, Obligation } from './types';

export class HouseholdNotFoundError extends Error {
  readonly householdId: string;

  constructor(householdId: string) {
    super(`Household not found: ${householdId}`);
    this.name = 'HouseholdNotFoundError';
    this.householdId = householdId;
  }
}

export class FinancialPositionRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FinancialPositionRequestError';
  }
}

export class IncomeObligationsRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'IncomeObligationsRequestError';
  }
}

/**
 * Fetches the read-only financial position for a household.
 * Throws `HouseholdNotFoundError` for a 404, `FinancialPositionRequestError`
 * for any other non-OK response or network failure. Accepts an
 * `AbortSignal` so a superseded in-flight request can be cancelled by the
 * caller (see src/hooks/useFinancialPosition.ts).
 */
export async function fetchFinancialPosition(
  householdId: string,
  signal?: AbortSignal
): Promise<FinancialPositionResponse> {
  let response: Response;
  try {
    response = await fetch(`/api/households/${householdId}/financial-position`, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new FinancialPositionRequestError('Could not reach the server.');
  }

  if (response.status === 404) {
    throw new HouseholdNotFoundError(householdId);
  }
  if (!response.ok) {
    let body: Partial<ApiErrorBody> | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    throw new FinancialPositionRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as FinancialPositionResponse;
}

/**
 * Fetches a household-scoped collection (income streams or obligations).
 * Throws `HouseholdNotFoundError` for a 404, `IncomeObligationsRequestError`
 * for any other non-OK response or network failure. Shared by
 * `fetchIncomeStreams` and `fetchObligations` below.
 */
async function fetchHouseholdCollection<T>(path: string, householdId: string, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new IncomeObligationsRequestError('Could not reach the server.');
  }

  if (response.status === 404) {
    throw new HouseholdNotFoundError(householdId);
  }
  if (!response.ok) {
    let body: Partial<ApiErrorBody> | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    throw new IncomeObligationsRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as T;
}

/** Lists a household's recorded income streams in creation order. */
export function fetchIncomeStreams(householdId: string, signal?: AbortSignal): Promise<IncomeStream[]> {
  return fetchHouseholdCollection(`/api/households/${householdId}/income-streams`, householdId, signal);
}

/** Lists a household's recorded recurring obligations in creation order. */
export function fetchObligations(householdId: string, signal?: AbortSignal): Promise<Obligation[]> {
  return fetchHouseholdCollection(`/api/households/${householdId}/obligations`, householdId, signal);
}

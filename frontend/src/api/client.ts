import type { ApiErrorBody, FinancialSnapshot, FinancialSnapshotComparison, FinancialPositionResponse } from './types';

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

export class FinancialSnapshotsRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FinancialSnapshotsRequestError';
  }
}

export class FinancialSnapshotNotFoundError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FinancialSnapshotNotFoundError';
  }
}

/** Thrown for the API's `IdenticalSnapshotComparisonException` (comparing a snapshot against itself). */
export class IdenticalSnapshotComparisonError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'IdenticalSnapshotComparisonError';
  }
}

/**
 * Fetches the read-only list of a household's recorded financial snapshots.
 * Throws `HouseholdNotFoundError` for a 404, `FinancialSnapshotsRequestError`
 * for any other non-OK response or network failure.
 */
export async function fetchFinancialSnapshots(householdId: string, signal?: AbortSignal): Promise<FinancialSnapshot[]> {
  let response: Response;
  try {
    response = await fetch(`/api/households/${householdId}/financial-snapshots`, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new FinancialSnapshotsRequestError('Could not reach the server.');
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
    throw new FinancialSnapshotsRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as FinancialSnapshot[];
}

/**
 * Compares two of a household's financial snapshots. Throws
 * `HouseholdNotFoundError` (household 404), `FinancialSnapshotNotFoundError`
 * (either snapshot id 404s), `IdenticalSnapshotComparisonError` (both ids
 * identical, 400), or `FinancialSnapshotsRequestError` for anything else.
 * The 400 branch below only ever reaches the identical-snapshot case in
 * practice: the UI always supplies both ids from the loaded snapshot list,
 * so the backend's other `VALIDATION_FAILED` cause (a missing query
 * parameter) cannot occur here.
 */
export async function fetchSnapshotComparison(
  householdId: string,
  earlierSnapshotId: string,
  laterSnapshotId: string,
  signal?: AbortSignal
): Promise<FinancialSnapshotComparison> {
  const params = new URLSearchParams({ earlierSnapshotId, laterSnapshotId });
  let response: Response;
  try {
    response = await fetch(`/api/households/${householdId}/financial-snapshots/comparison?${params}`, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new FinancialSnapshotsRequestError('Could not reach the server.');
  }

  if (!response.ok) {
    let body: Partial<ApiErrorBody> | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    if (response.status === 404 && body?.error === 'FINANCIAL_SNAPSHOT_NOT_FOUND') {
      throw new FinancialSnapshotNotFoundError(body.message ?? 'Financial snapshot not found.');
    }
    if (response.status === 404) {
      throw new HouseholdNotFoundError(householdId);
    }
    if (response.status === 400 && body?.error === 'VALIDATION_FAILED') {
      throw new IdenticalSnapshotComparisonError(body.message ?? 'Cannot compare a snapshot against itself.');
    }
    throw new FinancialSnapshotsRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as FinancialSnapshotComparison;
}

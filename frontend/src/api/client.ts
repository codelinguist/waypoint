import type {
  ApiErrorBody,
  CashFlowProjectionRequest,
  CashFlowProjectionResponse,
  EmergencyFundRunwayRequest,
  EmergencyFundRunwayResponse,
  FinancialGoal,
  FinancialPositionResponse,
  FinancialSnapshot,
  FinancialSnapshotComparison,
  GoalContributionRequestBody,
  GoalContributionResult,
} from './types';

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

export class GoalsRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'GoalsRequestError';
  }
}

/**
 * Fetches a household's financial goals, ordered by priority (matches the
 * backend's `findByHousehold_IdOrderByPriorityAscCreatedAtAscIdAsc`). Throws
 * `HouseholdNotFoundError` for a 404, `GoalsRequestError` for any other
 * non-OK response or network failure.
 */
export async function fetchGoals(householdId: string, signal?: AbortSignal): Promise<FinancialGoal[]> {
  let response: Response;
  try {
    response = await fetch(`/api/households/${householdId}/goals`, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new GoalsRequestError('Could not reach the server.');
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
    throw new GoalsRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as FinancialGoal[];
}

export class GoalContributionRequestError extends Error {
  /** "field: message" strings from a 400 VALIDATION_FAILED response, if any. */
  readonly details: string[];

  constructor(message: string, details: string[] = []) {
    super(message);
    this.name = 'GoalContributionRequestError';
    this.details = details;
  }
}

/**
 * Raised for a validation error the API rejected the request with (HTTP
 * 400), distinct from `CalculatorRequestError`'s network/server failures so
 * a caller can label the two differently (a mistyped input vs. "try again
 * later").
 */
export class CalculatorValidationError extends Error {
  readonly details: unknown[];

  constructor(message: string, details: unknown[]) {
    super(message);
    this.name = 'CalculatorValidationError';
    this.details = details;
  }
}

/**
 * Calls the stateless goal-contribution calculator. Throws
 * `GoalContributionRequestError` (carrying any validation `details`) for a
 * non-OK response or network failure. Never 404s — the endpoint accepts no
 * household or goal identifier.
 */
export async function calculateGoalContribution(
  request: GoalContributionRequestBody,
  signal?: AbortSignal
): Promise<GoalContributionResult> {
  let response: Response;
  try {
    response = await fetch('/api/planning/goal-contribution-calculator', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
      signal,
    });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new GoalContributionRequestError('Could not reach the server.');
  }

  if (!response.ok) {
    let body: Partial<ApiErrorBody> | undefined;
    try {
      body = (await response.json()) as ApiErrorBody;
    } catch {
      body = undefined;
    }
    const details = Array.isArray(body?.details) ? body.details.map(String) : [];
    throw new GoalContributionRequestError(body?.message ?? `Request failed with status ${response.status}.`, details);
  }

  return (await response.json()) as GoalContributionResult;
}

export class CalculatorRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CalculatorRequestError';
  }
}

/**
 * Posts a stateless planning-calculator request. Neither calculator reads or
 * writes any persisted household state, so this takes only the request body
 * — no householdId. Throws `CalculatorValidationError` for a 400 and
 * `CalculatorRequestError` for any other non-OK response or network failure.
 */
async function postCalculator<TResponse>(path: string, body: unknown): Promise<TResponse> {
  let response: Response;
  try {
    response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch {
    throw new CalculatorRequestError('Could not reach the server.');
  }

  if (!response.ok) {
    let errorBody: Partial<ApiErrorBody> | undefined;
    try {
      errorBody = (await response.json()) as ApiErrorBody;
    } catch {
      errorBody = undefined;
    }
    const message = errorBody?.message ?? `Request failed with status ${response.status}.`;
    if (response.status === 400) {
      throw new CalculatorValidationError(message, errorBody?.details ?? []);
    }
    throw new CalculatorRequestError(message);
  }

  return (await response.json()) as TResponse;
}

export function fetchCashFlowProjection(request: CashFlowProjectionRequest): Promise<CashFlowProjectionResponse> {
  return postCalculator<CashFlowProjectionResponse>('/api/planning/cash-flow-projection', request);
}

export function fetchEmergencyFundRunway(request: EmergencyFundRunwayRequest): Promise<EmergencyFundRunwayResponse> {
  return postCalculator<EmergencyFundRunwayResponse>('/api/planning/emergency-fund-runway', request);
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


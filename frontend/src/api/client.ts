import type {
  ApiErrorBody,
  CashFlowProjectionRequest,
  CashFlowProjectionResponse,
  EmergencyFundRunwayRequest,
  EmergencyFundRunwayResponse,
  FinancialGoal,
  FinancialPositionResponse,
  FinancialSnapshotListItem,
  GoalContributionRequestBody,
  GoalContributionResult,
  IncomeStream,
  Obligation,
  PlanVersusActualRequest,
  PlanVersusActualResponse,
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

export class FinancialSnapshotsRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FinancialSnapshotsRequestError';
  }
}

/**
 * Lists a household's recorded financial snapshots (for the plan-vs-actual
 * snapshot picker). Throws `HouseholdNotFoundError` for a 404,
 * `FinancialSnapshotsRequestError` for any other non-OK response or network
 * failure.
 */
export async function fetchFinancialSnapshots(householdId: string): Promise<FinancialSnapshotListItem[]> {
  let response: Response;
  try {
    response = await fetch(`/api/households/${householdId}/financial-snapshots`);
  } catch {
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

  return (await response.json()) as FinancialSnapshotListItem[];
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
 * Posts a JSON request and parses a JSON response, throwing
 * `CalculatorValidationError` for a 400 and `CalculatorRequestError` for any
 * other non-OK response or network failure. Named for its original stateless
 * planning-calculator callers below; also reused by
 * `fetchPlanVersusActual`, whose path is household/snapshot-scoped but whose
 * error shape is identical.
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

/**
 * Compares caller-supplied planned totals against one existing snapshot's
 * actual totals. Unlike the calculators above this reads persisted household
 * data (the snapshot), but nothing here is written back — see
 * PlanVersusActualService (backend) for the read-only contract.
 */
export function fetchPlanVersusActual(
  householdId: string,
  snapshotId: string,
  request: PlanVersusActualRequest
): Promise<PlanVersusActualResponse> {
  return postCalculator<PlanVersusActualResponse>(
    `/api/households/${householdId}/financial-snapshots/${snapshotId}/plan-comparison`,
    request
  );
}

export class IncomeObligationsRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'IncomeObligationsRequestError';
  }
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

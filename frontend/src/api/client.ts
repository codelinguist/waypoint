import type {
  ApiErrorBody,
  FinancialGoal,
  FinancialPositionResponse,
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

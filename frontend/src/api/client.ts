import type {
  ApiErrorBody,
  CashFlowProjectionRequest,
  CashFlowProjectionResponse,
  DebtPrepaymentComparisonRequest,
  DebtPrepaymentComparisonResponse,
  EmergencyFundRunwayRequest,
  EmergencyFundRunwayResponse,
  FinancialPositionResponse,
  IncomeInterruptionScenarioRequest,
  IncomeInterruptionScenarioResponse,
  PurchaseReserveImpactRequest,
  PurchaseReserveImpactResponse,
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

export function fetchPurchaseReserveImpact(
  request: PurchaseReserveImpactRequest
): Promise<PurchaseReserveImpactResponse> {
  return postCalculator<PurchaseReserveImpactResponse>('/api/scenarios/purchase-reserve-impact', request);
}

export function fetchIncomeInterruptionScenario(
  request: IncomeInterruptionScenarioRequest
): Promise<IncomeInterruptionScenarioResponse> {
  return postCalculator<IncomeInterruptionScenarioResponse>('/api/scenarios/income-interruption', request);
}

export function fetchDebtPrepaymentComparison(
  request: DebtPrepaymentComparisonRequest
): Promise<DebtPrepaymentComparisonResponse> {
  return postCalculator<DebtPrepaymentComparisonResponse>('/api/scenarios/debt-prepayment', request);
}

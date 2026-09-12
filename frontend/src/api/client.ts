import type {
  ApiErrorBody,
  Asset,
  AssetValuationState,
  CashFlowProjectionRequest,
  CashFlowProjectionResponse,
  CreateAssetRequest,
  CreateGoalRequest,
  CreateIncomeStreamRequest,
  CreateLiabilityRequest,
  CreateObligationRequest,
  CreatePlanningAssumptionRequest,
  CreateSnapshotRequest,
  DebtPrepaymentComparisonRequest,
  DebtPrepaymentComparisonResponse,
  EmergencyFundRunwayRequest,
  EmergencyFundRunwayResponse,
  FinancialGoal,
  FinancialPositionResponse,
  FinancialSnapshotDetail,
  FinancialSnapshotComparison,
  FinancialSnapshotListItem,
  GoalContributionRequestBody,
  GoalContributionResult,
  IncomeInterruptionScenarioRequest,
  IncomeInterruptionScenarioResponse,
  IncomeStream,
  Liability,
  Obligation,
  PlanningAssumption,
  PlanVersusActualRequest,
  PlanVersusActualResponse,
  PurchaseReserveImpactRequest,
  PurchaseReserveImpactResponse,
  RecordLiabilityBalanceRequest,
  UpdateAssetValuationRequest,
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

export class FinancialSnapshotRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'FinancialSnapshotRequestError';
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
 * Fetches the read-only, full-detail list of a household's recorded
 * financial snapshots (asset/liability line items and source type
 * included), for the snapshots list view. Distinct from
 * `fetchFinancialSnapshots` above, which types the same endpoint's response
 * narrowly for the plan-vs-actual picker. Throws `HouseholdNotFoundError`
 * for a 404, `FinancialSnapshotRequestError` for any other non-OK response
 * or network failure.
 */
export async function fetchFinancialSnapshotDetails(householdId: string, signal?: AbortSignal): Promise<FinancialSnapshotDetail[]> {
  let response: Response;
  try {
    response = await fetch(`/api/households/${householdId}/financial-snapshots`, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new FinancialSnapshotRequestError('Could not reach the server.');
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
    throw new FinancialSnapshotRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as FinancialSnapshotDetail[];
}

/**
 * Compares two of a household's financial snapshots. Throws
 * `HouseholdNotFoundError` (household 404), `FinancialSnapshotNotFoundError`
 * (either snapshot id 404s), `IdenticalSnapshotComparisonError` (both ids
 * identical, 400), or `FinancialSnapshotRequestError` for anything else.
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
    throw new FinancialSnapshotRequestError('Could not reach the server.');
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
    throw new FinancialSnapshotRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as FinancialSnapshotComparison;
}

// Data-entry mutations (WAP-25): creating and correcting household records.
// Distinct error types from the read-only fetchers above so a form can tell
// a validation failure (400, fixable by editing the form) apart from a
// correction's stale-revision conflict (409, fixable only by reloading the
// current value) and any other failure.

/** A 400 VALIDATION_FAILED response; `details` holds the backend's own "field: message" strings, unmodified. */
export class EntryValidationError extends Error {
  readonly details: unknown[];

  constructor(message: string, details: unknown[]) {
    super(message);
    this.name = 'EntryValidationError';
    this.details = details;
  }
}

/** A 409 revision conflict from a correction endpoint (D021/D022): the record changed since its revision was read. */
export class EntryConflictError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'EntryConflictError';
  }
}

export class EntryRequestError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'EntryRequestError';
  }
}

/**
 * POSTs a create/correction request and parses the JSON response, throwing
 * `EntryValidationError` (400), `EntryConflictError` (409, correction
 * endpoints only), or `EntryRequestError` for anything else (network
 * failure, 404, 500). Shared by every create/correct call below.
 */
async function postHouseholdRecord<TResponse>(path: string, body: unknown): Promise<TResponse> {
  let response: Response;
  try {
    response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch {
    throw new EntryRequestError('Could not reach the server.');
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
      throw new EntryValidationError(message, errorBody?.details ?? []);
    }
    if (response.status === 409) {
      throw new EntryConflictError(message);
    }
    throw new EntryRequestError(message);
  }

  return (await response.json()) as TResponse;
}

/**
 * GETs a single household-scoped record, throwing `HouseholdNotFoundError`
 * for a 404 and `EntryRequestError` for anything else. Used to load an
 * asset's or liability's current revision immediately before a correction
 * form opens.
 */
async function fetchHouseholdRecord<T>(path: string, householdId: string, signal?: AbortSignal): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, { signal });
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') {
      throw cause;
    }
    throw new EntryRequestError('Could not reach the server.');
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
    throw new EntryRequestError(body?.message ?? `Request failed with status ${response.status}.`);
  }

  return (await response.json()) as T;
}

export function createAsset(householdId: string, request: CreateAssetRequest): Promise<Asset> {
  return postHouseholdRecord(`/api/households/${householdId}/assets`, request);
}

/** The asset's current valuation and revision, fetched fresh each time a correction form opens. */
export function fetchAssetValuationState(householdId: string, assetId: string): Promise<AssetValuationState> {
  return fetchHouseholdRecord(`/api/households/${householdId}/assets/${assetId}/valuations`, householdId);
}

export function correctAssetValuation(
  householdId: string,
  assetId: string,
  request: UpdateAssetValuationRequest
): Promise<AssetValuationState> {
  return postHouseholdRecord(`/api/households/${householdId}/assets/${assetId}/valuations`, request);
}

export function createLiability(householdId: string, request: CreateLiabilityRequest): Promise<Liability> {
  return postHouseholdRecord(`/api/households/${householdId}/liabilities`, request);
}

/** The liability's current record (including revision), fetched fresh each time a correction form opens. */
export function fetchLiability(householdId: string, liabilityId: string): Promise<Liability> {
  return fetchHouseholdRecord(`/api/households/${householdId}/liabilities/${liabilityId}`, householdId);
}

export function correctLiabilityBalance(
  householdId: string,
  liabilityId: string,
  request: RecordLiabilityBalanceRequest
): Promise<unknown> {
  return postHouseholdRecord(`/api/households/${householdId}/liabilities/${liabilityId}/balances`, request);
}

export function createIncomeStream(householdId: string, request: CreateIncomeStreamRequest): Promise<IncomeStream> {
  return postHouseholdRecord(`/api/households/${householdId}/income-streams`, request);
}

export function createObligation(householdId: string, request: CreateObligationRequest): Promise<Obligation> {
  return postHouseholdRecord(`/api/households/${householdId}/obligations`, request);
}

export function createGoal(householdId: string, request: CreateGoalRequest): Promise<FinancialGoal> {
  return postHouseholdRecord(`/api/households/${householdId}/goals`, request);
}

export function createSnapshot(householdId: string, request: CreateSnapshotRequest): Promise<FinancialSnapshotDetail> {
  return postHouseholdRecord(`/api/households/${householdId}/financial-snapshots`, request);
}

/** Lists a household's planning assumptions (current and superseded), newest first. */
export function fetchPlanningAssumptions(householdId: string, signal?: AbortSignal): Promise<PlanningAssumption[]> {
  return fetchHouseholdRecord(`/api/households/${householdId}/assumptions`, householdId, signal);
}

export function createPlanningAssumption(
  householdId: string,
  request: CreatePlanningAssumptionRequest
): Promise<PlanningAssumption> {
  return postHouseholdRecord(`/api/households/${householdId}/assumptions`, request);
}

/** Creates a replacement assumption linked as the successor to `assumptionId` (backend rejects an already-superseded source). */
export function supersedePlanningAssumption(
  householdId: string,
  assumptionId: string,
  request: CreatePlanningAssumptionRequest
): Promise<PlanningAssumption> {
  return postHouseholdRecord(`/api/households/${householdId}/assumptions/${assumptionId}/supersede`, request);
}


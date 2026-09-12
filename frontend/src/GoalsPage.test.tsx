import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import type { FinancialGoal, FinancialPositionResponse, GoalContributionResult } from './api/types';

const HOUSEHOLD_ID = '3f7b1e2a-9c4d-4a1b-8f2e-6d5c4b3a2f10';

function setHouseholdConfig(householdId: string | undefined) {
  window.__WAYPOINT_CONFIG__ = householdId ? { householdId } : undefined;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

const emptyPositionResponse: FinancialPositionResponse = {
  householdId: HOUSEHOLD_ID,
  householdName: 'Ralph Household',
  baseCurrency: 'PHP',
  retrievedAt: '2026-09-10T06:15:22.104Z',
  assets: [],
  liabilities: [],
  totalsByCurrency: [],
};

const goalsFixture: FinancialGoal[] = [
  {
    id: '1b111111-1111-1111-1111-111111111111',
    householdId: HOUSEHOLD_ID,
    name: 'Emergency Fund',
    targetAmount: 300000,
    currency: 'PHP',
    targetDate: '2027-06-01',
    priority: 1,
    currentAmount: 120000,
    remainingAmount: 180000,
    progressPercentage: 40,
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  },
];

const calculatorResult: GoalContributionResult = {
  currency: 'PHP',
  targetAmount: 300000,
  currentAmount: 120000,
  contributionMonths: 12,
  remainingAmount: 180000,
  monthlyContribution: 15000,
  totalContributions: 180000,
  projectedAmount: 300000,
  amountAboveTarget: 0,
  status: 'CONTRIBUTIONS_REQUIRED',
};

function routedFetch({
  position = () => jsonResponse(emptyPositionResponse),
  goals = () => jsonResponse([]),
  calculator,
}: {
  position?: () => Response;
  goals?: () => Response;
  calculator?: (body: unknown) => Response;
}) {
  return vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if (url.includes('/financial-position')) return Promise.resolve(position());
    if (url.includes('/goal-contribution-calculator')) {
      const body = init?.body ? JSON.parse(init.body as string) : undefined;
      return Promise.resolve(calculator ? calculator(body) : jsonResponse({}));
    }
    if (url.includes('/goals')) return Promise.resolve(goals());
    throw new Error(`Unexpected fetch url: ${url}`);
  });
}

beforeEach(() => {
  setHouseholdConfig(HOUSEHOLD_ID);
});

afterEach(() => {
  vi.unstubAllGlobals();
  setHouseholdConfig(undefined);
});

async function navigateToGoals() {
  render(<App />);
  await screen.findByRole('heading', { name: 'Ralph Household' });
  await userEvent.click(screen.getByRole('button', { name: 'Goals' }));
  await screen.findByRole('heading', { name: 'Goals' });
}

describe('navigation', () => {
  it('switches between the financial position and goals sections without a page reload', async () => {
    vi.stubGlobal('fetch', routedFetch({ goals: () => jsonResponse(goalsFixture) }));

    render(<App />);
    await screen.findByRole('heading', { name: 'Ralph Household' });
    expect(screen.getByRole('button', { name: 'Financial position' })).toHaveAttribute('aria-current', 'page');

    await userEvent.click(screen.getByRole('button', { name: 'Goals' }));

    expect(await screen.findByRole('heading', { name: 'Goals' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Goals' })).toHaveAttribute('aria-current', 'page');
    expect(screen.queryByRole('heading', { name: 'Ralph Household' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Financial position' }));
    expect(await screen.findByRole('heading', { name: 'Ralph Household' })).toBeInTheDocument();
  });
});

describe('goals list', () => {
  it('renders every documented field for each goal', async () => {
    vi.stubGlobal('fetch', routedFetch({ goals: () => jsonResponse(goalsFixture) }));
    await navigateToGoals();

    const table = await screen.findByRole('region', { name: 'Goals, scrollable' });
    const row = within(table).getByText('Emergency Fund').closest('tr')!;
    expect(within(row).getByText('1')).toBeInTheDocument();
    expect(within(row).getByText('Jun 1, 2027')).toBeInTheDocument();
    expect(within(row).getByText(/300,000\.00 PHP/)).toBeInTheDocument();
    expect(within(row).getByText(/120,000\.00 PHP/)).toBeInTheDocument();
    expect(within(row).getByText(/180,000\.00 PHP/)).toBeInTheDocument();
    expect(within(row).getByText('40.00%')).toBeInTheDocument();
  });

  it('shows an empty state distinct from an error when there are no goals', async () => {
    vi.stubGlobal('fetch', routedFetch({ goals: () => jsonResponse([]) }));
    await navigateToGoals();

    expect(await screen.findByText(/no goals are recorded/i)).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows the not-found state distinctly, naming the configured household id', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        goals: () => jsonResponse({ error: 'HOUSEHOLD_NOT_FOUND', message: 'not found', details: [] }, 404),
      })
    );
    await navigateToGoals();

    expect(await screen.findByText(/no household was found for the configured id/i)).toBeInTheDocument();
    expect(screen.getByText(HOUSEHOLD_ID)).toBeInTheDocument();
  });
});

describe('contribution calculator', () => {
  it('prefills from a selected goal and renders the calculated result', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        goals: () => jsonResponse(goalsFixture),
        calculator: (body) => {
          expect(body).toEqual({
            currency: 'PHP',
            targetAmount: '300000',
            currentAmount: '120000',
            contributionMonths: 12,
          });
          return jsonResponse(calculatorResult);
        },
      })
    );
    await navigateToGoals();

    await userEvent.selectOptions(screen.getByLabelText('Prefill from goal'), 'Emergency Fund');
    await userEvent.type(screen.getByLabelText('Months to contribute'), '12');
    await userEvent.click(screen.getByRole('button', { name: 'Calculate' }));

    expect(await screen.findByText('Contributions required')).toBeInTheDocument();
    expect(screen.getByText(/15,000\.00 PHP/)).toBeInTheDocument();
  });

  it('surfaces a validation error, including its field-level details, instead of a fabricated result', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        goals: () => jsonResponse([]),
        calculator: () =>
          jsonResponse(
            {
              error: 'VALIDATION_FAILED',
              message: 'Request validation failed',
              details: ['targetAmount: targetAmount must be greater than zero'],
            },
            400
          ),
      })
    );
    await navigateToGoals();

    expect(screen.queryByLabelText('Prefill from goal')).not.toBeInTheDocument();

    // Scoped to the calculator section: the page's own "Add goal" form
    // (WAP-25) has fields with the same labels (Currency, Target amount,
    // Current amount), so an unscoped screen.getByLabelText would be
    // ambiguous.
    const calculator = screen.getByRole('heading', { name: 'Contribution calculator' }).closest('section')!;

    await userEvent.type(within(calculator).getByLabelText('Currency'), 'PHP');
    await userEvent.type(within(calculator).getByLabelText('Target amount'), '0');
    await userEvent.type(within(calculator).getByLabelText('Current amount'), '0');
    await userEvent.type(within(calculator).getByLabelText('Months to contribute'), '3');
    await userEvent.click(within(calculator).getByRole('button', { name: 'Calculate' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/request validation failed/i);
    expect(screen.getByText(/targetAmount must be greater than zero/i)).toBeInTheDocument();
    expect(screen.queryByText('Contributions required')).not.toBeInTheDocument();
  });
});

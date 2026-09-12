import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import type {
  DebtPrepaymentComparisonResponse,
  IncomeInterruptionScenarioResponse,
  PurchaseReserveImpactResponse,
} from '../api/types';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const runwayFixture = {
  currency: 'USD',
  availableReserve: 5000,
  monthlyExpenses: 2000,
  monthlyNetIncome: 1000,
  monthlyShortfall: 1000,
  status: 'FINITE' as const,
  runwayMonths: 5,
  fullMonthsCovered: 5,
  modelNote: 'Constant-input runway estimate.',
};

const purchaseReserveImpactResponse: PurchaseReserveImpactResponse = {
  currency: 'USD',
  availableReserve: 5000,
  purchaseAmount: 1000,
  monthlyExpenses: 2000,
  monthlyNetIncome: 1000,
  minimumReserve: 3000,
  reserveAfterPurchase: 4000,
  purchaseFundingGap: 0,
  purchaseFitsAvailableCash: true,
  baselineReserveFloorGap: 0,
  reserveFloorGapAfterPurchase: 0,
  reserveMeetsFloorAfterPurchase: true,
  beforePurchaseRunway: runwayFixture,
  afterPurchaseRunwayAvailability: 'AVAILABLE',
  afterPurchaseRunway: { ...runwayFixture, availableReserve: 4000, runwayMonths: 4, fullMonthsCovered: 4 },
  modelNote:
    'Neutral, read-only scenario facts computed only from the supplied inputs; this result does not approve, ' +
    'deny, or recommend a purchase or reserve floor, and it does not persist or represent any household decision.',
};

const incomeInterruptionResponse: IncomeInterruptionScenarioResponse = {
  currency: 'USD',
  openingReserve: 10000,
  normalMonthlyNetIncome: 3000,
  interruptedMonthlyNetIncome: 0,
  monthlyExpenses: 2500,
  horizonMonths: 3,
  interruptionStartMonth: 1,
  interruptionMonths: 1,
  baselineRows: [
    { month: 1, openingCash: 10000, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 10500 },
    { month: 2, openingCash: 10500, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 11000 },
    { month: 3, openingCash: 11000, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 11500 },
  ],
  scenarioRows: [
    { month: 1, openingCash: 10000, income: 0, expenses: 2500, netCashFlow: -2500, closingCash: 7500 },
    { month: 2, openingCash: 7500, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 8000 },
    { month: 3, openingCash: 8000, income: 3000, expenses: 2500, netCashFlow: 500, closingCash: 8500 },
  ],
  closingDeltas: [-3000, -3000, -3000],
  endingCash: 8500,
  minimumCash: 7500,
  firstNegativeMonth: null,
  additionalOpeningReserveNeeded: 0,
};

const debtPrepaymentResponse: DebtPrepaymentComparisonResponse = {
  principal: 10000,
  monthlyInterestRate: 0.01,
  monthlyPayment: 500,
  currency: 'USD',
  immediatePrepayment: 2000,
  baseline: {
    startingBalance: 10000,
    status: 'PAID_OFF',
    payoffMonths: 22,
    totalPaid: 10800,
    totalInterest: 800,
    remainingBalance: 0,
  },
  scenario: {
    startingBalance: 8000,
    status: 'PAID_OFF',
    payoffMonths: 17,
    totalPaid: 8500,
    totalInterest: 500,
    remainingBalance: 0,
  },
  scenarioTotalCashPaid: 10500,
  lifetimeInterestSaved: 300,
  payoffMonthsSaved: 5,
  lifetimeCashSaved: 300,
  comparisonUnavailableReason: null,
};

async function goToScenarios() {
  render(<App />);
  await userEvent.click(screen.getByRole('button', { name: 'Scenarios' }));
  await screen.findByRole('heading', { name: 'What-if scenarios' });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('navigation', () => {
  it('reaches the scenarios view without any household configured, and never calls the financial-position API', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    await goToScenarios();

    expect(screen.getByRole('heading', { name: 'Purchase impact on reserves' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Income interruption' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Debt prepayment comparison' })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('purchase reserve impact', () => {
  it('submits the form, calls the endpoint, and renders before/after runway coverage', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(purchaseReserveImpactResponse));
    vi.stubGlobal('fetch', fetchMock);
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Available reserve'), '5000.00');
    await userEvent.type(screen.getByLabelText('Purchase amount'), '1000.00');
    await userEvent.type(screen.getByLabelText('Monthly expenses', { selector: '#pri-monthly-expenses' }), '2000.00');
    await userEvent.type(screen.getByLabelText('Monthly net income'), '1000.00');
    await userEvent.type(screen.getByLabelText('Minimum reserve floor'), '3000.00');

    await userEvent.click(screen.getByRole('button', { name: 'Calculate reserve impact' }));

    expect(await screen.findByText(/4,000\.00 USD/)).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/scenarios/purchase-reserve-impact',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          currency: 'USD',
          availableReserve: '5000.00',
          purchaseAmount: '1000.00',
          monthlyExpenses: '2000.00',
          monthlyNetIncome: '1000.00',
          minimumReserve: '3000.00',
        }),
      })
    );

    expect(screen.getByText(purchaseReserveImpactResponse.modelNote)).toBeInTheDocument();
    expect(screen.getAllByText('Finite runway').length).toBe(2);
  });

  it('shows after-purchase runway as unavailable when cash would go negative', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          ...purchaseReserveImpactResponse,
          purchaseFitsAvailableCash: false,
          purchaseFundingGap: 500,
          afterPurchaseRunwayAvailability: 'INSUFFICIENT_CASH',
          afterPurchaseRunway: null,
        })
      )
    );
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Available reserve'), '500.00');
    await userEvent.type(screen.getByLabelText('Purchase amount'), '1000.00');
    await userEvent.type(screen.getByLabelText('Monthly expenses', { selector: '#pri-monthly-expenses' }), '2000.00');
    await userEvent.type(screen.getByLabelText('Monthly net income'), '1000.00');
    await userEvent.type(screen.getByLabelText('Minimum reserve floor'), '3000.00');
    await userEvent.click(screen.getByRole('button', { name: 'Calculate reserve impact' }));

    expect(await screen.findByText('Not available')).toBeInTheDocument();
    expect(screen.getByText(/cash would go negative/i)).toBeInTheDocument();
  });

  it('surfaces an API validation error instead of failing silently', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ error: 'VALIDATION_FAILED', message: 'purchaseAmount must not be negative', details: [] }, 400)
      )
    );
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Available reserve'), '0');
    await userEvent.type(screen.getByLabelText('Purchase amount'), '-1');
    await userEvent.type(screen.getByLabelText('Monthly expenses', { selector: '#pri-monthly-expenses' }), '0');
    await userEvent.type(screen.getByLabelText('Monthly net income'), '0');
    await userEvent.type(screen.getByLabelText('Minimum reserve floor'), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Calculate reserve impact' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/purchaseAmount must not be negative/i);
  });
});

describe('income interruption', () => {
  it('submits the form, calls the endpoint, and renders the baseline-vs-scenario table', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(incomeInterruptionResponse));
    vi.stubGlobal('fetch', fetchMock);
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Opening reserve'), '10000.00');
    await userEvent.type(screen.getByLabelText('Normal monthly net income'), '3000.00');
    await userEvent.type(screen.getByLabelText('Interrupted monthly net income'), '0.00');
    await userEvent.type(screen.getByLabelText('Monthly expenses', { selector: '#iis-monthly-expenses' }), '2500.00');
    await userEvent.clear(screen.getByLabelText('Horizon (months)'));
    await userEvent.type(screen.getByLabelText('Horizon (months)'), '3');

    await userEvent.click(screen.getByRole('button', { name: 'Run interruption scenario' }));

    expect(await screen.findByText('Month 2')).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/scenarios/income-interruption',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          currency: 'USD',
          openingReserve: '10000.00',
          normalMonthlyNetIncome: '3000.00',
          interruptedMonthlyNetIncome: '0.00',
          monthlyExpenses: '2500.00',
          horizonMonths: 3,
          interruptionStartMonth: 1,
          interruptionMonths: 1,
        }),
      })
    );

    expect(screen.getByText('None')).toBeInTheDocument();
    expect(screen.getAllByText(/3,000\.00/).length).toBeGreaterThan(0);
  });

  it('surfaces an API validation error instead of failing silently', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          { error: 'VALIDATION_FAILED', message: 'interruptedMonthlyNetIncome must not exceed normalMonthlyNetIncome', details: [] },
          400
        )
      )
    );
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Opening reserve'), '0');
    await userEvent.type(screen.getByLabelText('Normal monthly net income'), '0');
    await userEvent.type(screen.getByLabelText('Interrupted monthly net income'), '100');
    await userEvent.type(screen.getByLabelText('Monthly expenses', { selector: '#iis-monthly-expenses' }), '0');
    await userEvent.click(screen.getByRole('button', { name: 'Run interruption scenario' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/interruptedMonthlyNetIncome must not exceed/i);
  });
});

describe('debt prepayment comparison', () => {
  it('submits the form, calls the endpoint, and renders lifetime savings', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(debtPrepaymentResponse));
    vi.stubGlobal('fetch', fetchMock);
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Principal'), '10000.00');
    await userEvent.type(screen.getByLabelText('Monthly interest rate'), '0.01');
    await userEvent.type(screen.getByLabelText('Monthly payment'), '500.00');
    await userEvent.type(screen.getByLabelText('Immediate prepayment'), '2000.00');

    await userEvent.click(screen.getByRole('button', { name: 'Compare prepayment' }));

    expect(await screen.findByText('5 months sooner', { exact: false })).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/scenarios/debt-prepayment',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          principal: '10000.00',
          monthlyInterestRate: '0.01',
          monthlyPayment: '500.00',
          currency: 'USD',
          immediatePrepayment: '2000.00',
        }),
      })
    );

    expect(screen.getAllByText('Paid off').length).toBe(2);
  });

  it('shows savings as unavailable with the server-provided reason when a path never pays off', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          ...debtPrepaymentResponse,
          scenario: { ...debtPrepaymentResponse.scenario, status: 'HORIZON_LIMIT', payoffMonths: null },
          lifetimeInterestSaved: null,
          payoffMonthsSaved: null,
          lifetimeCashSaved: null,
          comparisonUnavailableReason:
            "The scenario path does not reach PAID_OFF (HORIZON_LIMIT), so its truncated or absent total cannot be compared against the baseline's lifetime total.",
        })
      )
    );
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Principal'), '10000.00');
    await userEvent.type(screen.getByLabelText('Monthly interest rate'), '0.01');
    await userEvent.type(screen.getByLabelText('Monthly payment'), '500.00');
    await userEvent.type(screen.getByLabelText('Immediate prepayment'), '2000.00');
    await userEvent.click(screen.getByRole('button', { name: 'Compare prepayment' }));

    expect(await screen.findByText('Not available')).toBeInTheDocument();
    expect(screen.getByText(/does not reach PAID_OFF/)).toBeInTheDocument();
  });

  it('surfaces an API validation error instead of failing silently', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ error: 'VALIDATION_FAILED', message: 'immediatePrepayment must not exceed principal', details: [] }, 400)
      )
    );
    await goToScenarios();

    await userEvent.type(screen.getByLabelText('Principal'), '1000.00');
    await userEvent.type(screen.getByLabelText('Monthly interest rate'), '0.01');
    await userEvent.type(screen.getByLabelText('Monthly payment'), '500.00');
    await userEvent.type(screen.getByLabelText('Immediate prepayment'), '2000.00');
    await userEvent.click(screen.getByRole('button', { name: 'Compare prepayment' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/immediatePrepayment must not exceed principal/i);
  });
});

describe('labeling as hypothetical', () => {
  it('labels the scenarios page and each scenario as exploratory, not a record of a decision', async () => {
    vi.stubGlobal('fetch', vi.fn());
    await goToScenarios();

    expect(screen.getByText(/none of their results represent an actual household decision/i)).toBeInTheDocument();
    expect(screen.getByText(/not a recommendation to make the purchase/i)).toBeInTheDocument();
    expect(screen.getByText(/not a forecast of an actual job loss/i)).toBeInTheDocument();
    expect(screen.getByText(/not a recommendation to make a prepayment/i)).toBeInTheDocument();
  });
});

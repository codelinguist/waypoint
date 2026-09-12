import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from '../App';
import type { CashFlowProjectionResponse, EmergencyFundRunwayResponse } from '../api/types';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

const projectionResponse: CashFlowProjectionResponse = {
  currency: 'USD',
  startMonth: '2027-01',
  startingCash: 1000,
  monthlyInflow: 300,
  monthlyOutflow: 500,
  months: 7,
  rows: [
    { month: '2027-01', openingCash: 1000, inflow: 300, outflow: 500, netCashFlow: -200, closingCash: 800 },
    { month: '2027-06', openingCash: 0, inflow: 300, outflow: 500, netCashFlow: -200, closingCash: -200 },
    // A row past firstNegativeMonth, where openingCash itself is negative
    // (opening carries forward the prior month's negative closing balance)
    // — regression coverage for the sign-stripped "Opening cash" bug found
    // in review (BLOCKING-1).
    { month: '2027-07', openingCash: -200, inflow: 300, outflow: 500, netCashFlow: -200, closingCash: -400 },
  ],
  endingCash: -400,
  lowestClosingBalance: -400,
  lowestClosingBalanceMonth: '2027-07',
  firstNegativeMonth: '2027-06',
  status: 'BECOMES_NEGATIVE',
};

const runwayResponse: EmergencyFundRunwayResponse = {
  currency: 'USD',
  availableReserve: 5000,
  monthlyExpenses: 2000,
  monthlyNetIncome: 1000,
  monthlyShortfall: 1000,
  status: 'FINITE',
  runwayMonths: 5,
  fullMonthsCovered: 5,
  modelNote:
    'Constant-input estimate computed only from the supplied reserve, expenses, and income; it excludes any ' +
    'change in income, spending, interest, inflation, or timing within a month.',
};

async function goToForecasting() {
  render(<App />);
  await userEvent.click(screen.getByRole('button', { name: 'Forecasting' }));
  await screen.findByRole('heading', { name: 'Forecasting' });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('navigation', () => {
  it('reaches the forecasting view without any household configured, and never calls the financial-position API', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    await goToForecasting();

    expect(screen.getByRole('heading', { name: 'Cash-flow projection' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Emergency-fund runway' })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });
});

describe('cash-flow projection', () => {
  it('submits the form, calls the projection endpoint, and renders rows plus the full summary', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(projectionResponse));
    vi.stubGlobal('fetch', fetchMock);
    await goToForecasting();

    await userEvent.clear(screen.getByLabelText('Currency', { selector: '#cfp-currency' }));
    await userEvent.type(screen.getByLabelText('Currency', { selector: '#cfp-currency' }), 'usd');
    await userEvent.type(screen.getByLabelText('Start month'), '2027-01');
    await userEvent.type(screen.getByLabelText('Starting cash'), '1000.00');
    await userEvent.type(screen.getByLabelText('Monthly inflow'), '300.00');
    await userEvent.type(screen.getByLabelText('Monthly outflow'), '500.00');
    await userEvent.clear(screen.getByLabelText('Months to project'));
    await userEvent.type(screen.getByLabelText('Months to project'), '7');

    await userEvent.click(screen.getByRole('button', { name: 'Run projection' }));

    expect(await screen.findByText(/becomes negative/i)).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/planning/cash-flow-projection',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          currency: 'USD',
          startMonth: '2027-01',
          startingCash: '1000.00',
          monthlyInflow: '300.00',
          monthlyOutflow: '500.00',
          months: 7,
        }),
      })
    );

    // Summary fields and a row's month.
    expect(screen.getByText('Jan 2027')).toBeInTheDocument();
    expect(screen.getAllByText(/400\.00 USD/).length).toBeGreaterThan(0);
    expect(screen.getAllByText('Jun 2027').length).toBeGreaterThan(0);
  });

  it('renders a negative opening cash with its sign preserved, on a row past the first negative month', async () => {
    // Regression coverage for BLOCKING-1 from review: openingCash carries
    // forward the prior month's closing balance and can itself go negative,
    // but was rendered through the sign-stripping magnitude formatter.
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(projectionResponse)));
    await goToForecasting();

    await userEvent.type(screen.getByLabelText('Start month'), '2027-01');
    await userEvent.type(screen.getByLabelText('Starting cash'), '1000.00');
    await userEvent.type(screen.getByLabelText('Monthly inflow'), '300.00');
    await userEvent.type(screen.getByLabelText('Monthly outflow'), '500.00');
    await userEvent.clear(screen.getByLabelText('Months to project'));
    await userEvent.type(screen.getByLabelText('Months to project'), '7');
    await userEvent.click(screen.getByRole('button', { name: 'Run projection' }));

    const julyRow = (await screen.findByText('Jul 2027')).closest('tr');
    expect(julyRow).not.toBeNull();
    const openingCashCell = julyRow!.querySelector('td[data-label="Opening cash"]');
    expect(openingCashCell).toHaveTextContent('200.00');
    expect(openingCashCell!.querySelector('.negative')).not.toBeNull();
    expect(openingCashCell!.querySelector('.positive')).toBeNull();
  });

  it('surfaces an API validation error instead of failing silently', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ error: 'VALIDATION_FAILED', message: 'currency must be a 3-letter currency code', details: [] }, 400)
      )
    );
    await goToForecasting();

    await userEvent.type(screen.getByLabelText('Start month'), '2027-01');
    await userEvent.type(screen.getByLabelText('Starting cash'), '0');
    await userEvent.type(screen.getByLabelText('Monthly inflow'), '0');
    await userEvent.type(screen.getByLabelText('Monthly outflow'), '0');

    await userEvent.click(screen.getByRole('button', { name: 'Run projection' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/currency must be a 3-letter currency code/i);
  });

  it('labels both calculators as an estimate, not a guaranteed outcome, per AGENTS.md facts-vs-assumptions', async () => {
    vi.stubGlobal('fetch', vi.fn());
    await goToForecasting();

    expect(screen.getAllByText(/not a guaranteed outcome/i)).toHaveLength(2);
  });
});

describe('emergency-fund runway', () => {
  it('submits the form, calls the runway endpoint, and renders the result including modelNote verbatim', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(runwayResponse));
    vi.stubGlobal('fetch', fetchMock);
    await goToForecasting();

    await userEvent.type(screen.getByLabelText('Available reserve'), '5000.00');
    await userEvent.type(screen.getByLabelText('Monthly expenses'), '2000.00');
    await userEvent.type(screen.getByLabelText('Monthly net income'), '1000.00');

    await userEvent.click(screen.getByRole('button', { name: 'Calculate runway' }));

    expect(await screen.findByText(/finite runway/i)).toBeInTheDocument();
    expect(screen.getByText('5.00 months')).toBeInTheDocument();
    expect(screen.getByText(runwayResponse.modelNote)).toBeInTheDocument();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/planning/emergency-fund-runway',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          availableReserve: '5000.00',
          monthlyExpenses: '2000.00',
          monthlyNetIncome: '1000.00',
          currency: 'USD',
        }),
      })
    );
  });

  it('renders a NO_SHORTFALL result distinctly, with a null runway shown as not applicable', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({
          ...runwayResponse,
          monthlyShortfall: 0,
          status: 'NO_SHORTFALL',
          runwayMonths: null,
          fullMonthsCovered: null,
          modelNote: runwayResponse.modelNote + ' NO_SHORTFALL describes only these supplied constant inputs.',
        })
      )
    );
    await goToForecasting();

    await userEvent.type(screen.getByLabelText('Available reserve'), '5000.00');
    await userEvent.type(screen.getByLabelText('Monthly expenses'), '1000.00');
    await userEvent.type(screen.getByLabelText('Monthly net income'), '2000.00');

    await userEvent.click(screen.getByRole('button', { name: 'Calculate runway' }));

    expect(await screen.findByText(/no shortfall/i)).toBeInTheDocument();
    expect(screen.getAllByText('Not applicable').length).toBe(2);
  });

  it('surfaces an API validation error instead of failing silently', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse({ error: 'VALIDATION_FAILED', message: 'monthlyExpenses must not be negative', details: [] }, 400)
      )
    );
    await goToForecasting();

    await userEvent.type(screen.getByLabelText('Available reserve'), '0');
    await userEvent.type(screen.getByLabelText('Monthly expenses'), '-1');
    await userEvent.type(screen.getByLabelText('Monthly net income'), '0');

    await userEvent.click(screen.getByRole('button', { name: 'Calculate runway' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/monthlyExpenses must not be negative/i);
  });
});

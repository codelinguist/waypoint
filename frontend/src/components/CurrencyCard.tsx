import { useId, useState } from 'react';
import { AssetTable } from './AssetTable';
import { LiabilityTable } from './LiabilityTable';
import { formatMoneyMagnitude, formatSignedMoney } from '../money';
import type { PositionAsset, PositionCurrencyTotals, PositionLiability } from '../api/types';

export function CurrencyCard({
  totals,
  assets,
  liabilities,
  dimmed,
  defaultExpanded,
}: {
  totals: PositionCurrencyTotals;
  assets: PositionAsset[];
  liabilities: PositionLiability[];
  dimmed: boolean;
  defaultExpanded?: boolean;
}) {
  const [expanded, setExpanded] = useState(defaultExpanded ?? false);
  const headingId = useId();
  const recordsId = useId();
  const recordCount = assets.length + liabilities.length;
  const netWorth = formatSignedMoney(totals.netWorth);

  return (
    <section className={`card${dimmed ? ' dimmed' : ''}`} aria-labelledby={headingId}>
      <div className="card-head">
        <div>
          <div className="currency-code" id={headingId}>
            {totals.currency}
          </div>
          <div className="net-worth-label">Net worth</div>
          <div className={`net-worth ${netWorth.negative ? 'negative' : 'positive'}`}>
            <span className="sr-only">{netWorth.negative ? 'negative' : 'positive'}</span>
            {netWorth.formatted} {totals.currency}
          </div>
        </div>
        <button
          className="toggle-btn"
          type="button"
          aria-expanded={expanded}
          aria-controls={recordsId}
          onClick={() => setExpanded((value) => !value)}
        >
          {expanded ? 'Hide' : 'Show'} {recordCount} record{recordCount === 1 ? '' : 's'}
        </button>
      </div>
      <div className="totals-row">
        <span>
          Assets total <span className="amount">{formatMoneyMagnitude(totals.assetTotal)}</span>
          {assets.length === 0 && <span className="badge">no assets recorded</span>}
        </span>
        <span>
          Liabilities total <span className="amount">{formatMoneyMagnitude(totals.liabilityTotal)}</span>
          {liabilities.length === 0 && <span className="badge">no liabilities recorded</span>}
        </span>
      </div>

      <div className="records" id={recordsId} hidden={!expanded}>
        <h4>Assets{assets.length > 0 ? ` (${assets.length})` : ''}</h4>
        {assets.length > 0 ? (
          <AssetTable currency={totals.currency} assets={assets} />
        ) : (
          <p className="no-records-side">No assets recorded in {totals.currency}.</p>
        )}

        <h4>Liabilities{liabilities.length > 0 ? ` (${liabilities.length})` : ''}</h4>
        {liabilities.length > 0 ? (
          <LiabilityTable currency={totals.currency} liabilities={liabilities} />
        ) : (
          <p className="no-records-side">No liabilities recorded in {totals.currency}.</p>
        )}
      </div>
    </section>
  );
}

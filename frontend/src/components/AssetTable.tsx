import { formatLocalDate } from '../dates';
import { assetTypeLabel, liquidityLabel, sourceTypeLabel } from '../labels';
import { formatMoneyMagnitude } from '../money';
import type { PositionAsset } from '../api/types';

export function AssetTable({ currency, assets }: { currency: string; assets: PositionAsset[] }) {
  return (
    <div className="table-scroll" tabIndex={0} role="region" aria-label={`${currency} assets, scrollable`}>
      <table className="stackable">
        <caption className="sr-only">{currency} assets</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Type</th>
            <th scope="col">Liquidity</th>
            <th scope="col" className="amount">
              Planning value
            </th>
            <th scope="col" className="amount">
              Estimated value
            </th>
            <th scope="col">Valued</th>
            <th scope="col">Source</th>
          </tr>
        </thead>
        <tbody>
          {assets.map((asset) => (
            <tr key={asset.id}>
              <td className="record-title">{asset.name}</td>
              <td data-label="Type">
                <span className="badge">{assetTypeLabel(asset.assetType)}</span>
              </td>
              <td data-label="Liquidity">
                <span className="badge">{liquidityLabel(asset.liquidity)}</span>
              </td>
              <td className="amount" data-label="Planning value">
                {formatMoneyMagnitude(asset.planningValue)}
              </td>
              <td className="amount" data-label="Estimated value">
                {formatMoneyMagnitude(asset.estimatedValue)}
              </td>
              <td data-label="Valued">
                <time dateTime={asset.valuedAt}>{formatLocalDate(asset.valuedAt)}</time>
              </td>
              <td data-label="Source">
                <span className="badge">{sourceTypeLabel(asset.sourceType)}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

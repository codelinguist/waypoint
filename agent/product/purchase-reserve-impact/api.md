# API: Purchase Impact on Cash Reserves

Stateless, single-currency scenario calculation of how one explicitly
supplied cash purchase would change reserve coverage against an explicitly
supplied reserve floor. Every input is a temporary, caller-supplied modeling
value; nothing is read from or written to household state, and no household
or entity identifier is accepted. Before/after emergency-fund coverage
reuses the merged `EmergencyFundRunwayCalculator` through a direct Java
call, read-only.

## `POST /api/scenarios/purchase-reserve-impact`

### Request body

| Field               | Type    | Required | Constraints |
|----------------------|---------|----------|-------------|
| `availableReserve`    | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits |
| `purchaseAmount`      | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits (zero purchase is valid) |
| `monthlyExpenses`     | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits, unchanged by the purchase |
| `monthlyNetIncome`    | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits, unchanged by the purchase |
| `minimumReserve`      | decimal | yes | `>= 0`, at most 17 integer digits and 2 fraction digits — an explicit caller-chosen floor, never a household policy default (zero floor is valid) |
| `currency`            | string  | yes | exactly 3 letters; normalized to uppercase, no conversion |

### Response body

| Field                            | Type            | Notes |
|-----------------------------------|-----------------|-------|
| `currency`                         | string          | normalized, uppercase |
| `availableReserve`                 | decimal         | echoes the request |
| `purchaseAmount`                   | decimal         | echoes the request |
| `monthlyExpenses`                  | decimal         | echoes the request |
| `monthlyNetIncome`                 | decimal         | echoes the request |
| `minimumReserve`                   | decimal         | echoes the request |
| `reserveAfterPurchase`             | decimal         | `availableReserve - purchaseAmount`; may be negative |
| `purchaseFundingGap`               | decimal         | `max(0, -reserveAfterPurchase)`: the cash shortfall, if any, to fund the purchase from the supplied reserve alone |
| `purchaseFitsAvailableCash`        | boolean         | neutral fact: `true` when `purchaseFundingGap` is zero — never an approval or denial of the purchase |
| `baselineReserveFloorGap`          | decimal         | `max(0, minimumReserve - availableReserve)`: any pre-existing floor gap, before the purchase |
| `reserveFloorGapAfterPurchase`     | decimal         | `max(0, minimumReserve - reserveAfterPurchase)`: the floor gap after the purchase |
| `reserveMeetsFloorAfterPurchase`   | boolean         | neutral fact: `true` when `reserveFloorGapAfterPurchase` is zero — never a recommendation of the floor |
| `beforePurchaseRunway`             | object          | the merged emergency-fund-runway response shape, computed from `availableReserve` with unchanged `monthlyExpenses`/`monthlyNetIncome` — see its own `status`/`runwayMonths`/`fullMonthsCovered`/`modelNote` fields |
| `afterPurchaseRunwayAvailability`  | `AVAILABLE` \| `INSUFFICIENT_CASH` | `INSUFFICIENT_CASH` when `purchaseAmount` exceeds `availableReserve`, since a negative reserve is never passed to the runway calculator |
| `afterPurchaseRunway`              | object or `null` | `null` when `afterPurchaseRunwayAvailability` is `INSUFFICIENT_CASH`; otherwise the runway response computed from `reserveAfterPurchase` |
| `modelNote`                        | string          | states these are neutral, read-only scenario facts that do not approve, deny, or recommend a purchase or reserve floor, and do not persist or represent a household decision |

Identical requests always return identical responses: there is no
clock-dependent field, no persistence, and no randomness.

### Example: the documented primary scenario

Reserve 1000, purchase 400, expenses 300, income 100, floor 800.

Request:

```json
{
  "availableReserve": "1000.00",
  "purchaseAmount": "400.00",
  "monthlyExpenses": "300.00",
  "monthlyNetIncome": "100.00",
  "minimumReserve": "800.00",
  "currency": "php"
}
```

Response (`200 OK`):

```json
{
  "currency": "PHP",
  "availableReserve": 1000.00,
  "purchaseAmount": 400.00,
  "monthlyExpenses": 300.00,
  "monthlyNetIncome": 100.00,
  "minimumReserve": 800.00,
  "reserveAfterPurchase": 600.00,
  "purchaseFundingGap": 0.00,
  "purchaseFitsAvailableCash": true,
  "baselineReserveFloorGap": 0.00,
  "reserveFloorGapAfterPurchase": 200.00,
  "reserveMeetsFloorAfterPurchase": false,
  "beforePurchaseRunway": {
    "currency": "PHP",
    "availableReserve": 1000.00,
    "monthlyExpenses": 300.00,
    "monthlyNetIncome": 100.00,
    "monthlyShortfall": 200.00,
    "status": "FINITE",
    "runwayMonths": 5.00,
    "fullMonthsCovered": 5,
    "modelNote": "Constant-input estimate computed only from the supplied reserve, expenses, and income; it excludes any change in income, spending, interest, inflation, or timing within a month."
  },
  "afterPurchaseRunwayAvailability": "AVAILABLE",
  "afterPurchaseRunway": {
    "currency": "PHP",
    "availableReserve": 600.00,
    "monthlyExpenses": 300.00,
    "monthlyNetIncome": 100.00,
    "monthlyShortfall": 200.00,
    "status": "FINITE",
    "runwayMonths": 3.00,
    "fullMonthsCovered": 3,
    "modelNote": "Constant-input estimate computed only from the supplied reserve, expenses, and income; it excludes any change in income, spending, interest, inflation, or timing within a month."
  },
  "modelNote": "Neutral, read-only scenario facts computed only from the supplied inputs; this result does not approve, deny, or recommend a purchase or reserve floor, and it does not persist or represent any household decision. Before/after coverage reuses the emergency-fund runway calculator's constant-input convention, which excludes any change in income, spending, interest, inflation, or timing within a month."
}
```

Captured verbatim from a real manual run (see the implementation log).

### Example: purchase exceeds the available reserve

Reserve 1000, purchase 1200: a negative cash balance, an exact funding gap,
and an explicitly unavailable after-purchase runway (relevant fields only):

```json
{
  "reserveAfterPurchase": -200.00,
  "purchaseFundingGap": 200.00,
  "purchaseFitsAvailableCash": false,
  "reserveFloorGapAfterPurchase": 1000.00,
  "afterPurchaseRunwayAvailability": "INSUFFICIENT_CASH",
  "afterPurchaseRunway": null
}
```

The `beforePurchaseRunway` is still returned — it is computed only from the
valid, unaffected `availableReserve`.

### Example: purchase exactly equal to the reserve

Reserve 1000, purchase 1000: a valid zero-reserve calculation, not an error
(relevant fields only):

```json
{
  "reserveAfterPurchase": 0.00,
  "purchaseFundingGap": 0.00,
  "purchaseFitsAvailableCash": true,
  "afterPurchaseRunwayAvailability": "AVAILABLE",
  "afterPurchaseRunway": {
    "availableReserve": 0.00,
    "status": "FINITE",
    "runwayMonths": 0.00,
    "fullMonthsCovered": 0
  }
}
```

### Example: zero purchase and income covering expenses

Zero purchase preserves the baseline reserve and floor gap exactly; income
covering expenses preserves `NO_SHORTFALL` and `null` runway semantics on
both the before- and after-purchase runway (relevant fields only):

```json
{
  "reserveAfterPurchase": 500.00,
  "purchaseFundingGap": 0.00,
  "beforePurchaseRunway": { "status": "NO_SHORTFALL", "runwayMonths": null, "fullMonthsCovered": null },
  "afterPurchaseRunwayAvailability": "AVAILABLE",
  "afterPurchaseRunway": { "status": "NO_SHORTFALL", "runwayMonths": null, "fullMonthsCovered": null }
}
```

### Validation errors (`400 Bad Request`)

Structured as the repository's existing error convention:

```json
{
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": ["purchaseAmount: purchaseAmount must not be negative"]
}
```

Rejected without silent rounding or truncation: missing/null required
fields, negative amounts, amounts with more than 2 fraction digits or more
than 17 integer digits, a currency that is not exactly 3 letters, and a
malformed JSON body (`MALFORMED_REQUEST`). Invalid input never reaches the
reused runway calculator and never produces a `500` response.

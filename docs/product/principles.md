# Product Principles

## 1. Structured state before conversational memory

Conversation is an interface.

Structured financial state is canonical.

## 2. Deterministic math before LLM reasoning

Cash flow, amortization, compound growth, runway, allocation, and scenario arithmetic must use deterministic code.

The LLM explains results; it does not replace calculation engines.

## 3. Explicit uncertainty

The system must identify whether a value is:

- observed
- manually entered
- imported
- estimated
- assumed
- inferred

Do not hide uncertainty.

## 4. User approval for material changes

The AI can propose:

> Update expected monthly income to PHP 480,000 starting January?

But it must not make the change silently.

## 5. Decisions need rationale

A decision should preserve:

- what was decided
- when
- by whom
- alternatives considered
- assumptions used
- rationale

## 6. Plans are versioned

A ten-year plan is not overwritten as though the old one never existed.

New plans should supersede older ones while retaining historical comparison.

## 7. Preserve enjoyment

The financial system is not designed to optimize every peso.

It should explicitly support:

- travel
- discretionary spending
- family enjoyment
- aspirational purchases

within boundaries that preserve long-term goals.

## 8. Household collaboration over policing

The system should help a couple agree on top-level financial boundaries without turning either partner into the other's spending monitor.

## 9. Conservative planning

Illiquid or uncertain assets should be treated conservatively.

Examples:

- startup equity may have a planning value of zero
- property should not count as emergency liquidity
- projected income should not count as received cash

## 10. Build for user zero first

Do not prematurely generalize the domain for every country, bank, tax regime, household structure, or investment product.

Solve the real Ralph-and-wife use case first.

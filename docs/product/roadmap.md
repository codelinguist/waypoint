# Initial Product Roadmap

This is an ordering guide, not a deadline schedule.

## Phase 0 — Repository and workflow

Deliverables:

- product context
- domain model
- architecture
- decision log
- agent workflow

Status: complete when this starter pack is committed.

## Phase 1 — Household foundation

Implement:

- Household
- Person
- basic API
- persistence
- migrations
- tests

## Phase 2 — Assets and liabilities

Implement:

- Asset
- Liability
- balances
- valuation dates
- liquidity classification

## Phase 3 — Income and recurring obligations

Implement:

- income streams
- recurring expenses/obligations
- gross/net/unknown classification
- start/end dates

## Phase 4 — Financial snapshots

Implement:

- point-in-time snapshot
- net worth
- cash flow summary
- historical comparison

## Phase 5 — Goals

Implement:

- financial goals
- target amounts
- target dates
- priority
- progress

## Phase 6 — Facts and assumptions

Implement:

- provenance
- planning assumptions
- effective dates
- review dates
- supersession

## Phase 7 — Deterministic planning engine

Implement:

- cash-flow projection
- emergency-fund runway
- debt amortization
- simple future-value calculations
- goal contribution calculations

## Phase 8 — Scenario engine

Implement:

- clone current planning state
- apply temporary changes
- calculate projected outcomes
- compare baseline vs scenario
- no mutation of canonical state

## Phase 9 — Basic React dashboard

Initial views:

- current financial position
- cash flow
- goals
- liabilities
- snapshots
- scenario comparison

## Phase 10 — First LLM integration

The first AI capability should be narrow.

Example:

> User describes a proposed financial change in natural language.

The LLM converts it into a structured scenario proposal.

The application validates it.

The deterministic engine calculates the scenario.

The LLM explains the result.

## Phase 11 — Conversational financial planning

Add tools for:

- retrieving financial state
- retrieving decisions
- running scenarios
- comparing plan versus actual
- proposing changes

## Phase 12 — Ongoing household operations

Potential later features:

- monthly review
- bank/statement imports
- recurring financial snapshots
- stale-assumption detection
- education planning
- retirement planning
- mortgage acceleration analysis
- business-capital planning
- travel planning
- major-purchase rules

# Financial Domain Model

This document is conceptual. It is not yet a database schema.

## Household

Represents the financial planning unit.

Potential fields:

- id
- name
- base_currency
- created_at
- updated_at

## Person

A household member.

Potential fields:

- id
- household_id
- name
- role
- date_of_birth
- dependent_status

Sensitive fields should be minimized.

## Fact

A confirmed financial value or state.

Examples:

- mortgage balance
- salary received
- cash account balance
- loan interest rate

Potential metadata:

- value
- unit/currency
- effective_at
- recorded_at
- source
- source_type
- confidence
- notes

## Assumption

A value used for planning that is not confirmed fact.

Examples:

- future monthly income
- expected investment return
- tuition inflation
- future business revenue

Potential fields:

- value
- effective_from
- effective_until
- confidence
- source
- review_date
- superseded_by

## Goal

A desired future state.

Examples:

- emergency fund = PHP 1.5M
- mortgage balance = 0 by 2033
- university fund target
- retirement target

Potential fields:

- target_metric
- target_value
- target_date
- priority
- status

## Recommendation

A proposed action produced by a user, adviser, or AI.

Examples:

- eliminate credit card before investing
- maintain low-rate loan payments
- delay luxury car

Recommendation is not the same as Decision.

## Decision

An approved course of action.

Potential fields:

- decision
- approved_at
- approved_by
- rationale
- linked_recommendation
- assumptions_used
- review_date

## Financial Event

A change that affects household state.

Examples:

- salary increase
- new job
- job loss
- loan payoff
- property purchase
- investment contribution
- business funding
- major expense

Events should make it possible to explain:

> What changed?

## Asset

Examples:

- cash
- bank account
- investment
- property
- business ownership

Attributes may include:

- liquidity classification
- planning value
- market value
- valuation date
- ownership

## Liability

Examples:

- mortgage
- credit card
- personal loan
- business loan

Attributes:

- principal
- rate
- rate type
- payment
- term
- start/end dates
- prepayment rules

## Income Stream

Examples:

- salary
- hourly contract income
- business distribution

Attributes:

- amount/rate
- frequency
- gross/net/unknown
- start/end dates
- certainty classification

## Expense / Obligation

Prefer meaningful recurring commitments rather than obsessive micro-categories.

Examples:

- household baseline
- mortgage
- insurance
- tuition
- travel sinking fund
- discretionary allowance

## Financial Rule

A household-approved operating rule.

Examples:

- pay credit-card statement in full
- allocate income increase to wealth before lifestyle
- luxury car requires 2x purchase price in qualifying financial assets

Rules may be:

- advisory
- enforced in planning
- used as scenario constraints

## Financial Snapshot

A point-in-time view of the household.

Should capture enough information to answer:

- What was our net worth then?
- What did we believe then?
- What goals were active?
- What was our mortgage balance?
- How are we doing versus that plan now?

Snapshots are important for long-term plan-versus-actual analysis.

## Scenario

A temporary alternate future.

Examples:

- job loss for six months
- buy PHP 3M car now
- invest extra PHP 50k/month
- prepay mortgage PHP 1M
- fund business with PHP 800k

A scenario must not mutate canonical state unless explicitly converted into an approved plan/decision.

## Plan

A versioned set of assumptions, goals, rules, and projected outcomes.

Plans should be comparable over time.

## Provenance

Material financial data should ideally preserve where it came from:

- manually entered
- imported document
- bank integration
- calculation
- AI extraction awaiting confirmation
- approved AI proposal

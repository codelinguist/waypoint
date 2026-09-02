# Problems to Solve

## Problem 1: Financial context is fragmented

A long financial discussion naturally produces corrections and refinements.

Examples:

- a recurring expense may already include a loan payment
- a temporary payment may initially look recurring
- projected income may be mistaken for guaranteed income
- a recommendation may later become an approved decision

A conversation transcript alone is not a reliable financial model.

### Product implication

The application must maintain canonical structured state.

---

## Problem 2: Facts and assumptions are easily confused

Examples:

- Mortgage balance: fact.
- Average future combined monthly income: assumption.
- Mortgage-free by 2033: goal.
- Build emergency fund before mortgage acceleration: recommendation.
- User approves that sequence: decision.

These concepts must be represented differently.

### Product implication

The domain model should explicitly distinguish:

- Fact
- Assumption
- Goal
- Recommendation
- Decision

---

## Problem 3: Users can afford something without being able to afford its consequences

Traditional affordability often asks:

> Can the monthly payment fit?

The user actually needs:

> What does this choice do to the rest of our financial plan?

### Product implication

Scenario analysis must measure opportunity cost against multiple goals.

---

## Problem 4: High income creates lifestyle creep

Rising income can disappear into:

- cars
- dining
- shopping
- subscriptions
- travel
- household upgrades

The household wants to enjoy higher income without losing the chance to build wealth.

### Product implication

Support explicit allocation rules, sinking funds, and lifestyle guardrails.

---

## Problem 5: Long-term plans become stale

A ten-year financial plan becomes wrong when:

- salaries change
- inflation changes
- a business grows or fails
- a child approaches university
- interest rates change
- a property is sold
- a layoff occurs
- a major purchase is made

### Product implication

The system needs versioned assumptions, historical snapshots, review dates, and plan-vs-actual analysis.

---

## Problem 6: Generic AI financial advice lacks household state

A generic assistant can explain concepts but may not reliably know:

- current balances
- existing commitments
- family goals
- decisions already approved
- previous scenario outcomes

### Product implication

The AI should retrieve structured household context through tools instead of relying on conversational memory.

---

## Problem 7: Household planning should not require obsessive tracking

The users do not want a financial system that requires recording every coffee or policing one another.

### Product implication

Prioritize meaningful categories, cash-flow allocations, milestones, and exception detection over extreme expense categorization.

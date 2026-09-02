# Initial Architecture

## Architectural objective

Build the simplest architecture that supports:

- canonical financial state
- deterministic financial logic
- long-term historical state
- scenario modeling
- future AI tool use

## Architecture direction

Use a Java modular monolith for the core application and REST API.

Introduce Python only when a concrete analytical capability materially benefits
from the Python ecosystem, such as statistical forecasting, optimization,
machine-learning model execution, or NLP pipelines. Python is not a second
general-purpose backend and is not required for the initial product.

This direction is accepted in D011 and D012.

## Suggested initial stack

### Frontend

- React
- TypeScript

### Backend

- Java
- Spring Boot
- Spring Web
- Bean Validation

### Persistence

- PostgreSQL
- Spring Data JPA
- Hibernate
- Flyway

### Testing

- JUnit 5
- Spring Boot Test
- Testcontainers where a real PostgreSQL boundary matters

### Local development

- Docker
- Docker Compose
- one container for the Spring Boot application
- one PostgreSQL container with persistent local storage

The primary local startup path should be `docker compose up --build`. Keep the
Compose topology aligned with the modular-monolith architecture: it is a
reproducible development environment, not a reason to split the application
into services. Developers may still run the application or tests directly on
the host when that provides a faster inner loop.

### Python analytical services

Add later, and only for a demonstrated capability need.

Likely tools may include:

- Python
- FastAPI for a synchronous internal API
- NumPy, pandas, SciPy, statsmodels, scikit-learn, or domain-specific libraries
  selected for the feature being built
- pytest

### AI layer

Add later.

The initial design should make it easy to expose deterministic financial services as tools to an LLM.

## Architecture style

Start as a modular monolith with one Java backend deployment and one database.

Conceptual modules may eventually include:

- household
- financial_state
- cash_flow
- liabilities
- goals
- planning
- scenarios
- decisions
- ai

Do not create separate deployable services without a concrete scaling or ownership need.

Language preference alone is not enough reason to create a service.

## Runtime and service boundaries

The Java application owns:

- the public REST API
- canonical household financial state
- validation and authorization policy
- transactional workflows
- facts, assumptions, goals, recommendations, decisions, plans, and snapshots
- deterministic calculations that are straightforward to implement and test in Java
- persistence of approved inputs and reproducible calculation results

A future Python service may own execution of a specialized analytical model. It
must not own or write canonical household state.

The initial call pattern should be synchronous HTTP using a versioned internal
contract. The Java application sends the minimum required, explicitly typed
inputs and receives a structured result. Do not add queues, shared database
access, or distributed workflow infrastructure until a concrete feature
requires them.

For every material analytical result, preserve enough metadata to reproduce and
audit it, including as applicable:

- input values and their provenance
- assumption and plan versions
- calculation or model name and version
- execution timestamp
- warnings, confidence information, and known limitations

The Java application validates returned results before exposing or persisting
them. A Python response is a calculation result, forecast, or recommendation;
it does not become a confirmed financial fact merely because a model produced it.

## When Python earns a service boundary

Use Python when all of the following are true:

1. A concrete product feature needs it now.
2. A mature Python library or model provides a meaningful implementation,
   correctness, or maintainability advantage.
3. The input/output contract can be explicit and versioned.
4. The feature tolerates an out-of-process call and its failure modes.
5. The additional deployment and operational cost is justified.

Examples that may qualify later:

- probabilistic cash-flow or income forecasting
- Monte Carlo retirement or education projections
- portfolio optimization
- document classification and extraction pipelines
- local or hosted NLP/model inference

Examples that should remain in the Java monolith initially:

- monthly cash-flow summaries
- emergency-fund runway
- loan and mortgage amortization
- mortgage prepayment comparisons
- deterministic compound-growth projections
- goal progress and required-contribution calculations
- plan-versus-actual variance

Mathematical importance requires deterministic, tested code; it does not by
itself require Python or a separate service.

## AI boundary

Future AI orchestration should call application tools such as:

- get_household_state()
- get_cash_flow()
- get_goals()
- get_assumptions()
- get_decisions()
- run_scenario()
- compare_scenarios()
- propose_change()
- record_approved_decision()

The LLM should not receive unrestricted database access.

AI orchestration may eventually run in either runtime. Its language should be
chosen when the first narrow AI feature is implemented. Regardless of runtime,
it must use application-level tools and must not bypass the Java application's
rules for canonical state and explicit approval.

## Deterministic calculation layer

Examples of calculations that belong in code:

- monthly cash flow
- emergency-fund runway
- debt amortization
- mortgage prepayment impact
- future value
- compound-growth projections
- target contribution calculation
- scenario comparison
- goal progress
- plan-versus-actual variance

## Future integrations

Potential later integrations:

- bank statement import
- CSV/XLSX ingestion
- investment account import
- OCR/document parsing
- exchange rates
- tax references
- notifications
- recurring monthly review

None are required for MVP.

## Security posture

Because this product contains household financial data:

- local/private deployment should remain an option
- secrets must not be committed
- minimize personal identifiers
- log carefully
- avoid placing raw sensitive financial data in unnecessary model prompts
- consider encryption at rest for sensitive fields later
- add authentication before exposing beyond a trusted local/private environment

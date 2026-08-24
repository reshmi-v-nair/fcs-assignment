# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes. Right now there are at least three different data-access styles living side by side:

- `Store` and `Product` are plain/active-record-style Panache entities accessed with static
  calls (`Store.findById(...)`, `store.persist()`), with business logic sitting directly in the
  JAX-RS resource class.
- `Warehouse` follows a hexagonal/ports-and-adapters style: a plain domain model, a
  `WarehouseStore` port, a `WarehouseRepository` adapter (Panache repository, not active record),
  and use-case classes that depend only on the port.
- The new bonus feature (`FulfillmentAssignment`) deliberately took a third, in-between approach:
  a plain Panache repository (not active record, but also no port/use-case abstraction), because
  its two strong relations (`Store`, `Product`) were already plain entities and a full hexagonal
  layer would have been over-engineering for its scope.

While implementing this assignment, the practical cost of that inconsistency showed up directly
in testing: `CreateWarehouseUseCase`, `ReplaceWarehouseUseCase` and `ArchiveWarehouseUseCase`
could get full positive/negative/error-condition unit test coverage with plain Mockito mocks in
milliseconds, no database needed, because they only depend on an interface (`WarehouseStore`,
`LocationResolver`). `StoreResource` and `ProductResource`, by contrast, can only be tested via
`@QuarkusTest` + RestAssured against a real Postgres instance, because `Store`/`Product` are
static active-record entities with no seam to mock - there's no interface for a business-rule
test to depend on. `FulfillmentAssignmentService` landed in the same boat: its rules are pure
counting logic, but because it goes through `Store.findById(...)` it also had to become a
`@QuarkusTest` instead of a fast unit test, even though the rules it enforces are conceptually the
same "count-and-compare" shape as the Warehouse rules.

If I were maintaining this codebase long-term, I would refactor `Store` and `Product` to the same
pattern already established for `Warehouse`: extract the business rules (uniqueness, "not found",
future stock/price validation) into use-case classes depending on a repository port, and keep the
Panache entity as a pure adapter-layer detail. That doesn't mean abolishing active-record Panache
entities everywhere - for simple, rule-free CRUD it's genuinely less code - but as soon as a
resource accumulates business rules worth unit-testing in isolation (as Store's legacy-system sync
already does, and as the Product package likely will once it grows), the ports/use-case pattern
pays for itself in testability. I would NOT introduce a new pattern for that refactor; I'd
converge everything on the Warehouse style since it's already proven out in this codebase and
demonstrably easier to test thoroughly (see TESTING.md).
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Concretely, while implementing the Warehouse endpoints I had to work within a contract that was
fixed before implementation started: the generated `com.warehouse.api.beans.Warehouse` DTO
exposes `id` as a `String`, while the domain model's natural identity is `businessUnitCode` and
its persistence surrogate key is a `Long`. That mismatch wasn't something I could just fix - the
DTO is regenerated from `warehouse-openapi.yaml` on every build - so I had to add mapping code
(`toWarehouseModel`/`toWarehouseResponse` in `WarehouseResourceImpl`) and a `String.valueOf`
conversion to bridge the two. For `Product`/`Store`, no such mapping exists: the JAX-RS resource
methods take and return the JPA/Panache entities directly.

Pros of the OpenAPI-generated approach (Warehouse):
- The contract is defined and reviewable before any implementation code exists - client teams can
  start integrating against `warehouse-openapi.yaml` immediately, in parallel with backend work.
- The generated interface (`WarehouseResource`) forces every implementation to satisfy the full
  contract - the compiler caught every unimplemented method as a build error, not a runtime
  surprise, which is exactly why the assignment could hand me clearly-scoped
  `UnsupportedOperationException` stubs to fill in.
- Request/response shapes can't silently drift from the spec, because the DTOs are regenerated
  from it every build - hand-written endpoints have no such guarantee (e.g. `ProductResource` and
  `StoreResource` happily accept/return the JPA entity directly, exposing whatever fields Hibernate
  happens to have, including ones like `Store.id`'s Panache internals).

Cons of the OpenAPI-generated approach:
- Extra mapping/boilerplate code (as above) whenever the generated DTO's shape doesn't line up
  1:1 with the domain model - which is common, since a REST contract and a persistence model
  usually should differ deliberately (e.g. hiding internal-only fields).
- A second source of truth to keep in sync: changing behavior can require editing the YAML,
  regenerating, and then updating the implementation, versus just editing one Java file for the
  hand-coded style.
- Slower local edit-compile loop (an extra code-generation step on every build) and a slightly
  steeper onboarding curve (a new contributor has to know to look in `src/main/resources/openapi`
  and understand the generator, not just read Java).

Pros of the hand-coded approach (Product/Store):
- Fastest to write for a small, stable, internal-facing CRUD surface, with no generation step.
- Full flexibility - want a `PATCH` (as `StoreResource` has) that isn't in any spec? Just add the
  method.

Cons:
- No enforced contract: nothing stops the JAX-RS method signature and the actual JSON on the wire
  from drifting from whatever documentation exists (if any), and nothing catches "did I implement
  every endpoint" at compile time the way the generated interface does.
- Directly exposing the entity class (as both packages do) couples the wire format to the
  persistence model - a schema change becomes an API change by accident.

My choice: for a public or cross-team API surface - like this system's Warehouse API, which the
briefing frames as the primary integration point for downstream consumers - I'd keep the
contract-first/generated approach precisely because of the parallelization and drift-prevention
benefits, and I'd invest in a thin, explicit DTO-to-domain mapping layer (as I did here) rather
than growing the domain model to match the DTO. For small, internal, single-team CRUD endpoints
like `Product` and `Store` in their current form, hand-coding is a reasonable pragmatic choice -
but I would stop directly returning JPA entities from those resources and introduce even a
minimal DTO, since that's what caused the coupling issues discussed in Question 1.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
The prioritization I actually followed on this assignment (see TESTING.md for the full breakdown):

1. Business-rule unit tests first, and made them cheap to write by depending on ports/interfaces
   rather than concrete infrastructure. `CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`, and
   `ArchiveWarehouseUseCase` each have a dedicated test class covering the happy path plus one
   test per negative/error rule (duplicate business unit code, invalid location, max-warehouses
   reached, capacity exceeded, stock mismatch, already-archived, etc.), all with Mockito mocks and
   no database. These are the highest-value tests per unit of effort: they run in milliseconds,
   they pin down every branch of the actual business logic the assignment asked for, and they
   don't need Docker/Postgres to execute - which matters practically, since in this exercise's
   sandbox environment Docker access itself was a blocked dependency at one point.

2. REST/integration tests second, to verify the wiring (JSON (de)serialization, HTTP status codes,
   exception-to-status mapping, transaction-commit-then-legacy-sync ordering) that unit tests
   can't reach. These necessarily need `@QuarkusTest` and a real Postgres, so they're slower and
   more fragile to infrastructure availability - I used them for exactly the things that only
   exist at that layer: `StoreResourceTest` verifying the legacy gateway fires only after commit
   (via `@InjectMock` + `TransactionSynchronizationRegistry`), and `WarehouseResourceTest`/
   `FulfillmentResourceTest` verifying the right HTTP status (400/404/409) comes back for each
   validation failure.

3. I deliberately did NOT write tests for pure data classes (the domain models, DTOs) or for
   framework wiring that Quarkus itself already guarantees (e.g. that `@Path` routes requests
   correctly) - that's low-value test surface that mostly tests the framework, not this code.

To keep this effective over time rather than just at this snapshot, I'd:
- Keep the JaCoCo `check` gate (80% instruction/line, wired into `mvn verify`) in CI so coverage
  can't silently regress as the codebase grows - a real gate that fails the build beats a
  dashboard nobody looks at.
- Push new business logic toward the ports/use-case pattern (Question 1) specifically because it
  keeps the cheap, fast unit-test tier viable - if all new features end up needing
  `@QuarkusTest` + Postgres to test any business rule, the whole suite gets slower and
  flakier as it grows, and people start skipping tests instead of writing them.
- Treat the exception hierarchy (`ApplicationException` + `ApplicationExceptionMapper`) as the
  contract test surface for error handling - each new domain exception should come with both a
  unit test (raised by the right rule) and a REST test (mapped to the right HTTP status), which
  keeps error-handling coverage growing in lockstep with new rules instead of as an afterthought.
```
# Testing

## Strategy

This project uses two tiers of tests, chosen deliberately for different parts of the codebase:

1. **Mockito-based unit tests** (`domain.usecases` package under `src/test/java`) - these test the
   Warehouse business rules (`CreateWarehouseUseCase`, `ReplaceWarehouseUseCase`,
   `ArchiveWarehouseUseCase`) in complete isolation, mocking the `WarehouseStore` and
   `LocationResolver` ports. They need no database and no Quarkus context, so they run in
   milliseconds and are where most positive/negative/error-condition branches are covered. This is
   possible because these classes are plain constructor-injected POJOs behind explicit ports - see
   `QUESTIONS.md` for the trade-off discussion versus the Panache active-record classes below.

2. **`@QuarkusTest` REST/integration tests** (`ProductEndpointTest`, `StoreResourceTest`,
   `WarehouseResourceTest`, `FulfillmentResourceTest`, `FulfillmentAssignmentServiceTest`) - these
   boot the full Quarkus context and a real Postgres instance (via Quarkus Dev Services) and drive
   the actual HTTP endpoints with RestAssured. They're needed wherever a class can't be unit-tested
   in isolation - e.g. `Store` and `Product` are Panache **active-record** entities, so their
   static `findById`/`persist` calls require a live persistence context; there is no interface to
   mock. `StoreResourceTest` also uses `@InjectMock` on `LegacyStoreManagerGateway` to verify it's
   only invoked once the surrounding transaction has actually committed.

3. `WarehouseEndpointIT` is a `@QuarkusIntegrationTest` (black-box, runs against the packaged
   artifact) and is only executed via `mvn verify -Pnative`/failsafe, not plain `mvn test` - kept
   as-is from the original scaffold, now with its second (previously disabled) test case enabled.

## Running the tests

```sh
./mvnw test
```

Requires Docker (for Quarkus Dev Services to provision Postgres automatically) or a manually
started Postgres matching `src/main/resources/application.properties`:

```sh
docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 15432:5432 postgres:13.3
```

The Mockito-based unit tests (`*UseCaseTest`, except integration-style tests) do not need Docker
or Postgres at all and will run in any environment.

## Coverage (JaCoCo)

```sh
./mvnw verify
```

- HTML report: `target/site/jacoco/index.html`
- The `verify` phase runs a `jacoco:check` gate requiring **80%** instruction and line coverage
  across the project bundle (excluding the build-time OpenAPI-generated `com.warehouse.api.*`
  classes, which are outside the assignment's control).
- If the gate fails, open the HTML report to see exactly which classes/branches are uncovered.

## Known limitations / non-goals

- `import.sql`'s seeded `MWH.001 @ ZWOLLE-001` has `capacity=100`, which exceeds `ZWOLLE-001`'s
  `maxCapacity=40` in `LocationGateway`. This row is inserted directly via SQL at boot, bypassing
  use-case validation entirely, so it does not break startup - but it does mean tests exercise the
  replace-path validations against freshly created warehouses rather than this particular seeded
  one.
- Tests do not wrap each method in a rolled-back transaction (no `@TestTransaction`), matching the
  existing `ProductEndpointTest` convention already in the codebase - new tests use distinct
  business unit codes/product names per test case to avoid interfering with each other or with
  seed data.
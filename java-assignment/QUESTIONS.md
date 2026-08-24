# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would consider refactoring the database access layer to make it consistent across the application.

At the moment, there are multiple approaches being used. 
Store and Product follow Active Record style which uses Panache - database operations are performed directly on the entity and most of the business logic resides in the resource class. 
Warehouse follows a clean implementation with a domain model, repository, and dedicated use-case classes. 
FulfillmentAssignment feature sits somewhere in between, using a repository pattern but without the port/use-case abstraction.

Maintainability and testing is difficult here. The Warehouse implementation is much easier to test because the business logic is isolated from the persistence layer. The use-case classes depend on interfaces, allowing unit tests to be written using simple mocks without requiring a database. In contrast, testing Store, Product, and FulfillmentAssignment often requires integration tests with a running database because their business logic is tightly coupled to the persistence implementation.

I would prefer to move Store and Product toward the same implementation approach as Warehouse. Business rules such as validations, uniqueness checks, and domain-specific code would be moved into a service or use-case class that depend on repository interface. This would improve separation of concerns, make the code easier to test, and provide a more consistent development approach. For simple CRUD functionality with minimal business logic, the Active Record approach can still be used, only where there is more business logic and it is supposed to evolve more later on, we can use the Warehouse implemntation approach.
```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Both approaches have benefits, and the choice depends on whether the API is small or internal or external.
OpenAPI-generated approach is useful because the API contract is defined first. This makes it clear what the API should look like and allows other teams to start integrating before the implementation is complete. It also helps keep the implementation and documentation in sync.
The downside is that it requires extra work. Whenever the API changes, the OpenAPI file must be updated and the code regenerated. Sometimes additional mapping code is also needed.
The hand-coded approach is simpler and faster to create. Developers can create and modify endpoints directly without maintaining a separate specification. For small and simple APIs, this can be a good option.
However, there is a higher risk that the API implementation and documentation become inconsistent over time. Also, directly exposing database entities through the API can make future changes more difficult.
My preference would be to use the OpenAPI contract-first approach for APIs that are shared across teams or used by external consumers, because it provides a clear and reliable contract. For small internal APIs, hand-coding is fine, but I would still use separate DTOs instead of exposing database entities directly. This keeps the API easier to maintain in the future.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
To balance testing quality with time and resource constraints, I would focus on the tests that provide the most value first, gives good coverage while keeping the test suite fast and maintainable.

First, I would prioritize unit tests for business logic. These tests are fast, easy to run, easy to maintain, and help verify that the core application rules work correctly without database or external services. Examples include validations, capacity checks, duplicate checks, and other business rules. 

Second, I would prioritize integration and API tests. These tests verify that the different integrations work together correctly, including API endpoints, request and response handling, database interactions, and error responses. While they take more time than unit tests, they ensure application behaves correctly from an end-user perspective.

I would not spend too much time testing simple data classes or framework. These doesnot provide much value because they contain minimal business logic and are already handled by the framework.

To keep test coverage effective over time, I would:
Maintain a minimum code coverage threshold in the CI/CD pipeline so coverage does not decrease as new features are added.
Continue separating business logic from infrastructure code, making it easier to write fast and reliable unit tests.
Ensure that every new business rule includes corresponding tests.
Add integration tests for important API behavior and error handling to make sure users receive the correct responses.
```
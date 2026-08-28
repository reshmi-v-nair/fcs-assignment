# FCS Assignment

This repository contains two independent exercises submitted as part of the FCS assignment.

## Highlights

- **All 4 code-assignment tasks implemented and tested**: Location resolution, Store↔legacy-system
  sync (via CDI transactional events, firing only after DB commit), Warehouse create/replace/archive
  with full validation, and the bonus Warehouse↔Store↔Product fulfillment feature with its
  cardinality rules.
- **94% test coverage** (80% required), verified via `./mvnw verify` — see
  [`java-assignment/README.md`](java-assignment/README.md#testing-and-code-coverage) for a coverage
  screenshot and [`java-assignment/TESTING.md`](java-assignment/TESTING.md) for the strategy.
- **Live screenshots and real captured API responses** in
  [`java-assignment/README.md`](java-assignment/README.md) — not just documentation, actual output
  from a running instance.
- **CI** via [`.github/workflows/ci.yml`](.github/workflows/ci.yml): build, test, JaCoCo gate, and
  coverage summary on every push/PR.
- Both the design trade-off questions ([`java-assignment/QUESTIONS.md`](java-assignment/QUESTIONS.md))
  and the business case study ([`case-study/CASE_STUDY.md`](case-study/CASE_STUDY.md)) are fully
  answered.

## Repository structure

```
.
├── java-assignment/   Quarkus REST API - the coding exercise (see below)
├── case-study/        Business case study exercise (see below)
├── .github/workflows/ CI pipeline
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
└── LICENSE
```

## 1. Java code assignment ([`java-assignment/`](java-assignment/))

A Quarkus-based "Warehouse colocation management system" with a set of implementation tasks to complete.

- Start with [`java-assignment/CODE_ASSIGNMENT.md`](java-assignment/CODE_ASSIGNMENT.md) for the tasks.
- Read [`case-study/BRIEFING.md`](case-study/BRIEFING.md) first to understand the domain (Location, Store, Warehouse, Product) before diving into the tasks.
- Written answers to the reflection questions are in [`java-assignment/QUESTIONS.md`](java-assignment/QUESTIONS.md).
- Build/run instructions, API reference, screenshots, sample requests, and testing strategy are in
  [`java-assignment/README.md`](java-assignment/README.md) and [`java-assignment/TESTING.md`](java-assignment/TESTING.md).

The code base is partly based on the [quarkus-quickstarts](https://github.com/quarkusio/quarkus-quickstarts) project.

## 2. Business case study ([`case-study/`](case-study/))

A set of open-ended business scenarios about cost allocation, optimization, financial-systems integration, budgeting/forecasting, and cost control, discussed against the same domain model as the code assignment.

- Read [`case-study/BRIEFING.md`](case-study/BRIEFING.md) first for the domain context.
- Scenarios and answers are in [`case-study/CASE_STUDY.md`](case-study/CASE_STUDY.md).

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for guidelines and [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) for community standards.

## License

[MIT](LICENSE)

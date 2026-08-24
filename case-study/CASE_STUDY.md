# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**
```txt
Accurately tracking costs in a system is challenging because different types of costs need to be allocated in different ways.

- Labor costs may be allocated based on hours worked or units handled.
- Inventory costs can be linked directly to products stored in a warehouse.
- Transportation costs often need to be shared across multiple products or stores in a shipment.
- Overhead costs such as rent and utilities need to be distributed using agreed business rules.

One main ask should be that the allocation method should be defined by the business and finance teams, not just the technical team.
Also the flow of costs to the accounting side will be more complex.

Before designing a solution, I would clarify:

What level of reporting is needed (warehouse, store, or product level)?
How often costs should be calculated (real-time or periodic)?
Whether there are existing accounting or cost-center structures to follow?
How historical cost data should be preserved for auditing and reporting?
Profit and Loss calculations in the system
Any alerts or notifications to respective roles on delays and processing

My approach would be to start with a simple, business-approved cost allocation model and design it in a generalised way so it can be expanded as requirements grow.
```

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**
```txt
To reduce fulfillment costs, the first step is understanding where the money is being spent. Once costs are visible, we can look for ways to operate more efficiently without affecting customer service.

Some possible cost-saving strategies are:

Use warehouse space more efficiently and avoid keeping underutilized warehouses open.
Reduce transportation costs by combining shipments and choosing better delivery routes.
Assign orders to the most suitable warehouse to avoid unnecessary movement of products.
Plan warehouse capacity in advance to avoid unexpected costs.
Negotiate better shipping rates based on higher shipment volumes.

To find improvement opportunities, I would review warehouse usage, shipping costs, inventory levels, and order fulfillment data.

I would prioritize changes based on:

How much money they can save.
How easy they are to implement.
Whether they affect customer service.

I would start with a small pilot, measure the results, and then roll out successful improvements to other locations. This helps reduce costs while maintaining service quality.
```

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**
```txt
Integrating the Cost Control Tool with the financial system is important to keep cost data accurate, updated, and consistent. It also reduces manual work and helps the finance team make faster decisions.

Key benefits:

Real-time cost visibility.
Less manual data entry and fewer errors.
Better financial reporting.
Faster business decisions.

For integration, I would use an event-based approach, where important changes such as warehouse creation, closure, or fulfillment changes are sent to the financial system.

I would also ensure:

Data is sent only after a successful transaction.
Temporary failures can be retried.
Duplicate messages do not create duplicate financial entries.
Regular reconciliation is done to identify missing data.
Both systems use consistent formats, currencies, and decimal rules.

This provides reliable financial data while keeping both systems loosely connected and easier to maintain.
```

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**
```txt
Budgeting and forecasting are important because they help the company plan future costs and capacity instead of reacting to problems later. For example, if a warehouse or location is likely to reach its capacity soon, the system should identify it early so the business can plan ahead.

When designing the system, I would consider:

Historical cost and demand data to make reliable forecasts.
Warehouse and location capacity to identify future capacity problems.
Seasonal demand because fulfillment requirements can change during peak periods.
Actual vs. budgeted costs so the business can quickly identify differences.
What-if scenarios, such as understanding the cost impact of replacing or adding a warehouse.
Forecast accuracy by ensuring the data is reliable and the assumptions, such as growth and transportation costs, are clearly defined.

I would start with simple monthly forecasts at the warehouse or location level and gradually move to more detailed forecasting as more historical data becomes available.
```

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**
```txt
This scenario maps directly onto the `ReplaceWarehouseUseCase` already implemented in this codebase. When replacing a warehouse, cost control is important because the company needs to make sure the new warehouse does not increase costs unnecessarily.
The old warehouse should be archived rather than deleted so that its cost history is preserved. Since the new warehouse uses the same Business Unit Code, this gives the business a continuous history of the warehouse before and after the replacement.

Preserving the history helps the company:

Compare the old and new warehouse costs.
Understand whether the replacement actually reduced costs.
Track costs for reporting and auditing.
Use the old warehouse cost as a baseline for future budgets.

Before approving the replacement, I would also compare the expected cost of the new warehouse with the current cost of the old one. This should include setup costs, transportation, migration, labor, and ongoing operating costs.

Finally, I would add a budget check to make sure the projected cost of the new warehouse is within the approved budget. This ensures that the replacement improves operations without creating an unexpected increase in costs.
```

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.

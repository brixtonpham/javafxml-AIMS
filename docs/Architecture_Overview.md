# AIMS Application - Architecture Overview

## 1. Introduction

This document describes the high-level architecture of the AIMS application, outlining its main components, their responsibilities, and interactions.

## 2. Architectural Style

AIMS primarily follows a **Layered Architecture**. This separates concerns into distinct layers, promoting modularity, maintainability, and testability. The typical layers are:

1.  **Presentation Layer (UI):** Responsible for user interaction and displaying information.
2.  **Application Layer (Services):** Orchestrates use cases and business logic, acting as an intermediary between the UI and domain layers.
3.  **Domain Layer (Core):** Contains business entities, value objects, and core business rules.
4.  **Infrastructure Layer (Data Access & External Services):** Handles data persistence, and interactions with external systems like payment gateways or messaging queues.

## 3. Key Components and Layers

### 3.1. Presentation Layer
*   **Technology:** JavaFX
*   **Components:**
    *   **Views (FXML):** Defines the structure and layout of the user interface. Located in `src/main/resources/fxml/` (or similar).
    *   **Controllers:** Handle user input, update views, and interact with the Application Layer. Located in `com.aims.gui.controllers` (or similar).
*   **Responsibilities:**
    *   Rendering UI elements.
    *   Capturing user actions.
    *   Displaying data received from the Application Layer.
    *   Basic input validation.

### 3.2. Application Layer
*   **Components:**
    *   **Service Interfaces:** Define contracts for application services (e.g., `ProductService`, `OrderService`). Located in `com.aims.core.application`.
    *   **Service Implementations:** Concrete implementations of the service interfaces, containing business logic orchestration. Located in `com.aims.core.application.impl`.
*   **Responsibilities:**
    *   Executing application-specific use cases.
    *   Coordinating interactions between domain objects.
    *   Transaction management (if applicable at this layer).
    *   Authorizing requests.

### 3.3. Domain Layer
*   **Components:**
    *   **Entities:** Objects with an identity that persists over time (e.g., `Product`, `User`, `Order`).
    *   **Value Objects:** Immutable objects representing descriptive aspects of the domain (e.g., `Address`, `Money`).
    *   **Domain Services:** Business logic that doesn't naturally fit within an entity or value object.
    *   (These are typically located in packages like `com.aims.core.domain.model` or `com.aims.core.domain.entity`).
*   **Responsibilities:**
    *   Encapsulating core business rules and logic.
    *   Maintaining the state of business objects.

### 3.4. Infrastructure Layer
*   **Components:**
    *   **Data Access Objects (DAOs):** Interfaces and implementations for database operations (e.g., `ProductDAO`, `OrderDAO`). Located in `com.aims.core.infrastructure.database.dao`.
    *   **Database Connector:** Manages connections to the SQLite database (e.g., `SQLiteConnector`).
    *   **External Service Integrations:** Clients or gateways for interacting with external systems (e.g., VNPay client). Located in `com.aims.core.infrastructure.payment` or similar.
    *   **Logging:** Configuration and utilities for Log4j2.
*   **Responsibilities:**
    *   Persisting and retrieving data from the database (SQLite).
    *   Communicating with external services (e.g., payment gateways).
    *   Handling file system operations, network communication, etc.

## 4. Data Flow Example (Placing an Order)

1.  **UI Layer:** User confirms order details in a JavaFX view. The corresponding Controller collects data.
2.  **Controller (UI) -> Application Layer:** Controller calls `OrderService.placeOrder(...)` with cart details, delivery info, etc.
3.  **Application Layer (`OrderService`):**
    *   Validates input.
    *   Retrieves `Cart` and `Product` information (possibly via other services or DAOs).
    *   Creates an `Order` domain entity.
    *   Calculates total price, delivery fees (possibly using `DeliveryCalculationService`).
    *   Initiates payment processing via `PaymentService`.
    *   Saves the `Order` and related entities (e.g., `OrderItem`) using `OrderDAO`.
    *   May send a notification via `NotificationService`.
4.  **Domain Layer:** `Order`, `Product`, `User` entities enforce business rules during their creation and modification.
5.  **Infrastructure Layer:**
    *   `OrderDAO` persists the order to the SQLite database.
    *   `PaymentService` interacts with VNPay gateway via its client.

## 5. Design Principles and Patterns

*   **SOLID Principles:** The project aims to adhere to SOLID principles for maintainable and flexible code. (Refer to `docs/SOLID_Principles_Analysis.md`).
*   **Design Patterns:** Various design patterns are employed. (Refer to `docs/Design_Patterns_Application.md`). Examples might include:
    *   MVC/MVP (for UI)
    *   Service Layer
    *   Repository/DAO (for data access)
    *   Factory
    *   Singleton
*   **Coupling and Cohesion:** Efforts are made to achieve low coupling and high cohesion. (Refer to `docs/Coupling_Cohesion_Analysis.md`).

## 6. Diagrams

Refer to the `docs/diagrams/` folder for UML diagrams illustrating various aspects of the architecture, such as:
*   Class Diagrams
*   Sequence Diagrams
*   Use Case Diagrams
*   Deployment Diagram (if available)

---
*This is a high-level overview. More detailed design decisions can be found in specific design documents.*

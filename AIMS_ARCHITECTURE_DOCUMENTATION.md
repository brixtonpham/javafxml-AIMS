# AIMS Architecture Documentation
**Version:** 1.0  
**Date:** December 6, 2025  
**Document Type:** Technical Architecture Guide

## Table of Contents
1. [System Architecture Overview](#1-system-architecture-overview)
2. [Application Layers](#2-application-layers)
3. [Core Components](#3-core-components)
4. [Data Flow Architecture](#4-data-flow-architecture)
5. [Security Architecture](#5-security-architecture)
6. [Integration Architecture](#6-integration-architecture)
7. [Deployment Architecture](#7-deployment-architecture)

## 1. System Architecture Overview

### 1.1 Architecture Pattern
AIMS follows a **Layered Architecture** pattern with clear separation of concerns:

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[JavaFX Controllers & FXML]
        Controller[Screen Controllers]
    end
    
    subgraph "Application Layer"
        Service[Business Services]
        Factory[Service Factory]
    end
    
    subgraph "Domain Layer"
        Entity[Domain Entities]
        Enum[Enums & Value Objects]
    end
    
    subgraph "Infrastructure Layer"
        DAO[Data Access Objects]
        Adapter[External Adapters]
        DB[(SQLite Database)]
    end
    
    UI --> Controller
    Controller --> Service
    Service --> Entity
    Service --> DAO
    DAO --> DB
    Service --> Adapter
    Factory --> Service
    Factory --> DAO
```

### 1.2 Key Architectural Principles
- **Dependency Inversion:** High-level modules don't depend on low-level modules
- **Single Responsibility:** Each class has one reason to change
- **Interface Segregation:** Clients depend only on interfaces they use
- **Separation of Concerns:** Clear boundaries between layers

## 2. Application Layers

### 2.1 Presentation Layer
**Location:** [`src/main/java/com/aims/core/presentation/`](src/main/java/com/aims/core/presentation/)

#### 2.1.1 Main Components
| Component | File | Responsibility |
|-----------|------|----------------|
| Application Entry | [`Main.java`](src/main/java/com/aims/Main.java) | JavaFX application launcher |
| Main App | [`AimsApp.java`](src/main/java/com/aims/AimsApp.java) | Application initialization & window setup |
| Scene Manager | [`FXMLSceneManager.java`](src/main/java/com/aims/core/presentation/utils/FXMLSceneManager.java) | Navigation & screen management |
| Main Layout | [`MainLayoutController.java`](src/main/java/com/aims/core/presentation/controllers/MainLayoutController.java) | Master page layout |

#### 2.1.2 Controller Architecture
```mermaid
graph TD
    MainLayout[MainLayoutController]
    MainLayout --> Login[LoginScreenController]
    MainLayout --> Home[HomeScreenController]
    MainLayout --> Product[ProductDetailScreenController]
    MainLayout --> Cart[CartScreenController]
    MainLayout --> Order[OrderSummaryController]
    MainLayout --> Admin[AdminDashboardController]
    MainLayout --> PM[ProductManagerDashboardController]
    
    Admin --> UserMgmt[EditUserFormController]
    Admin --> ProductMgmt[AdminAddEditProductController]
    PM --> OrderReview[OrderSummaryController]
```

#### 2.1.3 Screen Flow Architecture
| User Role | Entry Point | Available Screens |
|-----------|-------------|-------------------|
| **Customer (Guest)** | [`home_screen.fxml`](src/main/resources/com/aims/presentation/views/home_screen.fxml) | Home → Product Detail → Cart → Order → Payment |
| **Product Manager** | [`login_screen.fxml`](src/main/resources/com/aims/presentation/views/login_screen.fxml) | Login → PM Dashboard → Order Management |
| **Administrator** | [`login_screen.fxml`](src/main/resources/com/aims/presentation/views/login_screen.fxml) | Login → Admin Dashboard → User/Product Management |

### 2.2 Application Layer
**Location:** [`src/main/java/com/aims/core/application/`](src/main/java/com/aims/core/application/)

#### 2.2.1 Service Architecture
```mermaid
graph LR
    subgraph "Core Services"
        Auth[IAuthenticationService]
        Product[IProductService]
        Cart[ICartService]
        Order[IOrderService]
        Payment[IPaymentService]
    end
    
    subgraph "Supporting Services"
        User[IUserAccountService]
        Delivery[IDeliveryCalculationService]
        Notification[INotificationService]
    end
    
    Order --> Cart
    Order --> Product
    Order --> Payment
    Order --> Delivery
    Order --> Notification
    Payment --> User
    Auth --> User
```

#### 2.2.2 Service Implementations
| Interface | Implementation | Key Responsibilities |
|-----------|----------------|---------------------|
| [`IProductService`](src/main/java/com/aims/core/application/services/IProductService.java) | [`ProductServiceImpl`](src/main/java/com/aims/core/application/impl/ProductServiceImpl.java) | Product CRUD, business rules, search |
| [`ICartService`](src/main/java/com/aims/core/application/services/ICartService.java) | [`CartServiceImpl`](src/main/java/com/aims/core/application/impl/CartServiceImpl.java) | Cart operations, inventory validation |
| [`IOrderService`](src/main/java/com/aims/core/application/services/IOrderService.java) | [`OrderServiceImpl`](src/main/java/com/aims/core/application/impl/OrderServiceImpl.java) | Order lifecycle, payment processing |
| [`IAuthenticationService`](src/main/java/com/aims/core/application/services/IAuthenticationService.java) | [`AuthenticationServiceImpl`](src/main/java/com/aims/core/application/impl/AuthenticationServiceImpl.java) | User authentication, session management |

### 2.3 Domain Layer
**Location:** [`src/main/java/com/aims/core/entities/`](src/main/java/com/aims/core/entities/)

#### 2.3.1 Entity Relationship Model
```mermaid
erDiagram
    Product ||--o{ OrderItem : contains
    Product ||--o{ CartItem : contains
    Product {
        string productId PK
        string title
        string category
        float valueAmount
        float price
        int quantityInStock
        string barcode UK
        ProductType productType
    }
    
    Book ||--|| Product : extends
    CD ||--|| Product : extends
    DVD ||--|| Product : extends
    
    UserAccount ||--o{ OrderEntity : places
    OrderEntity ||--o{ OrderItem : contains
    OrderEntity ||--|| DeliveryInfo : has
    OrderEntity ||--|| Invoice : has
    OrderEntity ||--o{ PaymentTransaction : involves
    
    Cart ||--o{ CartItem : contains
    UserAccount ||--o{ Cart : owns
```

#### 2.3.2 Product Hierarchy
```mermaid
classDiagram
    class Product {
        +String productId
        +String title
        +String category
        +float valueAmount
        +float price
        +int quantityInStock
        +ProductType productType
    }
    
    class Book {
        +String authors
        +String coverType
        +String publisher
        +LocalDate publicationDate
        +int numPages
        +String language
        +String bookGenre
    }
    
    class CD {
        +String artists
        +String recordLabel
        +String tracklist
        +String cdGenre
        +LocalDate releaseDate
    }
    
    class DVD {
        +String discType
        +String director
        +int runtimeMinutes
        +String studio
        +String dvdLanguage
        +String subtitles
        +LocalDate dvdReleaseDate
    }
    
    Product <|-- Book
    Product <|-- CD
    Product <|-- DVD
```

### 2.4 Infrastructure Layer
**Location:** [`src/main/java/com/aims/core/infrastructure/`](src/main/java/com/aims/core/infrastructure/)

#### 2.4.1 Data Access Architecture
```mermaid
graph TB
    subgraph "DAO Layer"
        ProductDAO[IProductDAO]
        CartDAO[ICartDAO]
        OrderDAO[IOrderEntityDAO]
        UserDAO[IUserAccountDAO]
        PaymentDAO[IPaymentTransactionDAO]
    end
    
    subgraph "Database"
        SQLite[(SQLite Database)]
    end
    
    subgraph "External Adapters"
        VNPay[IVNPayAdapter]
        Email[IEmailSenderAdapter]
    end
    
    ProductDAO --> SQLite
    CartDAO --> SQLite
    OrderDAO --> SQLite
    UserDAO --> SQLite
    PaymentDAO --> SQLite
    
    VNPay --> ExternalAPI[VNPay Sandbox]
    Email --> SMTP[Email Service]
```

## 3. Core Components

### 3.1 Dependency Injection Container
**File:** [`ServiceFactory.java`](src/main/java/com/aims/core/shared/ServiceFactory.java)

#### 3.1.1 Initialization Sequence
```mermaid
sequenceDiagram
    participant App as AimsApp
    participant Factory as ServiceFactory
    participant DAO as DAO Layer
    participant Service as Service Layer
    participant Controller as Controllers
    
    App->>Factory: getInstance()
    Factory->>Factory: initializeDependencies()
    Factory->>DAO: Create DAOs
    Factory->>Service: Create Services with DAO deps
    App->>Controller: Inject services
    Controller->>Service: Business operations
```

#### 3.1.2 Dependency Graph
```mermaid
graph TD
    SF[ServiceFactory] --> ProdDAO[ProductDAO]
    SF --> UserDAO[UserAccountDAO]
    SF --> CartDAO[CartDAO]
    SF --> OrderDAO[OrderEntityDAO]
    
    SF --> ProdService[ProductService]
    SF --> AuthService[AuthService]
    SF --> CartService[CartService]
    SF --> OrderService[OrderService]
    
    CartService --> ProdDAO
    CartService --> CartDAO
    OrderService --> CartService
    OrderService --> ProdService
    OrderService --> PaymentService[PaymentService]
```

### 3.2 State Management
**Location:** [`src/main/java/com/aims/core/states/`](src/main/java/com/aims/core/states/)

#### 3.2.1 Order State Pattern
```mermaid
stateDiagram-v2
    [*] --> PendingDeliveryInfo
    PendingDeliveryInfo --> PendingPayment : setDeliveryInfo()
    PendingPayment --> PendingProcessing : processPayment()
    PendingProcessing --> Approved : approve()
    PendingProcessing --> Rejected : reject()
    PendingProcessing --> Canceled : cancel()
    Approved --> Shipping : ship()
    Shipping --> Delivered : deliver()
    Rejected --> [*]
    Canceled --> [*]
    Delivered --> [*]
```

#### 3.2.2 State Implementation
| State | Implementation | Allowed Transitions |
|-------|---------------|-------------------|
| Pending Delivery Info | [`PendingOrderState.java`](src/main/java/com/aims/core/states/PendingOrderState.java) | → Pending Payment |
| Pending Processing | [`PendingOrderState.java`](src/main/java/com/aims/core/states/PendingOrderState.java) | → Approved, Rejected, Canceled |
| Approved | [`ApprovedOrderState.java`](src/main/java/com/aims/core/states/ApprovedOrderState.java) | → Shipping |
| Shipping | [`ShippingOrderState.java`](src/main/java/com/aims/core/states/ShippingOrderState.java) | → Delivered |
| Delivered | [`DeliveredOrderState.java`](src/main/java/com/aims/core/states/DeliveredOrderState.java) | [Final] |

## 4. Data Flow Architecture

### 4.1 Customer Purchase Flow
```mermaid
sequenceDiagram
    participant C as Customer
    participant UI as HomeScreen
    participant CartS as CartService
    participant OrderS as OrderService
    participant PayS as PaymentService
    participant VNPay as VNPay Adapter
    
    C->>UI: Browse Products
    UI->>CartS: addItemToCart()
    CartS->>CartS: validateInventory()
    C->>UI: Proceed to Checkout
    UI->>OrderS: initiateOrderFromCart()
    OrderS->>OrderS: validateCartItems()
    C->>UI: Enter Delivery Info
    UI->>OrderS: setDeliveryInformation()
    OrderS->>OrderS: calculateShippingFee()
    C->>UI: Process Payment
    UI->>PayS: processPayment()
    PayS->>VNPay: initiatePayment()
    VNPay-->>PayS: paymentResult
    PayS-->>OrderS: paymentConfirmation
    OrderS->>CartS: clearCart()
```

### 4.2 Product Management Flow
```mermaid
sequenceDiagram
    participant PM as Product Manager
    participant UI as AdminController
    participant ProdS as ProductService
    participant ProdDAO as ProductDAO
    participant DB as Database
    
    PM->>UI: Add New Product
    UI->>ProdS: addBook/CD/DVD()
    ProdS->>ProdS: validateBusinessRules()
    ProdS->>ProdS: checkDailyLimits()
    ProdS->>ProdDAO: addBaseProduct()
    ProdDAO->>DB: INSERT Product
    ProdDAO->>DB: INSERT Book/CD/DVD
    DB-->>ProdDAO: Confirmation
    ProdDAO-->>ProdS: Product Created
    ProdS-->>UI: Success Response
```

### 4.3 Order Processing Flow
```mermaid
sequenceDiagram
    participant PM as Product Manager
    participant UI as PMDashboard
    participant OrderS as OrderService
    participant NotifyS as NotificationService
    participant Email as EmailAdapter
    
    PM->>UI: View Pending Orders
    UI->>OrderS: getOrdersByStatusForManager()
    PM->>UI: Select Order to Review
    UI->>OrderS: getOrderDetails()
    PM->>UI: Approve/Reject Order
    UI->>OrderS: approveOrder() / rejectOrder()
    OrderS->>OrderS: validateInventory()
    OrderS->>OrderS: updateOrderStatus()
    OrderS->>NotifyS: sendOrderStatusUpdate()
    NotifyS->>Email: sendEmail()
```

## 5. Security Architecture

### 5.1 Authentication Architecture
```mermaid
graph TB
    subgraph "Authentication Layer"
        Login[Login Controller]
        AuthService[Authentication Service]
        Session[Session Management]
    end
    
    subgraph "Authorization Layer"
        RoleCheck[Role Verification]
        AccessControl[Access Control]
    end
    
    subgraph "Data Layer"
        UserDAO[User Account DAO]
        RoleDAO[Role DAO]
        UserRoleDAO[User Role Assignment DAO]
    end
    
    Login --> AuthService
    AuthService --> Session
    AuthService --> UserDAO
    RoleCheck --> RoleDAO
    RoleCheck --> UserRoleDAO
    AccessControl --> RoleCheck
```

### 5.2 Role-Based Access Control
| User Role | Permissions | Implementation |
|-----------|-------------|----------------|
| **Administrator** | Full system access, user management | [`UserRole.ADMIN`](src/main/java/com/aims/core/enums/UserRole.java) |
| **Product Manager** | Product CRUD, order management | [`UserRole.PRODUCT_MANAGER`](src/main/java/com/aims/core/enums/UserRole.java) |
| **Customer/Guest** | Browse, cart, order (no login) | No authentication required |

### 5.3 Data Protection
```mermaid
graph LR
    subgraph "Input Validation"
        Form[Form Validation]
        Business[Business Rule Validation]
    end
    
    subgraph "Data Security"
        Hash[Password Hashing]
        Encryption[Sensitive Data Protection]
    end
    
    subgraph "Session Security"
        Timeout[Session Timeout]
        Token[Session Token Management]
    end
    
    Form --> Business
    Business --> Hash
    Hash --> Encryption
    Encryption --> Token
```

## 6. Integration Architecture

### 6.1 External Service Integration
```mermaid
graph TB
    subgraph "AIMS Core"
        PaymentService[Payment Service]
        NotificationService[Notification Service]
    end
    
    subgraph "Adapter Layer"
        VNPayAdapter[VNPay Adapter]
        EmailAdapter[Email Adapter]
    end
    
    subgraph "External Services"
        VNPayAPI[VNPay Sandbox API]
        EmailService[Email Service Provider]
    end
    
    PaymentService --> VNPayAdapter
    VNPayAdapter --> VNPayAPI
    NotificationService --> EmailAdapter
    EmailAdapter --> EmailService
```

### 6.2 VNPay Integration Architecture
| Component | File | Responsibility |
|-----------|------|----------------|
| Payment Service | [`IPaymentService.java`](src/main/java/com/aims/core/application/services/IPaymentService.java) | Payment orchestration |
| VNPay Adapter | [`IVNPayAdapter.java`](src/main/java/com/aims/core/infrastructure/adapters/external/payment_gateway/IVNPayAdapter.java) | VNPay API integration |
| Payment Strategies | [`IPaymentStrategy.java`](src/main/java/com/aims/core/application/services/strategies/IPaymentStrategy.java) | Different payment methods |
| VNPay Controllers | [`VNPayReturnController.java`](src/main/java/com/aims/core/presentation/controllers/VNPayReturnController.java) | Handle VNPay callbacks |

### 6.3 Payment Flow Integration
```mermaid
sequenceDiagram
    participant Order as Order Service
    participant Payment as Payment Service
    participant Strategy as Payment Strategy
    participant VNPay as VNPay Adapter
    participant External as VNPay API
    
    Order->>Payment: processPayment()
    Payment->>Strategy: processPayment()
    Strategy->>VNPay: preparePaymentParameters()
    VNPay->>External: POST /payment
    External-->>VNPay: Payment URL
    VNPay-->>Strategy: Payment URL
    Strategy-->>Payment: Payment Response
    Payment-->>Order: Transaction Result
```

## 7. Deployment Architecture

### 7.1 Application Structure
```
AIMS_Project/
├── src/main/java/com/aims/
│   ├── Main.java                    # Application entry point
│   ├── AimsApp.java                 # JavaFX application
│   └── core/
│       ├── application/             # Business logic layer
│       ├── entities/                # Domain models
│       ├── infrastructure/          # Data access & external adapters
│       ├── presentation/            # UI controllers
│       ├── shared/                  # Common utilities
│       └── states/                  # State pattern implementations
├── src/main/resources/
│   ├── com/aims/presentation/views/ # FXML files
│   ├── images/                      # Product images
│   └── *.properties               # Configuration files
└── target/                         # Build artifacts
```

### 7.2 Configuration Management
| File | Purpose | Location |
|------|---------|----------|
| [`app.properties`](src/main/resources/app.properties) | Application configuration | Resources root |
| [`vnpay_config.properties`](src/main/resources/vnpay_config.properties) | VNPay integration settings | Resources root |
| [`logback.xml`](src/main/resources/logback.xml) | Logging configuration | Resources root |

### 7.3 Database Architecture
```mermaid
graph TB
    subgraph "Database Layer"
        SQLite[(SQLite Database)]
        Schema[Database Schema]
        Migration[Migration Scripts]
    end
    
    subgraph "Data Access"
        DAO[DAO Implementations]
        Connection[Database Connection]
        Transaction[Transaction Management]
    end
    
    Schema --> SQLite
    Migration --> SQLite
    DAO --> Connection
    Connection --> SQLite
    Transaction --> Connection
```

### 7.4 Build & Deployment
| Component | Technology | Configuration |
|-----------|------------|---------------|
| **Build Tool** | Maven | [`pom.xml`](pom.xml) |
| **Java Runtime** | Java 11+ | JavaFX dependencies |
| **Database** | SQLite | Embedded, file-based |
| **External Dependencies** | VNPay SDK, Email libraries | Maven dependencies |

---

**Architecture Principles Summary:**
- **Modularity:** Clear separation between layers
- **Testability:** Dependency injection enables unit testing
- **Maintainability:** Interface-based design allows easy modifications
- **Scalability:** Service-oriented architecture supports feature expansion
- **Security:** Role-based access control and input validation
- **Integration:** Adapter pattern for external service integration

**Document Control:**
- **Author:** AIMS Architecture Team
- **Review Status:** Technical Review Complete
- **Next Review:** Post-deployment validation
- **Distribution:** Development Team, Technical Stakeholders
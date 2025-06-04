# AIMS Application - API Documentation

## 1. Introduction

This document provides an overview of the key internal APIs (service layer interfaces) within the AIMS application. These APIs define the core business logic and operations.

## 2. Core Service Interfaces

The application's business logic is primarily encapsulated in service interfaces found within the `com.aims.core.application` package (or similar).

### 2.1. `IAuthenticationService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Handles user authentication, registration (though registration seems to be part of `IUserAccountService`), and session management.
*   **Implementation:** `AuthenticationServiceImpl`
*   **Dependencies:** `IUserAccountDAO`, `IUserRoleAssignmentDAO`

*   **Key Methods:**

    *   `UserAccount login(String username, String plainTextPassword) throws AuthenticationException, SQLException, ResourceNotFoundException`
        *   **Description:** Authenticates a user based on username and password.
        *   **Parameters:**
            *   `username`: The user's username.
            *   `plainTextPassword`: The user's plain text password.
        *   **Returns:** The authenticated `UserAccount` object if successful.
        *   **Throws:**
            *   `AuthenticationException`: If login fails (e.g., invalid credentials, user blocked).
            *   `SQLException`: If a database error occurs.
            *   `ResourceNotFoundException`: If the user account does not exist.

    *   `void logout(String sessionId)`
        *   **Description:** Logs out the user associated with the given session ID. (Actual session management might be more complex and involve a session manager).
        *   **Parameters:**
            *   `sessionId`: The ID of the session to invalidate.

    *   `UserAccount validateSession(String sessionId) throws AuthenticationException`
        *   **Description:** Validates an existing session.
        *   **Parameters:**
            *   `sessionId`: The session ID to validate.
        *   **Returns:** The `UserAccount` associated with the valid session.
        *   **Throws:**
            *   `AuthenticationException`: If the session is invalid or expired.

    *   `UserAccount getCurrentAuthenticatedUser(String sessionId) throws AuthenticationException`
        *   **Description:** Retrieves the currently authenticated user for a given session.
        *   **Parameters:**
            *   `sessionId`: The session ID.
        *   **Returns:** The `UserAccount` of the authenticated user.
        *   **Throws:**
            *   `AuthenticationException`: If no user is authenticated for the session.

### 2.2. `IProductService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Manages product information, including CRUD operations for product managers and retrieval/search for customers.
*   **Implementation:** `ProductServiceImpl`
*   **Dependencies:** `IProductDAO`

*   **Key Methods (Illustrative - check interface for complete list):**

    *   **For Product Managers:**
        *   `Book addBook(Book book) throws SQLException, ValidationException`
        *   `CD addCD(CD cd) throws SQLException, ValidationException`
        *   `DVD addDVD(DVD dvd) throws SQLException, ValidationException`
            *   **Description:** Adds a new Book, CD, or DVD to the catalog.
            *   **Parameters:** The `Book`, `CD`, or `DVD` entity to add.
            *   **Returns:** The added entity, potentially re-fetched from the database.
            *   **Throws:** `SQLException`, `ValidationException` (e.g., price constraints).
        *   `Book updateBook(Book book) throws SQLException, ValidationException, ResourceNotFoundException`
        *   `CD updateCD(CD cd) throws SQLException, ValidationException, ResourceNotFoundException`
        *   `DVD updateDVD(DVD dvd) throws SQLException, ValidationException, ResourceNotFoundException`
            *   **Description:** Updates an existing Book, CD, or DVD.
            *   **Parameters:** The `Book`, `CD`, or `DVD` entity with updated information.
            *   **Returns:** The updated entity.
            *   **Throws:** `SQLException`, `ValidationException`, `ResourceNotFoundException`.
        *   `void deleteProduct(String productId, String managerId) throws SQLException, ValidationException, ResourceNotFoundException`
        *   `void deleteProducts(List<String> productIds, String managerId) throws SQLException, ValidationException`
            *   **Description:** Deletes one or more products.
            *   **Parameters:** `productId` (for single deletion), `productIds` (for batch deletion), `managerId` (for authorization/logging).
            *   **Throws:** `SQLException`, `ValidationException` (e.g., deletion limits), `ResourceNotFoundException`.
        *   `Product updateProductPrice(String productId, float newPriceExclVAT, String managerId) throws SQLException, ValidationException, ResourceNotFoundException`
            *   **Description:** Updates the price of a product.
            *   **Parameters:** `productId`, `newPriceExclVAT`, `managerId`.
            *   **Returns:** The product with the updated price.
            *   **Throws:** `SQLException`, `ValidationException`, `ResourceNotFoundException`.
        *   `Product updateProductStock(String productId, int quantityChange) throws SQLException, ValidationException, ResourceNotFoundException`
            *   **Description:** Updates the stock quantity of a product (can be positive or negative change).
            *   **Parameters:** `productId`, `quantityChange`.
            *   **Returns:** The product with the updated stock.
            *   **Throws:** `SQLException`, `ValidationException` (e.g., stock cannot go negative), `ResourceNotFoundException`.

    *   **For Customers:**
        *   `SearchResult<Product> getProductsForDisplay(int pageNumber, int pageSize) throws SQLException`
            *   **Description:** Retrieves a paginated list of products for customer display (VAT included).
            *   **Parameters:** `pageNumber`, `pageSize`.
            *   **Returns:** A `SearchResult` object containing the list of products and pagination details.
            *   **Throws:** `SQLException`.
        *   `SearchResult<Product> searchProducts(String searchTerm, String category, int pageNumber, int pageSize, String sortByPrice) throws SQLException`
            *   **Description:** Searches for products based on various criteria (VAT included).
            *   **Parameters:** `searchTerm`, `category`, `pageNumber`, `pageSize`, `sortByPrice` (e.g., "asc", "desc").
            *   **Returns:** A `SearchResult` of matching products.
            *   **Throws:** `SQLException`.
        *   `Product getProductDetailsForCustomer(String productId) throws SQLException, ResourceNotFoundException`
            *   **Description:** Retrieves detailed information for a single product for customer display (VAT included).
            *   **Parameters:** `productId`.
            *   **Returns:** The `Product` details.
            *   **Throws:** `SQLException`, `ResourceNotFoundException`.
        *   `List<Product> getAllProducts() throws SQLException` (As per original doc - likely returns products with VAT for display)
        *   `Product getProductById(String id) throws SQLException` (As per original doc - likely returns product with VAT for display, or could be an internal version without VAT)
        *   `List<Category> getAllCategories() throws SQLException` (As per original doc - Fetches all product categories)

### 2.3. `ICartService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Manages the user's shopping cart, including adding, updating, and removing items, and associating carts with users.
*   **Implementation:** `CartServiceImpl`
*   **Dependencies:** `ICartDAO`, `ICartItemDAO`, `IProductDAO`, `IUserAccountDAO`

*   **Key Methods:**

    *   `Cart getCart(String cartSessionId) throws SQLException`
        *   **Description:** Retrieves a cart by its session ID. May create a new guest cart if the ID is null or not found, depending on policy.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart.
        *   **Returns:** The `Cart` object, or `null` if not found and not creating new guest carts.
        *   **Throws:** `SQLException`.

    *   `Cart addItemToCart(String cartSessionId, String productId, int quantity) throws SQLException, ResourceNotFoundException, ValidationException, InventoryException`
        *   **Description:** Adds a product to the cart or updates its quantity if already present. Validates product existence and stock.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart. A new cart might be created if this ID is for a non-existent cart.
            *   `productId`: The ID of the product to add.
            *   `quantity`: The quantity to add.
        *   **Returns:** The updated `Cart` object.
        *   **Throws:**
            *   `SQLException`: If a database error occurs.
            *   `ResourceNotFoundException`: If the product is not found.
            *   `ValidationException`: If quantity is invalid or other validation fails.
            *   `InventoryException`: If the product has insufficient stock.

    *   `Cart updateItemInCart(String cartSessionId, String productId, int newQuantity) throws SQLException, ResourceNotFoundException, ValidationException, InventoryException`
        *   **Description:** Updates the quantity of an existing item in the cart. If quantity is 0, removes the item.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart.
            *   `productId`: The ID of the product (cart item) to update.
            *   `newQuantity`: The new quantity for the item. If 0, the item is removed.
        *   **Returns:** The updated `Cart` object.
        *   **Throws:**
            *   `SQLException`, `ResourceNotFoundException` (if cart or product not found), `ValidationException`, `InventoryException`.

    *   `Cart removeItemFromCart(String cartSessionId, String productId) throws SQLException, ResourceNotFoundException, ValidationException`
        *   **Description:** Removes an item (product) completely from the cart.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart.
            *   `productId`: The ID of the product to remove.
        *   **Returns:** The updated `Cart` object.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (if cart or item not found), `ValidationException`.

    *   `void clearCart(String cartSessionId) throws SQLException, ResourceNotFoundException`
        *   **Description:** Removes all items from a given cart.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart to clear.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (if cart not found).

    *   `Cart associateCartWithUser(String cartSessionId, String userId) throws SQLException, ResourceNotFoundException, ValidationException`
        *   **Description:** Associates an existing (possibly guest) cart with a logged-in user.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart.
            *   `userId`: The ID of the user to associate the cart with.
        *   **Returns:** The `Cart` object, now associated with the user.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (if cart or user not found), `ValidationException` (e.g., if cart already associated with another user).

    *   `Cart createNewCart(String userId) throws SQLException`
        *   **Description:** Creates a new, empty cart, optionally associated with a user.
        *   **Parameters:**
            *   `userId`: The ID of the user for whom to create the cart (can be null for a guest cart, though `getCart` might handle guest creation).
        *   **Returns:** The newly created `Cart` object.
        *   **Throws:** `SQLException`.

### 2.4. `IOrderService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Orchestrates the entire order processing lifecycle, from initiation from a cart to payment, status updates, and fulfillment.
*   **Implementation:** `OrderServiceImpl`
*   **Dependencies:** `IOrderEntityDAO`, `IOrderItemDAO`, `IDeliveryInfoDAO`, `IInvoiceDAO`, `IProductDAO`, `IProductService`, `ICartService`, `IPaymentService`, `IDeliveryCalculationService`, `INotificationService`, `IUserAccountDAO`

*   **Key Methods (Illustrative - check interface for complete list):**

    *   `OrderEntity initiateOrderFromCart(String cartSessionId, String userId) throws SQLException, ResourceNotFoundException, InventoryException, ValidationException`
        *   **Description:** Creates an initial order from the items in a user's cart. Checks inventory and calculates initial totals.
        *   **Parameters:**
            *   `cartSessionId`: The session ID of the cart.
            *   `userId`: The ID of the user placing the order (can be null for guest).
        *   **Returns:** An `OrderEntity` in a state like `PENDING_DELIVERY_INFO`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (cart/user not found), `InventoryException` (stock issues), `ValidationException`.

    *   `OrderEntity setDeliveryInformation(String orderId, DeliveryInfo deliveryInfoInput, boolean isRushOrder) throws SQLException, ResourceNotFoundException, ValidationException`
        *   **Description:** Sets or updates the delivery information for an order. Calculates shipping fees based on the information and rush order status. Updates order total and status (e.g., to `PENDING_PAYMENT`).
        *   **Parameters:**
            *   `orderId`: The ID of the order.
            *   `deliveryInfoInput`: The `DeliveryInfo` object containing address, recipient, etc.
            *   `isRushOrder`: Boolean indicating if rush delivery is requested.
        *   **Returns:** The updated `OrderEntity`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (order not found), `ValidationException` (invalid delivery info, or order not in correct state).

    *   `float calculateShippingFee(String orderId, DeliveryInfo deliveryInfo, boolean isRushOrder) throws SQLException, ResourceNotFoundException, ValidationException`
        *   **Description:** Calculates the shipping fee for an order based on delivery information and whether it's a rush order. This might be called internally by `setDeliveryInformation` or exposed if needed separately.
        *   **Parameters:**
            *   `orderId`: The ID of the order.
            *   `deliveryInfo`: The delivery information.
            *   `isRushOrder`: Boolean for rush delivery.
        *   **Returns:** The calculated shipping fee.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException`.

    *   `OrderEntity processPayment(String orderId, String paymentMethodId) throws SQLException, ResourceNotFoundException, ValidationException, PaymentException, InventoryException`
        *   **Description:** Processes the payment for an order. Interacts with `IPaymentService`. On success, updates stock, creates an invoice, and updates order status (e.g., to `PENDING_PROCESSING` or `APPROVED`).
        *   **Parameters:**
            *   `orderId`: The ID of the order.
            *   `paymentMethodId`: The ID of the chosen payment method.
        *   **Returns:** The updated `OrderEntity` after payment processing.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException` (order not ready for payment), `PaymentException` (payment failed), `InventoryException` (stock changed during payment).

    *   `OrderEntity getOrderDetails(String orderId) throws SQLException, ResourceNotFoundException`
        *   **Description:** Retrieves comprehensive details of a specific order, including items, delivery info, invoice, etc.
        *   **Parameters:**
            *   `orderId`: The ID of the order.
        *   **Returns:** The `OrderEntity` with all its details.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`.

    *   `List<OrderEntity> getOrdersByUserId(String userId) throws SQLException`
        *   **Description:** Retrieves a list of all orders placed by a specific user.
        *   **Parameters:**
            *   `userId`: The ID of the user.
        *   **Returns:** A list of `OrderEntity` objects.
        *   **Throws:** `SQLException`.

    *   `OrderEntity cancelOrder(String orderId, String customerId) throws SQLException, ResourceNotFoundException, ValidationException, PaymentException`
        *   **Description:** Allows a customer to cancel an order if it's in a cancelable state. May involve refund processing via `IPaymentService`.
        *   **Parameters:**
            *   `orderId`: The ID of the order to cancel.
            *   `customerId`: The ID of the customer requesting cancellation (for authorization).
        *   **Returns:** The updated `OrderEntity` with status `CANCELLED`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException` (order not cancelable), `PaymentException` (refund failed).

    *   `SearchResult<OrderEntity> getOrdersByStatusForManager(OrderStatus status, int pageNumber, int pageSize) throws SQLException`
        *   **Description:** Retrieves a paginated list of orders filtered by a specific status, for administrative/managerial review.
        *   **Parameters:**
            *   `status`: The `OrderStatus` to filter by.
            *   `pageNumber`: The page number for pagination.
            *   `pageSize`: The number of orders per page.
        *   **Returns:** A `SearchResult` containing the list of orders and pagination details.
        *   **Throws:** `SQLException`.

    *   `OrderEntity approveOrder(String orderId, String managerId) throws SQLException, ResourceNotFoundException, ValidationException, InventoryException`
        *   **Description:** Allows a manager to approve an order (e.g., if it was pending manual review). May trigger further processing or shipment.
        *   **Parameters:**
            *   `orderId`: The ID of the order to approve.
            *   `managerId`: The ID of the manager performing the action.
        *   **Returns:** The updated `OrderEntity` with status `APPROVED` or similar.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException`, `InventoryException`.

    *   `OrderEntity rejectOrder(String orderId, String managerId, String reason) throws SQLException, ResourceNotFoundException, ValidationException, PaymentException`
        *   **Description:** Allows a manager to reject an order. May involve refund processing.
        *   **Parameters:**
            *   `orderId`: The ID of the order to reject.
            *   `managerId`: The ID of the manager.
            *   `reason`: The reason for rejection.
        *   **Returns:** The updated `OrderEntity` with status `REJECTED`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException`, `PaymentException`.

    *   `OrderEntity updateOrderStatus(String orderId, OrderStatus newStatus, String adminOrManagerId) throws SQLException, ResourceNotFoundException, ValidationException`
        *   **Description:** A general method for an admin or manager to update the status of an order (e.g., `SHIPPED`, `DELIVERED`).
        *   **Parameters:**
            *   `orderId`: The ID of the order.
            *   `newStatus`: The new `OrderStatus`.
            *   `adminOrManagerId`: The ID of the admin/manager.
        *   **Returns:** The updated `OrderEntity`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException`.

### 2.5. `IPaymentService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Handles all aspects of payment processing, including initiating payments with external gateways (like VNPay), processing refunds, and checking payment statuses.
*   **Implementation:** `PaymentServiceImpl`
*   **Dependencies:** `IPaymentTransactionDAO`, `IPaymentMethodDAO`, `ICardDetailsDAO`, `IVNPayAdapter`, potentially `IOrderEntityDAO` (e.g., for refund context).

*   **Key Methods (Illustrative - check interface for complete list):**

    *   `PaymentTransaction processPayment(OrderEntity order, String paymentMethodId) throws SQLException, PaymentException, ValidationException, ResourceNotFoundException`
        *   **Description:** Processes a payment for a given order using a specified payment method. This involves creating a local payment transaction record and interacting with the payment gateway (e.g., VNPayAdapter to get a payment URL or process directly).
        *   **Parameters:**
            *   `order`: The `OrderEntity` for which payment is being made.
            *   `paymentMethodId`: The ID of the selected `PaymentMethod`.
            *   `cardDetailsInput` (Potentially): A DTO or `CardDetails` object if paying directly with a new card not saved on file.
        *   **Returns:** A `PaymentTransaction` object representing the outcome of the payment attempt (e.g., status `PENDING`, `SUCCESSFUL`, `FAILED`). For gateway payments like VNPay, this might initially be `PENDING` and include a redirect URL.
        *   **Throws:**
            *   `SQLException`: If a database error occurs.
            *   `PaymentException`: If the payment gateway interaction fails or returns an error.
            *   `ValidationException`: If input is invalid (e.g., order not in a payable state, invalid payment method).
            *   `ResourceNotFoundException`: If the order or payment method is not found.

    *   `PaymentTransaction processRefund(String orderIdToRefund, String originalGatewayTransactionId, float refundAmount, String reason) throws SQLException, PaymentException, ValidationException, ResourceNotFoundException`
        *   **Description:** Processes a refund for a previously completed payment. Interacts with the payment gateway to issue the refund.
        *   **Parameters:**
            *   `orderIdToRefund`: The ID of the order associated with the refund.
            *   `originalGatewayTransactionId`: The transaction ID from the payment gateway for the original payment.
            *   `refundAmount`: The amount to be refunded.
            *   `reason`: The reason for the refund.
        *   **Returns:** A `PaymentTransaction` object representing the refund transaction (status `REFUNDED`, `REFUND_FAILED`).
        *   **Throws:** `SQLException`, `PaymentException` (gateway refund error), `ValidationException` (invalid refund request), `ResourceNotFoundException` (original transaction not found).

    *   `String createVNPayPaymentURL(OrderEntity order, String ipAddress, Map<String, String> additionalParams) throws PaymentException, ValidationException, SQLException`
        *   **Description:** Specifically for VNPay, constructs the payment URL to redirect the user to the VNPay gateway.
        *   **Parameters:**
            *   `order`: The `OrderEntity` for which the payment is being made.
            *   `ipAddress`: The client's IP address (required by VNPay).
            *   `additionalParams`: Any other parameters required by VNPay or for tracking.
        *   **Returns:** The URL string for VNPay payment.
        *   **Throws:** `PaymentException` (error during URL generation), `ValidationException`, `SQLException`.

    *   `PaymentTransaction handleVNPayReturn(Map<String, String> vnpayReturnParams) throws SQLException, PaymentException, ValidationException, ResourceNotFoundException`
        *   **Description:** Handles the callback/return from VNPay after the user completes (or cancels) the payment on the VNPay gateway. Verifies the signature, updates the local payment transaction, and potentially the order status.
        *   **Parameters:**
            *   `vnpayReturnParams`: A map of parameters returned by VNPay in the callback URL.
        *   **Returns:** The updated `PaymentTransaction` object.
        *   **Throws:** `SQLException`, `PaymentException` (e.g., invalid signature, payment failed at gateway), `ValidationException`, `ResourceNotFoundException` (original transaction not found).

    *   `PaymentTransaction handleVNPayIPN(Map<String, String> vnpayIPNParams) throws SQLException, PaymentException, ValidationException, ResourceNotFoundException`
        *   **Description:** Handles the Instant Payment Notification (IPN) from VNPay. This is a server-to-server notification used to confirm the payment status reliably, independent of the user's browser return.
        *   **Parameters:**
            *   `vnpayIPNParams`: A map of parameters sent by VNPay in the IPN request.
        *   **Returns:** The updated `PaymentTransaction` (or a status object for VNPay to acknowledge receipt).
        *   **Throws:** `SQLException`, `PaymentException`, `ValidationException`, `ResourceNotFoundException`.

    *   `List<PaymentMethod> getAvailablePaymentMethods(String userId) throws SQLException`
        *   **Description:** Retrieves a list of available payment methods for a user (e.g., saved cards, general methods like COD, VNPay).
        *   **Parameters:**
            *   `userId`: The ID of the user (can be null for guest, showing only general methods).
        *   **Returns:** A list of `PaymentMethod` objects.
        *   **Throws:** `SQLException`.

    *   `PaymentTransaction checkPaymentStatus(String transactionId, String externalTransactionId) throws PaymentException, SQLException, ResourceNotFoundException, ValidationException`
        *   **Description:** Checks or queries the status of a previously initiated payment transaction, possibly by querying the external payment gateway.
        *   **Parameters:**
            *   `transactionId`: The internal AIMS payment transaction ID.
            *   `externalTransactionId`: The transaction ID from the external gateway (e.g., VNPay's `vnp_TransactionNo`).
        *   **Returns:** The updated `PaymentTransaction` object with the latest status.
        *   **Throws:** `PaymentException`, `SQLException`, `ResourceNotFoundException`, `ValidationException`.

### 2.6. `IDeliveryCalculationService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Calculates delivery fees and can estimate delivery times based on various factors like order contents (weight, dimensions, type of items), delivery location, and whether rush delivery is requested.
*   **Implementation:** `DeliveryCalculationServiceImpl`
*   **Dependencies:** (Likely none directly, or possibly configuration for shipping rules/rates if not hardcoded).

*   **Key Methods (Illustrative - check interface for complete list):**

    *   `float calculateShippingFee(OrderEntity order, DeliveryInfo deliveryInfo, boolean isRushOrder) throws ValidationException, SQLException`
        *   **Description:** Calculates the shipping fee for a given order. The calculation logic considers the delivery province, total weight/dimensions of items in the order, and whether rush delivery is requested. Specific rates for provinces and rush order surcharges are applied.
        *   **Parameters:**
            *   `order`: The `OrderEntity` containing the items to be shipped.
            *   `deliveryInfo`: The `DeliveryInfo` object containing the destination address (especially the province).
            *   `isRushOrder`: A boolean indicating if rush delivery is requested.
        *   **Returns:** The calculated shipping fee as a float.
        *   **Throws:**
            *   `ValidationException`: If `deliveryInfo` or `order` is null, or if the province is not supported/invalid.
            *   `SQLException`: If there's an issue accessing product details (e.g., weight/dimensions) if they are fetched from DB within this service (though ideally, they are part of `OrderEntity` or `Product` objects passed in).

    *   `float calculateShippingFee(List<OrderItem> items, String province, boolean isRushOrder, List<Product> productsInOrder) throws ValidationException` (Signature based on `DeliveryCalculationServiceImplTest`)
        *   **Description:** A more granular method to calculate shipping fees based on a list of items, destination province, rush order status, and product details. This seems to be the core logic used by the method above.
        *   **Parameters:**
            *   `items`: A list of `OrderItem` objects.
            *   `province`: The destination province string.
            *   `isRushOrder`: Boolean indicating if rush delivery is requested.
            *   `productsInOrder`: A list of `Product` entities corresponding to the order items, used to get weight/dimensions.
        *   **Returns:** The calculated shipping fee.
        *   **Throws:**
            *   `ValidationException`: If input parameters are invalid (e.g., null lists, empty province).

    *   `EstimatedDeliveryTime estimateDeliveryTime(OrderEntity order, DeliveryInfo deliveryInfo, boolean isRushOrder) throws ValidationException` (Hypothetical - if this functionality exists)
        *   **Description:** Estimates the delivery time or date range for an order.
        *   **Parameters:**
            *   `order`: The `OrderEntity`.
            *   `deliveryInfo`: The `DeliveryInfo` object.
            *   `isRushOrder`: Boolean indicating if rush delivery is requested.
        *   **Returns:** An object representing the estimated delivery time/date (e.g., `String`, `DateRange`).
        *   **Throws:**
            *   `ValidationException`: If inputs are invalid.

    *   (The original document mentioned `double calculateDeliveryFee(Order order, DeliveryInfo deliveryInfo)` and `Date estimateDeliveryDate(Order order, DeliveryInfo deliveryInfo)`. The actual implementation uses `float` for fees and the `OrderEntity`, `DeliveryInfo` types. Date estimation is not directly visible in the provided search results for `DeliveryCalculationServiceImpl` but is a common feature.)

### 2.7. `IUserAccountService`

*   **Package:** `com.aims.core.application.services` (Interface), `com.aims.core.application.impl` (Implementation)
*   **Purpose:** Manages user accounts, including creation, updates, deletion, role assignments, and password management. Also handles user login, which overlaps with `IAuthenticationService` but is often found in user services as well.
*   **Implementation:** `UserAccountServiceImpl`
*   **Dependencies:** `IUserAccountDAO`, `IRoleDAO`, `IUserRoleAssignmentDAO`, `INotificationService`.

*   **Key Methods (Illustrative - check interface for complete list):**

    *   `UserAccount createUser(UserAccount userAccount, Set<String> assignRoleIds, String adminId) throws SQLException, ValidationException, AuthorizationException`
        *   **Description:** Creates a new user account. Can assign roles during creation. Requires admin privileges.
        *   **Parameters:**
            *   `userAccount`: The `UserAccount` object with details for the new user (e.g., username, hashed password, email, etc.).
            *   `assignRoleIds`: A set of role IDs to assign to the new user.
            *   `adminId`: The ID of the administrator performing the action (for authorization/logging).
        *   **Returns:** The newly created `UserAccount` object.
        *   **Throws:** `SQLException`, `ValidationException` (e.g., username exists, invalid email), `AuthorizationException` (if `adminId` is not authorized).

    *   `UserAccount updateUser(UserAccount userAccount, String adminId) throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException`
        *   **Description:** Updates an existing user's account details (e.g., email, phone, address). Does not handle password changes or role changes here. Requires admin privileges.
        *   **Parameters:**
            *   `userAccount`: The `UserAccount` object with updated information.
            *   `adminId`: The ID of the administrator performing the action.
        *   **Returns:** The updated `UserAccount` object.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (user not found), `ValidationException`, `AuthorizationException`.

    *   `void deleteUser(String userIdToDelete, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException`
        *   **Description:** Deletes a user account. Requires admin privileges.
        *   **Parameters:**
            *   `userIdToDelete`: The ID of the user account to delete.
            *   `adminId`: The ID of the administrator performing the action.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `AuthorizationException`, `ValidationException` (e.g., cannot delete own admin account).

    *   `UserAccount getUserById(String userId) throws SQLException, ResourceNotFoundException`
        *   **Description:** Retrieves a user account by its ID.
        *   **Parameters:**
            *   `userId`: The ID of the user.
        *   **Returns:** The `UserAccount` object.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`.

    *   `UserAccount getUserByUsername(String username) throws SQLException, ResourceNotFoundException`
        *   **Description:** Retrieves a user account by its username.
        *   **Parameters:**
            *   `username`: The username.
        *   **Returns:** The `UserAccount` object.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`.

    *   `List<UserAccount> getAllUsers(String adminId) throws SQLException, AuthorizationException`
        *   **Description:** Retrieves a list of all user accounts. Requires admin privileges.
        *   **Parameters:**
            *   `adminId`: The ID of the administrator performing the action.
        *   **Returns:** A list of `UserAccount` objects.
        *   **Throws:** `SQLException`, `AuthorizationException`.

    *   `UserAccount blockUser(String userIdToBlock, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException`
        *   **Description:** Blocks a user account, preventing login. Requires admin privileges.
        *   **Parameters:**
            *   `userIdToBlock`: The ID of the user to block.
            *   `adminId`: The ID of the administrator.
        *   **Returns:** The updated `UserAccount` with status `BLOCKED`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `AuthorizationException`, `ValidationException`.

    *   `UserAccount unblockUser(String userIdToUnblock, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException`
        *   **Description:** Unblocks a previously blocked user account. Requires admin privileges.
        *   **Parameters:**
            *   `userIdToUnblock`: The ID of the user to unblock.
            *   `adminId`: The ID of the administrator.
        *   **Returns:** The updated `UserAccount` with status `ACTIVE`.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `AuthorizationException`.

    *   `void resetPassword(String userIdToReset, String newPassword, String adminId) throws SQLException, ResourceNotFoundException, AuthorizationException, ValidationException`
        *   **Description:** Allows an administrator to reset a user's password.
        *   **Parameters:**
            *   `userIdToReset`: The ID of the user whose password is to be reset.
            *   `newPassword`: The new plain text password (will be hashed by the service).
            *   `adminId`: The ID of the administrator.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `AuthorizationException`, `ValidationException` (e.g., password policy).

    *   `void changeOwnPassword(String userId, String oldPassword, String newPassword) throws SQLException, ResourceNotFoundException, AuthenticationException, ValidationException`
        *   **Description:** Allows a logged-in user to change their own password.
        *   **Parameters:**
            *   `userId`: The ID of the user changing their password.
            *   `oldPassword`: The user's current plain text password (for verification).
            *   `newPassword`: The new plain text password.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `AuthenticationException` (if old password doesn't match), `ValidationException` (password policy).

    *   `void assignRoleToUser(String userId, String roleId, String adminId) throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException`
        *   **Description:** Assigns a role to a user. Requires admin privileges.
        *   **Parameters:**
            *   `userId`: The ID of the user.
            *   `roleId`: The ID of the role to assign.
            *   `adminId`: The ID of the administrator.
        *   **Throws:** `SQLException`, `ResourceNotFoundException` (user or role not found), `ValidationException` (user already has role), `AuthorizationException`.

    *   `void removeRoleFromUser(String userId, String roleId, String adminId) throws SQLException, ResourceNotFoundException, ValidationException, AuthorizationException`
        *   **Description:** Removes a role from a user. Requires admin privileges.
        *   **Parameters:**
            *   `userId`: The ID of the user.
            *   `roleId`: The ID of the role to remove.
            *   `adminId`: The ID of the administrator.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`, `ValidationException` (user doesn't have role), `AuthorizationException`.

    *   `Set<Role> getUserRoles(String userId) throws SQLException, ResourceNotFoundException`
        *   **Description:** Retrieves all roles assigned to a specific user.
        *   **Parameters:**
            *   `userId`: The ID of the user.
        *   **Returns:** A set of `Role` objects.
        *   **Throws:** `SQLException`, `ResourceNotFoundException`.

    *   `List<Role> getAllRoles(String adminId) throws SQLException, AuthorizationException`
        *   **Description:** Retrieves a list of all available roles in the system. Requires admin privileges.
        *   **Parameters:**
            *   `adminId`: The ID of the administrator.
        *   **Returns:** A list of `Role` objects.
        *   **Throws:** `SQLException`, `AuthorizationException`.

    *   `UserAccount login(String username, String plainTextPassword) throws AuthenticationException, SQLException, ResourceNotFoundException`
        *   **Description:** Authenticates a user. (This method is also present in `IAuthenticationService`. Its presence here might be for convenience or if `IUserAccountService` is the primary point of interaction for user-related actions including login).
        *   **Parameters & Throws:** Same as `IAuthenticationService.login()`.

    *   (The original document mentioned `UserAccount getUserAccount(User user)`, `void updateUserAccount(User user, UserAccountDetails details)`, `List<DeliveryInfo> getSavedDeliveryAddresses(User user)`, `void addDeliveryAddress(User user, DeliveryInfo address)`. These seem to be more specific to a customer's own account management and might be part of a different, more focused service or handled directly by UI controllers interacting with DAOs if simple enough. The current `IUserAccountService` seems more admin/system-focused, plus basic user retrieval and self-password change.)

### 2.8. `INotificationService`

*   **Package:** `com.aims.core.application.services`

*   **Description:**
This service is responsible for handling all notifications sent to users. This primarily involves sending emails for various events such as account status changes, password resets, order confirmations, and order status updates.

*   **Implementation:**
    *   `com.aims.core.application.impl.NotificationServiceImpl`

*   **Key Dependencies:**
    *   `com.aims.core.infrastructure.adapters.external.email.IEmailSenderAdapter`: Used to send emails.

*   **Methods:**

    1.  **`sendUserStatusChangeNotification(UserAccount userAccount, String oldStatus, String newStatus, String adminNotes)`**
        *   **Description:** Sends a notification to a user when their account status changes (e.g., blocked, unblocked).
        *   **Parameters:**
            *   `userAccount` (UserAccount): The user account whose status has changed.
            *   `oldStatus` (String): The previous status of the user account.
            *   `newStatus` (String): The new status of the user account.
            *   `adminNotes` (String): Optional notes from the administrator regarding the change.
        *   **Returns:** `void`
        *   **Exceptions:** None explicitly declared in the interface. Implementation may log errors if email sending fails.

    2.  **`sendPasswordResetNotification(UserAccount userAccount, String temporaryPassword)`**
        *   **Description:** Sends a password reset notification to the user, potentially containing a temporary password.
        *   **Parameters:**
            *   `userAccount` (UserAccount): The user account for whom the password was reset.
            *   `temporaryPassword` (String): The new temporary password (if applicable).
        *   **Returns:** `void`
        *   **Exceptions:** None explicitly declared in the interface. Implementation may log errors if email sending fails.

    3.  **`sendPasswordChangedNotification(UserAccount userAccount)`**
        *   **Description:** Sends a notification confirming a successful password change by the user.
        *   **Parameters:**
            *   `userAccount` (UserAccount): The user account whose password was changed.
        *   **Returns:** `void`
        *   **Exceptions:** None explicitly declared in the interface. Implementation may log errors if email sending fails.

    4.  **`sendOrderConfirmationEmail(OrderEntity orderEntity, Invoice invoice, PaymentTransaction paymentTransaction)`**
        *   **Description:** Sends an order confirmation email to the customer after a successful order placement and payment. This email includes invoice details and payment transaction information.
        *   **Parameters:**
            *   `orderEntity` (OrderEntity): The successfully placed order.
            *   `invoice` (Invoice): The invoice associated with the order.
            *   `paymentTransaction` (PaymentTransaction): The successful payment transaction details.
        *   **Returns:** `void`
        *   **Exceptions:** None explicitly declared in the interface. Implementation may log errors if email sending fails.

    5.  **`sendOrderCancellationNotification(OrderEntity orderEntity, PaymentTransaction refundTransaction)`**
        *   **Description:** Sends a notification to the customer when their order has been cancelled.
        *   **Parameters:**
            *   `orderEntity` (OrderEntity): The order that has been cancelled.
            *   `refundTransaction` (PaymentTransaction): Optional. The payment transaction details if a refund was processed.
        *   **Returns:** `void`
        *   **Exceptions:** None explicitly declared in the interface. Implementation may log errors if email sending fails.

    6.  **`sendOrderStatusUpdateNotification(OrderEntity orderEntity, String oldStatus, String newStatus, String notes)`**
        *   **Description:** Sends a notification to the customer when their order status has been updated (e.g., approved, rejected, shipped, delivered).
        *   **Parameters:**
            *   `orderEntity` (OrderEntity): The order whose status has changed.
            *   `oldStatus` (String): The previous status of the order.
            *   `newStatus` (String): The new status of the order.
            *   `notes` (String): Optional notes regarding the status change (e.g., rejection reason, tracking number).
        *   **Returns:** `void`
        *   **Exceptions:** None explicitly declared in the interface. Implementation may log errors if email sending fails.

---
*This is an initial draft. Method signatures and details are illustrative and should be verified against the actual codebase. Consider using Javadoc for detailed API documentation within the code itself.*

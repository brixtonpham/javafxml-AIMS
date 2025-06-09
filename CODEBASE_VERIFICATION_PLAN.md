# AIMS Codebase Verification Plan
## Ensuring All UI Test Cases Have Proper Implementation

### Overview
This document provides a comprehensive plan to verify that the existing AIMS codebase supports all 50+ test cases defined in the UI Testing Plan. We will systematically check each controller, service, and UI component to ensure they have the necessary functionality.

---

## 📋 **Verification Strategy**

### **Phase 1: Core Infrastructure Verification**
Verify that the foundational components support all test scenarios.

### **Phase 2: Controller-by-Controller Analysis**
Check each controller against its corresponding test cases.

### **Phase 3: Service Layer Validation**
Ensure all services properly implement required business logic.

### **Phase 4: Integration Point Verification**
Validate that UI components properly integrate with services.

### **Phase 5: Gap Analysis & Remediation**
Identify missing functionality and create implementation plan.

---

## 🔍 **Phase 1: Core Infrastructure Verification**

### **1.1 Database Schema Validation**
**Target File:** `src/main/java/com/aims/core/infrastructure/database/scripts/V1__create_tables.sql`

**Requirements from Test Cases:**
- Product search and filtering (HS_TC02-TC05)
- Stock management (HS_TC09, PD_TC02, CS_TC03-TC05)
- Cart operations (CS_TC01-TC10)
- Order processing (DI_TC01-TC11, OS_TC01-TC03)
- Payment tracking (PM_TC01-TC04, PP_TC01-TC05, PR_TC01-TC04)
- Rush delivery eligibility (DI_TC04-TC06)

**Verification Points:**
- [ ] `PRODUCT` table supports search functionality (title, category fields)
- [ ] `quantityInStock` field properly tracks inventory
- [ ] `CART` and `CART_ITEM` tables support session-based shopping
- [ ] `ORDER_ENTITY` supports status tracking and delivery info
- [ ] `ORDER_ITEM` has `isEligibleForRushDelivery` field
- [ ] Payment transaction tracking is complete

### **1.2 Entity Model Validation**
**Target Files:** 
- `src/main/java/com/aims/core/entities/Product.java`
- `src/main/java/com/aims/core/entities/Book.java`
- `src/main/java/com/aims/core/entities/CD.java` 
- `src/main/java/com/aims/core/entities/DVD.java`

**Requirements from Test Cases:**
- Product-specific attributes display (PD_TC01)
- Stock validation (PD_TC02, CS_TC03-TC05)
- Price calculations with VAT (all test cases)

**Verification Points:**
- [ ] Product entities have all required fields for display
- [ ] Book/CD/DVD specific attributes are properly modeled
- [ ] Stock tracking methods are implemented
- [ ] Price calculation methods include VAT

---

## 🎯 **Phase 2: Controller-by-Controller Analysis**

### **2.1 HomeScreenController Analysis**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/HomeScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/home_screen.fxml`

**Test Cases to Support:** HS_TC01 - HS_TC09

#### **Required Functionality Checklist:**

**HS_TC01: Initial Product Display**
- [ ] Loads exactly 20 products on first page
- [ ] Displays product cards with: image, title, price (with VAT), stock status
- [ ] Shows pagination controls with correct page numbers
- [ ] Initializes category filter to "All Categories"
- [ ] Initializes sort to "Default Sort"
- [ ] Disables "Previous" button on first page

**HS_TC02-TC03: Search Functionality**
- [ ] Search field accepts text input
- [ ] Search button triggers product filtering
- [ ] Enter key activates search
- [ ] Results display with "Search Results for: X" header
- [ ] Shows "No products found" for empty results
- [ ] Maintains filter/sort options during search

**HS_TC04: Category Filtering**
- [ ] Category ComboBox populated with all available categories
- [ ] Selecting category filters products correctly
- [ ] Updates pagination based on filtered results
- [ ] Maintains search term during category filtering

**HS_TC05: Price Sorting**
- [ ] Sort ComboBox has "Price: Low to High" option
- [ ] Sort ComboBox has "Price: High to Low" option
- [ ] Sorting maintains current search/filter state
- [ ] Products reorder correctly by price

**HS_TC06: Pagination**
- [ ] "Next" button advances to next page
- [ ] "Previous" button goes to previous page
- [ ] Page indicator shows "Page X/Y" correctly
- [ ] Navigation buttons disabled at boundaries
- [ ] Page size consistent at 20 products

**HS_TC07: Product Navigation**
- [ ] Product card click navigates to product detail
- [ ] Product ID properly passed to detail screen
- [ ] Navigation maintains current page context

**HS_TC08-TC09: Cart Operations**
- [ ] "Add to Cart" button enabled for in-stock products
- [ ] "Add to Cart" button disabled/hidden for out-of-stock
- [ ] Success feedback shown after adding to cart
- [ ] Stock count updates after cart addition
- [ ] Cart icon updates with item count (if visible)

#### **Code Analysis Required:**
```java
// Check these methods exist and work correctly:
- loadProducts()
- handleSearch()
- handleCategoryFilter()
- handlePriceSort()
- handlePagination()
- navigateToProductDetail()
- addToCartFromCard()
```

### **2.2 ProductDetailScreenController Analysis**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/ProductDetailScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/product_detail_screen.fxml`

**Test Cases to Support:** PD_TC01 - PD_TC05

#### **Required Functionality Checklist:**

**PD_TC01: Product Information Display**
- [ ] Displays all basic product info: title, image, price (with VAT), category
- [ ] Shows complete product description
- [ ] Displays stock availability clearly
- [ ] Shows product-specific details based on type:
  - Book: Authors, publisher, pages, language, genre
  - CD: Artists, record label, tracklist, genre, release date
  - DVD: Director, runtime, studio, language, subtitles, genre
- [ ] Quantity spinner defaults to 1
- [ ] "Add to Cart" button enabled for in-stock products

**PD_TC02: Out of Stock Handling**
- [ ] Shows "Out of Stock" message clearly
- [ ] Disables quantity spinner for out-of-stock products
- [ ] Disables/hides "Add to Cart" button
- [ ] Maintains all other product information display

**PD_TC03-TC04: Add to Cart Operations**
- [ ] Quantity spinner allows selection within stock limits
- [ ] "Add to Cart" validates quantity against stock
- [ ] Success message displayed on successful addition
- [ ] Stock count updates after addition
- [ ] Error handling for insufficient stock
- [ ] Quantity validation prevents negative/zero values

**PD_TC05: Navigation**
- [ ] "Back to Products" button returns to previous screen
- [ ] Navigation preserves search/filter state
- [ ] Breadcrumb or navigation context maintained

#### **Code Analysis Required:**
```java
// Check these methods exist and work correctly:
- setProductId()
- loadProductDetails()
- populateProductData()
- populateSpecificDetails() // Book/CD/DVD specific
- addToCart()
- validateQuantity()
- handleBackNavigation()
```

### **2.3 CartScreenController Analysis**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/CartScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/cart_screen.fxml`

**Test Cases to Support:** CS_TC01 - CS_TC10

#### **Required Functionality Checklist:**

**CS_TC01: Empty Cart Display**
- [ ] Shows "Your shopping cart is currently empty" message
- [ ] Displays total as 0.00
- [ ] Disables "Proceed to Checkout" button
- [ ] Hides or disables "Clear Cart" button

**CS_TC02: Cart with Products**
- [ ] Displays all cart items with complete information
- [ ] Shows item details: image, name, unit price, quantity, total price
- [ ] Calculates and displays cart subtotal (without VAT)
- [ ] Calculates and displays VAT amount (10%)
- [ ] Shows total amount (with VAT)
- [ ] Enables "Proceed to Checkout" button

**CS_TC03: Stock Warning Display**
- [ ] Detects items with quantity > available stock
- [ ] Shows warning messages near affected items
- [ ] May disable "Proceed to Checkout" until resolved
- [ ] Highlights problematic items visually

**CS_TC04-CS_TC06: Quantity Management**
- [ ] Quantity spinner/field updates item quantities
- [ ] Validates new quantity against current stock
- [ ] Updates item total price when quantity changes
- [ ] Updates cart total when quantities change
- [ ] Setting quantity to 0 removes item from cart
- [ ] Shows error for invalid quantities

**CS_TC07: Item Removal**
- [ ] "Remove" button available for each item
- [ ] Confirmation dialog for item removal (optional)
- [ ] Item removed from cart completely
- [ ] Cart totals updated after removal

**CS_TC08: Clear Cart**
- [ ] "Clear Cart" button available when cart has items
- [ ] Confirmation dialog for clear operation
- [ ] All items removed from cart
- [ ] Cart displays empty state after clearing

**CS_TC09-CS_TC10: Checkout Process**
- [ ] "Proceed to Checkout" enabled for valid carts
- [ ] Stock validation before proceeding
- [ ] Navigation to delivery info screen
- [ ] Cart data preserved during checkout process

#### **Code Analysis Required:**
```java
// Check these methods exist and work correctly:
- loadCartDetails()
- displayEmptyCart()
- validateStock()
- updateQuantity()
- removeItem()
- clearCart()
- proceedToCheckout()
- calculateTotals()
```

### **2.4 DeliveryInfoScreenController Analysis**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/DeliveryInfoScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/delivery_info_screen.fxml`

**Test Cases to Support:** DI_TC01 - DI_TC11

#### **Required Functionality Checklist:**

**DI_TC01: Valid Delivery Info - Standard Delivery**
- [ ] Form fields for: recipient name, email, phone, address, province
- [ ] Validation for required fields
- [ ] Order summary display with all products
- [ ] Shipping fee calculation and display
- [ ] Total amount calculation (products + VAT + shipping)
- [ ] Navigation to order summary on success

**DI_TC02-TC03: Form Validation**
- [ ] Required field validation (name, phone, address, province)
- [ ] Email format validation
- [ ] Phone number format validation
- [ ] Error messages display for invalid inputs
- [ ] Prevents proceeding with invalid data

**DI_TC04-TC06: Rush Delivery Logic**
- [ ] "Request Rush Order Delivery" checkbox
- [ ] Rush delivery options appear when checked
- [ ] Address eligibility validation for rush delivery
- [ ] Product eligibility validation for rush delivery
- [ ] Rush delivery time selection (if applicable)
- [ ] Additional rush delivery fee calculation

**DI_TC07: Dynamic Fee Calculation**
- [ ] Shipping fee recalculates when address changes
- [ ] Fee varies based on destination province/district
- [ ] Real-time updates during form completion

**DI_TC08-TC09: Free Shipping Logic**
- [ ] Detects when order value exceeds free shipping threshold (100,000 VND)
- [ ] Applies free shipping discount (up to 25,000 VND)
- [ ] Free shipping discount displayed clearly
- [ ] Rush delivery excludes free shipping benefits
- [ ] Mixed orders (rush + standard) calculated correctly

**DI_TC10-TC11: Navigation**
- [ ] "Back to Cart" button returns to cart screen
- [ ] "Proceed to Payment" advances to order summary
- [ ] Form data preserved during navigation

#### **Code Analysis Required:**
```java
// Check these methods exist and work correctly:
- populateOrderSummary()
- validateForm()
- calculateShippingFee()
- handleRushDeliveryToggle()
- validateRushEligibility()
- applyFreeShippingDiscount()
- proceedToOrderSummary()
```

### **2.5 OrderSummaryController Analysis**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/OrderSummaryController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/order_summary_screen.fxml`

**Test Cases to Support:** OS_TC01 - OS_TC03

#### **Required Functionality Checklist:**

**OS_TC01: Complete Order Information Display**
- [ ] Order ID and date displayed
- [ ] Customer delivery information shown completely
- [ ] All order items listed with details
- [ ] Payment summary breakdown:
  - Subtotal (excluding VAT)
  - VAT amount (10%)
  - Total product price (including VAT)  
  - Shipping fee
  - Total amount to be paid
- [ ] Rush delivery details (if applicable)

**OS_TC02-TC03: Navigation**
- [ ] "Back to Delivery Info" returns to previous screen
- [ ] "Proceed to Select Payment Method" advances to payment
- [ ] Order data preserved between screens

#### **Code Analysis Required:**
```java
// Check these methods exist and work correctly:
- setOrderData()
- populateOrderSummary()
- calculatePaymentBreakdown()
- navigateToDeliveryInfo()
- proceedToPaymentMethod()
```

### **2.6 Payment Flow Controllers Analysis**

#### **2.6.1 PaymentMethodScreenController**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/PaymentMethodScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/payment_method_screen.fxml`

**Test Cases to Support:** PM_TC01 - PM_TC04

**Required Functionality:**
- [ ] "Credit Card / Debit Card (via VNPay)" pre-selected
- [ ] Payment method descriptions displayed
- [ ] "Proceed with VNPay" button functionality
- [ ] Navigation back to order summary

#### **2.6.2 PaymentProcessingScreenController**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/PaymentProcessingScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/payment_processing_screen.fxml`

**Test Cases to Support:** PP_TC01 - PP_TC05

**Required Functionality:**
- [ ] Payment URL redirection or WebView display
- [ ] "Check Payment Status Manually" button
- [ ] Payment status verification via service calls
- [ ] "Cancel Payment Attempt" functionality
- [ ] Progress indicators and user feedback

#### **2.6.3 PaymentResultScreenController**
**Target File:** `src/main/java/com/aims/core/presentation/controllers/PaymentResultScreenController.java`
**FXML File:** `src/main/resources/com/aims/presentation/views/payment_result_screen.fxml`

**Test Cases to Support:** PR_TC01 - PR_TC04

**Required Functionality:**
- [ ] Success/failure result display
- [ ] Transaction details display
- [ ] Order details display
- [ ] "View Order Details" navigation
- [ ] "Continue Shopping" navigation

---

## 🔧 **Phase 3: Service Layer Validation**

### **3.1 IProductService Implementation**
**Target File:** `src/main/java/com/aims/core/application/impl/ProductServiceImpl.java`

**Required Methods for Test Cases:**
```java
// HS_TC01-TC09: Home screen functionality
- searchProducts(String searchTerm, String category, String sortBy, int page, int pageSize)
- getProductsByCategory(String category, int page, int pageSize)
- getAllProducts(int page, int pageSize)
- getProductById(String productId)
- getTotalProductCount(String searchTerm, String category)

// PD_TC01-TC05: Product detail functionality  
- getProductDetails(String productId)
- isProductInStock(String productId)
- getAvailableStock(String productId)
```

### **3.2 ICartService Implementation**
**Target File:** `src/main/java/com/aims/core/application/impl/CartServiceImpl.java`

**Required Methods for Test Cases:**
```java
// HS_TC08, PD_TC03-TC04: Add to cart functionality
- addItemToCart(String cartSessionId, String productId, int quantity)

// CS_TC01-CS_10: Cart management
- getCartItems(String cartSessionId)
- updateCartItemQuantity(String cartSessionId, String productId, int newQuantity)
- removeItemFromCart(String cartSessionId, String productId)
- clearCart(String cartSessionId)
- validateCartStock(String cartSessionId)
- calculateCartTotal(String cartSessionId)
- getCartItemCount(String cartSessionId)
```

### **3.3 IOrderService Implementation**
**Target File:** `src/main/java/com/aims/core/application/impl/OrderServiceImpl.java`

**Required Methods for Test Cases:**
```java
// DI_TC01-TC11: Order creation and management
- createOrderFromCart(String cartSessionId, DeliveryInfo deliveryInfo)
- calculateOrderTotal(OrderEntity order)
- validateOrderStock(OrderEntity order)
- updateOrderStatus(String orderId, OrderStatus status)

// OS_TC01-TC03: Order summary
- getOrderById(String orderId)
- getOrderSummary(String orderId)
```

### **3.4 IDeliveryCalculationService Implementation**
**Target File:** `src/main/java/com/aims/core/application/impl/DeliveryCalculationServiceImpl.java`

**Required Methods for Test Cases:**
```java
// DI_TC01, DI_TC07-TC09: Shipping calculation
- calculateStandardShippingFee(DeliveryInfo deliveryInfo, List<OrderItem> items)
- calculateRushDeliveryFee(DeliveryInfo deliveryInfo, List<OrderItem> rushItems)
- isEligibleForRushDelivery(String address, Product product)
- applyFreeShippingDiscount(double standardShippingFee, double orderValue)
- validateRushDeliveryAddress(String province, String address)
```

### **3.5 IPaymentService Implementation**
**Target File:** `src/main/java/com/aims/core/application/impl/PaymentServiceImpl.java`

**Required Methods for Test Cases:**
```java
// PM_TC01-TC04, PP_TC01-TC05, PR_TC01-TC04: Payment processing
- initiatePayment(OrderEntity order, PaymentMethod paymentMethod)
- checkPaymentStatus(String transactionId)
- processPaymentCallback(Map<String, String> callbackData)
- cancelPayment(String transactionId)
- getPaymentResult(String transactionId)
```

---

## ⚠️ **Phase 4: Integration Point Verification**

### **4.1 Service Injection Verification**
**Target File:** `src/main/java/com/aims/core/shared/ServiceFactory.java`

**Verification Points:**
- [ ] All services properly instantiated
- [ ] Dependency injection working correctly
- [ ] Controllers receive correct service instances
- [ ] No null pointer exceptions during service calls

### **4.2 FXML-Controller Binding**
**Target Files:** All FXML files in `src/main/resources/com/aims/presentation/views/`

**Verification Points:**
- [ ] All UI controls have proper fx:id attributes
- [ ] Controller methods bound to UI events (@FXML annotations)
- [ ] UI controls accessible from controller code
- [ ] No missing FXML bindings

### **4.3 Navigation Flow Verification**
**Target File:** `src/main/java/com/aims/core/presentation/utils/FXMLSceneManager.java`

**Verification Points:**
- [ ] All screen transitions work correctly
- [ ] Data properly passed between screens
- [ ] Back navigation preserves state
- [ ] Error handling for navigation failures

---

## 📊 **Phase 5: Gap Analysis & Remediation Plan**

### **5.1 Missing Functionality Identification**
Create a systematic checklist of all required functionality and mark what's missing.

### **5.2 Priority Classification**
- **Critical**: Functionality required for core user flows
- **High**: Functionality affecting user experience
- **Medium**: Nice-to-have features
- **Low**: Edge cases and advanced features

### **5.3 Implementation Plan**
For each missing piece:
1. **Location**: Which file(s) need modification
2. **Scope**: How much code needs to be added/changed
3. **Dependencies**: What other components are affected
4. **Testing**: How to verify the fix works

---

## 🎯 **Verification Execution Plan**

### **Week 1: Infrastructure & Core Controllers**
- **Day 1**: Database schema and entity validation
- **Day 2**: HomeScreenController comprehensive analysis
- **Day 3**: ProductDetailScreenController analysis
- **Day 4**: CartScreenController analysis
- **Day 5**: Service layer basic validation

### **Week 2: Order Flow & Payment**
- **Day 1**: DeliveryInfoScreenController analysis
- **Day 2**: OrderSummaryController analysis  
- **Day 3**: Payment flow controllers analysis
- **Day 4**: Service layer comprehensive validation
- **Day 5**: Integration point verification

### **Week 3: Gap Analysis & Remediation**
- **Day 1-2**: Complete gap analysis and documentation
- **Day 3-5**: High-priority missing functionality implementation

---

## 📝 **Verification Documentation Template**

For each controller/service, document:

```markdown
## [Component Name] Verification Report

### Test Cases Supported: [List test case IDs]

### ✅ Working Functionality:
- [ ] Feature 1: Implementation status
- [ ] Feature 2: Implementation status

### ❌ Missing Functionality:
- [ ] Feature X: Not implemented
- [ ] Feature Y: Partially implemented

### 🐛 Issues Found:
- Issue 1: Description and severity
- Issue 2: Description and severity

### 🔧 Recommended Actions:
1. Priority 1: Critical fix needed
2. Priority 2: Enhancement required
3. Priority 3: Nice to have

### 📋 Test Results:
- Manual testing results
- Code review findings
- Integration test results
```

---

## 🚀 **Next Steps**

1. **Execute Phase 1**: Start with infrastructure verification
2. **Create verification scripts**: Automated checks where possible
3. **Document findings**: Use the template above for each component
4. **Prioritize gaps**: Focus on critical missing functionality first
5. **Implement fixes**: Address high-priority issues immediately

This comprehensive verification plan ensures that every test case in the UI Testing Plan has proper implementation support in the current codebase, and identifies exactly what needs to be built or fixed to achieve full functionality.